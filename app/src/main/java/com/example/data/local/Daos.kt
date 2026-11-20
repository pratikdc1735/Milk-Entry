package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkProductDao {
    @Query("SELECT * FROM milk_products ORDER BY isActive DESC, name ASC")
    fun observeAll(): Flow<List<MilkProductEntity>>

    @Query("SELECT * FROM milk_products WHERE isActive = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<MilkProductEntity>>

    @Query("SELECT * FROM milk_products ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<MilkProductEntity>

    @Query("SELECT * FROM milk_products WHERE id = :id")
    suspend fun getById(id: Long): MilkProductEntity?

    @Query("SELECT COUNT(*) FROM milk_products")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: MilkProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<MilkProductEntity>)

    @Update
    suspend fun update(product: MilkProductEntity)

    @Delete
    suspend fun delete(product: MilkProductEntity)

    @Query("DELETE FROM milk_products")
    suspend fun deleteAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY isActive DESC, name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun observeById(id: Long): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun findByMobile(mobile: String): CustomerEntity?

    @Query("SELECT MAX(id) FROM customers")
    suspend fun getMaxId(): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE isSampleData = 1")
    suspend fun deleteSampleCustomers()

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}

@Dao
interface DailyMilkEntryDao {
    @Query("SELECT * FROM daily_milk_entries ORDER BY entryDate DESC, updatedAt DESC")
    fun observeAll(): Flow<List<DailyMilkEntryEntity>>

    @Query("SELECT * FROM daily_milk_entries WHERE customerId = :customerId ORDER BY entryDate DESC, id DESC")
    fun observeByCustomer(customerId: Long): Flow<List<DailyMilkEntryEntity>>

    @Query("SELECT * FROM daily_milk_entries WHERE entryDate BETWEEN :startDate AND :endDate ORDER BY entryDate DESC, id DESC")
    fun observeByDateRange(startDate: String, endDate: String): Flow<List<DailyMilkEntryEntity>>

    @Query("SELECT * FROM daily_milk_entries WHERE customerId = :customerId AND entryDate = :date ORDER BY id ASC")
    suspend fun getForCustomerAndDate(customerId: Long, date: String): List<DailyMilkEntryEntity>

    @Query("SELECT * FROM daily_milk_entries WHERE customerId = :customerId AND entryDate = :date AND productId = :productId LIMIT 1")
    suspend fun findDuplicate(customerId: Long, date: String, productId: Long): DailyMilkEntryEntity?

    @Query("SELECT * FROM daily_milk_entries WHERE id = :id")
    suspend fun getById(id: Long): DailyMilkEntryEntity?

    @Query("SELECT * FROM daily_milk_entries WHERE customerId = :customerId AND entryDate BETWEEN :startDate AND :endDate ORDER BY entryDate ASC, id ASC")
    suspend fun getForCustomerInRange(customerId: Long, startDate: String, endDate: String): List<DailyMilkEntryEntity>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM daily_milk_entries WHERE customerId = :customerId AND entryDate < :beforeDate")
    suspend fun sumMilkAmountBeforeDate(customerId: Long, beforeDate: String): Double

    @Query("SELECT * FROM daily_milk_entries ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<DailyMilkEntryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: DailyMilkEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(entries: List<DailyMilkEntryEntity>)

    @Update
    suspend fun update(entry: DailyMilkEntryEntity)

    @Delete
    suspend fun delete(entry: DailyMilkEntryEntity)

    @Query("DELETE FROM daily_milk_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM daily_milk_entries WHERE isSampleData = 1")
    suspend fun deleteSampleEntries()

    @Query("DELETE FROM daily_milk_entries")
    suspend fun deleteAll()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paymentDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY paymentDate DESC, createdAt DESC")
    fun observeByCustomer(customerId: Long): Flow<List<PaymentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE customerId = :customerId AND paymentDate < :beforeDate")
    suspend fun sumPaymentsBeforeDate(customerId: Long, beforeDate: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE customerId = :customerId AND paymentDate BETWEEN :startDate AND :endDate")
    suspend fun sumPaymentsInRange(customerId: Long, startDate: String, endDate: String): Double

    @Query(
        """
        SELECT * FROM payments
        WHERE customerId = :customerId
          AND paymentDate = :paymentDate
          AND ABS(amount - :amount) < 0.005
          AND paymentMethod = :method
          AND createdAt >= :minCreatedAt
        LIMIT 1
        """
    )
    suspend fun findRecentDuplicate(
        customerId: Long,
        paymentDate: String,
        amount: Double,
        method: String,
        minCreatedAt: Long
    ): PaymentEntity?

    @Query("SELECT * FROM payments WHERE customerId = :customerId AND transactionRef != '' AND LOWER(transactionRef) = LOWER(:ref) LIMIT 1")
    suspend fun findByTransactionRef(customerId: Long, ref: String): PaymentEntity?

    @Query("SELECT * FROM payments ORDER BY id ASC")
    suspend fun getAllSnapshot(): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: PaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReplace(payments: List<PaymentEntity>)

    @Delete
    suspend fun delete(payment: PaymentEntity)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM payments WHERE isSampleData = 1")
    suspend fun deleteSamplePayments()

    @Query("DELETE FROM payments")
    suspend fun deleteAll()
}

@Dao
interface MonthlyBillDao {
    @Query("SELECT * FROM monthly_bills ORDER BY billingYear DESC, billingMonth DESC, customerNameSnapshot ASC")
    fun observeAll(): Flow<List<MonthlyBillEntity>>

    @Query("SELECT * FROM monthly_bills WHERE billingYear = :year AND billingMonth = :month ORDER BY customerNameSnapshot ASC")
    fun observeByPeriod(year: Int, month: Int): Flow<List<MonthlyBillEntity>>

    @Query("SELECT * FROM monthly_bills WHERE customerId = :customerId ORDER BY billingYear DESC, billingMonth DESC")
    fun observeByCustomer(customerId: Long): Flow<List<MonthlyBillEntity>>

    @Query("SELECT * FROM monthly_bills WHERE id = :id")
    fun observeById(id: Long): Flow<MonthlyBillEntity?>

    @Query("SELECT * FROM monthly_bills WHERE id = :id")
    suspend fun getById(id: Long): MonthlyBillEntity?

    @Query("SELECT * FROM monthly_bills WHERE customerId = :customerId AND billingYear = :year AND billingMonth = :month LIMIT 1")
    suspend fun getForCustomerAndPeriod(customerId: Long, year: Int, month: Int): MonthlyBillEntity?

    @Query("SELECT * FROM bill_audits WHERE billId = :billId ORDER BY version DESC, timestamp DESC")
    fun observeAuditsForBill(billId: Long): Flow<List<BillAuditEntity>>

    @Query("SELECT * FROM monthly_bills ORDER BY id ASC")
    suspend fun getAllBillsSnapshot(): List<MonthlyBillEntity>

    @Query("SELECT * FROM bill_audits ORDER BY id ASC")
    suspend fun getAllAuditsSnapshot(): List<BillAuditEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceBill(bill: MonthlyBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBillsReplace(bills: List<MonthlyBillEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: BillAuditEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAuditsReplace(audits: List<BillAuditEntity>)

    @Query("DELETE FROM monthly_bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("DELETE FROM monthly_bills WHERE isSampleData = 1")
    suspend fun deleteSampleBills()

    @Query("DELETE FROM monthly_bills")
    suspend fun deleteAllBills()

    @Query("DELETE FROM bill_audits")
    suspend fun deleteAllAudits()
}
