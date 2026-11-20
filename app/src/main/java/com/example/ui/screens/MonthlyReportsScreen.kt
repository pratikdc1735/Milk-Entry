package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomerWithBalance
import com.example.data.local.MonthlyReportData
import com.example.data.preferences.ShopSettings
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.components.MetricKpiCard
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyFreshGreenLight
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.DairyRoyalBlueLight
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainer
import com.example.util.CsvExporter
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.PdfGenerator
import com.example.util.WhatsAppShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportsScreen(
    customers: List<CustomerWithBalance>,
    settings: ShopSettings,
    strings: AppStrings,
    buildReport: (startDate: String, endDate: String, customerFilterId: Long?) -> MonthlyReportData,
    onNotify: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currency = settings.currencySymbol

    var useCustomRange by remember { mutableStateOf(false) }
    var selectedYear by remember { mutableIntStateOf(DateUtils.currentYear()) }
    var selectedMonth by remember { mutableIntStateOf(DateUtils.currentMonth()) }
    var customStart by remember { mutableStateOf(DateUtils.monthStartIso(DateUtils.currentYear(), DateUtils.currentMonth())) }
    var customEnd by remember { mutableStateOf(DateUtils.todayIso()) }
    var customerFilterId by remember { mutableStateOf<Long?>(null) }

    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showCustomerMenu by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val startDate = if (useCustomRange) minOf(customStart, customEnd) else DateUtils.monthStartIso(selectedYear, selectedMonth)
    val endDate = if (useCustomRange) maxOf(customStart, customEnd) else DateUtils.monthEndIso(selectedYear, selectedMonth)

    val report = buildReport(startDate, endDate, customerFilterId)

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfGenerator.writeMonthlyReportPdf(out, report, settings)
                }
                onNotify("Report PDF saved successfully")
            } catch (e: Exception) {
                onNotify("Export PDF failed: ${e.message}")
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = CsvExporter.buildMonthlyReportCsv(report, settings)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    CsvExporter.writeCsvToStream(out, csv)
                }
                onNotify("Excel CSV report saved successfully")
            } catch (e: Exception) {
                onNotify("Export CSV failed: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.monthlyReports, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filters Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !useCustomRange,
                                onClick = { useCustomRange = false },
                                label = { Text("Month / Year") }
                            )
                            FilterChip(
                                selected = useCustomRange,
                                onClick = { useCustomRange = true },
                                label = { Text("Custom Range") }
                            )
                            FilterChip(
                                selected = useCustomRange && customStart == DateUtils.todayIso() && customEnd == DateUtils.todayIso(),
                                onClick = {
                                    useCustomRange = true
                                    val t = DateUtils.todayIso()
                                    customStart = t
                                    customEnd = t
                                },
                                label = { Text("Today") }
                            )
                        }

                        if (!useCustomRange) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1.2f)) {
                                    OutlinedButton(onClick = { showMonthMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text(DateUtils.monthName(selectedMonth, strings.isMarathi), maxLines = 1)
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                                        (1..12).forEach { m ->
                                            DropdownMenuItem(
                                                text = { Text(DateUtils.monthName(m, strings.isMarathi)) },
                                                onClick = { selectedMonth = m; showMonthMenu = false }
                                            )
                                        }
                                    }
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(onClick = { showYearMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                        Text("$selectedYear")
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                                        val curr = DateUtils.currentYear()
                                        (curr - 2..curr + 1).forEach { y ->
                                            DropdownMenuItem(
                                                text = { Text("$y") },
                                                onClick = { selectedYear = y; showYearMenu = false }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                                    Text("From: ${DateUtils.formatShortDate(customStart)}", maxLines = 1)
                                }
                                OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                                    Text("To: ${DateUtils.formatShortDate(customEnd)}", maxLines = 1)
                                }
                            }
                        }

                        // Customer filter
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val selectedName = customers.firstOrNull { it.customer.id == customerFilterId }?.customer?.name
                            OutlinedButton(onClick = { showCustomerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedName ?: "All Customers", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = showCustomerMenu, onDismissRequest = { showCustomerMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("All Customers") },
                                    onClick = { customerFilterId = null; showCustomerMenu = false }
                                )
                                customers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.customer.name) },
                                        onClick = { customerFilterId = c.customer.id; showCustomerMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Export Actions (Save PDF, Save CSV, Share Report PDF)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { pdfExportLauncher.launch("Snehal_Milk_Report_${startDate}_to_${endDate}.pdf") },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyRoyalBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_report_pdf_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF", maxLines = 1)
                    }
                    Button(
                        onClick = { csvExportLauncher.launch("Snehal_Milk_Report_${startDate}_to_${endDate}.csv") },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyFreshGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_report_csv_btn")
                    ) {
                        Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val file = PdfGenerator.generateMonthlyReportPdfFile(context, report, settings)
                                val note = WhatsAppShareHelper.shareFileAttachment(
                                    context = context,
                                    file = file,
                                    mimeType = "application/pdf",
                                    chooserTitle = "Share Business Milk Report"
                                )
                                onNotify(note)
                            } catch (e: Exception) {
                                onNotify("Share failed: ${e.message}")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Report", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Summary KPI Cards
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricKpiCard(
                        title = "Total Milk Sales",
                        value = CurrencyFormatter.format(report.totalMilkSales, currency),
                        icon = Icons.Filled.CurrencyRupee,
                        accentColor = DairyRoyalBlue,
                        iconBackground = DairyRoyalBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                    MetricKpiCard(
                        title = "Packets Sold",
                        value = "${report.totalPacketsSold}",
                        icon = Icons.Filled.LocalDrink,
                        accentColor = DairyFreshGreen,
                        iconBackground = DairyFreshGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricKpiCard(
                        title = "Payments Received",
                        value = CurrencyFormatter.format(report.totalPaymentsReceived, currency),
                        icon = Icons.Filled.Groups,
                        accentColor = DairyFreshGreen,
                        iconBackground = DairyFreshGreenLight,
                        subtitle = "${report.activeCustomersCount} Active",
                        modifier = Modifier.weight(1f)
                    )
                    MetricKpiCard(
                        title = "Total Pending",
                        value = CurrencyFormatter.format(report.totalPendingAmount, currency),
                        icon = Icons.Filled.WarningAmber,
                        accentColor = StatusAmber,
                        iconBackground = StatusAmberContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Product-Wise Sales Breakdown
            item {
                Text(
                    text = "Product-Wise Sales",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (report.productSales.isEmpty()) {
                item {
                    Text(
                        text = "No product sales in this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxProdSales = report.productSales.maxOfOrNull { it.totalSales }?.coerceAtLeast(1.0) ?: 1.0
                items(report.productSales) { prod ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${prod.productName} (${prod.packetSizeMl}ml)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${prod.packets} pkts • ${CurrencyFormatter.format(prod.totalSales, currency)}",
                                    fontWeight = FontWeight.Bold,
                                    color = DairyRoyalBlue
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (prod.totalSales / maxProdSales).toFloat().coerceIn(0f, 1f) },
                                color = DairyFreshGreen,
                                trackColor = DairyFreshGreenLight,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }

            // Customer-Wise Sales Table
            item {
                Text(
                    text = "Customer-Wise Sales (${report.customerSales.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(report.customerSales, key = { it.customerId }) { row ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.customerName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${row.packets} pkts • Paid: ${CurrencyFormatter.format(row.paymentsReceived, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                CurrencyFormatter.format(row.milkSales, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DairyRoyalBlue
                            )
                            Text(
                                "Due: ${CurrencyFormatter.format(row.currentOutstanding, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (row.currentOutstanding > 0.01) StatusAmber else DairyFreshGreen
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }

    if (showStartPicker) {
        IsoDatePickerDialog(
            initialIsoDate = customStart,
            onDateSelected = { customStart = it },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        IsoDatePickerDialog(
            initialIsoDate = customEnd,
            onDateSelected = { customEnd = it },
            onDismiss = { showEndPicker = false }
        )
    }
}
