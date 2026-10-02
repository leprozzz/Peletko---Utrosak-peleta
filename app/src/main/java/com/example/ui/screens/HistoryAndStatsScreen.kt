package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppSettings
import com.example.data.PelletEntry
import com.example.util.AppStrings
import com.example.util.IndustryMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryAndStatsScreen(
    entries: List<PelletEntry>,
    metrics: IndustryMetrics,
    settings: AppSettings,
    availableSeasons: List<String> = emptyList(),
    selectedSeason: String? = null,
    onSelectSeason: (String?) -> Unit = {},
    onExportExcel: ((String) -> Unit)? = null,
    initialTab: Int = 0, // 0 = Istorija, 1 = Statistika
    onBack: () -> Unit,
    onSyncWeather: () -> Unit = {},
    onEditEntry: (PelletEntry) -> Unit,
    onDeleteEntry: (PelletEntry) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val lang = settings.language
    val curr = settings.currency

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp)
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tab 0: Istorija
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (selectedTab == 0) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedTab = 0 }
                                    .padding(vertical = 6.dp)
                                    .testTag("tab_history_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${AppStrings.get("history_tab", lang)} (${entries.size})",
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Tab 1: Detaljna statistika
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (selectedTab == 1) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedTab = 1 }
                                    .padding(vertical = 6.dp)
                                    .testTag("tab_stats_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AppStrings.get("stats_tab", lang),
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("history_stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.get("back", lang)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSyncWeather,
                        modifier = Modifier.testTag("history_stats_sync_weather_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = AppStrings.get("refresh_temps", lang),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab == 0) {
                HistoryScreen(
                    entries = entries,
                    currency = curr,
                    language = lang,
                    availableSeasons = availableSeasons,
                    selectedSeason = selectedSeason,
                    onSelectSeason = onSelectSeason,
                    onEditEntry = onEditEntry,
                    onDeleteEntry = onDeleteEntry
                )
            } else {
                StatisticsScreen(
                    metrics = metrics,
                    settings = settings,
                    entries = entries,
                    availableSeasons = availableSeasons,
                    selectedSeason = selectedSeason,
                    onSelectSeason = onSelectSeason,
                    onExportExcel = onExportExcel
                )
            }
        }
    }
}
