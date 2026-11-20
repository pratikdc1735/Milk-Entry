package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomerWithBalance
import com.example.data.local.DailyMilkEntryEntity
import com.example.data.local.MilkProductEntity
import com.example.data.preferences.ShopSettings
import com.example.data.repository.MilkManagerRepository
import com.example.data.repository.OperationResult
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyFreshGreenLight
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.DairyRoyalBlueLight
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainer
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyEntryFormScreen(
    editEntryId: Long,
    preselectedCustomerId: Long,
    customers: List<CustomerWithBalance>,
    products: List<MilkProductEntity>,
    settings: ShopSettings,
    strings: AppStrings,
    loadEntryById: suspend (Long) -> DailyMilkEntryEntity?,
    loadExistingEntriesForDate: suspend (Long, String) -> List<DailyMilkEntryEntity>,
    onSaveEntry: (
        entryId: Long,
        entryDate: String,
        customerId: Long,
        productId: Long,
        packetQuantity: Int,
        pricePerPacket: Double,
        remarks: String,
        onResult: (OperationResult<Long>) -> Unit
    ) -> Unit,
    onAddNewCustomer: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val currency = settings.currencySymbol
    var currentEditId by remember { mutableStateOf(editEntryId) }
    var entryDate by remember { mutableStateOf(DateUtils.todayIso()) }
    var selectedCustomerId by remember { mutableStateOf(preselectedCustomerId) }
    var customerSearch by remember { mutableStateOf("") }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
    var packetQuantity by remember { mutableIntStateOf(1) }
    var priceText by remember { mutableStateOf(MilkManagerRepository.formatShortRate(settings.defaultMilkPrice)) }
    var remarks by remember { mutableStateOf("") }

    var existingOnDate by remember { mutableStateOf<List<DailyMilkEntryEntity>>(emptyList()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var duplicateWarning by remember { mutableStateOf<OperationResult.DuplicateWarning?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val selectedCustomer = remember(customers, selectedCustomerId) {
        customers.firstOrNull { it.customer.id == selectedCustomerId }
    }
    val selectedProduct = remember(products, selectedProductId) {
        products.firstOrNull { it.id == selectedProductId }
    }

    fun applyDefaultRateFor(customer: CustomerWithBalance?, product: MilkProductEntity?) {
        val rate = when {
            customer?.customer?.customPacketRate != null &&
                (customer.customer.defaultProductId == null || customer.customer.defaultProductId == product?.id) ->
                customer.customer.customPacketRate
            product != null -> product.pricePerPacket
            else -> settings.defaultMilkPrice
        }
        priceText = MilkManagerRepository.formatShortRate(rate)
    }

    // Load entry if editing
    LaunchedEffect(currentEditId) {
        if (currentEditId > 0L) {
            val loaded = loadEntryById(currentEditId)
            if (loaded != null) {
                entryDate = loaded.entryDate
                selectedCustomerId = loaded.customerId
                selectedProductId = loaded.productId
                packetQuantity = loaded.packetQuantity
                priceText = MilkManagerRepository.formatShortRate(loaded.pricePerPacket)
                remarks = loaded.remarks
            }
        }
    }

    // Pre-fill default customer product & rate on customer change (only for new entries)
    LaunchedEffect(selectedCustomerId, products) {
        if (currentEditId == 0L && selectedCustomerId > 0L) {
            val cust = customers.firstOrNull { it.customer.id == selectedCustomerId }
            val defaultProdId = cust?.customer?.defaultProductId ?: products.firstOrNull()?.id ?: 0L
            selectedProductId = defaultProdId
            val prod = products.firstOrNull { it.id == defaultProdId }
            applyDefaultRateFor(cust, prod)
        }
    }

    // Check existing entries whenever customer or date changes (duplicate protection!)
    LaunchedEffect(selectedCustomerId, entryDate) {
        existingOnDate = if (selectedCustomerId > 0L) {
            loadExistingEntriesForDate(selectedCustomerId, entryDate)
        } else {
            emptyList()
        }
    }

    val priceValue = priceText.toDoubleOrNull() ?: 0.0
    val calculatedTotal = MilkManagerRepository.round2(packetQuantity * priceValue)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (currentEditId > 0L) "Edit Milk Entry" else "Daily Milk Entry",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Entry Date Card (Today / Yesterday / Custom backdated date)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Entry Date",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val today = DateUtils.todayIso()
                        val yesterday = DateUtils.yesterdayIso()
                        FilterChip(
                            selected = entryDate == today,
                            onClick = { entryDate = today },
                            label = { Text(strings.today) }
                        )
                        FilterChip(
                            selected = entryDate == yesterday,
                            onClick = { entryDate = yesterday },
                            label = { Text(strings.yesterday) }
                        )
                        FilterChip(
                            selected = entryDate != today && entryDate != yesterday,
                            onClick = { showDatePicker = true },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(DateUtils.formatDisplayDate(entryDate))
                                }
                            },
                            modifier = Modifier.testTag("entry_date_picker_chip")
                        )
                    }
                }
            }

            // 2. Searchable Customer Selector
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Customer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DairyRoyalBlue
                        )
                        TextButton(onClick = onAddNewCustomer) {
                            Text("+ New Customer")
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (customerDropdownExpanded) customerSearch else (selectedCustomer?.customer?.name ?: ""),
                            onValueChange = {
                                customerSearch = it
                                customerDropdownExpanded = true
                            },
                            label = { Text("Customer Name / Mobile") },
                            placeholder = { Text("Search customer...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { customerDropdownExpanded = true }
                                .testTag("entry_customer_search_field")
                        )

                        val filtered = customers.filter { c ->
                            c.customer.isActive && (
                                customerSearch.isBlank() ||
                                    c.customer.name.contains(customerSearch, ignoreCase = true) ||
                                    c.customer.mobileNumber.contains(customerSearch, ignoreCase = true) ||
                                    c.customer.customerCode.contains(customerSearch, ignoreCase = true)
                                )
                        }

                        DropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.88f)
                        ) {
                            if (filtered.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No active customers match") },
                                    onClick = { customerDropdownExpanded = false }
                                )
                            } else {
                                filtered.take(12).forEach { cust ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = "${cust.customer.name} (${cust.customer.customerCode})",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${cust.customer.mobileNumber} • Rate: ${CurrencyFormatter.format(cust.effectiveRate, currency)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedCustomerId = cust.customer.id
                                            customerSearch = ""
                                            customerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (customers.isNotEmpty() && selectedCustomer == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            customers.filter { it.customer.isActive }.take(5).forEach { c ->
                                FilterChip(
                                    selected = false,
                                    onClick = { selectedCustomerId = c.customer.id },
                                    label = { Text(c.customer.name) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Duplicate / Existing Entry Awareness Card
            if (existingOnDate.isNotEmpty() && currentEditId == 0L) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusAmberContainer),
                    border = BorderStroke(1.dp, StatusAmber)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = StatusAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.duplicateEntryFound,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        existingOnDate.forEach { existing ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${existing.productNameSnapshot} — ${existing.packetQuantity} pkts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Total: ${CurrencyFormatter.format(existing.totalAmount, currency)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                OutlinedButton(
                                    onClick = { currentEditId = existing.id },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.editExisting)
                                }
                            }
                        }
                        val unusedProduct = products.firstOrNull { prod ->
                            existingOnDate.none { it.productId == prod.id }
                        }
                        if (unusedProduct != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    selectedProductId = unusedProduct.id
                                    applyDefaultRateFor(selectedCustomer, unusedProduct)
                                }
                            ) {
                                Text(
                                    text = "${strings.addAnotherProduct} (${unusedProduct.name})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Milk Product Selector
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Milk Product (Packet)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        products.forEach { product ->
                            val isSelected = product.id == selectedProductId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedProductId = product.id
                                    applyDefaultRateFor(selectedCustomer, product)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DairyRoyalBlueLight,
                                    selectedLabelColor = DairyRoyalBlue
                                ),
                                label = {
                                    Text(
                                        "${product.name} (${product.packetSizeMl}ml) • ${CurrencyFormatter.format(product.pricePerPacket, currency)}"
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 5. Fast Packet Quantity + Auto-filled Rate + Auto Total
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Packet Quantity & Rate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Quantity Selection Buttons (1..6)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 4, 5, 6).forEach { qty ->
                            val selected = packetQuantity == qty
                            Surface(
                                color = if (selected) DairyRoyalBlue else DairyRoyalBlueLight,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { packetQuantity = qty }
                                    .testTag("qty_quick_$qty")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$qty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else DairyRoyalBlue
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big Stepper + Editable Price
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { if (packetQuantity > 1) packetQuantity-- },
                                modifier = Modifier.testTag("qty_minus_button")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                            }
                            Text(
                                text = "$packetQuantity",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                            IconButton(
                                onClick = { packetQuantity++ },
                                modifier = Modifier.testTag("qty_plus_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                            }
                        }

                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) priceText = input
                            },
                            label = { Text("Price / Packet ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("entry_price_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-calculated Total Banner
                    Surface(
                        color = DairyFreshGreenLight,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, DairyFreshGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Amount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DairyFreshGreen
                                )
                                Text(
                                    text = "$packetQuantity pkts × ${CurrencyFormatter.format(priceValue, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = CurrencyFormatter.format(calculatedTotal, currency),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = DairyFreshGreen,
                                modifier = Modifier.testTag("entry_calculated_total")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (Optional)") },
                        placeholder = { Text("e.g., Extra packet for guests, evening delivery") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    onSaveEntry(
                        currentEditId,
                        entryDate,
                        selectedCustomerId,
                        selectedProductId,
                        packetQuantity,
                        priceValue,
                        remarks
                    ) { result ->
                        isSaving = false
                        when (result) {
                            is OperationResult.Success -> onBack()
                            is OperationResult.DuplicateWarning -> duplicateWarning = result
                            is OperationResult.Error -> Unit
                        }
                    }
                },
                enabled = !isSaving && selectedCustomerId > 0L && selectedProductId > 0L && priceValue > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = DairyFreshGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_milk_entry_button")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentEditId > 0L) "Update Milk Entry" else "Save Milk Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        IsoDatePickerDialog(
            initialIsoDate = entryDate,
            onDateSelected = { entryDate = it },
            onDismiss = { showDatePicker = false }
        )
    }

    duplicateWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = { duplicateWarning = null },
            icon = { Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = StatusAmber) },
            title = { Text("Duplicate Entry Prevented", fontWeight = FontWeight.Bold) },
            text = { Text(warning.message) },
            confirmButton = {
                Button(
                    onClick = {
                        currentEditId = warning.existingId
                        duplicateWarning = null
                    }
                ) {
                    Text("Edit Existing Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicateWarning = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
