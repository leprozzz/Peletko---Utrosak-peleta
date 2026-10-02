package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PelletPurchase
import com.example.ui.components.AppleCard
import com.example.util.AppStrings
import com.example.util.InventorySummary
import com.example.util.PelletCalculator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    purchases: List<PelletPurchase>,
    inventory: InventorySummary,
    defaultSupplier: String,
    currency: String = "BAM",
    language: String = "bs",
    onBack: () -> Unit,
    onAddPurchase: (dateISO: String, pallets: Double, bags: Double, totalPriceKM: Double, supplier: String, notes: String) -> Unit,
    onUpdatePurchase: (id: Long, dateISO: String, pallets: Double, bags: Double, totalPriceKM: Double, supplier: String, notes: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeletePurchase: (PelletPurchase) -> Unit
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var purchaseToEdit by remember { mutableStateOf<PelletPurchase?>(null) }
    var purchaseToDelete by remember { mutableStateOf<PelletPurchase?>(null) }
    val currSymbol = PelletCalculator.getCurrencySymbol(currency)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.get("purchase_btn", language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("purchase_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.get("back", language)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("purchase_add_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = AppStrings.get("purchase_sub", language), fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Magacin info card
            item {
                AppleCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = AppStrings.get("stock_overview", language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }

                            Text(
                                text = AppStrings.get("one_pallet_rule", language),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(AppStrings.get("remaining", language), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = PelletCalculator.formatBagsWithUnit(inventory.remainingBags, language),
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (inventory.remainingBags < 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "(${PelletCalculator.formatNumber(inventory.remainingKg, 0)} kg)",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(AppStrings.get("total_purchased", language), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${PelletCalculator.formatNumber(inventory.totalPurchasedPallets, 1)} pal.",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = PelletCalculator.formatCurrency(inventory.totalPurchasedCostKM, currency),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = AppStrings.get("purchases_history", language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (purchases.isEmpty()) {
                item {
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = AppStrings.get("no_purchases", language),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AppStrings.get("add_purchase_prompt", language),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(purchases, key = { it.id }) { purchase ->
                    AppleCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val palletsUnit = AppStrings.get("pallets", language)
                                Text(
                                    text = "${PelletCalculator.formatNumber(purchase.pallets, 1)} $palletsUnit (${PelletCalculator.formatBagsWithUnit(purchase.bags, language)})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${PelletCalculator.formatDateHuman(purchase.dateISO, language)}${if (purchase.supplier.isNotBlank()) " • " + purchase.supplier else ""}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row {
                                    Text(
                                        text = "${PelletCalculator.formatCurrency(purchase.totalPriceKM, currency)} ",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "(${PelletCalculator.formatNumber(purchase.pricePerKgKM, 2)} $currSymbol/kg)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { purchaseToEdit = purchase },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("purchase_edit_button_${purchase.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = AppStrings.get("edit", language),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { purchaseToDelete = purchase },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("purchase_delete_button_${purchase.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = AppStrings.get("delete", language),
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddSheet) {
        PurchaseBottomSheet(
            existingPurchase = null,
            defaultSupplier = defaultSupplier,
            currency = currency,
            language = language,
            onDismiss = { showAddSheet = false },
            onSave = { date, pallets, bags, totalKM, supplier, notes ->
                onAddPurchase(date, pallets, bags, totalKM, supplier, notes)
                showAddSheet = false
            }
        )
    }

    purchaseToEdit?.let { purchase ->
        PurchaseBottomSheet(
            existingPurchase = purchase,
            defaultSupplier = defaultSupplier,
            currency = currency,
            language = language,
            onDismiss = { purchaseToEdit = null },
            onSave = { date, pallets, bags, totalKM, supplier, notes ->
                onUpdatePurchase(purchase.id, date, pallets, bags, totalKM, supplier, notes)
                purchaseToEdit = null
            }
        )
    }

    purchaseToDelete?.let { purchase ->
        val pDate = PelletCalculator.formatDateHuman(purchase.dateISO, language)
        val pPallets = PelletCalculator.formatNumber(purchase.pallets, 1)
        AlertDialog(
            onDismissRequest = { purchaseToDelete = null },
            title = { Text(AppStrings.get("delete", language)) },
            text = {
                Text(String.format(AppStrings.get("delete_purchase_confirm", language), pPallets, pDate))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePurchase(purchase)
                        purchaseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppStrings.get("delete", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { purchaseToDelete = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseBottomSheet(
    existingPurchase: PelletPurchase? = null,
    defaultSupplier: String,
    currency: String = "BAM",
    language: String = "bs",
    onDismiss: () -> Unit,
    onSave: (dateISO: String, pallets: Double, bags: Double, totalKM: Double, supplier: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currSymbol = PelletCalculator.getCurrencySymbol(currency)

    var dateISO by remember { mutableStateOf(existingPurchase?.dateISO ?: PelletCalculator.getTodayISO()) }
    var palletsText by remember { mutableStateOf(existingPurchase?.let { PelletCalculator.formatNumber(it.pallets, 1).replace(",", ".") } ?: "2.0") }
    var bagsText by remember { mutableStateOf(existingPurchase?.let { it.bags.toInt().toString() } ?: "140") }
    var pricePerPalletText by remember { mutableStateOf(existingPurchase?.let { PelletCalculator.formatNumber(it.pricePerPalletKM, 0).replace(",", ".") } ?: "525") }
    var supplierText by remember { mutableStateOf(existingPurchase?.supplier ?: defaultSupplier) }
    var notesText by remember { mutableStateOf(existingPurchase?.notes ?: "") }

    val declaredSeason = PelletCalculator.computeSeason(dateISO)

    val pallets = palletsText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val bags = bagsText.toIntOrNull()?.toDouble() ?: (pallets * PelletCalculator.BAGS_PER_PALLET)
    val kg = bags * PelletCalculator.KG_PER_BAG

    val perPallet = pricePerPalletText.replace(",", ".").toDoubleOrNull() ?: 525.0
    val calculatedTotal = pallets * perPallet
    val pricePerKg = if (kg > 0) calculatedTotal / kg else 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (existingPurchase != null) AppStrings.get("edit_purchase", language) else AppStrings.get("purchase_sheet_title", language),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = AppStrings.get("purchase_sheet_info", language),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Date picker i sezona
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    dateISO = String.format("%04d-%02d-%02d", y, m + 1, d)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = PelletCalculator.formatDateHuman(dateISO, language), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${AppStrings.get("season", language)} $declaredSeason",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dvostrani unos: Palete ili Vreće
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = palletsText,
                    onValueChange = { input ->
                        palletsText = input
                        val p = input.replace(",", ".").toDoubleOrNull()
                        if (p != null) {
                            val calcBags = (p * 70).toInt()
                            bagsText = calcBags.toString()
                        }
                    },
                    label = { Text(AppStrings.get("num_pallets", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("purchase_pallets_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedTextField(
                    value = bagsText,
                    onValueChange = { input ->
                        bagsText = input
                        val b = input.toIntOrNull()
                        if (b != null) {
                            val calcPallets = (b / 70.0 * 10).toInt() / 10.0
                            palletsText = calcPallets.toString()
                        }
                    },
                    label = { Text(AppStrings.get("exact_bags", language)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("purchase_bags_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Cijena po paleti
            OutlinedTextField(
                value = pricePerPalletText,
                onValueChange = { pricePerPalletText = it },
                label = { Text("${AppStrings.get("price_per_pallet", language)} ($currSymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("purchase_price_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Info kalkulacija
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(AppStrings.get("quantity", language), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${PelletCalculator.formatBagsWithUnit(bags, language)} (${PelletCalculator.formatNumber(kg, 0)} kg)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${AppStrings.get("total_cost", language)} / kg", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${PelletCalculator.formatCurrency(calculatedTotal, currency)} (${PelletCalculator.formatNumber(pricePerKg, 2)} $currSymbol/kg)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = supplierText,
                onValueChange = { supplierText = it },
                label = { Text(AppStrings.get("supplier", language)) },
                placeholder = { Text(AppStrings.get("supplier_placeholder", language)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (pallets <= 0.0 || calculatedTotal <= 0.0) return@Button
                    onSave(dateISO, pallets, bags, calculatedTotal, supplierText, notesText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("purchase_save_confirm_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = AppStrings.get("save", language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
