package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MilkProductEntity
import com.example.data.preferences.ShopSettings
import com.example.data.preferences.ShopSettingsRepository
import com.example.data.repository.MilkManagerRepository
import com.example.data.repository.OperationResult
import com.example.util.BackupRestoreManager
import com.example.util.CsvExporter
import com.example.util.PdfGenerator
import com.example.util.WhatsAppShareHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MilkManagerVerificationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: MilkManagerRepository
    private lateinit var settingsRepo: ShopSettingsRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.seedDefaultProducts(db.milkProductDao())
        repo = MilkManagerRepository(db)
        settingsRepo = ShopSettingsRepository(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun verifyFullCustomerMilkBillingLedgerAndImmutabilityWorkflow() = runBlocking {
        val products = repo.productsFlow.first()
        assertTrue("Default products should be seeded", products.size >= 4)
        val fullCream = products.first { it.name == "Full Cream Milk" }
        val cowMilk = products.first { it.name == "Cow Milk" }

        // 1 & 2. Add and edit customer
        val addRes = repo.saveCustomer(
            name = "Rahul Patil",
            mobileNumber = "9823011223",
            whatsappNumber = "9823011223",
            address = "Kothrud, Pune",
            defaultProductId = fullCream.id,
            customPacketRate = 30.0,
            isActive = true,
            openingBalance = 200.0,
            notes = "Morning delivery"
        )
        assertTrue(addRes is OperationResult.Success)
        val rahulId = (addRes as OperationResult.Success).data

        val editRes = repo.saveCustomer(
            id = rahulId,
            name = "Rahul Patil",
            mobileNumber = "9823011223",
            whatsappNumber = "9823011223",
            address = "Flat 302, Kothrud, Pune",
            defaultProductId = fullCream.id,
            customPacketRate = 30.0,
            isActive = true,
            openingBalance = 200.0,
            notes = "Updated address"
        )
        assertTrue(editRes is OperationResult.Success)

        // 3 & 4. Add daily entries in October 2026 & verify calculation (2 * 30 = 60)
        val e1 = repo.saveDailyMilkEntry(
            entryDate = "2026-10-05",
            customerId = rahulId,
            productId = fullCream.id,
            packetQuantity = 2,
            pricePerPacket = 30.0,
            remarks = ""
        )
        assertTrue(e1 is OperationResult.Success)
        val savedEntry1 = repo.getDailyEntryById((e1 as OperationResult.Success).data)!!
        assertEquals(60.0, savedEntry1.totalAmount, 0.01)

        // 6. Prevent duplicate entry for same customer + date + product
        val dupEntry = repo.saveDailyMilkEntry(
            entryDate = "2026-10-05",
            customerId = rahulId,
            productId = fullCream.id,
            packetQuantity = 3,
            pricePerPacket = 30.0,
            remarks = "Accidental duplicate"
        )
        assertTrue("Duplicate entry must be prevented", dupEntry is OperationResult.DuplicateWarning)

        // Allow second product for same customer on the same date!
        val secondProdSameDay = repo.saveDailyMilkEntry(
            entryDate = "2026-10-05",
            customerId = rahulId,
            productId = cowMilk.id,
            packetQuantity = 1,
            pricePerPacket = 28.0,
            remarks = "Extra cow milk"
        )
        assertTrue(secondProdSameDay is OperationResult.Success)

        // 5. Edit previous entry (change 2 pkts -> 4 pkts = 120)
        val editEntry = repo.saveDailyMilkEntry(
            entryId = savedEntry1.id,
            entryDate = "2026-10-05",
            customerId = rahulId,
            productId = fullCream.id,
            packetQuantity = 4,
            pricePerPacket = 30.0,
            remarks = "Corrected"
        )
        assertTrue(editEntry is OperationResult.Success)
        assertEquals(120.0, repo.getDailyEntryById(savedEntry1.id)!!.totalAmount, 0.01)

        // Verify historical price preservation when current product price increases
        repo.saveProduct(fullCream.copy(pricePerPacket = 45.0))
        assertEquals(30.0, repo.getDailyEntryById(savedEntry1.id)!!.pricePerPacket, 0.01)
        assertEquals(120.0, repo.getDailyEntryById(savedEntry1.id)!!.totalAmount, 0.01)

        // 7. Record payment + prevent accidental duplicate payment
        val pay1 = repo.recordPayment(
            customerId = rahulId,
            paymentDate = "2026-10-20",
            amount = 100.0,
            paymentMethod = "UPI",
            transactionRef = "UPI12345",
            notes = "Part payment"
        )
        assertTrue(pay1 is OperationResult.Success)

        val dupPay = repo.recordPayment(
            customerId = rahulId,
            paymentDate = "2026-10-20",
            amount = 100.0,
            paymentMethod = "UPI",
            transactionRef = "UPI12345",
            notes = "Double tap"
        )
        assertTrue("Duplicate payment must be blocked", dupPay is OperationResult.DuplicateWarning)

        // 8. Verify outstanding balance: Opening(200) + Milk(120 + 28 = 148) - Paid(100) = 248
        val custWithBal = repo.customersWithBalanceFlow.first().first { it.customer.id == rahulId }
        assertEquals(248.0, custWithBal.currentOutstanding, 0.01)

        // 9. Generate October 2026 bill
        val octBillRes = repo.generateMonthlyBill(rahulId, 2026, 10)
        assertTrue(octBillRes is OperationResult.Success)
        val octBill = (octBillRes as OperationResult.Success).data
        assertEquals(200.0, octBill.previousOutstanding, 0.01)
        assertEquals(148.0, octBill.totalMilkAmount, 0.01)
        assertEquals(5, octBill.totalPackets)
        assertEquals(100.0, octBill.paymentsReceived, 0.01)
        assertEquals(248.0, octBill.finalPayableAmount, 0.01)

        // 16. Add November entry & payment, verify October bill remains UNCHANGED
        repo.saveDailyMilkEntry(
            entryDate = "2026-11-02",
            customerId = rahulId,
            productId = fullCream.id,
            packetQuantity = 2,
            pricePerPacket = 45.0,
            remarks = ""
        )
        repo.recordPayment(
            customerId = rahulId,
            paymentDate = "2026-11-03",
            amount = 50.0,
            paymentMethod = "CASH",
            transactionRef = "",
            notes = ""
        )

        val octBillAgain = repo.generateMonthlyBill(rahulId, 2026, 10, forceRegenerate = false)
        assertEquals(248.0, (octBillAgain as OperationResult.Success).data.finalPayableAmount, 0.01)
        assertEquals(1, octBillAgain.data.version)

        // Generate November 2026 bill: Prev(248) + NovMilk(90) - NovPaid(50) = 288
        val novBillRes = repo.generateMonthlyBill(rahulId, 2026, 11)
        val novBill = (novBillRes as OperationResult.Success).data
        assertEquals(248.0, novBill.previousOutstanding, 0.01)
        assertEquals(90.0, novBill.totalMilkAmount, 0.01)
        assertEquals(50.0, novBill.paymentsReceived, 0.01)
        assertEquals(288.0, novBill.finalPayableAmount, 0.01)

        // 10. Generate all customer bills
        val bulkRes = repo.generateAllMonthlyBills(2026, 11, regenerateExisting = false)
        assertTrue(bulkRes is OperationResult.Success)

        // 11. WhatsApp message formatting
        val shopSettings = ShopSettings(shopName = "Snehal Super Market")
        val waText = WhatsAppShareHelper.buildBillMessage(novBill, shopSettings, useMarathi = false)
        assertTrue(waText.contains("*Snehal Super Market*"))
        assertTrue(waText.contains("Rahul Patil"))
        assertTrue(waText.contains("288"))

        // 12. Export PDF & CSV
        val pdfOut = ByteArrayOutputStream()
        PdfGenerator.writeMonthlyBillPdf(pdfOut, novBill, shopSettings)
        assertTrue("PDF output must be non-empty", pdfOut.size() > 500)

        val report = repo.computeReportData(
            customers = repo.customersWithBalanceFlow.first(),
            entries = repo.entriesWithDetailsFlow.first(),
            payments = repo.allPaymentsFlow.first(),
            startDate = "2026-11-01",
            endDate = "2026-11-30",
            customerFilterId = null
        )
        val csv = CsvExporter.buildMonthlyReportCsv(report, shopSettings)
        assertTrue(csv.contains("Rahul Patil"))
        assertTrue(csv.contains("Full Cream Milk"))

        // 13. Backup & Restore preserves all records cleanly
        val (backupJson, backupSummary) = BackupRestoreManager.exportBackupJson(db, settingsRepo)
        assertEquals(1, backupSummary.customersCount)
        assertEquals(3, backupSummary.entriesCount)
        assertEquals(2, backupSummary.billsCount)

        val restoredSummary = BackupRestoreManager.restoreFromBackupJson(backupJson, db, settingsRepo)
        assertEquals(1, restoredSummary.customersCount)
        assertEquals(3, restoredSummary.entriesCount)
        assertEquals(2, restoredSummary.billsCount)
    }

    @Test
    fun verifySampleDataRemainsSeparateFromRealCustomers() = runBlocking {
        val realRes = repo.saveCustomer(
            name = "Real Customer",
            mobileNumber = "9000000001",
            whatsappNumber = "9000000001",
            address = "Market Road",
            defaultProductId = null,
            customPacketRate = 30.0,
            isActive = true,
            openingBalance = 0.0,
            notes = ""
        )
        assertTrue(realRes is OperationResult.Success)

        repo.loadSampleDemoData()
        val withDemo = repo.customersWithBalanceFlow.first()
        assertEquals(5, withDemo.size)
        assertEquals(4, withDemo.count { it.customer.isSampleData })

        repo.clearSampleDemoData()
        val afterClear = repo.customersWithBalanceFlow.first()
        assertEquals(1, afterClear.size)
        assertEquals("Real Customer", afterClear.first().customer.name)
    }
}
