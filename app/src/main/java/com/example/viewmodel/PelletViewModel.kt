package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.PelletEntry
import com.example.data.PelletPurchase
import com.example.data.PelletRepository
import com.example.data.weather.WeatherResult
import com.example.data.weather.WeatherService
import com.example.util.AppStrings
import com.example.util.ExcelExporter
import com.example.util.IndustryMetrics
import com.example.util.InventorySummary
import com.example.util.PelletCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiNotification(
    val message: String,
    val isWarning: Boolean = false
)

class PelletViewModel(
    private val repository: PelletRepository
) : ViewModel() {

    val allEntries: StateFlow<List<PelletEntry>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<PelletPurchase>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _overrideSettings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings> = combine(
        repository.settings,
        _overrideSettings
    ) { roomSettings, override ->
        override ?: roomSettings ?: AppSettings()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private var isCompletingOnboarding = false

    private val _notification = MutableSharedFlow<UiNotification>()
    val notification: SharedFlow<UiNotification> = _notification.asSharedFlow()

    // Odabrani filter sezone za prikaz statistike (null = tekuća sezona iz postavki)
    val selectedSeasonFilter = MutableStateFlow<String?>(null)

    // Sve dostupne sezone detektovane iz unosa, nabavki i postavki
    val availableSeasons: StateFlow<List<String>> = combine(
        allEntries,
        allPurchases,
        settings
    ) { entries, purchases, sett ->
        PelletCalculator.getAllAvailableSeasons(entries, purchases, sett.currentSeason)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        listOf("2026/2027", "2025/2026")
    )

    // Kombinirano stanje magacina i zaliha (magacin se nikad ne resetuje, zalihe su trajne)
    val inventorySummary: StateFlow<InventorySummary> = combine(
        allPurchases,
        allEntries,
        settings
    ) { purchases, entries, sett ->
        PelletCalculator.computeInventory(purchases, entries, sett)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PelletCalculator.computeInventory(emptyList(), emptyList())
    )

    // Metrike za industriju (kg/m², kg/kW, KM/m², KM/dan, temp) za odabranu sezonu
    val industryMetrics: StateFlow<IndustryMetrics> = combine(
        allEntries,
        settings,
        selectedSeasonFilter
    ) { entries, sett, seasonFilter ->
        val season = seasonFilter ?: sett.currentSeason
        PelletCalculator.computeMetrics(entries, sett, targetSeason = season)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PelletCalculator.computeMetrics(emptyList(), AppSettings())
    )

    // Današnji unos ako postoji
    val todayEntry: StateFlow<PelletEntry?> = combine(
        allEntries,
        MutableStateFlow(PelletCalculator.getTodayISO())
    ) { entries, today ->
        entries.firstOrNull { it.dateISO == today }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayWeather = MutableStateFlow<WeatherResult?>(null)
    private var isSyncingWeather = false

    init {
        viewModelScope.launch {
            // Automatski povuci i osvježi temperature za sve dane čim se aplikacija pokrene (ekvivalent klika na dugme gore desno)
            syncWeatherHistory(notify = false, forceAll = true)
        }
    }

    /**
     * Sinhronizacija prognoze i istorije temperatura:
     * - Preuzima i kešira današnju temperaturu za grad iz postavki
     * - Za unose iz prethodnih dana koji nemaju upisanu prosječnu temperaturu ili pri osvježavanju
     *   (unutar sezone grijanja 31.08. - 31.05.), automatski povlači meteorološke podatke i upisuje u bazu
     */
    fun syncWeatherHistory(notify: Boolean = false, forceAll: Boolean = false) {
        if (isSyncingWeather) return
        viewModelScope.launch(Dispatchers.IO) {
            isSyncingWeather = true
            try {
                val sett = repository.getSettingsOnce() ?: settings.value
                if (!sett.autoFetchWeather) return@launch
                val city = sett.city.ifBlank { "Sarajevo" }

                // 1. Današnja temperatura
                val today = PelletCalculator.getTodayISO()
                val todayRes = WeatherService.fetchWeather(city, today)
                if (todayRes != null) {
                    todayWeather.value = todayRes
                    val existingToday = repository.getEntryByDateOnce(today)
                    if (existingToday != null && (forceAll || existingToday.avgTemp == null || existingToday.avgTemp == 0.0 || existingToday.avgTemp != todayRes.avgTemp)) {
                        val updated = existingToday.copy(
                            avgTemp = todayRes.avgTemp,
                            t08 = todayRes.t08,
                            t13 = todayRes.t13,
                            t20 = todayRes.t20,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.updateEntry(updated)
                    }
                }

                // 2. Prethodni dani bez upisane temperature ili osvježavanje (unutar sezone 31.08. - 31.05.)
                val entries = repository.getAllEntriesOnce()
                var updatedCount = 0
                for (entry in entries) {
                    if (entry.dateISO == today) continue
                    val isMissing = (entry.avgTemp == null || entry.avgTemp == 0.0)
                    if ((forceAll || isMissing) && PelletCalculator.isHeatingSeasonDate(entry.dateISO)) {
                        val pastRes = WeatherService.fetchWeather(city, entry.dateISO)
                        if (pastRes != null) {
                            if (forceAll || isMissing || entry.avgTemp != pastRes.avgTemp) {
                                val updated = entry.copy(
                                    avgTemp = pastRes.avgTemp,
                                    t08 = pastRes.t08,
                                    t13 = pastRes.t13,
                                    t20 = pastRes.t20,
                                    updatedAt = System.currentTimeMillis()
                                )
                                repository.updateEntry(updated)
                                updatedCount++
                            }
                        }
                    }
                }

                if (notify) {
                    val msg = if (updatedCount > 0) "Ažurirane temperature za $updatedCount dana" else "Temperature za sezonu su ažurne"
                    _notification.emit(UiNotification(msg, isWarning = false))
                }
            } catch (_: Exception) {
            } finally {
                isSyncingWeather = false
            }
        }
    }

    private suspend fun syncTodayWeather() {
        val sett = settings.value
        val city = sett.city.ifBlank { "Sarajevo" }
        val today = PelletCalculator.getTodayISO()
        val todayRes = WeatherService.fetchWeather(city, today)
        if (todayRes != null) {
            todayWeather.value = todayRes
            val existingToday = repository.getEntryByDateOnce(today)
            if (existingToday != null && (existingToday.avgTemp == null || existingToday.avgTemp == 0.0)) {
                val updated = existingToday.copy(
                    avgTemp = todayRes.avgTemp,
                    t08 = todayRes.t08,
                    t13 = todayRes.t13,
                    t20 = todayRes.t20,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateEntry(updated)
            }
        }
    }

    fun setSelectedSeasonFilter(season: String?) {
        selectedSeasonFilter.value = season
    }

    fun exportSeasonToExcel(context: Context, season: String) {
        val sett = settings.value
        val inv = inventorySummary.value
        val met = PelletCalculator.computeMetrics(allEntries.value, sett, targetSeason = season)
        val csv = ExcelExporter.generateSeasonCsv(
            season = season,
            settings = sett,
            inventory = inv,
            metrics = met,
            entries = allEntries.value,
            purchases = allPurchases.value
        )
        val cleanSeasonName = season.replace("/", "_").replace("\\", "_")
        val filename = "Peletko_Izvjestaj_${cleanSeasonName}.csv"
        ExcelExporter.shareCsv(context, filename, csv)
    }

    fun getSeasonCsvContent(season: String): String {
        val sett = settings.value
        val inv = inventorySummary.value
        val met = PelletCalculator.computeMetrics(allEntries.value, sett, targetSeason = season)
        return ExcelExporter.generateSeasonCsv(
            season = season,
            settings = sett,
            inventory = inv,
            metrics = met,
            entries = allEntries.value,
            purchases = allPurchases.value
        )
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            val updated = settings.value.copy(themeMode = themeMode)
            repository.updateSettings(updated)
        }
    }

    fun getWeightedPricePerKg(): Double {
        return PelletCalculator.computeWeightedAvgPricePerKg(
            allPurchases.value,
            settings.value.defaultPricePerPalletKM
        )
    }

    /**
     * Automatsko preuzimanje vremenske prognoze preko Open-Meteo API-ja
     */
    fun fetchWeatherForDate(dateISO: String, onResult: (WeatherResult?) -> Unit) {
        viewModelScope.launch {
            if (!settings.value.autoFetchWeather) {
                onResult(null)
                return@launch
            }
            val city = settings.value.city
            val result = WeatherService.fetchWeather(city, dateISO)
            onResult(result)
        }
    }

    /**
     * Pokušaj spremanja dnevnog unosa.
     * Ako prelazi zalihe, vraća se prompt callback radi potvrde.
     */
    fun saveDailyEntry(
        dateISO: String,
        bags: Double,
        temp: Double?,
        notes: String = "",
        t08: Double? = null,
        t13: Double? = null,
        t20: Double? = null,
        onRequireNegativeConfirmation: ((remainingKgAfter: Double) -> Unit)? = null,
        onComplete: (isUpdate: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.getEntryByDateOnce(dateISO)
            val oldKg = existing?.kg ?: 0.0
            val addedKg = PelletCalculator.computeKgFromBags(bags)
            val netKgChange = addedKg - oldKg

            val currentRemaining = inventorySummary.value.remainingKg
            val futureRemaining = currentRemaining - netKgChange

            // Ako nema dovoljno peleta i potrebno je upozoriti
            if (futureRemaining < 0 && onRequireNegativeConfirmation != null) {
                onRequireNegativeConfirmation(futureRemaining)
                return@launch
            }

            executeSaveEntry(existing, dateISO, bags, addedKg, temp, notes, t08, t13, t20, onComplete)
        }
    }

    fun forceSaveDailyEntry(
        dateISO: String,
        bags: Double,
        temp: Double?,
        notes: String = "",
        t08: Double? = null,
        t13: Double? = null,
        t20: Double? = null,
        onComplete: (isUpdate: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val existing = repository.getEntryByDateOnce(dateISO)
            val addedKg = PelletCalculator.computeKgFromBags(bags)
            executeSaveEntry(existing, dateISO, bags, addedKg, temp, notes, t08, t13, t20, onComplete)
        }
    }

    private suspend fun executeSaveEntry(
        existing: PelletEntry?,
        dateISO: String,
        bags: Double,
        kg: Double,
        temp: Double?,
        notes: String,
        t08: Double? = null,
        t13: Double? = null,
        t20: Double? = null,
        onComplete: (isUpdate: Boolean) -> Unit
    ) {
        val pricePerKg = getWeightedPricePerKg()
        val costKM = kg * pricePerKg
        val lang = settings.value.language

        var finalTemp = temp
        var finalT08 = t08
        var finalT13 = t13
        var finalT20 = t20

        // Ako temperatura nije unijeta a datum je u sezoni grijanja, automatski preuzmi
        if ((finalTemp == null || finalTemp == 0.0) && PelletCalculator.isHeatingSeasonDate(dateISO) && settings.value.autoFetchWeather) {
            val autoWeather = WeatherService.fetchWeather(settings.value.city, dateISO)
            if (autoWeather != null) {
                finalTemp = autoWeather.avgTemp
                finalT08 = autoWeather.t08
                finalT13 = autoWeather.t13
                finalT20 = autoWeather.t20
            }
        }

        val isUpdate = existing != null
        if (existing != null) {
            val updated = existing.copy(
                dateISO = dateISO,
                bags = bags,
                kg = kg,
                costKM = costKM,
                avgTemp = finalTemp,
                t08 = finalT08 ?: existing.t08,
                t13 = finalT13 ?: existing.t13,
                t20 = finalT20 ?: existing.t20,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateEntry(updated)
            val msg = "${AppStrings.get("updated", lang)} (${PelletCalculator.formatBagsWithUnit(bags, lang)})"
            _notification.emit(UiNotification(msg, isWarning = false))
        } else {
            val newEntry = PelletEntry(
                dateISO = dateISO,
                bags = bags,
                kg = kg,
                costKM = costKM,
                avgTemp = finalTemp,
                t08 = finalT08,
                t13 = finalT13,
                t20 = finalT20,
                notes = notes
            )
            repository.insertEntry(newEntry)
            val msg = "${AppStrings.get("saved", lang)} (${PelletCalculator.formatBagsWithUnit(bags, lang)})"
            _notification.emit(UiNotification(msg, isWarning = false))
        }
        onComplete(isUpdate)
    }

    fun deleteEntry(entry: PelletEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            val lang = settings.value.language
            _notification.emit(UiNotification(AppStrings.get("deleted", lang), isWarning = false))
        }
    }

    fun savePurchase(
        dateISO: String,
        pallets: Double,
        bags: Double,
        totalPriceKM: Double,
        supplier: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val kg = bags * PelletCalculator.KG_PER_BAG
            val pricePerPallet = if (pallets > 0) totalPriceKM / pallets else 0.0
            val pricePerKg = if (kg > 0) totalPriceKM / kg else 0.0

            val purchase = PelletPurchase(
                dateISO = dateISO,
                pallets = pallets,
                bags = bags,
                kg = kg,
                totalPriceKM = totalPriceKM,
                pricePerPalletKM = pricePerPallet,
                pricePerKgKM = pricePerKg,
                supplier = supplier,
                notes = notes
            )
            repository.insertPurchase(purchase)

            // Tekuća sezona deklarisana prema godini unosa (npr. 2026 -> 2026/2027)
            val declaredSeason = PelletCalculator.computeSeason(dateISO)
            val currentSett = repository.getSettingsOnce() ?: settings.value
            val updatedSettings = currentSett.copy(currentSeason = declaredSeason)
            repository.updateSettings(updatedSettings)

            val lang = settings.value.language
            _notification.emit(UiNotification("${AppStrings.get("purchase_saved", lang)}: ${bags.toInt()} vr. (Sezona $declaredSeason)", isWarning = false))
        }
    }

    init {
        // Automatsko čišćenje eventualnih duplikata početnog stanja nastalih višekratnim klikom
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val purchases = repository.getAllPurchasesOnce()
                val initialPurchases = purchases.filter { it.notes.contains("Početno stanje magacina", ignoreCase = true) }
                if (initialPurchases.size > 1) {
                    // Zadrži prvi (najnoviji po ID/datumu), a obriši duplikate
                    val duplicates = initialPurchases.drop(1)
                    duplicates.forEach { repository.deletePurchase(it) }
                }
            } catch (_: Exception) {}
        }
    }

    fun deletePurchase(purchase: PelletPurchase) {
        viewModelScope.launch {
            repository.deletePurchase(purchase)
            val lang = settings.value.language
            _notification.emit(UiNotification(AppStrings.get("deleted", lang), isWarning = false))
        }
    }

    fun updatePurchase(
        id: Long,
        dateISO: String,
        pallets: Double,
        bags: Double,
        totalPriceKM: Double,
        supplier: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val kg = bags * PelletCalculator.KG_PER_BAG
            val pricePerPallet = if (pallets > 0) totalPriceKM / pallets else 0.0
            val pricePerKg = if (kg > 0) totalPriceKM / kg else 0.0

            val purchase = PelletPurchase(
                id = id,
                dateISO = dateISO,
                pallets = pallets,
                bags = bags,
                kg = kg,
                totalPriceKM = totalPriceKM,
                pricePerPalletKM = pricePerPallet,
                pricePerKgKM = pricePerKg,
                supplier = supplier,
                notes = notes
            )
            repository.updatePurchase(purchase)

            val lang = settings.value.language
            _notification.emit(UiNotification(AppStrings.get("purchase_updated", lang), isWarning = false))
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        _overrideSettings.value = newSettings
        viewModelScope.launch {
            repository.updateSettings(newSettings)
            val lang = newSettings.language
            _notification.emit(UiNotification(AppStrings.get("saved", lang), isWarning = false))
            if (newSettings.autoFetchWeather) {
                syncWeatherHistory()
            }
        }
    }

    fun completeOnboarding(
        areaM2: Double,
        powerKw: Double,
        insulation: String,
        city: String,
        brand: String,
        pelletClass: String = "A1",
        initialPallets: Double,
        palletPriceKM: Double,
        currency: String = "BAM",
        language: String = "sr",
        themeMode: String = "SYSTEM"
    ): kotlinx.coroutines.Job {
        if (isCompletingOnboarding) return kotlinx.coroutines.Job().apply { complete() }
        isCompletingOnboarding = true

        val today = PelletCalculator.getTodayISO()
        val season = PelletCalculator.computeSeason(today)
        val newSettings = AppSettings(
            id = 1,
            heatingAreaM2 = areaM2,
            boilerPowerKw = powerKw,
            insulationLevel = insulation,
            city = city,
            pelletBrand = brand,
            pelletClass = pelletClass,
            defaultPricePerPalletKM = if (palletPriceKM > 0) palletPriceKM else 525.0,
            isOnboarded = true,
            currentSeason = season,
            currency = currency,
            language = language,
            themeMode = themeMode
        )

        // Odmah ažuriraj u memoriji da interfejs momentalno pređe na glavni ekran
        _overrideSettings.value = newSettings

        return viewModelScope.launch {
            try {
                repository.updateSettings(newSettings)

                // Ako je korisnik unio početno stanje u paletama ili vrećama
                if (initialPallets > 0) {
                    val existingPurchases = repository.getAllPurchasesOnce()
                    val alreadyHasInitial = existingPurchases.any { it.notes.contains("Početno stanje magacina", ignoreCase = true) }
                    if (!alreadyHasInitial) {
                        val bags = initialPallets * PelletCalculator.BAGS_PER_PALLET
                        val totalKM = initialPallets * (if (palletPriceKM > 0) palletPriceKM else 525.0)
                        val kg = bags * PelletCalculator.KG_PER_BAG
                        val pricePerPallet = if (initialPallets > 0) totalKM / initialPallets else 0.0
                        val pricePerKg = if (kg > 0) totalKM / kg else 0.0

                        val purchase = PelletPurchase(
                            dateISO = today,
                            pallets = initialPallets,
                            bags = bags,
                            kg = kg,
                            totalPriceKM = totalKM,
                            pricePerPalletKM = pricePerPallet,
                            pricePerKgKM = pricePerKg,
                            supplier = brand,
                            notes = "Početno stanje magacina"
                        )
                        repository.insertPurchase(purchase)
                    }
                }
            } catch (_: Exception) {
            } finally {
                isCompletingOnboarding = false
            }

            // Asinhrono povuci vremenske podatke u pozadini da ne blokira korisnički interfejs
            viewModelScope.launch(Dispatchers.IO) {
                syncWeatherHistory(notify = false)
            }
        }
    }
}

class PelletViewModelFactory(private val repository: PelletRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PelletViewModel::class.java)) {
            return PelletViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
