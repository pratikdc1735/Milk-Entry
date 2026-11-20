package com.example.util

import android.content.Context
import com.example.data.local.DailyEntryWithDetails
import com.example.data.local.MonthlyReportData
import com.example.data.preferences.ShopSettings
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object CsvExporter {

    fun buildMonthlyReportCsv(report: MonthlyReportData, settings: ShopSettings): String {
        val sb = StringBuilder()
        // UTF-8 BOM for clean Excel compatibility with ₹ symbol and Marathi characters
        sb.append('\uFEFF')
        sb.appendLine("${escape(settings.shopName)},Monthly Business Milk Report")
        sb.appendLine("Period,${report.startDate} to ${report.endDate}")
        sb.appendLine()
        sb.appendLine("Summary Metric,Value")
        sb.appendLine("Total Milk Sales,${report.totalMilkSales}")
        sb.appendLine("Total Packets Sold,${report.totalPacketsSold}")
        sb.appendLine("Active Customers,${report.activeCustomersCount}")
        sb.appendLine("Total Payments Received,${report.totalPaymentsReceived}")
        sb.appendLine("Total Pending Amount,${report.totalPendingAmount}")
        sb.appendLine()
        sb.appendLine("Product-Wise Sales")
        sb.appendLine("Product Name,Packet Size (ml),Packets Sold,Total Sales")
        report.productSales.forEach { prod ->
            sb.appendLine("${escape(prod.productName)},${prod.packetSizeMl},${prod.packets},${prod.totalSales}")
        }
        sb.appendLine()
        sb.appendLine("Customer-Wise Sales")
        sb.appendLine("Customer Code,Customer Name,Packets Sold,Milk Sales,Payments Received,Current Outstanding")
        report.customerSales.forEach { row ->
            sb.appendLine(
                "${escape(row.customerCode)},${escape(row.customerName)},${row.packets},${row.milkSales},${row.paymentsReceived},${row.currentOutstanding}"
            )
        }
        return sb.toString()
    }

    fun buildDailyRegisterCsv(entries: List<DailyEntryWithDetails>, settings: ShopSettings): String {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine("${escape(settings.shopName)},Daily Milk Register")
        sb.appendLine("Date,Customer Code,Customer Name,Product,Size (ml),Packets,Rate,Total Amount,Remarks")
        entries.forEach { item ->
            val e = item.entry
            sb.appendLine(
                "${e.entryDate},${escape(item.customerCode)},${escape(item.customerName)}," +
                    "${escape(e.productNameSnapshot)},${e.packetSizeMlSnapshot},${e.packetQuantity}," +
                    "${e.pricePerPacket},${e.totalAmount},${escape(e.remarks)}"
            )
        }
        return sb.toString()
    }

    fun writeReportCsvFile(context: Context, report: MonthlyReportData, settings: ShopSettings): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "Snehal_Milk_Report_${report.startDate}_${report.endDate}.csv")
        FileOutputStream(file).use { out ->
            out.write(buildMonthlyReportCsv(report, settings).toByteArray(Charsets.UTF_8))
        }
        return file
    }

    fun writeCsvToStream(outputStream: OutputStream, csvContent: String) {
        outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
    }

    private fun escape(text: String): String {
        val clean = text.replace("\"", "\"\"")
        return if (clean.contains(',') || clean.contains('"') || clean.contains('\n')) "\"$clean\"" else clean
    }
}
