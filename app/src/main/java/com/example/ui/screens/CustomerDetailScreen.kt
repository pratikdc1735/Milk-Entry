package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomerWithBalance
import com.example.data.local.DailyMilkEntryEntity
import com.example.data.local.LedgerEntryType
import com.example.data.local.LedgerTransaction
import com.example.data.local.MonthlyBillEntity
import com.example.data.local.PaymentEntity
import com.example.data.preferences.ShopSettings
import com.example.data.repository.MilkManagerRepository
import com.example.data.repository.OperationResult
import com.example.ui.components.BillStatusBadge
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.components.SampleDataBadge
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusError
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerWithBalance: CustomerWithBalance?,
    ledger: List<LedgerTransaction>,
    dailyHistory: List<DailyMilkEntryEntity>,
    payments: List<PaymentEntity>,
    bills: List<MonthlyBillEntity>,
    settings: ShopSettings,
    onAddMilkEntry: (Long) -> Unit,
    onEditCustomer: (Long) -> Unit,
    onDeleteCustomer: () -> Unit,
    onRecordPayment: (
        customerId: Long,
        date: String,
        amount: Double,
        method: String,
        ref: String,
        notes: String,
        allowOverride: Boolean,
        onResult: (OperationResult<Long>) -> Unit
    ) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onEditDailyEntry: (Long) -> Unit,
    onDeleteDailyEntry: (Long) -> Unit,
    onGenerateCurrentMonthBill: (Long) -> Unit,
    onOpenBillPreview: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val currency = settings.currencySymbol
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteCustomerDialog by remember { mutableStateOf(false) }
    var deletePaymentId by remember { mutableStateOf<Long?>(null) }
    var deleteEntryId by remember { mutableStateOf<Long?>(null) }

    if (customerWithBalance == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Customer") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                        }
                    }
                )
            }
        ) { pad ->
            Column(modifier = Modifier.padding(pad).padding(16.dp)) {
                Text("Customer record not found.")
            }
        }
        return
    }

    val customer = customerWithBalance.customer

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, maxLines = 1)
                            if (customer.isSampleData) SampleDataBadge()
                        }
                        Text(
                            text = "${customer.customerCode} • ${customer.mobileNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEditCustomer(customer.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit customer", tint = DairyRoyalBlue)
                    }
                    IconButton(onClick = { showDeleteCustomerDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete customer", tint = StatusError)
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
            // Financial Balance Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DairyRoyalBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Current Outstanding Balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = CurrencyFormatter.format(customerWithBalance.currentOutstanding, currency),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.testTag("customer_current_outstanding")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniStat("Opening", CurrencyFormatter.format(customer.openingBalance, currency))
                            MiniStat("Milk Total", CurrencyFormatter.format(customerWithBalance.totalMilkCharges, currency))
                            MiniStat("Paid", CurrencyFormatter.format(customerWithBalance.totalPayments, currency))
                        }
                    }
                }
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAddMilkEntry(customer.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyRoyalBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_add_milk_btn")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Milk Entry", maxLines = 1)
                    }
                    Button(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyFreshGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_record_payment_btn")
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Payment", maxLines = 1)
                    }
                }
            }

            // Info card (address, WhatsApp, rate)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Milk Plan: ${customerWithBalance.defaultProductName ?: "Standard"} • Rate: ${CurrencyFormatter.format(customerWithBalance.effectiveRate, currency)}/packet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DairyRoyalBlue
                        )
                        if (customer.whatsappNumber.isNotBlank()) {
                            Text("WhatsApp: ${customer.whatsappNumber}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (customer.address.isNotBlank()) {
                            Text("Address: ${customer.address}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (customer.notes.isNotBlank()) {
                            Text("Notes: ${customer.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Tabs: Ledger | Daily Milk | Payments | Monthly Bills
            item {
                val tabs = listOf(
                    "Ledger (${ledger.size})",
                    "Milk (${dailyHistory.size})",
                    "Payments (${payments.size})",
                    "Bills (${bills.size})"
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title, fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium) }
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    if (ledger.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.AccountBalanceWallet,
                                title = "Ledger is empty",
                                description = "Milk entries and payments will appear here with running debit/credit balances."
                            )
                        }
                    } else {
                        items(ledger, key = { it.id }) { tx ->
                            LedgerRowCard(tx = tx, currency = currency)
                        }
                    }
                }
                1 -> {
                    if (dailyHistory.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.LocalDrink,
                                title = "No milk deliveries yet",
                                description = "Tap 'Milk Entry' above to record packets for ${customer.name}."
                            )
                        }
                    } else {
                        items(dailyHistory, key = { it.id }) { entry ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(DateUtils.formatDisplayDate(entry.entryDate), style = MaterialTheme.typography.labelSmall, color = DairyRoyalBlue)
                                        Text("${entry.productNameSnapshot} — ${entry.packetQuantity} pkts", fontWeight = FontWeight.SemiBold)
                                        Text("Rate: ${CurrencyFormatter.format(entry.pricePerPacket, currency)}/pkt", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        CurrencyFormatter.format(entry.totalAmount, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DairyFreshGreen
                                    )
                                    IconButton(onClick = { onEditDailyEntry(entry.id) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = DairyRoyalBlue)
                                    }
                                    IconButton(onClick = { deleteEntryId = entry.id }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = StatusError)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    if (payments.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Payments,
                                title = "No payments recorded",
                                description = "Tap 'Payment' to record Cash, UPI, or Bank Transfer receipts."
                            )
                        }
                    } else {
                        items(payments, key = { it.id }) { pay ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${MilkManagerRepository.formatPaymentMethod(pay.paymentMethod)} • ${DateUtils.formatDisplayDate(pay.paymentDate)}",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (pay.transactionRef.isNotBlank()) {
                                            Text("Ref: ${pay.transactionRef}", style = MaterialTheme.typography.bodySmall, color = DairyRoyalBlue)
                                        }
                                        if (pay.notes.isNotBlank()) {
                                            Text(pay.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        "+ ${CurrencyFormatter.format(pay.amount, currency)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DairyFreshGreen
                                    )
                                    IconButton(onClick = { deletePaymentId = pay.id }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete payment", tint = StatusError)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    item {
                        OutlinedButton(
                            onClick = { onGenerateCurrentMonthBill(customer.id) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ReceiptLong, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate ${DateUtils.monthYearLabel(DateUtils.currentYear(), DateUtils.currentMonth())} Bill")
                        }
                    }
                    if (bills.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.ReceiptLong,
                                title = "No monthly bills generated yet",
                                description = "Generate this month's bill above or from the Monthly Bills tab."
                            )
                        }
                    } else {
                        items(bills, key = { it.id }) { bill ->
                            Card(
                                onClick = { onOpenBillPreview(bill.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            DateUtils.monthYearLabel(bill.billingYear, bill.billingMonth),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "${bill.billNumber} • ${bill.totalPackets} pkts • v${bill.version}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            CurrencyFormatter.format(bill.finalPayableAmount, currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = DairyRoyalBlue
                                        )
                                        BillStatusBadge(bill.status)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }

    if (showPaymentDialog) {
        RecordPaymentDialog(
            customerId = customer.id,
            customerName = customer.name,
            suggestedAmount = customerWithBalance.currentOutstanding.coerceAtLeast(0.0),
            currency = currency,
            onSave = onRecordPayment,
            onDismiss = { showPaymentDialog = false }
        )
    }

    if (showDeleteCustomerDialog) {
        ConfirmDeleteDialog(
            title = "Delete ${customer.name}?",
            message = "This will permanently delete this customer along with their daily milk entries, payment records, and monthly bills.",
            onConfirm = {
                showDeleteCustomerDialog = false
                onDeleteCustomer()
            },
            onDismiss = { showDeleteCustomerDialog = false }
        )
    }

    deletePaymentId?.let { payId ->
        ConfirmDeleteDialog(
            title = "Delete Payment?",
            message = "This will reverse the payment from ${customer.name}'s balance.",
            onConfirm = {
                onDeletePayment(payId)
                deletePaymentId = null
            },
            onDismiss = { deletePaymentId = null }
        )
    }

    deleteEntryId?.let { eId ->
        ConfirmDeleteDialog(
            title = "Delete Milk Entry?",
            message = "Remove this daily milk entry from ${customer.name}'s ledger?",
            onConfirm = {
                onDeleteDailyEntry(eId)
                deleteEntryId = null
            },
            onDismiss = { deleteEntryId = null }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun LedgerRowCard(tx: LedgerTransaction, currency: String) {
    val isCredit = tx.type == LedgerEntryType.PAYMENT_CREDIT
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(DateUtils.formatDisplayDate(tx.date), style = MaterialTheme.typography.labelSmall, color = DairyRoyalBlue)
                Text(tx.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(tx.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isCredit) "- ${CurrencyFormatter.format(tx.credit, currency)}"
                    else "+ ${CurrencyFormatter.format(tx.debit, currency)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) DairyFreshGreen else StatusAmber
                )
                Text(
                    text = "Bal: ${CurrencyFormatter.format(tx.runningBalance, currency)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecordPaymentDialog(
    customerId: Long,
    customerName: String,
    suggestedAmount: Double,
    currency: String,
    onSave: (
        customerId: Long,
        date: String,
        amount: Double,
        method: String,
        ref: String,
        notes: String,
        allowOverride: Boolean,
        onResult: (OperationResult<Long>) -> Unit
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var paymentDate by remember { mutableStateOf(DateUtils.todayIso()) }
    var amountText by remember {
        mutableStateOf(if (suggestedAmount > 0.0) MilkManagerRepository.formatShortRate(suggestedAmount) else "")
    }
    var method by remember { mutableStateOf("CASH") }
    var transactionRef by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var duplicateWarningMsg by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val methods = listOf(
        "CASH" to "Cash",
        "UPI" to "UPI",
        "BANK_TRANSFER" to "Bank",
        "OTHER" to "Other"
    )

    fun submit(allowOverride: Boolean) {
        val amt = amountText.toDoubleOrNull() ?: return
        if (isSaving) return
        isSaving = true
        onSave(customerId, paymentDate, amt, method, transactionRef, notes, allowOverride) { res ->
            isSaving = false
            when (res) {
                is OperationResult.Success -> onDismiss()
                is OperationResult.DuplicateWarning -> duplicateWarningMsg = res.message
                is OperationResult.Error -> Unit
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment — $customerName", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Date: ${DateUtils.formatDisplayDate(paymentDate)}")
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> if (input.all { it.isDigit() || it == '.' }) amountText = input },
                    label = { Text("Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    methods.forEach { (code, label) ->
                        FilterChip(
                            selected = method == code,
                            onClick = { method = code },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text("UPI / Transaction Ref (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                duplicateWarningMsg?.let { msg ->
                    HorizontalDivider()
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { submit(allowOverride = true) }) {
                        Text("Confirm Separate Payment Anyway")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submit(allowOverride = false) },
                enabled = !isSaving && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = DairyFreshGreen),
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text("Save Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        IsoDatePickerDialog(
            initialIsoDate = paymentDate,
            onDateSelected = { paymentDate = it },
            onDismiss = { showDatePicker = false }
        )
    }
}
