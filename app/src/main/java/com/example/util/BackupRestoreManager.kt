package com.example.util

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.BillAuditEntity
import com.example.data.local.CustomerEntity
import com.example.data.local.DailyMilkEntryEntity
import com.example.data.local.MilkProductEntity
import com.example.data.local.MonthlyBillEntity
import com.example.data.local.PaymentEntity
import com.example.data.preferences.ShopSettings
import com.example.data.preferences.ShopSettingsRepository
import org.json.JSONArray
import org.json.JSONObject

data class BackupSummary(
    val productsCount: Int,
    val customersCount: Int,
    val entriesCount: Int,
    val paymentsCount: Int,
    val billsCount: Int
)

object BackupRestoreManager {

    private const val BACKUP_FORMAT_VERSION = 1

    suspend fun exportBackupJson(
        db: AppDatabase,
        settingsRepo: ShopSettingsRepository
    ): Pair<String, BackupSummary> {
        val products = db.milkProductDao().getAllSnapshot()
        val customers = db.customerDao().getAllSnapshot()
        val entries = db.dailyMilkEntryDao().getAllSnapshot()
        val payments = db.paymentDao().getAllSnapshot()
        val bills = db.monthlyBillDao().getAllBillsSnapshot()
        val audits = db.monthlyBillDao().getAllAuditsSnapshot()
        val settings = settingsRepo.getSnapshot()

        val root = JSONObject().apply {
            put("app", "Snehal Super Market Milk Manager")
            put("formatVersion", BACKUP_FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())

            put("settings", JSONObject().apply {
                put("shopName", settings.shopName)
                put("shopAddress", settings.shopAddress)
                put("shopMobile", settings.shopMobile)
                put("shopWhatsApp", settings.shopWhatsApp)
                put("currencySymbol", settings.currencySymbol)
                put("defaultMilkPrice", settings.defaultMilkPrice)
                put("defaultBillingMonth", settings.defaultBillingMonth)
                put("defaultBillingYear", settings.defaultBillingYear)
                put("languageCode", settings.languageCode)
            })

            put("products", JSONArray().apply {
                products.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("packetSizeMl", p.packetSizeMl)
                        put("pricePerPacket", p.pricePerPacket)
                        put("isActive", p.isActive)
                        put("description", p.description)
                        put("createdAt", p.createdAt)
                    })
                }
            })

            put("customers", JSONArray().apply {
                customers.forEach { c ->
                    put(JSONObject().apply {
                        put("id", c.id)
                        put("customerCode", c.customerCode)
                        put("name", c.name)
                        put("mobileNumber", c.mobileNumber)
                        put("whatsappNumber", c.whatsappNumber)
                        put("address", c.address)
                        put("defaultProductId", c.defaultProductId ?: JSONObject.NULL)
                        put("customPacketRate", c.customPacketRate ?: JSONObject.NULL)
                        put("isActive", c.isActive)
                        put("openingBalance", c.openingBalance)
                        put("notes", c.notes)
                        put("isSampleData", c.isSampleData)
                        put("createdAt", c.createdAt)
                    })
                }
            })

            put("entries", JSONArray().apply {
                entries.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("entryDate", e.entryDate)
                        put("customerId", e.customerId)
                        put("productId", e.productId)
                        put("productNameSnapshot", e.productNameSnapshot)
                        put("packetSizeMlSnapshot", e.packetSizeMlSnapshot)
                        put("packetQuantity", e.packetQuantity)
                        put("pricePerPacket", e.pricePerPacket)
                        put("totalAmount", e.totalAmount)
                        put("remarks", e.remarks)
                        put("isSampleData", e.isSampleData)
                        put("createdAt", e.createdAt)
                        put("updatedAt", e.updatedAt)
                    })
                }
            })

            put("payments", JSONArray().apply {
                payments.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("paymentDate", p.paymentDate)
                        put("customerId", p.customerId)
                        put("amount", p.amount)
                        put("paymentMethod", p.paymentMethod)
                        put("transactionRef", p.transactionRef)
                        put("notes", p.notes)
                        put("idempotencyKey", p.idempotencyKey)
                        put("isSampleData", p.isSampleData)
                        put("createdAt", p.createdAt)
                    })
                }
            })

            put("bills", JSONArray().apply {
                bills.forEach { b ->
                    put(JSONObject().apply {
                        put("id", b.id)
                        put("billNumber", b.billNumber)
                        put("customerId", b.customerId)
                        put("customerNameSnapshot", b.customerNameSnapshot)
                        put("customerMobileSnapshot", b.customerMobileSnapshot)
                        put("customerWhatsappSnapshot", b.customerWhatsappSnapshot)
                        put("billingYear", b.billingYear)
                        put("billingMonth", b.billingMonth)
                        put("openingBalance", b.openingBalance)
                        put("previousOutstanding", b.previousOutstanding)
                        put("totalPackets", b.totalPackets)
                        put("totalMilkAmount", b.totalMilkAmount)
                        put("paymentsReceived", b.paymentsReceived)
                        put("finalPayableAmount", b.finalPayableAmount)
                        put("entriesJsonSnapshot", b.entriesJsonSnapshot)
                        put("version", b.version)
                        put("status", b.status)
                        put("generatedAt", b.generatedAt)
                        put("auditNote", b.auditNote)
                        put("isSampleData", b.isSampleData)
                    })
                }
            })

            put("audits", JSONArray().apply {
                audits.forEach { a ->
                    put(JSONObject().apply {
                        put("id", a.id)
                        put("billId", a.billId)
                        put("customerId", a.customerId)
                        put("billingYear", a.billingYear)
                        put("billingMonth", a.billingMonth)
                        put("version", a.version)
                        put("previousFinalPayable", a.previousFinalPayable)
                        put("newFinalPayable", a.newFinalPayable)
                        put("reason", a.reason)
                        put("timestamp", a.timestamp)
                    })
                }
            })
        }

        return root.toString(2) to BackupSummary(
            productsCount = products.size,
            customersCount = customers.size,
            entriesCount = entries.size,
            paymentsCount = payments.size,
            billsCount = bills.size
        )
    }

    suspend fun restoreFromBackupJson(
        jsonString: String,
        db: AppDatabase,
        settingsRepo: ShopSettingsRepository
    ): BackupSummary {
        val root = JSONObject(jsonString)
        val version = root.optInt("formatVersion", 0)
        require(version >= 1) { "Invalid or unsupported backup file format" }

        val productsArr = root.optJSONArray("products") ?: JSONArray()
        val customersArr = root.optJSONArray("customers") ?: JSONArray()
        val entriesArr = root.optJSONArray("entries") ?: JSONArray()
        val paymentsArr = root.optJSONArray("payments") ?: JSONArray()
        val billsArr = root.optJSONArray("bills") ?: JSONArray()
        val auditsArr = root.optJSONArray("audits") ?: JSONArray()

        val products = (0 until productsArr.length()).map { i ->
            val o = productsArr.getJSONObject(i)
            MilkProductEntity(
                id = o.getLong("id"),
                name = o.getString("name"),
                packetSizeMl = o.optInt("packetSizeMl", 500),
                pricePerPacket = o.getDouble("pricePerPacket"),
                isActive = o.optBoolean("isActive", true),
                description = o.optString("description", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val customers = (0 until customersArr.length()).map { i ->
            val o = customersArr.getJSONObject(i)
            CustomerEntity(
                id = o.getLong("id"),
                customerCode = o.getString("customerCode"),
                name = o.getString("name"),
                mobileNumber = o.getString("mobileNumber"),
                whatsappNumber = o.optString("whatsappNumber", ""),
                address = o.optString("address", ""),
                defaultProductId = if (o.isNull("defaultProductId")) null else o.getLong("defaultProductId"),
                customPacketRate = if (o.isNull("customPacketRate")) null else o.getDouble("customPacketRate"),
                isActive = o.optBoolean("isActive", true),
                openingBalance = o.optDouble("openingBalance", 0.0),
                notes = o.optString("notes", ""),
                isSampleData = o.optBoolean("isSampleData", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val entries = (0 until entriesArr.length()).map { i ->
            val o = entriesArr.getJSONObject(i)
            DailyMilkEntryEntity(
                id = o.getLong("id"),
                entryDate = o.getString("entryDate"),
                customerId = o.getLong("customerId"),
                productId = o.getLong("productId"),
                productNameSnapshot = o.getString("productNameSnapshot"),
                packetSizeMlSnapshot = o.optInt("packetSizeMlSnapshot", 500),
                packetQuantity = o.getInt("packetQuantity"),
                pricePerPacket = o.getDouble("pricePerPacket"),
                totalAmount = o.getDouble("totalAmount"),
                remarks = o.optString("remarks", ""),
                isSampleData = o.optBoolean("isSampleData", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }

        val payments = (0 until paymentsArr.length()).map { i ->
            val o = paymentsArr.getJSONObject(i)
            PaymentEntity(
                id = o.getLong("id"),
                paymentDate = o.getString("paymentDate"),
                customerId = o.getLong("customerId"),
                amount = o.getDouble("amount"),
                paymentMethod = o.getString("paymentMethod"),
                transactionRef = o.optString("transactionRef", ""),
                notes = o.optString("notes", ""),
                idempotencyKey = o.optString("idempotencyKey", "restored_${o.getLong("id")}"),
                isSampleData = o.optBoolean("isSampleData", false),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }

        val bills = (0 until billsArr.length()).map { i ->
            val o = billsArr.getJSONObject(i)
            MonthlyBillEntity(
                id = o.getLong("id"),
                billNumber = o.getString("billNumber"),
                customerId = o.getLong("customerId"),
                customerNameSnapshot = o.getString("customerNameSnapshot"),
                customerMobileSnapshot = o.optString("customerMobileSnapshot", ""),
                customerWhatsappSnapshot = o.optString("customerWhatsappSnapshot", ""),
                billingYear = o.getInt("billingYear"),
                billingMonth = o.getInt("billingMonth"),
                openingBalance = o.optDouble("openingBalance", 0.0),
                previousOutstanding = o.optDouble("previousOutstanding", 0.0),
                totalPackets = o.optInt("totalPackets", 0),
                totalMilkAmount = o.optDouble("totalMilkAmount", 0.0),
                paymentsReceived = o.optDouble("paymentsReceived", 0.0),
                finalPayableAmount = o.optDouble("finalPayableAmount", 0.0),
                entriesJsonSnapshot = o.optString("entriesJsonSnapshot", "[]"),
                version = o.optInt("version", 1),
                status = o.optString("status", "UNPAID"),
                generatedAt = o.optLong("generatedAt", System.currentTimeMillis()),
                auditNote = o.optString("auditNote", "Restored from backup"),
                isSampleData = o.optBoolean("isSampleData", false)
            )
        }

        val audits = (0 until auditsArr.length()).map { i ->
            val o = auditsArr.getJSONObject(i)
            BillAuditEntity(
                id = o.getLong("id"),
                billId = o.getLong("billId"),
                customerId = o.getLong("customerId"),
                billingYear = o.getInt("billingYear"),
                billingMonth = o.getInt("billingMonth"),
                version = o.optInt("version", 1),
                previousFinalPayable = o.optDouble("previousFinalPayable", 0.0),
                newFinalPayable = o.optDouble("newFinalPayable", 0.0),
                reason = o.optString("reason", ""),
                timestamp = o.optLong("timestamp", System.currentTimeMillis())
            )
        }

        db.withTransaction {
            db.monthlyBillDao().deleteAllAudits()
            db.monthlyBillDao().deleteAllBills()
            db.paymentDao().deleteAll()
            db.dailyMilkEntryDao().deleteAll()
            db.customerDao().deleteAll()
            db.milkProductDao().deleteAll()

            if (products.isNotEmpty()) db.milkProductDao().insertAll(products)
            if (customers.isNotEmpty()) db.customerDao().insertAllReplace(customers)
            if (entries.isNotEmpty()) db.dailyMilkEntryDao().insertAllReplace(entries)
            if (payments.isNotEmpty()) db.paymentDao().insertAllReplace(payments)
            if (bills.isNotEmpty()) db.monthlyBillDao().insertAllBillsReplace(bills)
            if (audits.isNotEmpty()) db.monthlyBillDao().insertAllAuditsReplace(audits)
        }

        root.optJSONObject("settings")?.let { s ->
            val current = settingsRepo.getSnapshot()
            settingsRepo.updateSettings(
                ShopSettings(
                    shopName = s.optString("shopName", current.shopName),
                    shopAddress = s.optString("shopAddress", current.shopAddress),
                    shopMobile = s.optString("shopMobile", current.shopMobile),
                    shopWhatsApp = s.optString("shopWhatsApp", current.shopWhatsApp),
                    shopLogoUri = current.shopLogoUri,
                    currencySymbol = s.optString("currencySymbol", current.currencySymbol),
                    defaultMilkPrice = s.optDouble("defaultMilkPrice", current.defaultMilkPrice),
                    defaultBillingMonth = s.optInt("defaultBillingMonth", current.defaultBillingMonth),
                    defaultBillingYear = s.optInt("defaultBillingYear", current.defaultBillingYear),
                    languageCode = s.optString("languageCode", current.languageCode),
                    dailyEntryReminderEnabled = current.dailyEntryReminderEnabled,
                    dailyReminderHour = current.dailyReminderHour,
                    dailyReminderMinute = current.dailyReminderMinute,
                    monthlyBillReminderEnabled = current.monthlyBillReminderEnabled,
                    pendingPaymentReminderEnabled = current.pendingPaymentReminderEnabled
                )
            )
        }

        return BackupSummary(
            productsCount = products.size,
            customersCount = customers.size,
            entriesCount = entries.size,
            paymentsCount = payments.size,
            billsCount = bills.size
        )
    }
}
