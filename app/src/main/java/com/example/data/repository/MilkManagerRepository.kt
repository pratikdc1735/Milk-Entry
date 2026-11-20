package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.BillAuditEntity
import com.example.data.local.BillLineItem
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerSalesRow
import com.example.data.local.CustomerWithBalance
import com.example.data.local.DailyEntryWithDetails
import com.example.data.local.DailyMilkEntryEntity
import com.example.data.local.DashboardSummary
import com.example.data.local.LedgerEntryType
import com.example.data.local.LedgerTransaction
import com.example.data.local.MilkProductEntity
import com.example.data.local.MonthlyBillEntity
import com.example.data.local.MonthlyReportData
import com.example.data.local.PaymentEntity
import com.example.data.local.ProductSalesRow
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

sealed class OperationResult<out T> {
    data class Success<T>(val data: T, val message: String = "") : OperationResult<T>()
    data class DuplicateWarning(val existingId: Long, val message: String) : OperationResult<Nothing>()
    data class Error(val message: String) : OperationResult<Nothing>()
}

class MilkManagerRepository(private val db: AppDatabase) {

    private val productDao = db.milkProductDao()
    private val customerDao = db.customerDao()
    private val entryDao = db.dailyMilkEntryDao()
    private val paymentDao = db.paymentDao()
    private val billDao = db.monthlyBillDao()

    val productsFlow: Flow<List<MilkProductEntity>> = productDao.observeAll()
    val activeProductsFlow: Flow<List<MilkProductEntity>> = productDao.observeActive()
    val customersRawFlow: Flow<List<CustomerEntity>> = customerDao.observeAll()
    val allEntriesRawFlow: Flow<List<DailyMilkEntryEntity>> = entryDao.observeAll()
    val allPaymentsFlow: Flow<List<PaymentEntity>> = paymentDao.observeAll()
    val allBillsFlow: Flow<List<MonthlyBillEntity>> = billDao.observeAll()

    val entriesWithDetailsFlow: Flow<List<DailyEntryWithDetails>> = combine(
        entryDao.observeAll(),
        customerDao.observeAll()
    ) { entries, customers ->
        val customerMap = customers.associateBy { it.id }
        entries.map { entry ->
            val customer = customerMap[entry.customerId]
            DailyEntryWithDetails(
                entry = entry,
                customerName = customer?.name ?: "Unknown Customer",
                customerCode = customer?.customerCode ?: "-",
                customerMobile = customer?.mobileNumber ?: ""
            )
        }
    }

    val customersWithBalanceFlow: Flow<List<CustomerWithBalance>> = combine(
        customerDao.observeAll(),
        productDao.observeAll(),
        entryDao.observeAll(),
        paymentDao.observeAll()
    ) { customers, products, entries, payments ->
        val productMap = products.associateBy { it.id }
        val entriesByCustomer = entries.groupBy { it.customerId }
        val paymentsByCustomer = payments.groupBy { it.customerId }
        val monthStart = DateUtils.monthStartIso(DateUtils.currentYear(), DateUtils.currentMonth())
        val monthEnd = DateUtils.monthEndIso(DateUtils.currentYear(), DateUtils.currentMonth())

        customers.map { customer ->
            val custEntries = entriesByCustomer[customer.id].orEmpty()
            val custPayments = paymentsByCustomer[customer.id].orEmpty()
            val totalMilk = custEntries.sumOf { it.totalAmount }
            val totalPaid = custPayments.sumOf { it.amount }
            val outstanding = round2(customer.openingBalance + totalMilk - totalPaid)
            val thisMonthEntries = custEntries.filter { it.entryDate in monthStart..monthEnd }
            val product = customer.defaultProductId?.let { productMap[it] } ?: products.firstOrNull { it.isActive }
            val effectiveRate = customer.customPacketRate ?: product?.pricePerPacket ?: 30.0
            CustomerWithBalance(
                customer = customer,
                defaultProductName = product?.name,
                effectiveRate = effectiveRate,
                totalMilkCharges = round2(totalMilk),
                totalPayments = round2(totalPaid),
                currentOutstanding = outstanding,
                thisMonthPackets = thisMonthEntries.sumOf { it.packetQuantity },
                thisMonthAmount = round2(thisMonthEntries.sumOf { it.totalAmount })
            )
        }
    }

