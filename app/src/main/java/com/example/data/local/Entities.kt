package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "milk_products")
data class MilkProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val packetSizeMl: Int = 500,
    val pricePerPacket: Double,
    val isActive: Boolean = true,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerCode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["mobileNumber"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val customerCode: String,
    val name: String,
    val mobileNumber: String,
    val whatsappNumber: String = "",
    val address: String = "",
    val defaultProductId: Long? = null,
    val customPacketRate: Double? = null,
    val isActive: Boolean = true,
    val openingBalance: Double = 0.0,
    val notes: String = "",
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_milk_entries",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entryDate"]),
        Index(value = ["customerId"]),
        Index(value = ["productId"]),
        Index(value = ["entryDate", "customerId", "productId"], unique = true)
    ]
)
data class DailyMilkEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryDate: String, // ISO yyyy-MM-dd
    val customerId: Long,
    val productId: Long,
    val productNameSnapshot: String,
    val packetSizeMlSnapshot: Int,
    val packetQuantity: Int,
    val pricePerPacket: Double, // Snapshot price at entry time — immune to future rate changes
    val totalAmount: Double,
    val remarks: String = "",
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["paymentDate"]),
        Index(value = ["customerId"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val paymentDate: String, // ISO yyyy-MM-dd
    val customerId: Long,
    val amount: Double,
    val paymentMethod: String, // CASH, UPI, BANK_TRANSFER, OTHER
    val transactionRef: String = "",
    val notes: String = "",
    val idempotencyKey: String,
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "monthly_bills",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId", "billingYear", "billingMonth"], unique = true),
        Index(value = ["billingYear", "billingMonth"])
    ]
)
data class MonthlyBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val billNumber: String,
    val customerId: Long,
    val customerNameSnapshot: String,
    val customerMobileSnapshot: String,
    val customerWhatsappSnapshot: String,
    val billingYear: Int,
    val billingMonth: Int, // 1..12
    val openingBalance: Double,
    val previousOutstanding: Double,
    val totalPackets: Int,
    val totalMilkAmount: Double,
    val paymentsReceived: Double,
    val finalPayableAmount: Double,
    val entriesJsonSnapshot: String, // Immutable snapshot of date-wise milk entries
    val version: Int = 1,
    val status: String = "UNPAID", // UNPAID, PARTIAL, PAID
    val generatedAt: Long = System.currentTimeMillis(),
    val auditNote: String = "Initial bill generation",
    val isSampleData: Boolean = false
)

@Entity(
    tableName = "bill_audits",
    indices = [Index(value = ["billId"]), Index(value = ["customerId"])]
)
data class BillAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val billId: Long,
    val customerId: Long,
    val billingYear: Int,
    val billingMonth: Int,
    val version: Int,
    val previousFinalPayable: Double,
    val newFinalPayable: Double,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Domain / Projection Models

data class DailyEntryWithDetails(
    val entry: DailyMilkEntryEntity,
    val customerName: String,
    val customerCode: String,
    val customerMobile: String
)

data class CustomerWithBalance(
    val customer: CustomerEntity,
    val defaultProductName: String?,
    val effectiveRate: Double,
    val totalMilkCharges: Double,
    val totalPayments: Double,
    val currentOutstanding: Double,
    val thisMonthPackets: Int,
    val thisMonthAmount: Double
)

enum class LedgerEntryType { OPENING, MILK_DEBIT, PAYMENT_CREDIT }

data class LedgerTransaction(
    val id: String,
    val date: String,
    val type: LedgerEntryType,
    val title: String,
    val subtitle: String,
    val debit: Double,
    val credit: Double,
    val runningBalance: Double,
    val sourceId: Long = 0L
)

data class BillLineItem(
    val date: String,
    val productName: String,
    val packetSizeMl: Int,
    val packets: Int,
    val rate: Double,
    val totalAmount: Double
)

data class DashboardSummary(
    val todayDateIso: String,
    val activeCustomersCount: Int,
    val todayTotalPackets: Int,
    val todayTotalSales: Double,
    val totalPendingPayments: Double,
    val thisMonthTotalSales: Double,
    val thisMonthCollected: Double,
    val thisMonthOutstanding: Double,
    val hasSampleData: Boolean,
    val recentEntries: List<DailyEntryWithDetails>
)

data class CustomerSalesRow(
    val customerId: Long,
    val customerName: String,
    val customerCode: String,
    val packets: Int,
    val milkSales: Double,
    val paymentsReceived: Double,
    val currentOutstanding: Double
)

data class ProductSalesRow(
    val productId: Long,
    val productName: String,
    val packetSizeMl: Int,
    val packets: Int,
    val totalSales: Double
)

data class MonthlyReportData(
    val startDate: String,
    val endDate: String,
    val totalMilkSales: Double,
    val totalPacketsSold: Int,
    val activeCustomersCount: Int,
    val totalPaymentsReceived: Double,
    val totalPendingAmount: Double,
    val customerSales: List<CustomerSalesRow>,
    val productSales: List<ProductSalesRow>
)
