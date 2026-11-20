package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DailyEntryWithDetails
import com.example.data.local.DashboardSummary
import com.example.data.preferences.ShopSettings
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MetricKpiCard
import com.example.ui.components.SampleDataBadge
import com.example.ui.components.SectionHeaderRow
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyFreshGreenDark
import com.example.ui.theme.DairyFreshGreenLight
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.DairyRoyalBlueDark
import com.example.ui.theme.DairyRoyalBlueLight
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberContainer
import com.example.ui.theme.WhatsAppGreen
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    settings: ShopSettings,
    strings: AppStrings,
    onAddMilkEntry: () -> Unit,
    onAddCustomer: () -> Unit,
    onGenerateMonthlyBills: () -> Unit,
    onViewReports: () -> Unit,
    onShareStatements: () -> Unit,
    onManageProducts: () -> Unit,
    onOpenDailyRegister: () -> Unit,
    onEditEntry: (Long) -> Unit,
    onOpenCustomer: (Long) -> Unit,
    onLoadSampleData: () -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = settings.currencySymbol

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Branded Hero Banner Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_dairy_hero_banner),
                        contentDescription = stringResource(R.string.cd_shop_banner),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        DairyRoyalBlueDark.copy(alpha = 0.90f),
                                        DairyRoyalBlue.copy(alpha = 0.78f),
                                        DairyFreshGreenDark.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = settings.shopName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = DateUtils.formatFullTodayDisplay(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onToggleLanguage)
                                    .testTag("dashboard_language_toggle")
                            ) {
                                Text(
                                    text = if (strings.isMarathi) "EN" else "मराठी",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onAddMilkEntry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DairyFreshGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_add_milk_entry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.addMilkEntry,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onManageProducts)
                                    .testTag("dashboard_manage_products_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Inventory2,
                                        contentDescription = strings.milkProducts,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (strings.isMarathi) "दर" else "Rates",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Overview Grid
        item {
            SectionHeaderRow(
                title = strings.todaysOverview,
                actionText = strings.viewTodaysReport,
                onActionClick = onViewReports
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricKpiCard(
                    title = strings.todayPackets,
                    value = "${summary.todayTotalPackets} ${strings.packetShort}",
                    icon = Icons.Filled.LocalDrink,
                    accentColor = DairyRoyalBlue,
                    iconBackground = DairyRoyalBlueLight,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenDailyRegister
                )
                MetricKpiCard(
                    title = strings.todaySales,
                    value = CurrencyFormatter.format(summary.todayTotalSales, currency),
                    icon = Icons.Filled.CurrencyRupee,
                    accentColor = DairyFreshGreen,
                    iconBackground = DairyFreshGreenLight,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenDailyRegister
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricKpiCard(
                    title = strings.activeCustomers,
                    value = "${summary.activeCustomersCount}",
                    icon = Icons.Filled.Groups,
                    accentColor = DairyRoyalBlue,
                    iconBackground = DairyRoyalBlueLight,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = strings.pendingPayments,
                    value = CurrencyFormatter.format(summary.totalPendingPayments, currency),
                    icon = Icons.Filled.WarningAmber,
                    accentColor = StatusAmber,
                    iconBackground = StatusAmberContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onGenerateMonthlyBills
                )
            }
        }

        // This Month's Financial Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = DairyRoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = DateUtils.monthYearLabel(
                                    DateUtils.currentYear(),
                                    DateUtils.currentMonth(),
                                    strings.isMarathi
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DairyRoyalBlue
                            )
                        }
                        if (summary.hasSampleData) {
                            SampleDataBadge()
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MonthStatColumn(
                            label = strings.thisMonthSales,
                            value = CurrencyFormatter.format(summary.thisMonthTotalSales, currency),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        MonthStatColumn(
                            label = strings.collectedAmount,
                            value = CurrencyFormatter.format(summary.thisMonthCollected, currency),
                            color = DairyFreshGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MonthStatColumn(
                            label = strings.outstandingAmount,
                            value = CurrencyFormatter.format(summary.thisMonthOutstanding, currency),
                            color = StatusAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Action Buttons (All 5 required actions!)
        item {
            SectionHeaderRow(title = strings.quickActions)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = strings.addCustomer,
                    icon = Icons.Filled.PersonAdd,
                    tint = DairyRoyalBlue,
                    bg = DairyRoyalBlueLight,
                    onClick = onAddCustomer,
                    testTag = "quick_action_add_customer",
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = strings.generateMonthlyBills,
                    icon = Icons.Filled.ReceiptLong,
                    tint = DairyFreshGreen,
                    bg = DairyFreshGreenLight,
                    onClick = onGenerateMonthlyBills,
                    testTag = "quick_action_generate_bills",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = strings.viewTodaysReport,
                    icon = Icons.Filled.Assessment,
                    tint = DairyRoyalBlue,
                    bg = DairyRoyalBlueLight,
                    onClick = onViewReports,
                    testTag = "quick_action_view_reports",
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = strings.shareMonthlyStatements,
                    icon = Icons.Filled.Share,
                    tint = WhatsAppGreen,
                    bg = DairyFreshGreenLight,
                    onClick = onShareStatements,
                    testTag = "quick_action_share_statements",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Activity
        item {
            SectionHeaderRow(
                title = strings.recentActivity,
                actionText = if (summary.recentEntries.isNotEmpty()) strings.dailyRegister else null,
                onActionClick = onOpenDailyRegister
            )
        }

        if (summary.recentEntries.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.LocalDrink,
                    title = strings.noEntriesYet,
                    description = "Start by adding your first customer and recording today's milk packets, or load demo data to explore billing & reports.",
                    primaryActionText = strings.addCustomer,
                    onPrimaryAction = onAddCustomer,
                    secondaryActionText = "Load Demo Data",
                    onSecondaryAction = onLoadSampleData
                )
            }
        } else {
            items(summary.recentEntries, key = { it.entry.id }) { item ->
                RecentEntryCard(
                    item = item,
                    currency = currency,
                    strings = strings,
                    onClick = { onEditEntry(item.entry.id) },
                    onCustomerClick = { onOpenCustomer(item.entry.customerId) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun MonthStatColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentEntryCard(
    item: DailyEntryWithDetails,
    currency: String,
    strings: AppStrings,
    onClick: () -> Unit,
    onCustomerClick: () -> Unit
) {
    val entry = item.entry
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DairyRoyalBlueLight)
                    .clickable(onClick = onCustomerClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalDrink,
                    contentDescription = null,
                    tint = DairyRoyalBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.isSampleData) {
                        SampleDataBadge()
                    }
                }
                Text(
                    text = "${entry.productNameSnapshot} • ${entry.packetQuantity} ${strings.packets} × ${CurrencyFormatter.format(entry.pricePerPacket, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateUtils.formatDisplayDate(entry.entryDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = DairyRoyalBlue
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = CurrencyFormatter.format(entry.totalAmount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DairyFreshGreen
            )
        }
    }
}
