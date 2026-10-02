package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import com.example.data.PelletEntry
import com.example.ui.components.AppleCard
import com.example.util.AppStrings
import com.example.util.PelletCalculator

@Composable
fun HistoryScreen(
    entries: List<PelletEntry>,
    currency: String = "BAM",
    language: String = "bs",
    availableSeasons: List<String> = emptyList(),
    selectedSeason: String? = null,
    onSelectSeason: (String?) -> Unit = {},
    onBack: () -> Unit = {},
    onEditEntry: (PelletEntry) -> Unit,
    onDeleteEntry: (PelletEntry) -> Unit
) {
    var entryToDelete by remember { mutableStateOf<PelletEntry?>(null) }

    val activeSeasonText = selectedSeason ?: "ALL"
    val displayedEntries = remember(entries, selectedSeason) {
        if (selectedSeason == null || selectedSeason == "ALL") {
            entries
        } else {
            entries.filter { PelletCalculator.isDateInSeason(it.dateISO, selectedSeason) }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zaglavlje sa sažetkom
            item {
                val totalBags = displayedEntries.sumOf { it.bags }
                val totalKg = displayedEntries.sumOf { it.kg }
                val totalCost = displayedEntries.sumOf { it.costKM }

                AppleCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = AppStrings.get("total_recorded", language),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val entriesLabel = if (language.lowercase() == "en") {
                                    if (displayedEntries.size == 1) "1 entry" else "${displayedEntries.size} entries"
                                } else {
                                    val count = displayedEntries.size
                                    val rem100 = count % 100
                                    val rem10 = count % 10
                                    val word = if (rem100 in 11..14) "unosa" else when (rem10) {
                                        1 -> "unos"
                                        2, 3, 4 -> "unosa"
                                        else -> "unosa"
                                    }
                                    "$count $word"
                                }
                                Text(
                                    text = "$entriesLabel (${PelletCalculator.formatNumber(totalKg, 0)} kg)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = PelletCalculator.formatBagsWithUnit(totalBags, language),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = PelletCalculator.formatCurrency(totalCost, currency),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Filter sezone
                        if (availableSeasons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                    val seasonList = listOf("ALL") + availableSeasons.distinct()
                                    for (s in seasonList) {
                                        val isSelected = (selectedSeason == null && s == "ALL") || (selectedSeason == s)
                                        val label = if (s == "ALL") AppStrings.get("all_seasons", language) else s

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onSelectSeason(if (s == "ALL") null else s) }
                                                .testTag("history_season_chip_$s")
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            if (displayedEntries.isEmpty()) {
                item {
                    AppleCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (selectedSeason != null) AppStrings.get("no_entries_season", language) else AppStrings.get("no_entries_yet", language),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AppStrings.get("no_entries_sub", language),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(displayedEntries, key = { it.id }) { entry ->
                    AppleCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onEditEntry(entry) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = PelletCalculator.formatDateHuman(entry.dateISO, language),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )

                                    if (entry.avgTemp != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeviceThermostat,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "${PelletCalculator.formatNumber(entry.avgTemp, 1)}°C",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Row {
                                    Text(
                                        text = "${PelletCalculator.formatNumber(entry.kg, 0)} kg",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = " • ${PelletCalculator.formatCurrency(entry.costKM, currency)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (entry.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = entry.notes,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = PelletCalculator.formatBagsWithUnit(entry.bags, language),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { entryToDelete = entry },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = AppStrings.get("delete", language),
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    entryToDelete?.let { entry ->
        val formattedDate = PelletCalculator.formatDateHuman(entry.dateISO, language)
        val formattedBags = PelletCalculator.formatBagsWithUnit(entry.bags, language)
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text(AppStrings.get("delete", language)) },
            text = {
                Text(String.format(AppStrings.get("delete_entry_confirm", language), formattedDate, formattedBags))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteEntry(entry)
                        entryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppStrings.get("delete", language))
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(AppStrings.get("cancel", language))
                }
            }
        )
    }
}