    val dashboardSummaryFlow: Flow<DashboardSummary> = combine(
        customersWithBalanceFlow,
        entriesWithDetailsFlow,
        paymentDao.observeAll()
    ) { customers, entries, payments ->
        val today = DateUtils.todayIso()
        val year = DateUtils.currentYear()
        val month = DateUtils.currentMonth()
        val monthStart = DateUtils.monthStartIso(year, month)
        val monthEnd = DateUtils.monthEndIso(year, month)

        val todayEntries = entries.filter { it.entry.entryDate == today }
        val monthEntries = entries.filter { it.entry.entryDate in monthStart..monthEnd }
        val monthPayments = payments.filter { it.paymentDate in monthStart..monthEnd }

        val activeCount = customers.count { it.customer.isActive }
        val todayPackets = todayEntries.sumOf { it.entry.packetQuantity }
        val todaySales = round2(todayEntries.sumOf { it.entry.totalAmount })
        val totalPending = round2(customers.sumOf { it.currentOutstanding.coerceAtLeast(0.0) })
        val monthSales = round2(monthEntries.sumOf { it.entry.totalAmount })
        val monthCollected = round2(monthPayments.sumOf { it.amount })
        val monthOutstanding = round2((monthSales - monthCollected).coerceAtLeast(0.0))

        DashboardSummary(
            todayDateIso = today,
            activeCustomersCount = activeCount,
            todayTotalPackets = todayPackets,
            todayTotalSales = todaySales,
            totalPendingPayments = totalPending,
            thisMonthTotalSales = monthSales,
            thisMonthCollected = monthCollected,
            thisMonthOutstanding = monthOutstanding,
            hasSampleData = customers.any { it.customer.isSampleData },
            recentEntries = entries.take(8)
        )
    }

    suspend fun ensureDefaultProductsSeeded() {
        AppDatabase.seedDefaultProducts(productDao)
    }

    // --- Milk Products ---

    suspend fun saveProduct(product: MilkProductEntity): OperationResult<Long> {
        if (product.name.isBlank()) return OperationResult.Error("Product name is required")
        if (product.pricePerPacket <= 0.0) return OperationResult.Error("Selling price must be greater than zero")
        if (product.packetSizeMl <= 0) return OperationResult.Error("Packet size (ml) must be greater than zero")

        return try {
            val id = if (product.id == 0L) {
                productDao.insert(product.copy(name = product.name.trim(), description = product.description.trim()))
            } else {
                productDao.update(product.copy(name = product.name.trim(), description = product.description.trim()))
                product.id
            }
            OperationResult.Success(id, "Product saved")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to save product")
        }
    }

    suspend fun deleteProduct(product: MilkProductEntity): OperationResult<Unit> {
        return try {
            productDao.delete(product)
            OperationResult.Success(Unit, "Product deleted")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Unable to delete product")
        }
    }

    // --- Customers ---

