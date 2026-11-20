package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerWithBalance
import com.example.data.local.MonthlyBillEntity
import com.example.data.preferences.ShopSettings
import com.example.ui.components.BillStatusBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SampleDataBadge
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.WhatsAppGreen
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.PdfGenerator
import com.example.util.WhatsAppShareHelper

@Composable
fun MonthlyBillsScreen(
    bills: List<MonthlyBillEntity>,
    customers: List<CustomerWithBalance>,
    settings: ShopSettings,
    strings: AppStrings,
    onGenerateAllBills: (year: Int, month: Int) -> Unit,
    onGenerateIndividualBill: (customerId: Long, year: Int, month: Int) -> Unit,
    onOpenBillPreview: (Long) -> Unit,
    onOpenReports: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currency = settings.currencySymbol
    var selectedYear by remember { mutableIntStateOf(DateUtils.currentYear()) }
    var selectedMonth by remember { mutableIntStateOf(DateUtils.currentMonth()) }
    var selectedCustomerFilterId by remember { mutableStateOf<Long?>(null) }

    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showCustomerMenu by remember { mutableStateOf(false) }

    val years = remember {
        val curr = DateUtils.currentYear()
        (curr - 2..curr + 1).toList()
    }

    val periodBills = remember(bills, selectedYear, selectedMonth, selectedCustomerFilterId) {
        bills.filter { b ->
            b.billingYear == selectedYear &&
                b.billingMonth == selectedMonth &&
                (selectedCustomerFilterId == null || b.customerId == selectedCustomerFilterId)
        }
    }

    val totalBilled = remember(periodBills) { periodBills.sumOf { it.totalMilkAmount } }
    val totalPayable = remember(periodBills) { periodBills.sumOf { it.finalPayableAmount.coerceAtLeast(0.0) } }
    val totalPackets = remember(periodBills) { periodBills.sumOf { it.totalPackets } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.navMonthlyBills,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenReports) {
                    Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.monthlyReports)
                }
            }
        }

        // Month / Year / Customer Selector Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedButton(
                                onClick = { showMonthMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(DateUtils.monthName(selectedMonth, strings.isMarathi), maxLines = 1)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                                (1..12).forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(DateUtils.monthName(m, strings.isMarathi)) },
                                        onClick = {
                                            selectedMonth = m
                                            showMonthMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showYearMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("$selectedYear")
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                                years.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text("$y") },
                                        onClick = {
                                            selectedYear = y
                                            showYearMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Customer Selector (All or Individual)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val selectedCustName = customers.firstOrNull { it.customer.id == selectedCustomerFilterId }?.customer?.name
                        OutlinedButton(
                            onClick = { showCustomerMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedCustName ?: "All Active Customers",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = showCustomerMenu, onDismissRequest = { showCustomerMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("All Active Customers") },
                                onClick = {
                                    selectedCustomerFilterId = null
                                    showCustomerMenu = false
                                }
                            )
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.customer.name} (${c.customer.customerCode})") },
                                    onClick = {
                                        selectedCustomerFilterId = c.customer.id
                                        showCustomerMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Action Buttons: Generate All / Generate Selected
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val targetCust = selectedCustomerFilterId
                                if (targetCust != null) {
                                    onGenerateIndividualBill(targetCust, selectedYear, selectedMonth)
                                } else {
                                    onGenerateAllBills(selectedYear, selectedMonth)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DairyRoyalBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_monthly_bills_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedCustomerFilterId != null) {
                                    strings.generateIndividualBill
                                } else {
                                    "${strings.generateAllBills} (${DateUtils.monthName(selectedMonth).take(3)} $selectedYear)"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Period Summary Strip
        if (periodBills.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DairyFreshGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BillSummaryMetric("Bills", "${periodBills.size}")
                        BillSummaryMetric("Packets", "$totalPackets")
                        BillSummaryMetric("Month Milk", CurrencyFormatter.format(totalBilled, currency))
                        BillSummaryMetric("Final Due", CurrencyFormatter.format(totalPayable, currency))
                    }
                }
            }
        }

        // Bills List
        if (periodBills.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No bills generated for ${DateUtils.monthYearLabel(selectedYear, selectedMonth)}",
                    description = "Tap 'Generate All Bills' above to calculate immutable monthly statements for every active customer.",
                    primaryActionText = strings.generateAllBills,
                    onPrimaryAction = { onGenerateAllBills(selectedYear, selectedMonth) }
                )
            }
        } else {
            items(periodBills, key = { it.id }) { bill ->
                MonthlyBillCard(
                    bill = bill,
                    currency = currency,
                    strings = strings,
                    onPreview = { onOpenBillPreview(bill.id) },
                    onQuickWhatsApp = {
                        val msg = WhatsAppShareHelper.buildBillMessage(bill, settings, strings.isMarathi)
                        val note = WhatsAppShareHelper.openWhatsAppTextShare(
                            context = context,
                            phoneNumber = bill.customerWhatsappSnapshot,
                            message = msg
                        )
                        onNotify(note)
                    },
                    onQuickPdfShare = {
                        try {
                            val pdf = PdfGenerator.generateMonthlyBillPdfFile(context, bill, settings)
                            val note = WhatsAppShareHelper.shareFileAttachment(
                                context = context,
                                file = pdf,
                                mimeType = "application/pdf",
                                captionText = "Monthly Milk Bill - ${bill.customerNameSnapshot}",
                                chooserTitle = "Share Monthly Bill PDF",
                                preferWhatsApp = false
                            )
                            onNotify(note)
                        } catch (e: Exception) {
                            onNotify("PDF error: ${e.message}")
                        }
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun BillSummaryMetric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun MonthlyBillCard(
    bill: MonthlyBillEntity,
    currency: String,
    strings: AppStrings,
    onPreview: () -> Unit,
    onQuickWhatsApp: () -> Unit,
    onQuickPdfShare: () -> Unit
) {
    Card(
        onClick = onPreview,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_bill_card_${bill.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = bill.customerNameSnapshot,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (bill.isSampleData) SampleDataBadge()
                    }
                    Text(
                        text = "${bill.billNumber} • ${bill.totalPackets} pkts • v${bill.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.format(bill.finalPayableAmount, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )
                    BillStatusBadge(bill.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Prev: ${CurrencyFormatter.format(bill.previousOutstanding, currency)} + Milk: ${CurrencyFormatter.format(bill.totalMilkAmount, currency)} - Paid: ${CurrencyFormatter.format(bill.paymentsReceived, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.previewBill, maxLines = 1)
                }
                Button(
                    onClick = onQuickWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onQuickPdfShare,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "Share PDF", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
