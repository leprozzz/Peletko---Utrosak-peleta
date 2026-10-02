package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PelletEntry
import com.example.data.PelletRepository
import com.example.ui.screens.DailyEntrySheet
import com.example.ui.screens.HistoryAndStatsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PurchaseScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AppStrings
import com.example.util.PelletCalculator
import com.example.viewmodel.PelletViewModel
import com.example.viewmodel.PelletViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    HISTORY_AND_STATS,
    PURCHASES,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PelletRepository(database.pelletDao())
        val factory = PelletViewModelFactory(repository)

        setContent {
            val pelletViewModel: PelletViewModel = viewModel(factory = factory)
            val settings by pelletViewModel.settings.collectAsStateWithLifecycle()

            // Izbor teme: Sistem / Svijetla / Tamna
            val isDarkTheme = when (settings.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                PelletApp(viewModel = pelletViewModel, isDarkTheme = isDarkTheme)
            }
        }
    }
}

@Composable
fun PelletApp(viewModel: PelletViewModel, isDarkTheme: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val entries by viewModel.allEntries.collectAsStateWithLifecycle()
    val purchases by viewModel.allPurchases.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val inventory by viewModel.inventorySummary.collectAsStateWithLifecycle()
    val metrics by viewModel.industryMetrics.collectAsStateWithLifecycle()
    val todayEntry by viewModel.todayEntry.collectAsStateWithLifecycle()
    val todayWeather by viewModel.todayWeather.collectAsStateWithLifecycle()
    val availableSeasons by viewModel.availableSeasons.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeasonFilter.collectAsStateWithLifecycle()

    // Ako je prvo pokretanje, prikaži puni ekran za onboarding (nema skrivenih tipki ili problema sa scrollom)
    if (!settings.isOnboarded) {
        OnboardingScreen(
            onComplete = { area, power, insulation, city, brand, pelletClass, initialPallets, palletPrice, curr, language ->
                viewModel.completeOnboarding(
                    areaM2 = area,
                    powerKw = power,
                    insulation = insulation,
                    city = city,
                    brand = brand,
                    pelletClass = pelletClass,
                    initialPallets = initialPallets,
                    palletPriceKM = palletPrice,
                    currency = curr,
                    language = language
                )
            }
        )
        return
    }

    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var showDailyEntrySheet by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<PelletEntry?>(null) }

    // Automatsko povlačenje i osvježavanje svih temperatura pri ulasku u aplikaciju (automatski klik na ažuriranje)
    LaunchedEffect(settings.isOnboarded) {
        if (settings.isOnboarded) {
            viewModel.syncWeatherHistory(notify = false, forceAll = true)
        }
    }

    // Dialog za upozorenje kad zalihe idu u minus
    var pendingNegativeDateISO by remember { mutableStateOf("") }
    var pendingNegativeBags by remember { mutableDoubleStateOf(0.0) }
    var pendingNegativeTemp by remember { mutableStateOf<Double?>(null) }
    var pendingNegativeT08 by remember { mutableStateOf<Double?>(null) }
    var pendingNegativeT13 by remember { mutableStateOf<Double?>(null) }
    var pendingNegativeT20 by remember { mutableStateOf<Double?>(null) }
    var pendingNegativeNotes by remember { mutableStateOf("") }
    var showNegativeStockDialog by remember { mutableStateOf(false) }
    var negativeStockProjectedKg by remember { mutableDoubleStateOf(0.0) }

    val lang = settings.language

    // Slušaj obavijesti / toast poruke
    LaunchedEffect(Unit) {
        viewModel.notification.collectLatest { notification ->
            Toast.makeText(context, notification.message, Toast.LENGTH_SHORT).show()
            coroutineScope.launch {
                snackbarHostState.showSnackbar(notification.message)
            }
        }
    }

    // Navigacija unazad
    BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = Screen.HOME
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.HOME -> {
                    HomeScreen(
                        inventory = inventory,
                        todayEntry = todayEntry,
                        settings = settings,
                        todayWeather = todayWeather,
                        onOpenDailyEntry = {
                            entryToEdit = todayEntry
                            showDailyEntrySheet = true
                        },
                        onOpenHistoryAndStats = {
                            currentScreen = Screen.HISTORY_AND_STATS
                        },
                        onOpenPurchases = {
                            currentScreen = Screen.PURCHASES
                        },
                        onOpenSettings = {
                            currentScreen = Screen.SETTINGS
                        }
                    )
                }

                Screen.HISTORY_AND_STATS -> {
                    HistoryAndStatsScreen(
                        entries = entries,
                        metrics = metrics,
                        settings = settings,
                        availableSeasons = availableSeasons,
                        selectedSeason = selectedSeason,
                        onSelectSeason = { season ->
                            viewModel.setSelectedSeasonFilter(season)
                        },
                        onExportExcel = { season ->
                            viewModel.exportSeasonToExcel(context, season)
                        },
                        onBack = { currentScreen = Screen.HOME },
                        onSyncWeather = {
                            viewModel.syncWeatherHistory(notify = true, forceAll = true)
                        },
                        onEditEntry = { entry ->
                            entryToEdit = entry
                            showDailyEntrySheet = true
                        },
                        onDeleteEntry = { entry ->
                            viewModel.deleteEntry(entry)
                        }
                    )
                }

                Screen.PURCHASES -> {
                    PurchaseScreen(
                        purchases = purchases,
                        inventory = inventory,
                        defaultSupplier = settings.pelletBrand,
                        currency = settings.currency,
                        language = settings.language,
                        onBack = { currentScreen = Screen.HOME },
                        onAddPurchase = { date, pallets, bags, totalKM, supplier, notes ->
                            viewModel.savePurchase(date, pallets, bags, totalKM, supplier, notes)
                        },
                        onUpdatePurchase = { id, date, pallets, bags, totalKM, supplier, notes ->
                            viewModel.updatePurchase(id, date, pallets, bags, totalKM, supplier, notes)
                        },
                        onDeletePurchase = { purchase ->
                            viewModel.deletePurchase(purchase)
                        }
                    )
                }

                Screen.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onBack = { currentScreen = Screen.HOME },
                        onThemeChange = { mode ->
                            viewModel.updateThemeMode(mode)
                        },
                        onSaveSettings = { updated ->
                            viewModel.updateSettings(updated)
                            currentScreen = Screen.HOME
                        }
                    )
                }
            }

            SnackbarHost(hostState = snackbarHostState)
        }
    }

    // Modal za unos dnevne potrošnje
    if (showDailyEntrySheet) {
        DailyEntrySheet(
            existingEntry = entryToEdit,
            targetDateISO = entryToEdit?.dateISO ?: PelletCalculator.getTodayISO(),
            pricePerKgKM = viewModel.getWeightedPricePerKg(),
            boilerPowerKw = settings.boilerPowerKw,
            currency = settings.currency,
            language = settings.language,
            autoFetchWeather = settings.autoFetchWeather,
            initialWeatherResult = todayWeather,
            onRequestWeather = { date, onResult ->
                viewModel.fetchWeatherForDate(date, onResult)
            },
            onDismiss = {
                showDailyEntrySheet = false
                entryToEdit = null
            },
            onSave = { dateISO, bags, temp, notes, t08, t13, t20, forceNegative ->
                if (forceNegative) {
                    viewModel.forceSaveDailyEntry(dateISO, bags, temp, notes, t08, t13, t20) {
                        showDailyEntrySheet = false
                        entryToEdit = null
                    }
                } else {
                    viewModel.saveDailyEntry(
                        dateISO = dateISO,
                        bags = bags,
                        temp = temp,
                        notes = notes,
                        t08 = t08,
                        t13 = t13,
                        t20 = t20,
                        onRequireNegativeConfirmation = { remainingIfAdded ->
                            pendingNegativeDateISO = dateISO
                            pendingNegativeBags = bags
                            pendingNegativeTemp = temp
                            pendingNegativeT08 = t08
                            pendingNegativeT13 = t13
                            pendingNegativeT20 = t20
                            pendingNegativeNotes = notes
                            negativeStockProjectedKg = remainingIfAdded
                            showNegativeStockDialog = true
                        },
                        onComplete = {
                            showDailyEntrySheet = false
                            entryToEdit = null
                        }
                    )
                }
            }
        )
    }

    // Potvrda za minus na stanju
    if (showNegativeStockDialog) {
        AlertDialog(
            onDismissRequest = { showNegativeStockDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(text = AppStrings.get("stock_negative_title", lang))
            },
            text = {
                Text(
                    text = "${AppStrings.get("stock_negative_msg", lang)}\n\nMinus: ${PelletCalculator.formatNumber(-negativeStockProjectedKg, 1)} kg"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNegativeStockDialog = false
                        viewModel.forceSaveDailyEntry(
                            pendingNegativeDateISO,
                            pendingNegativeBags,
                            pendingNegativeTemp,
                            pendingNegativeNotes,
                            pendingNegativeT08,
                            pendingNegativeT13,
                            pendingNegativeT20
                        ) {
                            showDailyEntrySheet = false
                            entryToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppStrings.get("allow_negative_confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNegativeStockDialog = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }
}
