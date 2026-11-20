package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MilkManagerApplication
import com.example.data.local.BillAuditEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerWithBalance
import com.example.data.local.DailyEntryWithDetails
import com.example.data.local.DailyMilkEntryEntity
import com.example.data.local.DashboardSummary
import com.example.data.local.LedgerTransaction
import com.example.data.local.MilkProductEntity
import com.example.data.local.MonthlyBillEntity
import com.example.data.local.MonthlyReportData
import com.example.data.local.PaymentEntity
import com.example.data.preferences.ShopSettings
import com.example.data.repository.OperationResult
import com.example.util.BackupRestoreManager
import com.example.util.DateUtils
import com.example.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MilkManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MilkManagerApplication
    val repository = app.repository
    private val settingsRepo = app.settingsRepository
    private val db = app.database

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val settings: StateFlow<ShopSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ShopSettings()
    )

    val dashboardSummary: StateFlow<DashboardSummary> = repository.dashboardSummaryFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardSummary(
            todayDateIso = DateUtils.todayIso(),
            activeCustomersCount = 0,
            todayTotalPackets = 0,
            todayTotalSales = 0.0,
            totalPendingPayments = 0.0,
            thisMonthTotalSales = 0.0,
            thisMonthCollected = 0.0,
            thisMonthOutstanding = 0.0,
            hasSampleData = false,
            recentEntries = emptyList()
        )
    )

    val products: StateFlow<List<MilkProductEntity>> = repository.productsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeProducts: StateFlow<List<MilkProductEntity>> = repository.activeProductsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customers: StateFlow<List<CustomerWithBalance>> = repository.customersWithBalanceFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dailyEntries: StateFlow<List<DailyEntryWithDetails>> = repository.entriesWithDetailsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPayments: StateFlow<List<PaymentEntity>> = repository.allPaymentsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allBills: StateFlow<List<MonthlyBillEntity>> = repository.allBillsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun notifyUser(message: String) {
        _snackbarMessage.tryEmit(message)
    }

    // --- Product Operations ---

    fun saveProduct(product: MilkProductEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.saveProduct(product)) {
                is OperationResult.Success -> {
                    notifyUser(res.message)
                    onSuccess()
                }
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }

    fun deleteProduct(product: MilkProductEntity) {
        viewModelScope.launch {
            when (val res = repository.deleteProduct(product)) {
                is OperationResult.Success -> notifyUser(res.message)
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }

    // --- Customer Operations ---

    fun saveCustomer(
        id: Long = 0L,
        name: String,
        mobileNumber: String,
        whatsappNumber: String,
        address: String,
        defaultProductId: Long?,
        customPacketRate: Double?,
        isActive: Boolean,
        openingBalance: Double,
        notes: String,
        onResult: (OperationResult<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.saveCustomer(
                id = id,
                name = name,
                mobileNumber = mobileNumber,
                whatsappNumber = whatsappNumber,
                address = address,
                defaultProductId = defaultProductId,
                customPacketRate = customPacketRate,
                isActive = isActive,
                openingBalance = openingBalance,
                notes = notes
            )
            when (result) {
                is OperationResult.Success -> notifyUser(result.message)
                is OperationResult.Error -> notifyUser(result.message)
                is OperationResult.DuplicateWarning -> notifyUser(result.message)
            }
            onResult(result)
        }
    }

    fun deleteCustomer(customer: CustomerEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.deleteCustomer(customer)) {
                is OperationResult.Success -> {
                    notifyUser(res.message)
                    onDeleted()
                }
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }

    fun observeCustomerLedger(customerId: Long): Flow<List<LedgerTransaction>> =
        repository.observeCustomerLedger(customerId)

    fun observeCustomerEntries(customerId: Long): Flow<List<DailyMilkEntryEntity>> =
        repository.observeCustomerEntries(customerId)

    fun observeCustomerPayments(customerId: Long): Flow<List<PaymentEntity>> =
        repository.observeCustomerPayments(customerId)

    fun observeCustomerBills(customerId: Long): Flow<List<MonthlyBillEntity>> =
        repository.observeCustomerBills(customerId)

    // --- Daily Milk Entry Operations ---

    suspend fun getExistingEntriesForCustomerOnDate(
        customerId: Long,
        dateIso: String
    ): List<DailyMilkEntryEntity> = repository.getExistingEntriesForCustomerOnDate(customerId, dateIso)

    suspend fun getDailyEntryById(entryId: Long): DailyMilkEntryEntity? =
        repository.getDailyEntryById(entryId)

    fun saveDailyMilkEntry(
        entryId: Long = 0L,
        entryDate: String,
        customerId: Long,
        productId: Long,
        packetQuantity: Int,
        pricePerPacket: Double,
        remarks: String,
        onResult: (OperationResult<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.saveDailyMilkEntry(
                entryId = entryId,
                entryDate = entryDate,
                customerId = customerId,
                productId = productId,
                packetQuantity = packetQuantity,
                pricePerPacket = pricePerPacket,
                remarks = remarks
            )
            when (res) {
                is OperationResult.Success -> notifyUser(res.message)
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> { /* handled cleanly in form dialog */ }
            }
            onResult(res)
        }
    }

    fun deleteDailyEntry(entryId: Long) {
        viewModelScope.launch {
            when (val res = repository.deleteDailyEntry(entryId)) {
                is OperationResult.Success -> notifyUser(res.message)
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }

    // --- Payment Operations ---

    fun recordPayment(
        customerId: Long,
        paymentDate: String,
        amount: Double,
        paymentMethod: String,
        transactionRef: String,
        notes: String,
        allowDuplicateOverride: Boolean = false,
        onResult: (OperationResult<Long>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.recordPayment(
                customerId = customerId,
                paymentDate = paymentDate,
                amount = amount,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                notes = notes,
                allowDuplicateOverride = allowDuplicateOverride
            )
            when (res) {
                is OperationResult.Success -> notifyUser(res.message)
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> { /* handled cleanly in payment dialog */ }
            }
            onResult(res)
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            when (val res = repository.deletePayment(paymentId)) {
                is OperationResult.Success -> notifyUser(res.message)
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }

    // --- Monthly Billing Operations ---

    fun observeBillById(billId: Long): Flow<MonthlyBillEntity?> = repository.observeBillById(billId)

    fun observeAuditsForBill(billId: Long): Flow<List<BillAuditEntity>> =
        repository.observeAuditsForBill(billId)

    fun generateMonthlyBill(
        customerId: Long,
        year: Int,
        month: Int,
        forceRegenerate: Boolean = false,
        regenerationReason: String = "",
        onGenerated: (MonthlyBillEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            when (val res = repository.generateMonthlyBill(customerId, year, month, forceRegenerate, regenerationReason)) {
                is OperationResult.Success -> {
                    notifyUser(res.message)
                    onGenerated(res.data)
                }
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }

    fun generateAllMonthlyBills(
        year: Int,
        month: Int,
        regenerateExisting: Boolean = false
    ) {
        viewModelScope.launch {
            when (val res = repository.generateAllMonthlyBills(year, month, regenerateExisting)) {
                is OperationResult.Success -> notifyUser(res.message)
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }

    // --- Reports ---

    fun buildReport(
        startDate: String,
        endDate: String,
        customerFilterId: Long?
    ): MonthlyReportData {
        return repository.computeReportData(
            customers = customers.value,
            entries = dailyEntries.value,
            payments = allPayments.value,
            startDate = startDate,
            endDate = endDate,
            customerFilterId = customerFilterId
        )
    }

    // --- Settings, Backup & Restore, Sample Data ---

    fun saveSettings(updated: ShopSettings) {
        viewModelScope.launch {
            settingsRepo.updateSettings(updated)
            ReminderScheduler.syncReminders(app, updated)
            notifyUser("Settings saved")
        }
    }

    fun switchLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsRepo.updateLanguage(languageCode)
            notifyUser(if (languageCode == "mr") "भाषा मराठी निवडली" else "Language set to English")
        }
    }

    fun updateShopLogo(uri: String) {
        viewModelScope.launch {
            settingsRepo.updateLogoUri(uri)
            notifyUser("Shop logo updated")
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val (json, summary) = withContext(Dispatchers.IO) {
                    BackupRestoreManager.exportBackupJson(db, settingsRepo)
                }
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open destination file")
                }
                notifyUser(
                    "Backup saved (${summary.customersCount} customers, ${summary.entriesCount} entries, ${summary.billsCount} bills)"
                )
            } catch (e: Exception) {
                notifyUser("Backup failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Unable to read backup file")
                }
                val summary = withContext(Dispatchers.IO) {
                    BackupRestoreManager.restoreFromBackupJson(json, db, settingsRepo)
                }
                notifyUser(
                    "Restored ${summary.customersCount} customers, ${summary.entriesCount} entries & ${summary.billsCount} bills"
                )
            } catch (e: Exception) {
                notifyUser("Restore failed: ${e.message ?: "Invalid backup file"}")
            }
        }
    }

    suspend fun createShareableBackupFile(): java.io.File = withContext(Dispatchers.IO) {
        val (json, _) = BackupRestoreManager.exportBackupJson(db, settingsRepo)
        val dir = java.io.File(app.cacheDir, "exports").apply { mkdirs() }
        val file = java.io.File(dir, "Snehal_Milk_Backup_${DateUtils.todayIso()}.json")
        file.writeText(json, Charsets.UTF_8)
        file
    }

    fun loadSampleDemoData() {
        viewModelScope.launch {
            when (val res = repository.loadSampleDemoData()) {
                is OperationResult.Success -> notifyUser(res.message)
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }

    fun clearSampleDemoData() {
        viewModelScope.launch {
            when (val res = repository.clearSampleDemoData()) {
                is OperationResult.Success -> notifyUser(res.message)
                is OperationResult.Error -> notifyUser(res.message)
                is OperationResult.DuplicateWarning -> notifyUser(res.message)
            }
        }
    }
}
