package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.preferences.ShopSettings
import com.example.ui.components.CustomerAvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SampleDataBadge
import com.example.ui.components.StatusPillBadge
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyFreshGreenLight
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainer
import com.example.util.CurrencyFormatter

private enum class CustomerFilterTab { ALL, ACTIVE, PENDING, INACTIVE }

@Composable
fun CustomersScreen(
    customers: List<CustomerWithBalance>,
    settings: ShopSettings,
    strings: AppStrings,
    onAddCustomer: () -> Unit,
    onOpenCustomerDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = settings.currencySymbol
    var query by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf(CustomerFilterTab.ALL) }

    val filtered = remember(customers, query, filterTab) {
        customers.filter { item ->
            val c = item.customer
            val matchesTab = when (filterTab) {
                CustomerFilterTab.ALL -> true
                CustomerFilterTab.ACTIVE -> c.isActive
                CustomerFilterTab.PENDING -> item.currentOutstanding > 0.01
                CustomerFilterTab.INACTIVE -> !c.isActive
            }
            val matchesQuery = query.isBlank() ||
                c.name.contains(query, ignoreCase = true) ||
                c.mobileNumber.contains(query, ignoreCase = true) ||
                c.customerCode.contains(query, ignoreCase = true) ||
                c.address.contains(query, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomer,
                containerColor = DairyRoyalBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("customers_add_fab")
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = strings.addCustomer)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.navCustomers} (${customers.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Due: ${CurrencyFormatter.format(customers.sumOf { it.currentOutstanding.coerceAtLeast(0.0) }, currency)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StatusAmber
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(strings.searchCustomerHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customers_search_input")
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterTab == CustomerFilterTab.ALL,
                        onClick = { filterTab = CustomerFilterTab.ALL },
                        label = { Text(strings.allCustomers) }
                    )
                    FilterChip(
                        selected = filterTab == CustomerFilterTab.ACTIVE,
                        onClick = { filterTab = CustomerFilterTab.ACTIVE },
                        label = { Text(strings.activeOnly) }
                    )
                    FilterChip(
                        selected = filterTab == CustomerFilterTab.PENDING,
                        onClick = { filterTab = CustomerFilterTab.PENDING },
                        label = { Text("Pending") }
                    )
                    FilterChip(
                        selected = filterTab == CustomerFilterTab.INACTIVE,
                        onClick = { filterTab = CustomerFilterTab.INACTIVE },
                        label = { Text(strings.inactiveOnly) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Groups,
                        title = if (customers.isEmpty()) "No customers added yet" else "No matching customers",
                        description = if (customers.isEmpty()) {
                            "Register your regular milk customers with their preferred milk packet and custom pricing."
                        } else {
                            "Try clearing your search or switching filter tabs."
                        },
                        primaryActionText = strings.addCustomer,
                        onPrimaryAction = onAddCustomer
                    )
                }
            } else {
                items(filtered, key = { it.customer.id }) { item ->
                    CustomerListCard(
                        item = item,
                        currency = currency,
                        onClick = { onOpenCustomerDetail(item.customer.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun CustomerListCard(
    item: CustomerWithBalance,
    currency: String,
    onClick: () -> Unit
) {
    val c = item.customer
    val isPending = item.currentOutstanding > 0.01

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_card_${c.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomerAvatarBadge(name = c.name, isActive = c.isActive)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = c.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (c.isSampleData) SampleDataBadge()
                    if (!c.isActive) {
                        StatusPillBadge(
                            text = "Inactive",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${c.customerCode} • ${c.mobileNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.defaultProductName ?: "Standard Milk"} • ${CurrencyFormatter.format(item.effectiveRate, currency)}/pkt • ${item.thisMonthPackets} pkts this month",
                    style = MaterialTheme.typography.labelSmall,
                    color = DairyRoyalBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(item.currentOutstanding, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPending) StatusAmber else DairyFreshGreen
                )
                StatusPillBadge(
                    text = if (isPending) "Due" else "Clear",
                    containerColor = if (isPending) StatusAmberContainer else DairyFreshGreenLight,
                    contentColor = if (isPending) StatusAmber else DairyFreshGreen
                )
            }
        }
    }
}
