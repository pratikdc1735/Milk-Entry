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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MilkProductEntity
import com.example.data.preferences.ShopSettings
import com.example.data.repository.MilkManagerRepository
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.StatusPillBadge
import com.example.ui.theme.DairyFreshGreen
import com.example.ui.theme.DairyFreshGreenLight
import com.example.ui.theme.DairyRoyalBlue
import com.example.ui.theme.DairyRoyalBlueLight
import com.example.ui.theme.StatusError
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilkProductsScreen(
    products: List<MilkProductEntity>,
    settings: ShopSettings,
    onSaveProduct: (MilkProductEntity, () -> Unit) -> Unit,
    onDeleteProduct: (MilkProductEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val currency = settings.currencySymbol
    var editingProduct by remember { mutableStateOf<MilkProductEntity?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<MilkProductEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Milk Products & Packet Rates", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showProductDialog = true
                },
                containerColor = DairyRoyalBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Milk Product")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = DairyRoyalBlueLight,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = DairyRoyalBlue)
                        Text(
                            text = "Selling method: PACKET. Editing a price here updates future entries only — historical daily records and past bills keep their original saved price.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DairyRoyalBlue
                        )
                    }
                }
            }

            items(products, key = { it.id }) { product ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusPillBadge(
                                    text = if (product.isActive) "Active" else "Inactive",
                                    containerColor = if (product.isActive) DairyFreshGreenLight else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (product.isActive) DairyFreshGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Packet Size: ${product.packetSizeMl} ml",
                                style = MaterialTheme.typography.bodySmall,
                                color = DairyRoyalBlue
                            )
                            if (product.description.isNotBlank()) {
                                Text(
                                    text = product.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "${CurrencyFormatter.format(product.pricePerPacket, currency)}/pkt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DairyFreshGreen
                        )
                        IconButton(
                            onClick = {
                                editingProduct = product
                                showProductDialog = true
                            }
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit product", tint = DairyRoyalBlue)
                        }
                        IconButton(onClick = { deleteCandidate = product }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete product", tint = StatusError)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showProductDialog) {
        ProductEditorDialog(
            initial = editingProduct,
            currency = currency,
            onSave = { updated ->
                onSaveProduct(updated) {
                    showProductDialog = false
                }
            },
            onDismiss = { showProductDialog = false }
        )
    }

    deleteCandidate?.let { prod ->
        ConfirmDeleteDialog(
            title = "Delete ${prod.name}?",
            message = "Past daily entries will keep their saved product name and price snapshot.",
            onConfirm = {
                onDeleteProduct(prod)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
private fun ProductEditorDialog(
    initial: MilkProductEntity?,
    currency: String,
    onSave: (MilkProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var sizeText by remember(initial) { mutableStateOf((initial?.packetSizeMl ?: 500).toString()) }
    var priceText by remember(initial) {
        mutableStateOf(initial?.let { MilkManagerRepository.formatShortRate(it.pricePerPacket) } ?: "30")
    }
    var description by remember(initial) { mutableStateOf(initial?.description ?: "") }
    var isActive by remember(initial) { mutableStateOf(initial?.isActive ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Milk Product" else "Edit Milk Product", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g., Full Cream Milk") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) sizeText = it },
                    label = { Text("Packet Size (ml) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) priceText = it },
                    label = { Text("Selling Price Per Packet ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Product", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val size = sizeText.toIntOrNull() ?: 500
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    onSave(
                        MilkProductEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            packetSizeMl = size,
                            pricePerPacket = price,
                            isActive = isActive,
                            description = description.trim(),
                            createdAt = initial?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                },
                enabled = name.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = DairyFreshGreen)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
