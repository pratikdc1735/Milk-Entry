package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.ui.i18n.appStringsFor
import com.example.ui.screens.BillPreviewScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.CustomerFormScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DailyEntriesListScreen
import com.example.ui.screens.DailyEntryFormScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MilkProductsScreen
import com.example.ui.screens.MonthlyBillsScreen
import com.example.ui.screens.MonthlyReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.DairyRoyalBlueDark
import com.example.ui.theme.DairyRoyalBlueLight
import com.example.ui.viewmodel.MilkManagerViewModel
import com.example.util.DateUtils
import kotlinx.coroutines.delay

object Routes {
    const val DASHBOARD = "dashboard"
    const val DAILY_ENTRIES = "daily_entries"
    const val CUSTOMERS = "customers"
    const val MONTHLY_BILLS = "monthly_bills"
    const val SETTINGS = "settings"

    const val ENTRY_FORM = "entry_form?entryId={entryId}&customerId={customerId}"
    const val CUSTOMER_FORM = "customer_form?customerId={customerId}"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    const val BILL_PREVIEW = "bill_preview/{billId}"
    const val MILK_PRODUCTS = "milk_products"
    const val MONTHLY_REPORTS = "monthly_reports"

    fun entryForm(entryId: Long = 0L, customerId: Long = 0L) =
        "entry_form?entryId=$entryId&customerId=$customerId"

    fun customerForm(customerId: Long = 0L) = "customer_form?customerId=$customerId"
    fun customerDetail(customerId: Long) = "customer_detail/$customerId"
    fun billPreview(billId: Long) = "bill_preview/$billId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MilkManagerApp(
    viewModel: MilkManagerViewModel = viewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val activeProducts by viewModel.activeProducts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val dailyEntries by viewModel.dailyEntries.collectAsStateWithLifecycle()
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()

    val strings = remember(settings.languageCode) { appStringsFor(settings.languageCode) }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(850)
        showSplash = false
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelTabs = remember(strings) {
        listOf(
            BottomNavTab(
                route = Routes.DASHBOARD,
                label = strings.navDashboard,
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                testTag = "nav_dashboard"
            ),
            BottomNavTab(
                route = Routes.DAILY_ENTRIES,
                label = strings.navDailyEntries,
                selectedIcon = Icons.Filled.EditCalendar,
                unselectedIcon = Icons.Outlined.EditCalendar,
                testTag = "nav_daily_entries"
            ),
            BottomNavTab(
                route = Routes.CUSTOMERS,
                label = strings.navCustomers,
                selectedIcon = Icons.Filled.Groups,
                unselectedIcon = Icons.Outlined.Groups,
                testTag = "nav_customers"
            ),
            BottomNavTab(
                route = Routes.MONTHLY_BILLS,
                label = strings.navMonthlyBills,
                selectedIcon = Icons.Filled.ReceiptLong,
                unselectedIcon = Icons.Outlined.ReceiptLong,
                testTag = "nav_monthly_bills"
            ),
            BottomNavTab(
                route = Routes.SETTINGS,
                label = strings.navSettings,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings"
            )
        )
    }