    suspend fun saveCustomer(
        id: Long = 0L,
        name: String,
        mobileNumber: String,
        whatsappNumber: String,
        address: String,
        defaultProductId: Long?,
        customPacketRate: Double?,
        isActive: Boolean,
        openingBalance: Double,
        notes: String
    ): OperationResult<Long> {
        val cleanName = name.trim()
        val cleanMobile = mobileNumber.trim()
        val cleanWhatsApp = whatsappNumber.trim().ifBlank { cleanMobile }

        if (cleanName.isBlank()) return OperationResult.Error("Customer name is required")
        if (cleanMobile.isBlank()) return OperationResult.Error("Mobile number is required")
        if (customPacketRate != null && customPacketRate <= 0.0) {
            return OperationResult.Error("Packet rate must be greater than zero")
        }

        return db.withTransaction {
            val existingByMobile = customerDao.findByMobile(cleanMobile)
            if (existingByMobile != null && existingByMobile.id != id) {
                return@withTransaction OperationResult.DuplicateWarning(
                    existingByMobile.id,
                    "A customer (${existingByMobile.name}) already exists with mobile $cleanMobile"
                )
            }

            if (id == 0L) {
                val nextNum = (customerDao.getMaxId() ?: 0L) + 1L
                val code = String.format(Locale.ENGLISH, "SSM-%03d", nextNum)
                val newId = customerDao.insert(
                    CustomerEntity(
                        customerCode = code,
                        name = cleanName,
                        mobileNumber = cleanMobile,
                        whatsappNumber = cleanWhatsApp,
                        address = address.trim(),
                        defaultProductId = defaultProductId,
                        customPacketRate = customPacketRate,
                        isActive = isActive,
                        openingBalance = round2(openingBalance),
                        notes = notes.trim()
                    )
                )
                OperationResult.Success(newId, "Customer added ($code)")
            } else {
                val current = customerDao.getById(id)
                    ?: return@withTransaction OperationResult.Error("Customer not found")
                customerDao.update(
                    current.copy(
                        name = cleanName,
                        mobileNumber = cleanMobile,
                        whatsappNumber = cleanWhatsApp,
                        address = address.trim(),
                        defaultProductId = defaultProductId,
                        customPacketRate = customPacketRate,
                        isActive = isActive,
                        openingBalance = round2(openingBalance),
                        notes = notes.trim()
                    )
                )
                OperationResult.Success(id, "Customer updated")
            }
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity): OperationResult<Unit> {
        return try {
            db.withTransaction {
                customerDao.delete(customer)
            }
            OperationResult.Success(Unit, "Customer deleted")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to delete customer")
        }
    }

    fun observeCustomer(customerId: Long): Flow<CustomerEntity?> = customerDao.observeById(customerId)

    fun observeCustomerEntries(customerId: Long): Flow<List<DailyMilkEntryEntity>> =
        entryDao.observeByCustomer(customerId)

    fun observeCustomerPayments(customerId: Long): Flow<List<PaymentEntity>> =
        paymentDao.observeByCustomer(customerId)

    fun observeCustomerBills(customerId: Long): Flow<List<MonthlyBillEntity>> =
        billDao.observeByCustomer(customerId)

    fun observeCustomerLedger(customerId: Long): Flow<List<LedgerTransaction>> = combine(
        customerDao.observeById(customerId),
        entryDao.observeByCustomer(customerId),
        paymentDao.observeByCustomer(customerId)
    ) { customer, entries, payments ->
        if (customer == null) return@combine emptyList()

        data class RawEvent(
            val date: String,
            val orderPriority: Int,
            val createdAt: Long,
            val id: String,
            val type: LedgerEntryType,
            val title: String,
            val subtitle: String,
            val debit: Double,
            val credit: Double,
            val sourceId: Long
        )

        val events = mutableListOf<RawEvent>()
        entries.forEach { e ->
            events.add(
                RawEvent(
                    date = e.entryDate,
                    orderPriority = 1,
                    createdAt = e.createdAt,
                    id = "milk_${e.id}",
                    type = LedgerEntryType.MILK_DEBIT,
                    title = "${e.productNameSnapshot} (${e.packetQuantity} pkt)",
                    subtitle = "${e.packetQuantity} × ₹${formatShortRate(e.pricePerPacket)}" +
                        if (e.remarks.isNotBlank()) " • ${e.remarks}" else "",
                    debit = e.totalAmount,
                    credit = 0.0,
                    sourceId = e.id
                )
            )
        }
        payments.forEach { p ->
            events.add(
                RawEvent(
                    date = p.paymentDate,
                    orderPriority = 2,
                    createdAt = p.createdAt,
                    id = "pay_${p.id}",
                    type = LedgerEntryType.PAYMENT_CREDIT,
                    title = "Payment (${formatPaymentMethod(p.paymentMethod)})",
                    subtitle = listOfNotNull(
                        p.transactionRef.takeIf { it.isNotBlank() }?.let { "Ref: $it" },
                        p.notes.takeIf { it.isNotBlank() }
                    ).joinToString(" • ").ifBlank { "Recorded payment" },
                    debit = 0.0,
                    credit = p.amount,
                    sourceId = p.id
                )
            )
        }

        val sorted = events.sortedWith(compareBy<RawEvent> { it.date }.thenBy { it.orderPriority }.thenBy { it.createdAt })
        val result = mutableListOf<LedgerTransaction>()
        var running = round2(customer.openingBalance)

        if (abs(customer.openingBalance) > 0.004) {
            result.add(
                LedgerTransaction(
                    id = "opening_${customer.id}",
                    date = DateUtils.formatIsoFromMillis(customer.createdAt),
                    type = LedgerEntryType.OPENING,
                    title = "Opening Balance",
                    subtitle = "Brought forward balance",
                    debit = if (customer.openingBalance > 0) customer.openingBalance else 0.0,
                    credit = if (customer.openingBalance < 0) abs(customer.openingBalance) else 0.0,
                    runningBalance = running
                )
            )
        }

        sorted.forEach { ev ->
            running = round2(running + ev.debit - ev.credit)
            result.add(
                LedgerTransaction(
                    id = ev.id,
                    date = ev.date,
                    type = ev.type,
                    title = ev.title,
                    subtitle = ev.subtitle,
                    debit = ev.debit,
                    credit = ev.credit,
                    runningBalance = running,
                    sourceId = ev.sourceId
                )
            )
        }

        result.reversed() // Most recent at top with accurate running balance
    }

    // --- Daily Milk Entries ---

    suspend fun getExistingEntriesForCustomerOnDate(
        customerId: Long,
        dateIso: String
    ): List<DailyMilkEntryEntity> = entryDao.getForCustomerAndDate(customerId, dateIso)

    suspend fun getDailyEntryById(entryId: Long): DailyMilkEntryEntity? = entryDao.getById(entryId)

    suspend fun saveDailyMilkEntry(
        entryId: Long = 0L,
        entryDate: String,
        customerId: Long,
        productId: Long,
        packetQuantity: Int,
        pricePerPacket: Double,
        remarks: String
    ): OperationResult<Long> {
        if (!DateUtils.isValidIsoDate(entryDate)) return OperationResult.Error("Please select a valid date")
        if (customerId <= 0L) return OperationResult.Error("Please select a customer")
        if (productId <= 0L) return OperationResult.Error("Please select a milk product")
        if (packetQuantity <= 0) return OperationResult.Error("Packet quantity must be at least 1")
        if (pricePerPacket <= 0.0) return OperationResult.Error("Price per packet must be greater than zero")

        return db.withTransaction {
            val product = productDao.getById(productId)
                ?: return@withTransaction OperationResult.Error("Selected milk product not found")

            val duplicate = entryDao.findDuplicate(customerId, entryDate, productId)
            if (duplicate != null && duplicate.id != entryId) {
                return@withTransaction OperationResult.DuplicateWarning(
                    existingId = duplicate.id,
                    message = "An entry for ${product.name} already exists on ${DateUtils.formatDisplayDate(entryDate)} (${duplicate.packetQuantity} packets). Edit the existing entry or select another product."
                )
            }

            val total = round2(packetQuantity * pricePerPacket)
            val now = System.currentTimeMillis()

            if (entryId == 0L) {
                val newId = entryDao.insert(
                    DailyMilkEntryEntity(
                        entryDate = entryDate,
                        customerId = customerId,
                        productId = product.id,
                        productNameSnapshot = product.name,
                        packetSizeMlSnapshot = product.packetSizeMl,
                        packetQuantity = packetQuantity,
                        pricePerPacket = round2(pricePerPacket),
                        totalAmount = total,
                        remarks = remarks.trim(),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                OperationResult.Success(newId, "Milk entry saved")
            } else {
                val existing = entryDao.getById(entryId)
                    ?: return@withTransaction OperationResult.Error("Entry not found")
                entryDao.update(
                    existing.copy(
                        entryDate = entryDate,
                        customerId = customerId,
                        productId = product.id,
                        productNameSnapshot = product.name,
                        packetSizeMlSnapshot = product.packetSizeMl,
                        packetQuantity = packetQuantity,
                        pricePerPacket = round2(pricePerPacket),
                        totalAmount = total,
                        remarks = remarks.trim(),
                        updatedAt = now
                    )
                )
                OperationResult.Success(entryId, "Milk entry updated")
            }
        }
    }

    suspend fun deleteDailyEntry(entryId: Long): OperationResult<Unit> {
        return try {
            db.withTransaction { entryDao.deleteById(entryId) }
            OperationResult.Success(Unit, "Entry deleted")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to delete entry")
        }
    }

    // --- Payments ---

    suspend fun recordPayment(
        customerId: Long,
        paymentDate: String,
        amount: Double,
        paymentMethod: String,
        transactionRef: String,
        notes: String,
        allowDuplicateOverride: Boolean = false
    ): OperationResult<Long> {
        if (!DateUtils.isValidIsoDate(paymentDate)) return OperationResult.Error("Invalid payment date")
        if (customerId <= 0L) return OperationResult.Error("Please select a customer")
        if (amount <= 0.0) return OperationResult.Error("Payment amount must be greater than zero")

        val cleanRef = transactionRef.trim()
        val normalizedMethod = paymentMethod.uppercase(Locale.ENGLISH)

        return db.withTransaction {
            if (cleanRef.isNotBlank()) {
                val refMatch = paymentDao.findByTransactionRef(customerId, cleanRef)
                if (refMatch != null) {
                    return@withTransaction OperationResult.DuplicateWarning(
                        refMatch.id,
                        "A payment with reference '$cleanRef' is already recorded on ${DateUtils.formatDisplayDate(refMatch.paymentDate)}."
                    )
                }
            }

            if (!allowDuplicateOverride) {
                val fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000L
                val recentMatch = paymentDao.findRecentDuplicate(
                    customerId = customerId,
                    paymentDate = paymentDate,
                    amount = round2(amount),
                    method = normalizedMethod,
                    minCreatedAt = fiveMinutesAgo
                )
                if (recentMatch != null) {
                    return@withTransaction OperationResult.DuplicateWarning(
                        recentMatch.id,
                        "An identical payment of ₹${formatShortRate(amount)} ($normalizedMethod) was just recorded for this customer. Confirm only if this is a second separate payment."
                    )
                }
            }

            val paymentId = paymentDao.insert(
                PaymentEntity(
                    paymentDate = paymentDate,
                    customerId = customerId,
                    amount = round2(amount),
                    paymentMethod = normalizedMethod,
                    transactionRef = cleanRef,
                    notes = notes.trim(),
                    idempotencyKey = UUID.randomUUID().toString()
                )
            )
            OperationResult.Success(paymentId, "Payment recorded")
        }
    }

    suspend fun deletePayment(paymentId: Long): OperationResult<Unit> {
        return try {
            db.withTransaction { paymentDao.deleteById(paymentId) }
            OperationResult.Success(Unit, "Payment deleted")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to delete payment")
        }
    }

    // --- Monthly Billing (Immutable Snapshots + Audited Regeneration) ---

    fun observeBillsByPeriod(year: Int, month: Int): Flow<List<MonthlyBillEntity>> =
        billDao.observeByPeriod(year, month)

    fun observeBillById(billId: Long): Flow<MonthlyBillEntity?> = billDao.observeById(billId)

    fun observeAuditsForBill(billId: Long): Flow<List<BillAuditEntity>> =
        billDao.observeAuditsForBill(billId)

    suspend fun generateMonthlyBill(
        customerId: Long,
        year: Int,
        month: Int,
        forceRegenerate: Boolean = false,
        regenerationReason: String = ""
    ): OperationResult<MonthlyBillEntity> {
        return db.withTransaction {
            val customer = customerDao.getById(customerId)
                ?: return@withTransaction OperationResult.Error("Customer not found")

            val existing = billDao.getForCustomerAndPeriod(customerId, year, month)
            if (existing != null && !forceRegenerate) {
                return@withTransaction OperationResult.Success(
                    existing,
                    "Existing bill preserved (v${existing.version})"
                )
            }

            val monthStart = DateUtils.monthStartIso(year, month)
            val monthEnd = DateUtils.monthEndIso(year, month)

            val priorMilk = entryDao.sumMilkAmountBeforeDate(customerId, monthStart)
            val priorPayments = paymentDao.sumPaymentsBeforeDate(customerId, monthStart)
            val previousOutstanding = round2(customer.openingBalance + priorMilk - priorPayments)

            val monthEntries = entryDao.getForCustomerInRange(customerId, monthStart, monthEnd)
            val totalPackets = monthEntries.sumOf { it.packetQuantity }
            val totalMilkAmount = round2(monthEntries.sumOf { it.totalAmount })
            val paymentsReceived = round2(paymentDao.sumPaymentsInRange(customerId, monthStart, monthEnd))
            val finalPayable = round2(previousOutstanding + totalMilkAmount - paymentsReceived)

            val status = when {
                finalPayable <= 0.01 -> "PAID"
                paymentsReceived > 0.01 -> "PARTIAL"
                else -> "UNPAID"
            }

            val entriesJson = serializeBillLineItems(
                monthEntries.map {
                    BillLineItem(
                        date = it.entryDate,
                        productName = it.productNameSnapshot,
                        packetSizeMl = it.packetSizeMlSnapshot,
                        packets = it.packetQuantity,
                        rate = it.pricePerPacket,
                        totalAmount = it.totalAmount
                    )
                }
            )

            val nextVersion = (existing?.version ?: 0) + 1
            val billNumber = existing?.billNumber ?: String.format(
                Locale.ENGLISH,
                "SSM-%04d%02d-%03d",
                year,
                month,
                customer.id
            )
            val note = if (existing == null) {
                "Initial monthly bill generated"
            } else {
                regenerationReason.trim().ifBlank { "Manual bill regeneration (v$nextVersion)" }
            }

            val billToSave = MonthlyBillEntity(
                id = existing?.id ?: 0L,
                billNumber = billNumber,
                customerId = customer.id,
                customerNameSnapshot = customer.name,
                customerMobileSnapshot = customer.mobileNumber,
                customerWhatsappSnapshot = customer.whatsappNumber.ifBlank { customer.mobileNumber },
                billingYear = year,
                billingMonth = month,
                openingBalance = customer.openingBalance,
                previousOutstanding = previousOutstanding,
                totalPackets = totalPackets,
                totalMilkAmount = totalMilkAmount,
                paymentsReceived = paymentsReceived,
                finalPayableAmount = finalPayable,
                entriesJsonSnapshot = entriesJson,
                version = nextVersion,
                status = status,
                generatedAt = System.currentTimeMillis(),
                auditNote = note,
                isSampleData = customer.isSampleData
            )

            val savedId = billDao.insertOrReplaceBill(billToSave)
            val savedBill = billToSave.copy(id = savedId)

            billDao.insertAudit(
                BillAuditEntity(
                    billId = savedId,
                    customerId = customer.id,
                    billingYear = year,
                    billingMonth = month,
                    version = nextVersion,
                    previousFinalPayable = existing?.finalPayableAmount ?: 0.0,
                    newFinalPayable = finalPayable,
                    reason = note
                )
            )

            OperationResult.Success(
                savedBill,
                if (existing == null) "Bill generated for ${customer.name}" else "Bill regenerated (v$nextVersion)"
            )
        }
    }

    suspend fun generateAllMonthlyBills(
        year: Int,
        month: Int,
        regenerateExisting: Boolean = false
    ): OperationResult<Int> {
        val customers = customerDao.getAllSnapshot().filter { it.isActive }
        if (customers.isEmpty()) {
            return OperationResult.Error("No active customers found to bill")
        }
        var createdOrUpdated = 0
        for (customer in customers) {
            val existing = billDao.getForCustomerAndPeriod(customer.id, year, month)
            if (existing == null || regenerateExisting) {
                val res = generateMonthlyBill(
                    customerId = customer.id,
                    year = year,
                    month = month,
                    forceRegenerate = regenerateExisting,
                    regenerationReason = if (regenerateExisting) "Bulk month-end regeneration" else ""
                )
                if (res is OperationResult.Success) createdOrUpdated++
            }
        }
        return OperationResult.Success(
            createdOrUpdated,
            if (createdOrUpdated > 0) "Generated $createdOrUpdated monthly bills" else "All active customer bills already exist for this month"
        )
    }

    // --- Reports ---

    fun computeReportData(
        customers: List<CustomerWithBalance>,
        entries: List<DailyEntryWithDetails>,
        payments: List<PaymentEntity>,
        startDate: String,
        endDate: String,
        customerFilterId: Long? = null
    ): MonthlyReportData {
        val filteredEntries = entries.filter { e ->
            e.entry.entryDate in startDate..endDate &&
                (customerFilterId == null || e.entry.customerId == customerFilterId)
        }
        val filteredPayments = payments.filter { p ->
            p.paymentDate in startDate..endDate &&
                (customerFilterId == null || p.customerId == customerFilterId)
        }
        val filteredCustomers = if (customerFilterId == null) {
            customers
        } else {
            customers.filter { it.customer.id == customerFilterId }
        }

        val entriesByCustomer = filteredEntries.groupBy { it.entry.customerId }
        val paymentsByCustomer = filteredPayments.groupBy { it.customerId }

        val customerRows = filteredCustomers.map { cwb ->
            val cEntries = entriesByCustomer[cwb.customer.id].orEmpty()
            val cPayments = paymentsByCustomer[cwb.customer.id].orEmpty()
            CustomerSalesRow(
                customerId = cwb.customer.id,
                customerName = cwb.customer.name,
                customerCode = cwb.customer.customerCode,
                packets = cEntries.sumOf { it.entry.packetQuantity },
                milkSales = round2(cEntries.sumOf { it.entry.totalAmount }),
                paymentsReceived = round2(cPayments.sumOf { it.amount }),
                currentOutstanding = cwb.currentOutstanding
            )
        }.sortedByDescending { it.milkSales }

        val productRows = filteredEntries
            .groupBy { Triple(it.entry.productId, it.entry.productNameSnapshot, it.entry.packetSizeMlSnapshot) }
            .map { (key, list) ->
                ProductSalesRow(
                    productId = key.first,
                    productName = key.second,
                    packetSizeMl = key.third,
                    packets = list.sumOf { it.entry.packetQuantity },
                    totalSales = round2(list.sumOf { it.entry.totalAmount })
                )
            }.sortedByDescending { it.totalSales }

        return MonthlyReportData(
            startDate = startDate,
            endDate = endDate,
            totalMilkSales = round2(filteredEntries.sumOf { it.entry.totalAmount }),
            totalPacketsSold = filteredEntries.sumOf { it.entry.packetQuantity },
            activeCustomersCount = filteredCustomers.count { it.customer.isActive },
            totalPaymentsReceived = round2(filteredPayments.sumOf { it.amount }),
            totalPendingAmount = round2(filteredCustomers.sumOf { it.currentOutstanding.coerceAtLeast(0.0) }),
            customerSales = customerRows,
            productSales = productRows
        )
    }

    // --- Sample Demo Data (Clearly tagged isSampleData = true) ---

    suspend fun loadSampleDemoData(): OperationResult<Unit> {
        return db.withTransaction {
            AppDatabase.seedDefaultProducts(productDao)
            val existingSample = customerDao.getAllSnapshot().any { it.isSampleData }
            if (existingSample) {
                return@withTransaction OperationResult.Success(Unit, "Sample demo data is already loaded")
            }

            val products = productDao.getAllSnapshot()
            val fullCream = products.firstOrNull { it.name.contains("Full Cream", true) && it.packetSizeMl == 500 } ?: products.first()
            val standard = products.firstOrNull { it.name.contains("Standard", true) } ?: products.first()
            val toned = products.firstOrNull { it.name.contains("Toned", true) } ?: products.first()
            val cow = products.firstOrNull { it.name.contains("Cow", true) } ?: products.first()

            val baseNext = (customerDao.getMaxId() ?: 0L) + 1L
            val sampleCustomers = listOf(
                CustomerEntity(
                    customerCode = String.format(Locale.ENGLISH, "DEMO-%03d", baseNext),
                    name = "Rahul Patil",
                    mobileNumber = "9823011223",
                    whatsappNumber = "9823011223",
                    address = "Flat 302, Sai Residency, Kothrud",
                    defaultProductId = fullCream.id,
                    customPacketRate = 30.0,
                    isActive = true,
                    openingBalance = 200.0,
                    notes = "Morning 7 AM delivery • Sample Demo Record",
                    isSampleData = true
                ),
                CustomerEntity(
                    customerCode = String.format(Locale.ENGLISH, "DEMO-%03d", baseNext + 1),
                    name = "Sunita Deshmukh",
                    mobileNumber = "9850122334",
                    whatsappNumber = "9850122334",
                    address = "Row House 12, Shivaji Nagar",
                    defaultProductId = cow.id,
                    customPacketRate = null,
                    isActive = true,
                    openingBalance = 0.0,
                    notes = "Prefers Cow Milk • Sample Demo Record",
                    isSampleData = true
                ),
                CustomerEntity(
                    customerCode = String.format(Locale.ENGLISH, "DEMO-%03d", baseNext + 2),
                    name = "Amit Kulkarni",
                    mobileNumber = "9765433221",
                    whatsappNumber = "9765433221",
                    address = "Plot 8, Ganesh Colony",
                    defaultProductId = standard.id,
                    customPacketRate = 30.0,
                    isActive = true,
                    openingBalance = 150.0,
                    notes = "Pays via UPI every 10th • Sample Demo Record",
                    isSampleData = true
                ),
                CustomerEntity(
                    customerCode = String.format(Locale.ENGLISH, "DEMO-%03d", baseNext + 3),
                    name = "Priya Joshi",
                    mobileNumber = "9822988776",
                    whatsappNumber = "9822988776",
                    address = "B-404, Green Park Society",
                    defaultProductId = toned.id,
                    customPacketRate = null,
                    isActive = true,
                    openingBalance = 0.0,
                    notes = "Sample Demo Record",
                    isSampleData = true
                )
            )

            val insertedIds = sampleCustomers.map { customerDao.insert(it) }
            val rahulId = insertedIds[0]
            val sunitaId = insertedIds[1]
            val amitId = insertedIds[2]
            val priyaId = insertedIds[3]

            // Seed past 7 days of daily milk entries
            for (dayOffset in 0..6) {
                val dateIso = DateUtils.daysAgoIso(dayOffset)
                entryDao.insert(
                    DailyMilkEntryEntity(
                        entryDate = dateIso,
                        customerId = rahulId,
                        productId = fullCream.id,
                        productNameSnapshot = fullCream.name,
                        packetSizeMlSnapshot = fullCream.packetSizeMl,
                        packetQuantity = 2,
                        pricePerPacket = 30.0,
                        totalAmount = 60.0,
                        remarks = "Morning delivery",
                        isSampleData = true
                    )
                )
                entryDao.insert(
                    DailyMilkEntryEntity(
                        entryDate = dateIso,
                        customerId = sunitaId,
                        productId = cow.id,
                        productNameSnapshot = cow.name,
                        packetSizeMlSnapshot = cow.packetSizeMl,
                        packetQuantity = if (dayOffset % 2 == 0) 2 else 1,
                        pricePerPacket = cow.pricePerPacket,
                        totalAmount = round2((if (dayOffset % 2 == 0) 2 else 1) * cow.pricePerPacket),
                        remarks = "",
                        isSampleData = true
                    )
                )
                if (dayOffset <= 4) {
                    entryDao.insert(
                        DailyMilkEntryEntity(
                            entryDate = dateIso,
                            customerId = amitId,
                            productId = standard.id,
                            productNameSnapshot = standard.name,
                            packetSizeMlSnapshot = standard.packetSizeMl,
                            packetQuantity = 3,
                            pricePerPacket = 30.0,
                            totalAmount = 90.0,
                            remarks = "",
                            isSampleData = true
                        )
                    )
                }
                if (dayOffset <= 3) {
                    entryDao.insert(
                        DailyMilkEntryEntity(
                            entryDate = dateIso,
                            customerId = priyaId,
                            productId = toned.id,
                            productNameSnapshot = toned.name,
                            packetSizeMlSnapshot = toned.packetSizeMl,
                            packetQuantity = 1,
                            pricePerPacket = toned.pricePerPacket,
                            totalAmount = toned.pricePerPacket,
                            remarks = "",
                            isSampleData = true
                        )
                    )
                }
            }

            // Record sample payments
            paymentDao.insert(
                PaymentEntity(
                    paymentDate = DateUtils.daysAgoIso(2),
                    customerId = rahulId,
                    amount = 300.0,
                    paymentMethod = "UPI",
                    transactionRef = "DEMO-UPI-101",
                    notes = "Part payment via GPay",
                    idempotencyKey = UUID.randomUUID().toString(),
                    isSampleData = true
                )
            )
            paymentDao.insert(
                PaymentEntity(
                    paymentDate = DateUtils.daysAgoIso(1),
                    customerId = sunitaId,
                    amount = 200.0,
                    paymentMethod = "CASH",
                    transactionRef = "",
                    notes = "Cash at shop counter",
                    idempotencyKey = UUID.randomUUID().toString(),
                    isSampleData = true
                )
            )

            OperationResult.Success(Unit, "Loaded 4 sample demo customers & 7 days of milk entries")
        }
    }

    suspend fun clearSampleDemoData(): OperationResult<Unit> {
        return db.withTransaction {
            billDao.deleteSampleBills()
            paymentDao.deleteSamplePayments()
            entryDao.deleteSampleEntries()
            customerDao.deleteSampleCustomers()
            OperationResult.Success(Unit, "All sample demo records removed cleanly")
        }
    }

    companion object {
        fun round2(value: Double): Double = Math.round(value * 100.0) / 100.0

        fun formatShortRate(rate: Double): String {
            return if (abs(rate - rate.toLong()) < 0.005) rate.toLong().toString()
            else String.format(Locale.ENGLISH, "%.2f", rate)
        }

        fun formatPaymentMethod(method: String): String = when (method.uppercase(Locale.ENGLISH)) {
            "CASH" -> "Cash"
            "UPI" -> "UPI"
            "BANK_TRANSFER" -> "Bank Transfer"
            else -> "Other"
        }

        fun serializeBillLineItems(items: List<BillLineItem>): String {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("date", item.date)
                        put("productName", item.productName)
                        put("packetSizeMl", item.packetSizeMl)
                        put("packets", item.packets)
                        put("rate", item.rate)
                        put("totalAmount", item.totalAmount)
                    }
                )
            }
            return arr.toString()
        }

        fun parseBillLineItems(json: String): List<BillLineItem> {
            if (json.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    BillLineItem(
                        date = obj.optString("date"),
                        productName = obj.optString("productName"),
                        packetSizeMl = obj.optInt("packetSizeMl", 500),
                        packets = obj.optInt("packets", 0),
                        rate = obj.optDouble("rate", 0.0),
                        totalAmount = obj.optDouble("totalAmount", 0.0)
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
