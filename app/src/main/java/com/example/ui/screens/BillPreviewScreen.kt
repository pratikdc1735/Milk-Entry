package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.BillAuditEntity
import com.example.data.local.MonthlyBillEntity
import com.example.data.preferences.ShopSettings
import com.example.data.repository.MilkManagerRepository
import com.example.ui.components.BillStatusBadge
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyFreshGreenLight
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.WhatsAppGreen
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.util.PdfGenerator
import com.example.util.PrintHelperUtil
import com.example.util.WhatsAppShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPreviewScreen(
    bill: MonthlyBillEntity?,
    audits: List<BillAuditEntity>,
    settings: ShopSettings,
    strings: AppStrings,
    onRegenerateBill: (customerId: Long, year: Int, month: Int, reason: String) -> Unit,
    onNotify: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currency = settings.currencySymbol

    if (bill == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Bill Statement") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                        }
                    }
                )
            }
        ) { pad ->
            Column(modifier = Modifier.padding(pad).padding(16.dp)) {
                Text("Monthly bill not found.")
            }
        }
        return
    }

    val lineItems = remember(bill.entriesJsonSnapshot) {
        MilkManagerRepository.parseBillLineItems(bill.entriesJsonSnapshot)
    }

    var messageUseMarathi by remember { mutableStateOf(settings.languageCode == "mr") }
    var editableMessage by remember(bill, messageUseMarathi) {
        mutableStateOf(WhatsAppShareHelper.buildBillMessage(bill, settings, messageUseMarathi))
    }
    var showRegenerateDialog by remember { mutableStateOf(false) }
    var showAuditDialog by remember { mutableStateOf(false) }
    var regenerateReason by remember { mutableStateOf("") }

    LaunchedEffect(bill.id, bill.version, messageUseMarathi) {
        editableMessage = WhatsAppShareHelper.buildBillMessage(bill, settings, messageUseMarathi)
    }

    val pdfDownloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfGenerator.writeMonthlyBillPdf(out, bill, settings)
                }
                onNotify("PDF bill saved successfully")
            } catch (e: Exception) {
                onNotify("Failed to save PDF: ${e.message}")
            }
        }
    }

    val monthYear = DateUtils.monthYearLabel(bill.billingYear, bill.billingMonth)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Monthly Statement", fontWeight = FontWeight.Bold)
                        Text("${bill.billNumber} • v${bill.version}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAuditDialog = true }) {
                        Icon(Icons.Filled.History, contentDescription = "Bill Audit History")
                    }
                    IconButton(onClick = { showRegenerateDialog = true }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Regenerate Bill")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Printable Statement Preview Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        // Blue Branded Header
                        Surface(color = DairyRoyalBlue, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = settings.shopName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    BillStatusBadge(bill.status)
                                }
                                Text(
                                    text = "Monthly Milk Bill — $monthYear",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                                if (settings.shopAddress.isNotBlank()) {
                                    Text(
                                        text = settings.shopAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Customer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(bill.customerNameSnapshot, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Mobile: ${bill.customerMobileSnapshot}", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Bill Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(bill.billNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = DairyRoyalBlue)
                                    Text("Snapshot v${bill.version}", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            HorizontalDivider()

                            BillSummaryLine("Opening Balance (Account)", CurrencyFormatter.format(bill.openingBalance, currency))
                            BillSummaryLine("Previous Outstanding", CurrencyFormatter.format(bill.previousOutstanding, currency))
                            BillSummaryLine("$monthYear Milk (${bill.totalPackets} packets)", CurrencyFormatter.format(bill.totalMilkAmount, currency))
                            BillSummaryLine("Payments Received", "- ${CurrencyFormatter.format(bill.paymentsReceived, currency)}", valueColor = DairyFreshGreen)

                            HorizontalDivider()

                            Surface(
                                color = DairyFreshGreenLight,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.finalPayable,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DairyFreshGreen
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(bill.finalPayableAmount, currency),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DairyFreshGreen,
                                        modifier = Modifier.testTag("bill_final_payable_text")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PDF / Print / Share Action Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val safeName = bill.customerNameSnapshot.replace(Regex("[^A-Za-z0-9_-]"), "_")
                            pdfDownloadLauncher.launch("MilkBill_${safeName}_${bill.billingYear}_${bill.billingMonth}.pdf")
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bill_download_pdf_btn")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                val file = PdfGenerator.generateMonthlyBillPdfFile(context, bill, settings)
                                val started = PrintHelperUtil.printPdfFile(context, file, "Milk Bill ${bill.billNumber}")
                                onNotify(if (started) "Opened Android Print Preview" else "Printing not supported on this device")
                            } catch (e: Exception) {
                                onNotify("Print failed: ${e.message}")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bill_print_btn")
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print", maxLines = 1)
                    }
                }
            }

            // WhatsApp & Share Composer Card (Editable message + Copy + Direct WA + Share PDF + Share Sheet)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WhatsApp Bill Message",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WhatsAppGreen
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = !messageUseMarathi,
                                    onClick = { messageUseMarathi = false },
                                    label = { Text("EN") }
                                )
                                FilterChip(
                                    selected = messageUseMarathi,
                                    onClick = { messageUseMarathi = true },
                                    label = { Text("मराठी") }
                                )
                            }
                        }

                        Text(
                            text = if (bill.customerWhatsappSnapshot.isNotBlank()) {
                                "WhatsApp No: ${bill.customerWhatsappSnapshot}"
                            } else {
                                "No WhatsApp number saved — you can pick a contact in WhatsApp or copy the message."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = editableMessage,
                            onValueChange = { editableMessage = it },
                            label = { Text("Edit Message Before Sharing") },
                            minLines = 7,
                            maxLines = 14,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("whatsapp_message_editor")
                        )

                        Button(
                            onClick = {
                                val note = WhatsAppShareHelper.openWhatsAppTextShare(
                                    context = context,
                                    phoneNumber = bill.customerWhatsappSnapshot,
                                    message = editableMessage
                                )
                                onNotify(note)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("bill_share_whatsapp_btn")
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.shareWhatsApp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val file = PdfGenerator.generateMonthlyBillPdfFile(context, bill, settings)
                                        val note = WhatsAppShareHelper.shareFileAttachment(
                                            context = context,
                                            file = file,
                                            mimeType = "application/pdf",
                                            captionText = editableMessage,
                                            chooserTitle = "Share Monthly Bill PDF",
                                            preferWhatsApp = true
                                        )
                                        onNotify(note)
                                    } catch (e: Exception) {
                                        onNotify("PDF share failed: ${e.message}")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share PDF", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = {
                                    val note = WhatsAppShareHelper.openGenericTextShare(
                                        context = context,
                                        message = editableMessage,
                                        title = "Monthly Milk Bill — ${bill.customerNameSnapshot}"
                                    )
                                    onNotify(note)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share Menu", maxLines = 1)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                WhatsAppShareHelper.copyToClipboard(context, "Milk Bill", editableMessage)
                                onNotify("Bill message copied to clipboard")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bill_copy_message_btn")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.copyMessage)
                        }
                    }
                }
            }

            // Date-Wise Entries Snapshot Table
            item {
                Text(
                    text = "Date-Wise Milk Entries (${lineItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (lineItems.isEmpty()) {
                item {
                    Text(
                        text = "No milk packet entries were recorded during $monthYear.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(lineItems) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
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
                                Text(DateUtils.formatDisplayDate(item.date), style = MaterialTheme.typography.labelSmall, color = DairyRoyalBlue)
                                Text("${item.productName} (${item.packetSizeMl}ml)", fontWeight = FontWeight.SemiBold)
                                Text("${item.packets} pkts × ${CurrencyFormatter.format(item.rate, currency)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                CurrencyFormatter.format(item.totalAmount, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DairyFreshGreen
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showRegenerateDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateDialog = false },
            title = { Text("Regenerate Bill (v${bill.version + 1})?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Previous generated bills are kept immutable automatically. Regenerate only if you corrected a past entry or payment and want an updated audited snapshot."
                    )
                    OutlinedTextField(
                        value = regenerateReason,
                        onValueChange = { regenerateReason = it },
                        label = { Text("Audit Reason (Optional)") },
                        placeholder = { Text("e.g., Added missed 15th delivery") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRegenerateBill(bill.customerId, bill.billingYear, bill.billingMonth, regenerateReason)
                        regenerateReason = ""
                        showRegenerateDialog = false
                    }
                ) {
                    Text("Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAuditDialog) {
        AlertDialog(
            onDismissRequest = { showAuditDialog = false },
            title = { Text("Bill Audit History", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (audits.isEmpty()) {
                        Text("No audit history records.")
                    } else {
                        audits.forEach { audit ->
                            Column {
                                Text(
                                    text = "Version v${audit.version} • ${DateUtils.formatTimestamp(audit.timestamp)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DairyRoyalBlue
                                )
                                Text(
                                    text = "Payable: ${CurrencyFormatter.format(audit.previousFinalPayable, currency)} → ${CurrencyFormatter.format(audit.newFinalPayable, currency)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = audit.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAuditDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun BillSummaryLine(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