    val isTopLevelRoute = topLevelTabs.any { it.route == currentRoute }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (isTopLevelRoute && !useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            topLevelTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DairyRoyalBlue,
                                        selectedTextColor = DairyRoyalBlue,
                                        indicatorColor = DairyRoyalBlueLight
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isTopLevelRoute && useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            topLevelTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.DASHBOARD,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 780.dp)
                        ) {
                            composable(Routes.DASHBOARD) {
                                DashboardScreen(
                                    summary = dashboardSummary,
                                    settings = settings,
                                    strings = strings,
                                    onAddMilkEntry = { navController.navigate(Routes.entryForm()) },
                                    onAddCustomer = { navController.navigate(Routes.customerForm()) },
                                    onGenerateMonthlyBills = { navigateTopLevel(Routes.MONTHLY_BILLS) },
                                    onViewReports = { navController.navigate(Routes.MONTHLY_REPORTS) },
                                    onShareStatements = { navigateTopLevel(Routes.MONTHLY_BILLS) },
                                    onManageProducts = { navController.navigate(Routes.MILK_PRODUCTS) },
                                    onOpenDailyRegister = { navigateTopLevel(Routes.DAILY_ENTRIES) },
                                    onEditEntry = { id -> navController.navigate(Routes.entryForm(entryId = id)) },
                                    onOpenCustomer = { cid -> navController.navigate(Routes.customerDetail(cid)) },
                                    onLoadSampleData = { viewModel.loadSampleDemoData() },
                                    onToggleLanguage = {
                                        viewModel.switchLanguage(if (settings.languageCode == "mr") "en" else "mr")
                                    }
                                )
                            }

                            composable(Routes.DAILY_ENTRIES) {
                                DailyEntriesListScreen(
                                    entries = dailyEntries,
                                    customers = customers,
                                    products = products,
                                    settings = settings,
                                    strings = strings,
                                    onAddEntry = { navController.navigate(Routes.entryForm()) },
                                    onEditEntry = { id -> navController.navigate(Routes.entryForm(entryId = id)) },
                                    onDeleteEntry = { id -> viewModel.deleteDailyEntry(id) },
                                    onOpenCustomer = { cid -> navController.navigate(Routes.customerDetail(cid)) }
                                )
                            }

                            composable(Routes.CUSTOMERS) {
                                CustomersScreen(
                                    customers = customers,
                                    settings = settings,
                                    strings = strings,
                                    onAddCustomer = { navController.navigate(Routes.customerForm()) },
                                    onOpenCustomerDetail = { cid -> navController.navigate(Routes.customerDetail(cid)) }
                                )
                            }

                            composable(Routes.MONTHLY_BILLS) {
                                MonthlyBillsScreen(
                                    bills = allBills,
                                    customers = customers,
                                    settings = settings,
                                    strings = strings,
                                    onGenerateAllBills = { year, month ->
                                        viewModel.generateAllMonthlyBills(year, month)
                                    },
                                    onGenerateIndividualBill = { cid, year, month ->
                                        viewModel.generateMonthlyBill(cid, year, month) { bill ->
                                            navController.navigate(Routes.billPreview(bill.id))
                                        }
                                    },
                                    onOpenBillPreview = { billId ->
                                        navController.navigate(Routes.billPreview(billId))
                                    },
                                    onOpenReports = { navController.navigate(Routes.MONTHLY_REPORTS) },
                                    onNotify = viewModel::notifyUser
                                )
                            }

                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    settings = settings,
                                    hasSampleData = dashboardSummary.hasSampleData,
                                    strings = strings,
                                    onSaveSettings = viewModel::saveSettings,
                                    onUpdateLogoUri = viewModel::updateShopLogo,
                                    onExportBackupUri = viewModel::exportBackupToUri,
                                    onRestoreBackupUri = viewModel::restoreBackupFromUri,
                                    onCreateShareableBackup = { viewModel.createShareableBackupFile() },
                                    onOpenMilkProducts = { navController.navigate(Routes.MILK_PRODUCTS) },
                                    onOpenReports = { navController.navigate(Routes.MONTHLY_REPORTS) },
                                    onLoadSampleData = viewModel::loadSampleDemoData,
                                    onClearSampleData = viewModel::clearSampleDemoData,
                                    onNotify = viewModel::notifyUser
                                )
                            }

                            composable(
                                route = Routes.ENTRY_FORM,
                                arguments = listOf(
                                    navArgument("entryId") { type = NavType.LongType; defaultValue = 0L },
                                    navArgument("customerId") { type = NavType.LongType; defaultValue = 0L }
                                )
                            ) { entry ->
                                val entryId = entry.arguments?.getLong("entryId") ?: 0L
                                val customerId = entry.arguments?.getLong("customerId") ?: 0L
                                DailyEntryFormScreen(
                                    editEntryId = entryId,
                                    preselectedCustomerId = customerId,
                                    customers = customers,
                                    products = if (activeProducts.isNotEmpty()) activeProducts else products,
                                    settings = settings,
                                    strings = strings,
                                    loadEntryById = viewModel::getDailyEntryById,
                                    loadExistingEntriesForDate = viewModel::getExistingEntriesForCustomerOnDate,
                                    onSaveEntry = viewModel::saveDailyMilkEntry,
                                    onAddNewCustomer = { navController.navigate(Routes.customerForm()) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Routes.CUSTOMER_FORM,
                                arguments = listOf(
                                    navArgument("customerId") { type = NavType.LongType; defaultValue = 0L }
                                )
                            ) { entry ->
                                val customerId = entry.arguments?.getLong("customerId") ?: 0L
                                val existing = customers.firstOrNull { it.customer.id == customerId }?.customer
                                CustomerFormScreen(
                                    existingCustomer = existing,
                                    products = if (activeProducts.isNotEmpty()) activeProducts else products,
                                    settings = settings,
                                    onSave = viewModel::saveCustomer,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Routes.CUSTOMER_DETAIL,
                                arguments = listOf(navArgument("customerId") { type = NavType.LongType })
                            ) { entry ->
                                val customerId = entry.arguments?.getLong("customerId") ?: 0L
                                val cwb = customers.firstOrNull { it.customer.id == customerId }
                                val ledger by remember(customerId) { viewModel.observeCustomerLedger(customerId) }
                                    .collectAsStateWithLifecycle(initialValue = emptyList())
                                val dailyHistory by remember(customerId) { viewModel.observeCustomerEntries(customerId) }
                                    .collectAsStateWithLifecycle(initialValue = emptyList())
                                val custPayments by remember(customerId) { viewModel.observeCustomerPayments(customerId) }
                                    .collectAsStateWithLifecycle(initialValue = emptyList())
                                val custBills by remember(customerId) { viewModel.observeCustomerBills(customerId) }
                                    .collectAsStateWithLifecycle(initialValue = emptyList())

                                CustomerDetailScreen(
                                    customerWithBalance = cwb,
                                    ledger = ledger,
                                    dailyHistory = dailyHistory,
                                    payments = custPayments,
                                    bills = custBills,
                                    settings = settings,
                                    onAddMilkEntry = { cid -> navController.navigate(Routes.entryForm(customerId = cid)) },
                                    onEditCustomer = { cid -> navController.navigate(Routes.customerForm(customerId = cid)) },
                                    onDeleteCustomer = {
                                        cwb?.customer?.let { c ->
                                            viewModel.deleteCustomer(c) { navController.popBackStack() }
                                        }
                                    },
                                    onRecordPayment = viewModel::recordPayment,
                                    onDeletePayment = viewModel::deletePayment,
                                    onEditDailyEntry = { eid -> navController.navigate(Routes.entryForm(entryId = eid)) },
                                    onDeleteDailyEntry = viewModel::deleteDailyEntry,
                                    onGenerateCurrentMonthBill = { cid ->
                                        viewModel.generateMonthlyBill(
                                            customerId = cid,
                                            year = DateUtils.currentYear(),
                                            month = DateUtils.currentMonth()
                                        ) { bill ->
                                            navController.navigate(Routes.billPreview(bill.id))
                                        }
                                    },
                                    onOpenBillPreview = { bid -> navController.navigate(Routes.billPreview(bid)) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = Routes.BILL_PREVIEW,
                                arguments = listOf(navArgument("billId") { type = NavType.LongType })
                            ) { entry ->
                                val billId = entry.arguments?.getLong("billId") ?: 0L
                                val bill by remember(billId) { viewModel.observeBillById(billId) }
                                    .collectAsStateWithLifecycle(initialValue = null)
                                val audits by remember(billId) { viewModel.observeAuditsForBill(billId) }
                                    .collectAsStateWithLifecycle(initialValue = emptyList())

                                BillPreviewScreen(
                                    bill = bill,
                                    audits = audits,
                                    settings = settings,
                                    strings = strings,
                                    onRegenerateBill = { cid, year, month, reason ->
                                        viewModel.generateMonthlyBill(
                                            customerId = cid,
                                            year = year,
                                            month = month,
                                            forceRegenerate = true,
                                            regenerationReason = reason
                                        )
                                    },
                                    onNotify = viewModel::notifyUser,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.MILK_PRODUCTS) {
                                MilkProductsScreen(
                                    products = products,
                                    settings = settings,
                                    onSaveProduct = viewModel::saveProduct,
                                    onDeleteProduct = viewModel::deleteProduct,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.MONTHLY_REPORTS) {
                                MonthlyReportsScreen(
                                    customers = customers,
                                    settings = settings,
                                    strings = strings,
                                    buildReport = viewModel::buildReport,
                                    onNotify = viewModel::notifyUser,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Branded Snehal Super Market Splash Screen Overlay
        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut()
        ) {
            BrandedSplashOverlay(shopName = settings.shopName)
        }
    }
}

@Composable
private fun BrandedSplashOverlay(shopName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DairyRoyalBlueDark, DairyRoyalBlue, DairyFreshGreen)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 10.dp,
                color = Color.White
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_milk_fg),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(28.dp))
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = shopName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Milk Manager • Daily Sales & Monthly Billing",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
