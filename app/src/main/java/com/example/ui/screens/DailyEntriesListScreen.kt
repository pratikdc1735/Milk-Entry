package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerWithBalance
import com.example.data.local.DailyEntryWithDetails
import com.example.data.local.MilkProductEntity
import com.example.data.preferences.ShopSettings
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IsoDatePickerDialog
import com.example.ui.components.SampleDataBadge
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.DairyRoyalBlueLight
import com.example.ui.theme.StatusError
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

private enum class DateFilterMode { TODAY, YESTERDAY, SINGLE_DATE, CUSTOM_RANGE, ALL }

@Composable
fun DailyEntriesListScreen(
    entries: List<DailyEntryWithDetails>,
    customers: List<CustomerWithBalance>,
    products: List<MilkProductEntity>,
    settings: ShopSettings,
    strings: AppStrings,
    onAddEntry: () -> Unit,
    onEditEntry: (Long) -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onOpenCustomer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = settings.currencySymbol
    var dateMode by remember { mutableStateOf(DateFilterMode.TODAY) }
    var singleDate by remember { mutableStateOf(DateUtils.todayIso()) }
    var rangeStart by remember { mutableStateOf(DateUtils.daysAgoIso(6)) }
    var rangeEnd by remember { mutableStateOf(DateUtils.todayIso()) }

    var showSingleDatePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var showCustomerMenu by remember { mutableStateOf(false) }
    var showProductMenu by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<DailyEntryWithDetails?>(null) }

    val filteredEntries = remember(
        entries,
        dateMode,
        singleDate,
        rangeStart,
        rangeEnd,
        searchQuery,
        selectedCustomerId,
        selectedProductId
    ) {
        val today = DateUtils.todayIso()
        val yesterday = DateUtils.yesterdayIso()
        val effectiveStart = minOf(rangeStart, rangeEnd)
        val effectiveEnd = maxOf(rangeStart, rangeEnd)

        entries.filter { item ->
            val e = item.entry
            val dateMatches = when (dateMode) {
                DateFilterMode.TODAY -> e.entryDate == today
                DateFilterMode.YESTERDAY -> e.entryDate == yesterday
                DateFilterMode.SINGLE_DATE -> e.entryDate == singleDate
                DateFilterMode.CUSTOM_RANGE -> e.entryDate in effectiveStart..effectiveEnd
                DateFilterMode.ALL -> true
            }
            val customerMatches = selectedCustomerId == null || e.customerId == selectedCustomerId
            val productMatches = selectedProductId == null || e.productId == selectedProductId
            val queryMatches = searchQuery.isBlank() ||
                item.customerName.contains(searchQuery, ignoreCase = true) ||
                item.customerMobile.contains(searchQuery, ignoreCase = true) ||
                e.productNameSnapshot.contains(searchQuery, ignoreCase = true) ||
                e.entryDate.contains(searchQuery, ignoreCase = true)

            dateMatches && customerMatches && productMatches && queryMatches
        }
    }

    val uniqueCustomersServed = remember(filteredEntries) {
        filteredEntries.map { it.entry.customerId }.distinct().size
    }
    val totalPackets = remember(filteredEntries) {
        filteredEntries.sumOf { it.entry.packetQuantity }
    }
    val totalSales = remember(filteredEntries) {
        filteredEntries.sumOf { it.entry.totalAmount }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEntry,
                containerColor = DairyFreshGreen,
                contentColor = Color.White,
                modifier = Modifier.testTag("daily_list_add_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.addMilkEntry)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = strings.dailyRegister,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Daily Summary Strip
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DairyRoyalBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryBadgeCol(
                            label = strings.customersServed,
                            value = "$uniqueCustomersServed",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryBadgeCol(
                            label = strings.totalPackets,
                            value = "$totalPackets",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryBadgeCol(
                            label = strings.totalSales,
                            value = CurrencyFormatter.format(totalSales, currency),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search customer, product, date...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_entries_search")
                )
            }

            // Date Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dateMode == DateFilterMode.TODAY,
                        onClick = { dateMode = DateFilterMode.TODAY },
                        label = { Text(strings.today) }
                    )
                    FilterChip(
                        selected = dateMode == DateFilterMode.YESTERDAY,
                        onClick = { dateMode = DateFilterMode.YESTERDAY },
                        label = { Text(strings.yesterday) }
                    )
                    FilterChip(
                        selected = dateMode == DateFilterMode.SINGLE_DATE,
                        onClick = {
                            dateMode = DateFilterMode.SINGLE_DATE
                            showSingleDatePicker = true
                        },
                        label = {
                            Text(
                                if (dateMode == DateFilterMode.SINGLE_DATE) DateUtils.formatShortDate(singleDate)
                                else strings.selectDate
                            )
                        }
                    )
                    FilterChip(
                        selected = dateMode == DateFilterMode.CUSTOM_RANGE,
                        onClick = { dateMode = DateFilterMode.CUSTOM_RANGE },
                        label = { Text(strings.customRange) }
                    )
                    FilterChip(
                        selected = dateMode == DateFilterMode.ALL,
                        onClick = { dateMode = DateFilterMode.ALL },
                        label = { Text("All Dates") }
                    )
                }

                if (dateMode == DateFilterMode.CUSTOM_RANGE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("From: ${DateUtils.formatShortDate(rangeStart)}", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("To: ${DateUtils.formatShortDate(rangeEnd)}", maxLines = 1)
                        }
                    }
                }
            }

            // Customer & Product dropdown filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        val selectedCustName = customers.firstOrNull { it.customer.id == selectedCustomerId }?.customer?.name
                        FilterChip(
                            selected = selectedCustomerId != null,
                            onClick = { showCustomerMenu = true },
                            leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = selectedCustName ?: "All Customers",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DairyRoyalBlueLight,
                                selectedLabelColor = DairyRoyalBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = showCustomerMenu,
                            onDismissRequest = { showCustomerMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Customers") },
                                onClick = {
                                    selectedCustomerId = null
                                    showCustomerMenu = false
                                }
                            )
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.customer.name) },
                                    onClick = {
                                        selectedCustomerId = c.customer.id
                                        showCustomerMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val selectedProdName = products.firstOrNull { it.id == selectedProductId }?.name
                        FilterChip(
                            selected = selectedProductId != null,
                            onClick = { showProductMenu = true },
                            leadingIcon = { Icon(Icons.Filled.LocalDrink, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    text = selectedProdName ?: "All Products",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DairyRoyalBlueLight,
                                selectedLabelColor = DairyRoyalBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = showProductMenu,
                            onDismissRequest = { showProductMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Products") },
                                onClick = {
                                    selectedProductId = null
                                    showProductMenu = false
                                }
                            )
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.packetSizeMl}ml)") },
                                    onClick = {
                                        selectedProductId = p.id
                                        showProductMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Entries Register List
            if (filteredEntries.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.LocalDrink,
                        title = "No entries match this filter",
                        description = "Switch to All Dates or record a new daily milk entry.",
                        primaryActionText = strings.addMilkEntry,
                        onPrimaryAction = onAddEntry,
                        secondaryActionText = if (dateMode != DateFilterMode.ALL) "Show All Dates" else null,
                        onSecondaryAction = if (dateMode != DateFilterMode.ALL) {
                            { dateMode = DateFilterMode.ALL }
                        } else null
                    )
                }
            } else {
                items(filteredEntries, key = { it.entry.id }) { item ->
                    DailyRegisterEntryCard(
                        item = item,
                        currency = currency,
                        onEdit = { onEditEntry(item.entry.id) },
                        onDelete = { deleteCandidate = item },
                        onOpenCustomer = { onOpenCustomer(item.entry.customerId) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showSingleDatePicker) {
        IsoDatePickerDialog(
            initialIsoDate = singleDate,
            onDateSelected = { singleDate = it },
            onDismiss = { showSingleDatePicker = false }
        )
    }
    if (showStartDatePicker) {
        IsoDatePickerDialog(
            initialIsoDate = rangeStart,
            onDateSelected = { rangeStart = it },
            onDismiss = { showStartDatePicker = false }
        )
    }
    if (showEndDatePicker) {
        IsoDatePickerDialog(
            initialIsoDate = rangeEnd,
            onDateSelected = { rangeEnd = it },
            onDismiss = { showEndDatePicker = false }
        )
    }

    deleteCandidate?.let { candidate ->
        ConfirmDeleteDialog(
            title = "Delete Milk Entry?",
            message = "Remove ${candidate.entry.packetQuantity} pkts of ${candidate.entry.productNameSnapshot} for ${candidate.customerName} on ${DateUtils.formatDisplayDate(candidate.entry.entryDate)}?",
            onConfirm = {
                onDeleteEntry(candidate.entry.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
private fun SummaryBadgeCol(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.82f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyRegisterEntryCard(
    item: DailyEntryWithDetails,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenCustomer: () -> Unit
) {
    val e = item.entry
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (e.isSampleData) SampleDataBadge()
                    }
                    Text(
                        text = "${DateUtils.formatDisplayDate(e.entryDate)} • ${item.customerCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DairyRoyalBlue
                    )
                }
                Text(
                    text = CurrencyFormatter.format(e.totalAmount, currency),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DairyFreshGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${e.productNameSnapshot} (${e.packetSizeMlSnapshot}ml)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Packets: ${e.packetQuantity}  •  Rate: ${CurrencyFormatter.format(e.pricePerPacket, currency)}/pkt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (e.remarks.isNotBlank()) {
                        Text(
                            text = "Note: ${e.remarks}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit entry",
                            tint = DairyRoyalBlue
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete entry",
                            tint = StatusError
                        )
                    }
                }
            }
        }
    }
}
