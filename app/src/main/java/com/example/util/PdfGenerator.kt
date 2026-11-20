package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.MonthlyBillEntity
import com.example.data.local.MonthlyReportData
import com.example.data.preferences.ShopSettings
import com.example.data.repository.MilkManagerRepository
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 at 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    private val COLOR_BLUE = Color.parseColor("#0859CC")
    private val COLOR_BLUE_LIGHT = Color.parseColor("#E8F1FF")
    private val COLOR_GREEN = Color.parseColor("#0E9F6E")
    private val COLOR_GREEN_LIGHT = Color.parseColor("#E3F7EF")
    private val COLOR_INK = Color.parseColor("#0F1E36")
    private val COLOR_MUTED = Color.parseColor("#526580")
    private val COLOR_BORDER = Color.parseColor("#D4DFEE")
    private val COLOR_ROW_ALT = Color.parseColor("#F7FAFF")

    fun generateMonthlyBillPdfFile(
        context: Context,
        bill: MonthlyBillEntity,
        settings: ShopSettings
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = bill.customerNameSnapshot.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "MilkBill_${safeName}_${bill.billingYear}_${String.format("%02d", bill.billingMonth)}.pdf"
        val outFile = File(exportDir, fileName)
        FileOutputStream(outFile).use { stream ->
            writeMonthlyBillPdf(stream, bill, settings)
        }
        return outFile
    }

    fun writeMonthlyBillPdf(
        outputStream: OutputStream,
        bill: MonthlyBillEntity,
        settings: ShopSettings
    ) {
        val doc = PdfDocument()
        try {
            writeMonthlyBillPdfWithAndroid(doc, outputStream, bill, settings)
        } catch (e: IllegalStateException) {
            writeMinimalFallbackPdf(
                outputStream = outputStream,
                lines = listOf(
                    "${settings.shopName} - Monthly Milk Statement",
                    "Bill #: ${bill.billNumber} (v${bill.version})",
                    "Customer: ${bill.customerNameSnapshot} (${bill.customerMobileSnapshot})",
                    "Billing Month: ${DateUtils.monthYearLabel(bill.billingYear, bill.billingMonth)}",
                    "Previous Outstanding: ${bill.previousOutstanding}",
                    "Total Packets: ${bill.totalPackets}",
                    "Monthly Milk Charges: ${bill.totalMilkAmount}",
                    "Payments Received: ${bill.paymentsReceived}",
                    "Final Payable Amount: ${bill.finalPayableAmount}"
                )
            )
        }
    }

    private fun writeMinimalFallbackPdf(outputStream: OutputStream, lines: List<String>) {
        val escaped = lines.map { line ->
            line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        }
        val streamContent = buildString {
            append("BT\n/F1 12 Tf\n50 780 Td\n18 TL\n")
            escaped.forEachIndexed { idx, line ->
                if (idx == 0) append("($line) Tj\n") else append("T* ($line) Tj\n")
            }
            append("ET\n")
        }
        val streamBytes = streamContent.toByteArray(Charsets.ISO_8859_1)
        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        fun addObj(num: Int, body: String) {
            offsets.add(sb.length)
            sb.append("$num 0 obj\n$body\nendobj\n")
        }
        addObj(1, "<< /Type /Catalog /Pages 2 0 R >>")
        addObj(2, "<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
        addObj(3, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>")
        addObj(4, "<< /Length ${streamBytes.size} >>\nstream\n$streamContent\nendstream")
        addObj(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
        val xrefOffset = sb.length
        sb.append("xref\n0 ${offsets.size + 1}\n")
        sb.append("0000000000 65535 f \n")
        offsets.forEach { off ->
            sb.append(String.format("%010d 00000 n \n", off))
        }
        sb.append("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
        outputStream.write(sb.toString().toByteArray(Charsets.ISO_8859_1))
    }

    private fun writeMonthlyBillPdfWithAndroid(
        doc: PdfDocument,
        outputStream: OutputStream,
        bill: MonthlyBillEntity,
        settings: ShopSettings
    ) {
        val items = MilkManagerRepository.parseBillLineItems(bill.entriesJsonSnapshot)
        val currency = settings.currencySymbol
        val monthLabel = DateUtils.monthYearLabel(bill.billingYear, bill.billingMonth, marathi = false)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCEBFF")
            textSize = 10.5f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLUE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 10f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 9.5f
        }
        val bodyRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = COLOR_BORDER
            strokeWidth = 1f
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        // Header banner
        fillPaint.color = COLOR_BLUE
        canvas.drawRoundRect(RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 78f), 10f, 10f, fillPaint)
        canvas.drawText(settings.shopName, MARGIN + 16f, MARGIN + 28f, titlePaint)
        canvas.drawText("Monthly Milk Statement — $monthLabel", MARGIN + 16f, MARGIN + 46f, subtitlePaint)
        val shopContact = listOfNotNull(
            settings.shopAddress.takeIf { it.isNotBlank() },
            settings.shopMobile.takeIf { it.isNotBlank() }?.let { "Ph: $it" }
        ).joinToString(" | ")
        if (shopContact.isNotBlank()) {
            canvas.drawText(shopContact.take(88), MARGIN + 16f, MARGIN + 63f, subtitlePaint)
        }

        val billMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Bill #${bill.billNumber}", PAGE_WIDTH - MARGIN - 14f, MARGIN + 28f, billMetaPaint)
        billMetaPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Version: v${bill.version}", PAGE_WIDTH - MARGIN - 14f, MARGIN + 44f, billMetaPaint)
        canvas.drawText("Generated: ${DateUtils.formatShortDate(DateUtils.formatIsoFromMillis(bill.generatedAt))}", PAGE_WIDTH - MARGIN - 14f, MARGIN + 60f, billMetaPaint)

        var y = MARGIN + 96f

        // Customer info & summary box side-by-side
        val leftBox = RectF(MARGIN, y, MARGIN + 255f, y + 76f)
        val rightBox = RectF(MARGIN + 267f, y, PAGE_WIDTH - MARGIN, y + 76f)
        fillPaint.color = COLOR_ROW_ALT
        canvas.drawRoundRect(leftBox, 8f, 8f, fillPaint)
        canvas.drawRoundRect(leftBox, 8f, 8f, strokePaint)

        fillPaint.color = COLOR_GREEN_LIGHT
        canvas.drawRoundRect(rightBox, 8f, 8f, fillPaint)
        strokePaint.color = COLOR_GREEN
        canvas.drawRoundRect(rightBox, 8f, 8f, strokePaint)
        strokePaint.color = COLOR_BORDER

        canvas.drawText("BILL TO", leftBox.left + 12f, leftBox.top + 18f, sectionPaint)
        canvas.drawText(bill.customerNameSnapshot, leftBox.left + 12f, leftBox.top + 36f, valuePaint)
        canvas.drawText("Mobile: ${bill.customerMobileSnapshot}", leftBox.left + 12f, leftBox.top + 52f, labelPaint)
        canvas.drawText("Billing Month: $monthLabel", leftBox.left + 12f, leftBox.top + 66f, labelPaint)

        val payableLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GREEN
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val payableValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("FINAL PAYABLE AMOUNT", rightBox.left + 14f, rightBox.top + 20f, payableLabelPaint)
        canvas.drawText(
            CurrencyFormatter.format(bill.finalPayableAmount, currency),
            rightBox.left + 14f,
            rightBox.top + 48f,
            payableValuePaint
        )
        canvas.drawText("Status: ${bill.status}  •  Total Packets: ${bill.totalPackets}", rightBox.left + 14f, rightBox.top + 66f, labelPaint)

        y += 94f

        // Table Header helper
        fun drawTableHeader(startY: Float): Float {
            fillPaint.color = COLOR_BLUE_LIGHT
            canvas.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + 22f, fillPaint)
            canvas.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + 22f, strokePaint)
            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_BLUE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("Date", MARGIN + 8f, startY + 15f, thPaint)
            canvas.drawText("Milk Product", MARGIN + 92f, startY + 15f, thPaint)
            canvas.drawText("Packets", MARGIN + 330f, startY + 15f, thRight)
            canvas.drawText("Rate", MARGIN + 415f, startY + 15f, thRight)
            canvas.drawText("Amount", PAGE_WIDTH - MARGIN - 8f, startY + 15f, thRight)
            return startY + 22f
        }

        canvas.drawText("DATE-WISE MILK ENTRIES", MARGIN, y, sectionPaint)
        y += 10f
        y = drawTableHeader(y)

        if (items.isEmpty()) {
            canvas.drawText("No milk entries recorded in $monthLabel.", MARGIN + 10f, y + 18f, labelPaint)
            y += 28f
        } else {
            items.forEachIndexed { index, item ->
                if (y > PAGE_HEIGHT - 165f) {
                    doc.finishPage(page)
                    pageNum++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                    canvas = page.canvas
                    y = MARGIN
                    canvas.drawText("${settings.shopName} — ${bill.customerNameSnapshot} ($monthLabel) [Page $pageNum]", MARGIN, y + 12f, labelPaint)
                    y += 22f
                    y = drawTableHeader(y)
                }

                if (index % 2 == 1) {
                    fillPaint.color = COLOR_ROW_ALT
                    canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 19f, fillPaint)
                }
                canvas.drawLine(MARGIN, y + 19f, PAGE_WIDTH - MARGIN, y + 19f, strokePaint)

                canvas.drawText(DateUtils.formatDisplayDate(item.date), MARGIN + 8f, y + 13f, bodyPaint)
                canvas.drawText("${item.productName} (${item.packetSizeMl}ml)".take(34), MARGIN + 92f, y + 13f, bodyPaint)
                canvas.drawText("${item.packets}", MARGIN + 330f, y + 13f, bodyRightPaint)
                canvas.drawText(CurrencyFormatter.format(item.rate, currency), MARGIN + 415f, y + 13f, bodyRightPaint)
                canvas.drawText(CurrencyFormatter.format(item.totalAmount, currency), PAGE_WIDTH - MARGIN - 8f, y + 13f, bodyRightPaint)
                y += 19f
            }
        }

        y += 16f
        if (y > PAGE_HEIGHT - 150f) {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            canvas = page.canvas
            y = MARGIN
        }

        // Financial summary card
        val sumBox = RectF(PAGE_WIDTH - MARGIN - 260f, y, PAGE_WIDTH - MARGIN, y + 112f)
        fillPaint.color = COLOR_ROW_ALT
        canvas.drawRoundRect(sumBox, 8f, 8f, fillPaint)
        canvas.drawRoundRect(sumBox, 8f, 8f, strokePaint)

        fun drawSummaryRow(rowY: Float, label: String, value: String, bold: Boolean = false, accentColor: Int = COLOR_INK) {
            val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (bold) accentColor else COLOR_MUTED
                textSize = if (bold) 11f else 10f
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            }
            val vp = Paint(lp).apply {
                color = accentColor
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(label, sumBox.left + 12f, rowY, lp)
            canvas.drawText(value, sumBox.right - 12f, rowY, vp)
        }

        drawSummaryRow(sumBox.top + 20f, "Previous Outstanding", CurrencyFormatter.format(bill.previousOutstanding, currency))
        drawSummaryRow(sumBox.top + 38f, "$monthLabel Milk (${bill.totalPackets} pkts)", CurrencyFormatter.format(bill.totalMilkAmount, currency))
        drawSummaryRow(sumBox.top + 56f, "Payments Received", "- ${CurrencyFormatter.format(bill.paymentsReceived, currency)}", accentColor = COLOR_GREEN)
        canvas.drawLine(sumBox.left + 10f, sumBox.top + 68f, sumBox.right - 10f, sumBox.top + 68f, strokePaint)
        drawSummaryRow(sumBox.top + 90f, "Final Payable Amount", CurrencyFormatter.format(bill.finalPayableAmount, currency), bold = true, accentColor = COLOR_BLUE)

        // Footer note
        val footerY = sumBox.bottom + 24f
        canvas.drawText("Thank you for shopping with ${settings.shopName}.", MARGIN, footerY, valuePaint)
        canvas.drawText("Please pay via Cash or UPI at the store counter. Generated offline by Milk Manager.", MARGIN, footerY + 14f, labelPaint)

        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }

    fun generateMonthlyReportPdfFile(
        context: Context,
        report: MonthlyReportData,
        settings: ShopSettings
    ): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "MilkReport_${report.startDate}_to_${report.endDate}.pdf"
        val outFile = File(exportDir, fileName)
        FileOutputStream(outFile).use { stream ->
            writeMonthlyReportPdf(stream, report, settings)
        }
        return outFile
    }

    fun writeMonthlyReportPdf(
        outputStream: OutputStream,
        report: MonthlyReportData,
        settings: ShopSettings
    ) {
        val doc = PdfDocument()
        val currency = settings.currencySymbol
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = COLOR_BORDER
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCEBFF")
            textSize = 10f
        }
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BLUE
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 9.5f
        }
        val rightPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }

        // Header
        fillPaint.color = COLOR_BLUE
        canvas.drawRoundRect(RectF(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 66f), 10f, 10f, fillPaint)
        canvas.drawText("${settings.shopName} — Business Milk Report", MARGIN + 14f, MARGIN + 28f, titlePaint)
        canvas.drawText(
            "Period: ${DateUtils.formatDisplayDate(report.startDate)} to ${DateUtils.formatDisplayDate(report.endDate)}",
            MARGIN + 14f,
            MARGIN + 48f,
            subPaint
        )

        var y = MARGIN + 86f
        // KPI strip
        val kpis = listOf(
            "Total Sales" to CurrencyFormatter.format(report.totalMilkSales, currency),
            "Packets Sold" to "${report.totalPacketsSold}",
            "Collected" to CurrencyFormatter.format(report.totalPaymentsReceived, currency),
            "Pending Dues" to CurrencyFormatter.format(report.totalPendingAmount, currency)
        )
        val boxW = (PAGE_WIDTH - 2 * MARGIN - 24f) / 4f
        kpis.forEachIndexed { idx, (label, value) ->
            val left = MARGIN + idx * (boxW + 8f)
            val rect = RectF(left, y, left + boxW, y + 50f)
            fillPaint.color = COLOR_ROW_ALT
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
            canvas.drawRoundRect(rect, 8f, 8f, strokePaint)
            val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_MUTED; textSize = 8.5f }
            val vp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (idx == 2) COLOR_GREEN else COLOR_INK
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(label, left + 8f, y + 18f, lp)
            canvas.drawText(value, left + 8f, y + 38f, vp)
        }

        y += 70f
        canvas.drawText("PRODUCT-WISE SALES", MARGIN, y, secPaint)
        y += 14f
        report.productSales.forEach { prod ->
            canvas.drawText("${prod.productName} (${prod.packetSizeMl}ml)", MARGIN + 6f, y + 12f, bodyPaint)
            canvas.drawText("${prod.packets} pkts", MARGIN + 340f, y + 12f, rightPaint)
            canvas.drawText(CurrencyFormatter.format(prod.totalSales, currency), PAGE_WIDTH - MARGIN - 6f, y + 12f, rightPaint)
            canvas.drawLine(MARGIN, y + 17f, PAGE_WIDTH - MARGIN, y + 17f, strokePaint)
            y += 18f
        }

        y += 18f
        canvas.drawText("CUSTOMER-WISE SALES & OUTSTANDING", MARGIN, y, secPaint)
        y += 10f
        fillPaint.color = COLOR_BLUE_LIGHT
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, fillPaint)
        val th = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_BLUE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD }
        val thr = Paint(th).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Customer", MARGIN + 6f, y + 14f, th)
        canvas.drawText("Pkts", MARGIN + 265f, y + 14f, thr)
        canvas.drawText("Sales", MARGIN + 345f, y + 14f, thr)
        canvas.drawText("Paid", MARGIN + 430f, y + 14f, thr)
        canvas.drawText("Pending", PAGE_WIDTH - MARGIN - 6f, y + 14f, thr)
        y += 20f

        report.customerSales.forEach { row ->
            if (y > PAGE_HEIGHT - 60f) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                y = MARGIN
            }
            canvas.drawText("${row.customerName} (${row.customerCode})".take(32), MARGIN + 6f, y + 13f, bodyPaint)
            canvas.drawText("${row.packets}", MARGIN + 265f, y + 13f, rightPaint)
            canvas.drawText(CurrencyFormatter.format(row.milkSales, currency), MARGIN + 345f, y + 13f, rightPaint)
            canvas.drawText(CurrencyFormatter.format(row.paymentsReceived, currency), MARGIN + 430f, y + 13f, rightPaint)
            canvas.drawText(CurrencyFormatter.format(row.currentOutstanding, currency), PAGE_WIDTH - MARGIN - 6f, y + 13f, rightPaint)
            canvas.drawLine(MARGIN, y + 18f, PAGE_WIDTH - MARGIN, y + 18f, strokePaint)
            y += 18f
        }

        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }
}
