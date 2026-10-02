package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PelletEntry
import com.example.data.weather.WeatherResult
import com.example.ui.components.AppleCard
import com.example.ui.components.QuickNumberStepper
import com.example.util.AppStrings
import com.example.util.PelletCalculator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntrySheet(
    existingEntry: PelletEntry? = null,
    targetDateISO: String = PelletCalculator.getTodayISO(),
    pricePerKgKM: Double,
    boilerPowerKw: Double = 20.0,
    currency: String = "BAM",
    language: String = "bs",
    autoFetchWeather: Boolean = true,
    initialWeatherResult: WeatherResult? = null,
    onRequestWeather: (dateISO: String, onResult: (WeatherResult?) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (dateISO: String, bags: Double, temp: Double?, notes: String, t08: Double?, t13: Double?, t20: Double?, forceNegative: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val maxBags = PelletCalculator.getMaxDailyBagsForBoiler(boilerPowerKw)
    var selectedDateISO by remember { mutableStateOf(existingEntry?.dateISO ?: targetDateISO) }
    var bags by remember { mutableDoubleStateOf((existingEntry?.bags ?: 1.0).coerceIn(1.0, maxBags.toDouble())) }

    // Ako postoji keširana temperatura za danas i ovo je novi unos
    val prefilledTemp = existingEntry?.avgTemp ?: if (selectedDateISO == PelletCalculator.getTodayISO()) initialWeatherResult?.avgTemp else null
    var tempText by remember { mutableStateOf(prefilledTemp?.let { PelletCalculator.formatNumber(it, 1) } ?: "") }
    var t08Val by remember { mutableStateOf(existingEntry?.t08 ?: if (selectedDateISO == PelletCalculator.getTodayISO()) initialWeatherResult?.t08 else null) }
    var t13Val by remember { mutableStateOf(existingEntry?.t13 ?: if (selectedDateISO == PelletCalculator.getTodayISO()) initialWeatherResult?.t13 else null) }
    var t20Val by remember { mutableStateOf(existingEntry?.t20 ?: if (selectedDateISO == PelletCalculator.getTodayISO()) initialWeatherResult?.t20 else null) }
    var notesText by remember { mutableStateOf(existingEntry?.notes ?: "") }

    var isFetchingWeather by remember { mutableStateOf(false) }

    val isEnglish = language.lowercase() == "en"
    val initialHourlyParts = listOfNotNull(
        t08Val?.let { "08h: ${PelletCalculator.formatNumber(it, 1)}°C" },
        t13Val?.let { "13h: ${PelletCalculator.formatNumber(it, 1)}°C" },
        t20Val?.let { "20h: ${PelletCalculator.formatNumber(it, 1)}°C" }
    )
    val recordedLabel = if (isEnglish) "Recorded" else "Zabilježeno"
    var weatherFetchedNote by remember {
        mutableStateOf(
            if (initialHourlyParts.isNotEmpty()) {
                "$recordedLabel: ${initialHourlyParts.joinToString(" • ")}"
            } else if (prefilledTemp != null) {
                "$recordedLabel: ${PelletCalculator.formatNumber(prefilledTemp, 1)}°C"
            } else null
        )
    }

    var showNegativeConfirmDialog by remember { mutableStateOf(false) }
    var negativeWarningAmount by remember { mutableDoubleStateOf(0.0) }

    val calculatedKg = PelletCalculator.computeKgFromBags(bags)
    val calculatedCost = calculatedKg * pricePerKgKM

    // Automatsko preuzimanje vremena kada se otvori ili promijeni datum
    fun triggerFetchWeather(date: String) {
        if (!autoFetchWeather) return
        isFetchingWeather = true
        onRequestWeather(date) { res ->
            isFetchingWeather = false
            if (res != null) {
                // Polje je zaključano - vrijednost se uvijek ažurira direktno iz meteorološkog servisa
                tempText = PelletCalculator.formatNumber(res.avgTemp, 1)
                t08Val = res.t08
                t13Val = res.t13
                t20Val = res.t20

                val hourlyParts = listOfNotNull(
                    res.t08?.let { "08h: ${PelletCalculator.formatNumber(it, 1)}°C" },
                    res.t13?.let { "13h: ${PelletCalculator.formatNumber(it, 1)}°C" },
                    res.t20?.let { "20h: ${PelletCalculator.formatNumber(it, 1)}°C" }
                )
                val forecastLabel = if (isEnglish) "Forecast" else "Prognoza"
                val forWord = if (isEnglish) "for" else "za"
                weatherFetchedNote = if (hourlyParts.isNotEmpty()) {
                    "$forecastLabel (${res.cityName}): ${hourlyParts.joinToString(" • ")}"
                } else {
                    "$forecastLabel $forWord ${res.cityName}: ${PelletCalculator.formatNumber(res.avgTemp, 1)}°C"
                }
            }
        }
    }

    LaunchedEffect(selectedDateISO) {
        if (tempText.isBlank() || existingEntry == null || existingEntry.avgTemp == null || existingEntry.avgTemp == 0.0) {
            triggerFetchWeather(selectedDateISO)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (existingEntry != null) AppStrings.get("update_daily", language) else AppStrings.get("daily_entry_btn", language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = AppStrings.get("cancel", language),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Datum selector
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        val cal = Calendar.getInstance()
                        try {
                            val parts = selectedDateISO.split("-")
                            if (parts.size == 3) {
                                cal.set(Calendar.YEAR, parts[0].toInt())
                                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                            }
                        } catch (_: Exception) {}

                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val formatted = String.format("%04d-%02d-%02d", y, m + 1, d)
                                selectedDateISO = formatted
                                triggerFetchWeather(formatted)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .testTag("entry_date_picker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = PelletCalculator.formatDateHuman(selectedDateISO),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stepper za vreće (cijeli brojevi i ograničenje prema snazi peći)
            AppleCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                QuickNumberStepper(
                    bags = bags.toInt().coerceIn(1, maxBags),
                    maxBags = maxBags,
                    boilerPowerKw = boilerPowerKw,
                    onBagsChanged = { bags = it.toDouble() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Info traka: Masa i Trošak u odabranoj valuti (BAM / EUR / DIN)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("total_mass", language),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${PelletCalculator.formatNumber(calculatedKg, 0)} kg",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${AppStrings.get("total_cost", language)} (${PelletCalculator.getCurrencySymbol(currency)})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = PelletCalculator.formatCurrency(calculatedCost, currency),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Temperatura polje sa automatskom vremenskom prognozom (zaključano polje)
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { /* Zaključano - automatski se preuzima */ },
                    readOnly = true,
                    label = { Text(AppStrings.get("outside_temp", language)) },
                    placeholder = { Text(AppStrings.get("auto_temp_placeholder", language)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DeviceThermostat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    suffix = {
                        if (tempText.isNotBlank()) {
                            Text(
                                text = "°C",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = {
                        if (isFetchingWeather) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = AppStrings.get("locked_temp_field", language),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_temp_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                )

                if (weatherFetchedNote != null) {
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp, start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = weatherFetchedNote ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bilješka
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text(AppStrings.get("notes", language)) },
                placeholder = { Text(AppStrings.get("notes_placeholder", language)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_notes_input"),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Veliko Save dugme
            Button(
                onClick = {
                    if (bags <= 0.0) return@Button
                    val temp = tempText.replace("°C", "").replace(",", ".").trim().toDoubleOrNull()
                    onSave(selectedDateISO, bags, temp, notesText, t08Val, t13Val, t20Val, false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("entry_save_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingEntry != null) AppStrings.get("update_daily", language) else AppStrings.get("save_daily", language),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    if (showNegativeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showNegativeConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = AppStrings.get("stock_negative_title", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${AppStrings.get("stock_negative_msg", language)}\n\nMinus: ${PelletCalculator.formatNumber(negativeWarningAmount, 1)} kg",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNegativeConfirmDialog = false
                        val temp = tempText.replace("°C", "").replace(",", ".").trim().toDoubleOrNull()
                        onSave(selectedDateISO, bags, temp, notesText, t08Val, t13Val, t20Val, true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppStrings.get("allow_negative_confirm", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNegativeConfirmDialog = false }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }
}
