package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.preferences.ShopSettings
import com.example.data.repository.MilkManagerRepository
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.StatusAmber
import com.example.util.DateUtils
import com.example.util.ReminderScheduler
import com.example.util.WhatsAppShareHelper
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    settings: ShopSettings,
    hasSampleData: Boolean,
    strings: AppStrings,
    onSaveSettings: (ShopSettings) -> Unit,
    onUpdateLogoUri: (String) -> Unit,
    onExportBackupUri: (Uri) -> Unit,
    onRestoreBackupUri: (Uri) -> Unit,
    onCreateShareableBackup: suspend () -> File,
    onOpenMilkProducts: () -> Unit,
    onOpenReports: () -> Unit,
    onLoadSampleData: () -> Unit,
    onClearSampleData: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var shopName by remember(settings) { mutableStateOf(settings.shopName) }
    var shopAddress by remember(settings) { mutableStateOf(settings.shopAddress) }
    var shopMobile by remember(settings) { mutableStateOf(settings.shopMobile) }
    var shopWhatsApp by remember(settings) { mutableStateOf(settings.shopWhatsApp) }
    var currencySymbol by remember(settings) { mutableStateOf(settings.currencySymbol) }
    var defaultMilkPrice by remember(settings) {
        mutableStateOf(MilkManagerRepository.formatShortRate(settings.defaultMilkPrice))
    }
    var languageCode by remember(settings) { mutableStateOf(settings.languageCode) }
    var dailyReminder by remember(settings) { mutableStateOf(settings.dailyEntryReminderEnabled) }
    var monthlyReminder by remember(settings) { mutableStateOf(settings.monthlyBillReminderEnabled) }
    var pendingReminder by remember(settings) { mutableStateOf(settings.pendingPaymentReminderEnabled) }

    LaunchedEffect(settings) {
        shopName = settings.shopName
        shopAddress = settings.shopAddress
        shopMobile = settings.shopMobile
        shopWhatsApp = settings.shopWhatsApp
        currencySymbol = settings.currencySymbol
        defaultMilkPrice = MilkManagerRepository.formatShortRate(settings.defaultMilkPrice)
        languageCode = settings.languageCode
        dailyReminder = settings.dailyEntryReminderEnabled
        monthlyReminder = settings.monthlyBillReminderEnabled
        pendingReminder = settings.pendingPaymentReminderEnabled
    }

    // Play-policy-compliant zero-permission Android Photo Picker for Shop Logo
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateLogoUri(uri.toString())
        }
    }

    // Storage Access Framework (SAF) zero-permission Backup & Restore launchers
    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackupUri(uri)
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onRestoreBackupUri(uri)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onNotify("Notification permission granted — reminders active")
        } else {
            onNotify("Notification permission denied — reminders won't appear in tray")
        }
    }

    fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick Tools: Milk Products & Business Reports
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMilkProducts,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_milk_products_btn")
                ) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Milk Products", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenReports,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("settings_reports_btn")
                ) {
                    Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reports", maxLines = 1)
                }
            }
        }

        // Shop Profile & Billing Defaults Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Shop Details & Defaults",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Shop Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { shopAddress = it },
                        label = { Text("Shop Address") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = shopMobile,
                            onValueChange = { shopMobile = it },
                            label = { Text("Shop Mobile") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = shopWhatsApp,
                            onValueChange = { shopWhatsApp = it },
                            label = { Text("Shop WhatsApp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currencySymbol,
                            onValueChange = { if (it.length <= 4) currencySymbol = it },
                            label = { Text("Currency") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = defaultMilkPrice,
                            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) defaultMilkPrice = it },
                            label = { Text("Default Milk Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // Shop Logo Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Shop Logo", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (settings.shopLogoUri.isBlank()) "Using Snehal Milk Manager emblem" else "Custom logo selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Logo")
                        }
                    }

                    // Language Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Language / भाषा", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = languageCode == "en",
                                onClick = { languageCode = "en" },
                                label = { Text("English") }
                            )
                            FilterChip(
                                selected = languageCode == "mr",
                                onClick = { languageCode = "mr" },
                                label = { Text("मराठी") }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    shopName = shopName.trim().ifBlank { "Snehal Super Market" },
                                    shopAddress = shopAddress.trim(),
                                    shopMobile = shopMobile.trim(),
                                    shopWhatsApp = shopWhatsApp.trim(),
                                    currencySymbol = currencySymbol.trim().ifBlank { "₹" },
                                    defaultMilkPrice = defaultMilkPrice.toDoubleOrNull() ?: 30.0,
                                    languageCode = languageCode,
                                    dailyEntryReminderEnabled = dailyReminder,
                                    monthlyBillReminderEnabled = monthlyReminder,
                                    pendingPaymentReminderEnabled = pendingReminder
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DairyFreshGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_settings_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Shop Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Reminders & Notifications Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.reminders,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )
                    Text(
                        text = "Reminders stay strictly on your device. No WhatsApp or SMS is ever sent automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ReminderSwitchRow(
                        title = "Daily Milk Entry Reminder",
                        subtitle = "Morning 9:00 AM daily notification",
                        checked = dailyReminder,
                        onCheckedChange = { enabled ->
                            dailyReminder = enabled
                            if (enabled) requestNotifPermissionIfNeeded()
                            onSaveSettings(settings.copy(dailyEntryReminderEnabled = enabled))
                        }
                    )
                    ReminderSwitchRow(
                        title = "Monthly Bill Generation Reminder",
                        subtitle = "1st of every month at 10:00 AM",
                        checked = monthlyReminder,
                        onCheckedChange = { enabled ->
                            monthlyReminder = enabled
                            if (enabled) requestNotifPermissionIfNeeded()
                            onSaveSettings(settings.copy(monthlyBillReminderEnabled = enabled))
                        }
                    )
                    ReminderSwitchRow(
                        title = "Pending Payment Reminder",
                        subtitle = "Weekly evening reminder to follow up dues",
                        checked = pendingReminder,
                        onCheckedChange = { enabled ->
                            pendingReminder = enabled
                            if (enabled) requestNotifPermissionIfNeeded()
                            onSaveSettings(settings.copy(pendingPaymentReminderEnabled = enabled))
                        }
                    )
                    TextButton(
                        onClick = {
                            requestNotifPermissionIfNeeded()
                            val posted = ReminderScheduler.sendTestReminderNow(context, settings.shopName)
                            onNotify(
                                if (posted) "Test reminder notification sent"
                                else "Grant notification permission to see reminders"
                            )
                        }
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Test Reminder Notification")
                    }
                }
            }
        }

        // Backup, Restore & Export Database Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.backupAndRestore,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )
                    Text(
                        text = "All data is saved offline in Room SQLite database. Export a full JSON backup anytime or restore on a new phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                backupExportLauncher.launch("Snehal_Milk_Backup_${DateUtils.todayIso()}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DairyRoyalBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_export_btn")
                        ) {
                            Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup File", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                backupRestoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("backup_restore_btn")
                        ) {
                            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore", maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val file = onCreateShareableBackup()
                                    val note = WhatsAppShareHelper.shareFileAttachment(
                                        context = context,
                                        file = file,
                                        mimeType = "application/json",
                                        chooserTitle = "Share Database Backup"
                                    )
                                    onNotify(note)
                                } catch (e: Exception) {
                                    onNotify("Share backup failed: ${e.message}")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export & Share Database Backup")
                    }
                }
            }
        }

        // Sample Test Data Management (Clearly separated from real customer records)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sample Test Data (Tagged DEMO)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusAmber
                    )
                    Text(
                        text = "Test records are tagged with a DEMO badge and kept separate from real customers so you can remove them in one tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onLoadSampleData,
                            enabled = !hasSampleData,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("load_sample_data_btn")
                        ) {
                            Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load Demo Data", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onClearSampleData,
                            enabled = hasSampleData,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_sample_data_btn")
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Demo", maxLines = 1)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun ReminderSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
