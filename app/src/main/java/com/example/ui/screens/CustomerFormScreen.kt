package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.CustomerEntity
import com.example.data.local.MilkProductEntity
import com.example.data.preferences.ShopSettings
import com.example.data.repository.MilkManagerRepository
import com.example.data.repository.OperationResult
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyRoyalBlue
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomerFormScreen(
    existingCustomer: CustomerEntity?,
    products: List<MilkProductEntity>,
    settings: ShopSettings,
    onSave: (
        id: Long,
        name: String,
        mobile: String,
        whatsapp: String,
        address: String,
        defaultProductId: Long?,
        customRate: Double?,
        isActive: Boolean,
        openingBalance: Double,
        notes: String,
        onResult: (OperationResult<Long>) -> Unit
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val currency = settings.currencySymbol
    var name by remember(existingCustomer) { mutableStateOf(existingCustomer?.name ?: "") }
    var mobile by remember(existingCustomer) { mutableStateOf(existingCustomer?.mobileNumber ?: "") }
    var whatsapp by remember(existingCustomer) { mutableStateOf(existingCustomer?.whatsappNumber ?: "") }
    var address by remember(existingCustomer) { mutableStateOf(existingCustomer?.address ?: "") }
    var selectedProductId by remember(existingCustomer, products) {
        mutableStateOf(existingCustomer?.defaultProductId ?: products.firstOrNull()?.id)
    }
    var customRateText by remember(existingCustomer) {
        mutableStateOf(existingCustomer?.customPacketRate?.let { MilkManagerRepository.formatShortRate(it) } ?: "")
    }
    var openingBalanceText by remember(existingCustomer) {
        mutableStateOf(
            if (existingCustomer != null && existingCustomer.openingBalance != 0.0) {
                MilkManagerRepository.formatShortRate(existingCustomer.openingBalance)
            } else {
                ""
            }
        )
    }
    var isActive by remember(existingCustomer) { mutableStateOf(existingCustomer?.isActive ?: true) }
    var notes by remember(existingCustomer) { mutableStateOf(existingCustomer?.notes ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val selectedProduct = products.firstOrNull { it.id == selectedProductId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (existingCustomer == null) "Add New Customer" else "Edit Customer (${existingCustomer.customerCode})",
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
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                        text = "Customer Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name *") },
                        placeholder = { Text("e.g., Rahul Patil") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_input")
                    )
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { input -> if (input.length <= 15) mobile = input },
                        label = { Text("Mobile Number *") },
                        placeholder = { Text("10-digit mobile number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_mobile_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { input -> if (input.length <= 15) whatsapp = input },
                            label = { Text("WhatsApp Number") },
                            placeholder = { Text("Defaults to mobile if blank") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customer_whatsapp_input")
                        )
                        TextButton(onClick = { whatsapp = mobile }) {
                            Text("Same")
                        }
                    }
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Delivery Address") },
                        placeholder = { Text("Flat / House no., Society, Landmark") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Milk Preference & Custom Pricing
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
                        text = "Milk Type & Custom Rate",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = DairyRoyalBlue
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        products.forEach { product ->
                            FilterChip(
                                selected = product.id == selectedProductId,
                                onClick = { selectedProductId = product.id },
                                label = {
                                    Text("${product.name} (${CurrencyFormatter.format(product.pricePerPacket, currency)})")
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customRateText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' }) customRateText = input
                        },
                        label = { Text("Custom Packet Rate ($currency) — Optional") },
                        placeholder = {
                            Text(
                                "Leave blank to use product rate (${
                                    CurrencyFormatter.format(selectedProduct?.pricePerPacket ?: settings.defaultMilkPrice, currency)
                                })"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_rate_input")
                    )

                    OutlinedTextField(
                        value = openingBalanceText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' || it == '-' }) openingBalanceText = input
                        },
                        label = { Text("Opening Balance ($currency)") },
                        placeholder = { Text("0 (Previous unpaid balance before using app)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_opening_balance_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Customer Status",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isActive) "Active (Included in monthly billing)" else "Inactive (Paused delivery)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("Delivery time, floor, special instructions") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    onSave(
                        existingCustomer?.id ?: 0L,
                        name,
                        mobile,
                        whatsapp,
                        address,
                        selectedProductId,
                        customRateText.toDoubleOrNull(),
                        isActive,
                        openingBalanceText.toDoubleOrNull() ?: 0.0,
                        notes
                    ) { res ->
                        isSaving = false
                        if (res is OperationResult.Success) onBack()
                    }
                },
                enabled = !isSaving && name.isNotBlank() && mobile.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DairyFreshGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_customer_button")
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingCustomer == null) "Save Customer" else "Update Customer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
