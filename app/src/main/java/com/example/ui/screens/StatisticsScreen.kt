package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.PelletEntry
import com.example.ui.components.AppleCard
import com.example.util.AppStrings
import com.example.util.IndustryMetrics
import com.example.util.PelletCalculator

@Composable
fun StatisticsScreen(
    metrics: IndustryMetrics,
    settings: AppSettings,
    entries: List<PelletEntry>,
    availableSeasons: List<String> = emptyList(),
    selectedSeason: String? = null,
    onSelectSeason: (String?) -> Unit = {},
    onExportExcel: ((String) -> Unit)? = null,
    onBack: () -> Unit = {}
) {
    val lang = settings.language
    val curr = settings.currency
    val activeSeasonText = selectedSeason ?: settings.currentSeason
    val currSymbol = PelletCalculator.getCurrencySymbol(curr)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Objekat i sezona badge
            item {
                AppleCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val propLabel = AppStrings.get("property_label", lang)
                            val insulLabel = AppStrings.get("insulation_label", lang)
                            val localizedInsul = PelletCalculator.getInsulationLabel(settings.insulationLevel, lang)
                            Text(
                                text = "$propLabel: ${PelletCalculator.formatNumber(settings.heatingAreaM2, 0)} m² • ${PelletCalculator.formatNumber(settings.boilerPowerKw, 0)} kW",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${settings.city} • $insulLabel: $localizedInsul",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val seasonDisplay = if (activeSeasonText == "ALL") AppStrings.get("all_seasons", lang) else "${AppStrings.get("season", lang)} $activeSeasonText"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = seasonDisplay,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // KARTICA ZA ODABIR SEZONE I IZVOZ U EXCEL
            item {
                AppleCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = AppStrings.get("select_season", lang),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            if (onExportExcel != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onExportExcel(activeSeasonText) }
                                        .testTag("export_excel_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FileDownload,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = AppStrings.get("export_excel", lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontalni izbor sezona
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val seasonList = (availableSeasons.ifEmpty { listOf(settings.currentSeason) } + listOf("ALL")).distinct()
                            for (s in seasonList) {
                                val isSelected = if (s == "ALL") {
                                    selectedSeason == "ALL"
                                } else {
                                    (selectedSeason == s) || (selectedSeason == null && s == settings.currentSeason)
                                }
                                val label = if (s == "ALL") AppStrings.get("all_seasons", lang) else "${AppStrings.get("season", lang)} $s"

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSelectSeason(if (s == settings.currentSeason && selectedSeason != "ALL") null else s)
                                        }
                                        .testTag("season_chip_$s")
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // OBAVJEŠTENJE AKO ZA ODABRANU SEZONU NEMA PODATAKA (NOVA SEZONA)
            if (metrics.activeDaysCount == 0 && metrics.totalKg == 0.0) {
                item {
                    AppleCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        ) {
                            Text(
                                text = AppStrings.get("no_entries_season", lang),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = AppStrings.get("season_saved_hint", lang),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // OBAVEZNE INDUSTRIJSKE METRIKE
            item {
                Text(
                    text = AppStrings.get("kpi_title", lang),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Grid 2x2 kartica
            item {
                val dayUnit = AppStrings.get("unit_per_day", lang)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. kg po m² po sezoni
                        IndustryMetricBox(
                            title = AppStrings.get("kg_per_m2", lang),
                            value = PelletCalculator.formatNumber(metrics.kgPerM2Season, 1),
                            unit = "kg/m²",
                            subtitle = AppStrings.get("specific_consumption", lang),
                            icon = Icons.Default.Home,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )

                        // 2. kg po kW peći
                        IndustryMetricBox(
                            title = AppStrings.get("kg_per_kw", lang),
                            value = PelletCalculator.formatNumber(metrics.kgPerKwBoiler, 1),
                            unit = "kg/kW",
                            subtitle = AppStrings.get("boiler_load", lang),
                            icon = Icons.Default.Bolt,
                            iconColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 3. Valuta po m²
                        IndustryMetricBox(
                            title = "$currSymbol / m²",
                            value = PelletCalculator.formatNumber(metrics.kmPerM2, 2),
                            unit = "$currSymbol/m²",
                            subtitle = AppStrings.get("cost_by_area", lang),
                            icon = Icons.Default.Payments,
                            iconColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )

                        // 4. Valuta po danu
                        IndustryMetricBox(
                            title = "$currSymbol / $dayUnit",
                            value = PelletCalculator.formatNumber(metrics.kmPerDay, 2),
                            unit = "$currSymbol/$dayUnit",
                            subtitle = "${AppStrings.get("daily_average", lang)} (~${PelletCalculator.formatNumber(metrics.avgBagsPerDay, 1)} ${if (lang.lowercase() == "en") "b/d" else "vr/d"})",
                            icon = Icons.Default.CalendarToday,
                            iconColor = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // POTROŠNJA VS TEMPERATURA
            item {
                Text(
                    text = AppStrings.get("temp_vs_consumption", lang),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                AppleCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeviceThermostat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = AppStrings.get("temp_correlation_title", lang),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }

                        val maxBags = metrics.tempZoneStats.maxOfOrNull { it.avgBags }?.coerceAtLeast(1.0) ?: 3.0
                        val bagsPerDayUnit = AppStrings.get("unit_per_day_short", lang)
                        val daysSampleUnit = AppStrings.get("sample_days_short", lang)

                        metrics.tempZoneStats.forEach { zone ->
                            Column {
                                val daysSampleText = if (lang.lowercase() == "en") {
                                    if (zone.sampleCount == 1) "1 day" else "${zone.sampleCount} days"
                                } else {
                                    "${zone.sampleCount} $daysSampleUnit"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = PelletCalculator.getTempZoneDisplayName(zone.zoneName, lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (zone.sampleCount > 0)
                                            "${PelletCalculator.formatNumber(zone.avgBags, 1)} $bagsPerDayUnit ($daysSampleText)"
                                        else AppStrings.get("no_data", lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (zone.sampleCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val fraction = if (zone.sampleCount > 0) (zone.avgBags / maxBags).toFloat().coerceIn(0.05f, 1f) else 0f
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (zone.zoneName.contains("Mraz")) Color(0xFF3B82F6) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        Text(
                            text = AppStrings.get("temp_correlation_tip", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // MJESEČNA STATISTIKA
            item {
                Text(
                    text = AppStrings.get("monthly_review", lang),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                val seasonFilteredEntries = if (activeSeasonText == "ALL") {
                    entries
                } else {
                    entries.filter { PelletCalculator.isDateInSeason(it.dateISO, activeSeasonText) }
                }
                MonthlyConsumptionCard(entries = seasonFilteredEntries, currency = curr, language = lang)
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun MonthlyConsumptionCard(entries: List<PelletEntry>, currency: String = "BAM", language: String = "bs") {
    val monthlyData = entries.groupBy {
        if (it.dateISO.length >= 7) it.dateISO.substring(0, 7) else "Ostalo"
    }.toSortedMap(compareByDescending { it })

    AppleCard(modifier = Modifier.fillMaxWidth()) {
        if (monthlyData.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = AppStrings.get("no_monthly_data", language),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else {
            val daysLoggedLabel = AppStrings.get("days_count_short", language)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                monthlyData.forEach { (monthKey, monthEntries) ->
                    val totalBags = monthEntries.sumOf { it.bags }
                    val totalKg = monthEntries.sumOf { it.kg }
                    val totalCost = monthEntries.sumOf { it.costKM }
                    val daysCount = monthEntries.size

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = PelletCalculator.formatMonthName(monthKey, language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            val daysLoggedLabel = PelletCalculator.getDaysLoggedLabel(daysCount, language)
                            Text(
                                text = "$daysCount $daysLoggedLabel • ${PelletCalculator.formatNumber(totalKg, 0)} kg",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = PelletCalculator.formatBagsWithUnit(totalBags, language),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = PelletCalculator.formatCurrency(totalCost, currency),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IndustryMetricBox(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    AppleCard(
        modifier = modifier,
        contentPadding = 14.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
