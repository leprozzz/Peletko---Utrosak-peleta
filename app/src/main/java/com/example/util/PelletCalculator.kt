package com.example.util

import com.example.data.AppSettings
import com.example.data.PelletEntry
import com.example.data.PelletPurchase
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object PelletCalculator {
    const val KG_PER_BAG = 15.0
    const val BAGS_PER_PALLET = 70.0
    const val KG_PER_PALLET = 1050.0 // 70 * 15

    fun computeKgFromBags(bags: Double): Double = bags * KG_PER_BAG

    fun computeBagsFromKg(kg: Double): Double = if (kg <= 0) 0.0 else kg / KG_PER_BAG

    fun computePalletsFromKg(kg: Double): Double = if (kg <= 0) 0.0 else kg / KG_PER_PALLET

    /**
     * Pravilo: Maksimalan dozvoljen broj vreća vezan za snagu peći (kW)
     * - 5-15 kW: 3 vreće dnevno
     * - 16-25 kW: 5 vreća dnevno
     * - 26-45 kW: 7 vreća dnevno
     * - 46-70 kW: 12 vreća dnevno
     * - 71 kW i više: 30 vreća dnevno
     */
    fun getMaxDailyBagsForBoiler(powerKw: Double): Int {
        return when {
            powerKw <= 15.0 -> 3
            powerKw <= 25.0 -> 5
            powerKw <= 45.0 -> 7
            powerKw <= 70.0 -> 12
            else -> 30
        }
    }

    /**
     * Tekuća sezona - deklarira se kao godina unosa / naredna godina.
     * Npr. ako unosimo 2026 deklarise se kao 2026/2027, ako unosimo 2027 kao 2027/2028.
     */
    fun computeSeason(dateOrYear: String): String {
        return try {
            val clean = dateOrYear.trim()
            val year = when {
                clean.length >= 4 -> clean.substring(0, 4).toInt()
                else -> Calendar.getInstance().get(Calendar.YEAR)
            }
            "$year/${year + 1}"
        } catch (_: Exception) {
            "2026/2027"
        }
    }

    /**
     * Sezona grijanja je od 31.08. do 31.05.
     * U periodu van sezone grijanja (01.06. - 30.08.) temperatura se ne mora pamtiti/smještati.
     */
    fun isHeatingSeasonDate(dateISO: String): Boolean {
        return try {
            val parts = dateISO.split("-")
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            when (month) {
                6, 7 -> false
                8 -> day >= 31
                else -> true // 1, 2, 3, 4, 5, 9, 10, 11, 12
            }
        } catch (_: Exception) {
            true
        }
    }

    /**
     * weightedAvgPricePerKg = sum(purchaseKg * purchasePricePerKg) / sum(purchaseKg)
     */
    fun computeWeightedAvgPricePerKg(
        purchases: List<PelletPurchase>,
        defaultPalletPriceKM: Double = 525.0
    ): Double {
        val totalKg = purchases.sumOf { it.kg }
        if (totalKg <= 0.0) {
            return defaultPalletPriceKM / KG_PER_PALLET
        }
        val totalCost = purchases.sumOf { it.totalPriceKM }
        return totalCost / totalKg
    }

    /**
     * Računa broj punih kalendarskih dana između dva datuma (ISO format YYYY-MM-DD).
     * Koristi UTC za eliminaciju uticaja ljetnog/zimskog računanja vremena i vremenskih zona.
     */
    fun calendarDaysBetween(fromISO: String, toISO: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val d1 = sdf.parse(fromISO) ?: return 0L
            val d2 = sdf.parse(toISO) ?: return 0L
            val diffMs = d2.time - d1.time
            val days = diffMs / (1000L * 60L * 60L * 24L)
            if (days < 0L) 0L else days
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Određuje kojoj sezoni pripada zadati datum (npr. 2026-01-15 -> 2025/2026, 2026-10-10 -> 2026/2027).
     * Sezona grijanja počinje krajem avgusta (31.08.) i traje do 31.05./31.07.
     */
    fun getSeasonForDate(dateISO: String): String {
        return try {
            val parts = dateISO.split("-")
            val year = parts[0].toInt()
            val month = if (parts.size > 1) parts[1].toInt() else 10
            val day = if (parts.size > 2) parts[2].toInt() else 1
            val isNewSeason = if (month == 8) day >= 31 else month >= 9
            if (isNewSeason) {
                "$year/${year + 1}"
            } else {
                "${year - 1}/$year"
            }
        } catch (_: Exception) {
            computeSeason(dateISO)
        }
    }

    /**
     * Vraća sve dostupne sezone na osnovu unosa, nabavki i trenutnih postavki.
     * Početna podržana sezona je 2025/2026 (starije od 2025/2026 se ne prikazuju).
     */
    fun getAllAvailableSeasons(
        entries: List<PelletEntry>,
        purchases: List<PelletPurchase>,
        currentSeason: String = "2025/2026"
    ): List<String> {
        val seasons = mutableSetOf<String>()
        if (currentSeason.isNotBlank()) seasons.add(currentSeason.trim())

        entries.forEach { entry ->
            seasons.add(getSeasonForDate(entry.dateISO))
        }
        purchases.forEach { purchase ->
            seasons.add(getSeasonForDate(purchase.dateISO))
        }

        seasons.add("2026/2027")
        seasons.add("2025/2026")

        // Filtriraj tako da je najstarija sezona 2025/2026
        return seasons.filter { season ->
            val firstYear = season.take(4).toIntOrNull() ?: 2025
            firstYear >= 2025
        }.sortedDescending()
    }

    /**
     * Provjerava da li datum (YYYY-MM-DD) pripada zadatoj sezoni (npr. "2024/2025" ili "2024").
     * Ako format sezone nije prepoznat ili je "ALL", vraća true.
     */
    fun isDateInSeason(dateISO: String, seasonStr: String): Boolean {
        if (seasonStr.equals("ALL", ignoreCase = true) || seasonStr.isBlank()) return true
        return try {
            val regex = Regex("""(\d{4})\s*[/\\-]\s*(\d{4})""")
            val match = regex.find(seasonStr)
            if (match != null) {
                val y1 = match.groupValues[1].toInt()
                val y2 = match.groupValues[2].toInt()
                val seasonStart = String.format(Locale.US, "%04d-08-01", y1)
                val seasonEnd = String.format(Locale.US, "%04d-07-31", y2)
                dateISO in seasonStart..seasonEnd
            } else {
                val singleYearMatch = Regex("""\b(\d{4})\b""").find(seasonStr)
                if (singleYearMatch != null) {
                    val y = singleYearMatch.groupValues[1].toInt()
                    val seasonStart = String.format(Locale.US, "%04d-08-01", y)
                    val seasonEnd = String.format(Locale.US, "%04d-07-31", y + 1)
                    dateISO in seasonStart..seasonEnd
                } else {
                    true
                }
            }
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Određuje krajnji datum perioda posmatranja za računanje dnevnog prosjeka.
     * Ako je danas unutar unosa ili do 3 dana nakon posljednjeg unosa (redovno loženje),
     * računa se do danas.
     * Ako je danas znatno kasnije (npr. ljeto ili se testiraju unosi iz decembra/januara),
     * period se završava na dan posljednjeg unosa kako se ne bi dijelilo sa mjesecima pauze.
     */
    fun computeEffectivePeriodEndDate(firstDate: String, lastDate: String, todayISO: String): String {
        return when {
            todayISO in firstDate..lastDate -> todayISO
            todayISO > lastDate -> {
                val gap = calendarDaysBetween(lastDate, todayISO)
                if (gap <= 3) todayISO else lastDate
            }
            else -> lastDate
        }
    }

    /**
     * Izračun preostalih zaliha i prognoze trajanja.
     * Magacin (preostalo kg, vreća, paleta) se nikada ne resetuje - prenosi se u novu sezonu.
     * Prosjek dnevne potrošnje se računa od prvog dana unosa u definisanoj sezoni do kraja aktivnog perioda:
     * npr. decembar 45 vreća + januar 30 vreća = 75 vreća kroz 62 dana = 1.2 vreće/dan.
     */
    fun computeInventory(
        purchases: List<PelletPurchase>,
        entries: List<PelletEntry>,
        settings: AppSettings? = null,
        todayISO: String = getTodayISO(),
        targetSeason: String? = null
    ): InventorySummary {
        val totalPurchasedKg = purchases.sumOf { it.kg }
        val totalPurchasedBags = purchases.sumOf { it.bags }
        val totalPurchasedPallets = purchases.sumOf { it.pallets }
        val totalPurchasedCostKM = purchases.sumOf { it.totalPriceKM }

        val totalConsumedKg = entries.sumOf { it.kg }
        val totalConsumedBags = entries.sumOf { it.bags }
        val totalConsumedCostKM = entries.sumOf { it.costKM }

        val remainingKg = totalPurchasedKg - totalConsumedKg
        val remainingBags = remainingKg / KG_PER_BAG
        val remainingPallets = remainingKg / KG_PER_PALLET

        // Filtriraj unose za tekuću/odabranu sezonu radi proračuna trenda potrošnje
        val seasonStr = targetSeason ?: settings?.currentSeason ?: "2025/2026"
        val isAll = seasonStr.equals("ALL", ignoreCase = true)
        val seasonEntries = if (isAll) entries else entries.filter { isDateInSeason(it.dateISO, seasonStr) }
        val trendEntries = if (seasonEntries.isNotEmpty()) seasonEntries else entries

        // Računaj prosjek od prvog dana unosa u sezoni do kraja aktivnog perioda
        val (avgBagsPerDay, seasonDaysElapsed, firstEntryDateISO) = if (trendEntries.isNotEmpty()) {
            val firstDate = trendEntries.minOf { it.dateISO }
            val lastDate = trendEntries.maxOf { it.dateISO }
            val endDate = computeEffectivePeriodEndDate(firstDate, lastDate, todayISO)
            val daysCount = (calendarDaysBetween(firstDate, endDate) + 1L).toInt().coerceAtLeast(1)
            val totalSeasonBags = trendEntries.sumOf { it.bags }
            val avg = totalSeasonBags / daysCount.toDouble()
            Triple(avg, daysCount, firstDate)
        } else {
            Triple(0.0, 0, null)
        }

        val estimatedDaysLeft = if (avgBagsPerDay > 0.001 && remainingBags > 0) {
            (remainingBags / avgBagsPerDay).toInt()
        } else null

        return InventorySummary(
            totalPurchasedKg = totalPurchasedKg,
            totalPurchasedBags = totalPurchasedBags,
            totalPurchasedPallets = totalPurchasedPallets,
            totalPurchasedCostKM = totalPurchasedCostKM,
            totalConsumedKg = totalConsumedKg,
            totalConsumedBags = totalConsumedBags,
            totalConsumedCostKM = totalConsumedCostKM,
            remainingKg = remainingKg,
            remainingBags = remainingBags,
            remainingPallets = remainingPallets,
            avgBags7Days = avgBagsPerDay,
            estimatedDaysLeft = estimatedDaysLeft,
            avgBagsPerDay = avgBagsPerDay,
            seasonDaysElapsed = seasonDaysElapsed,
            firstEntryDateISO = firstEntryDateISO
        )
    }

    /**
     * Računa prosječnu dnevnu potrošnju vreća za zadnjih N unosa
     */
    fun computeRecentAverageBags(entries: List<PelletEntry>, days: Int = 7): Double {
        if (entries.isEmpty()) return 0.0
        val sorted = entries.sortedByDescending { it.dateISO }
        val recent = sorted.take(days)
        return if (recent.isNotEmpty()) {
            recent.sumOf { it.bags } / recent.size.toDouble()
        } else 0.0
    }

    /**
     * Izračun metričkih pokazatelja za odabranu sezonu (ili "ALL" za sve).
     */
    fun computeMetrics(
        entries: List<PelletEntry>,
        settings: AppSettings,
        targetSeason: String? = null
    ): IndustryMetrics {
        val seasonStr = targetSeason ?: settings.currentSeason
        val isAll = seasonStr.equals("ALL", ignoreCase = true)
        val validEntries = if (isAll) {
            entries
        } else {
            entries.filter { isDateInSeason(it.dateISO, seasonStr) }
        }

        val totalKg = validEntries.sumOf { it.kg }
        val totalCostKM = validEntries.sumOf { it.costKM }
        val activeDaysCount = validEntries.map { it.dateISO }.distinct().size

        // Broj kalendarskih dana od prvog unosa u sezoni do kraja aktivnog perioda
        val seasonDaysElapsed = if (validEntries.isNotEmpty()) {
            val firstDate = validEntries.minOf { it.dateISO }
            val lastDate = validEntries.maxOf { it.dateISO }
            val today = getTodayISO()
            val endDate = computeEffectivePeriodEndDate(firstDate, lastDate, today)
            (calendarDaysBetween(firstDate, endDate) + 1L).toInt().coerceAtLeast(1)
        } else 0

        val daysForDailyAverage = if (seasonDaysElapsed > 0) seasonDaysElapsed else activeDaysCount

        val area = if (settings.heatingAreaM2 > 0) settings.heatingAreaM2 else 100.0
        val power = if (settings.boilerPowerKw > 0) settings.boilerPowerKw else 20.0

        val kgPerM2Season = totalKg / area
        val kgPerKwBoiler = totalKg / power
        val kmPerM2 = totalCostKM / area
        val kmPerDay = if (daysForDailyAverage > 0) totalCostKM / daysForDailyAverage.toDouble() else 0.0
        val avgBagsPerDay = if (daysForDailyAverage > 0) validEntries.sumOf { it.bags } / daysForDailyAverage.toDouble() else 0.0

        // Analiza potrošnje po temperaturnim zonama (isključivo za odabranu sezonu)
        val tempBands = mutableMapOf<String, MutableList<Double>>()
        tempBands["Ispod 0°C (Mraz)"] = mutableListOf()
        tempBands["0°C do 5°C (Hladno)"] = mutableListOf()
        tempBands["5°C do 10°C (Svježe)"] = mutableListOf()
        tempBands["Iznad 10°C (Blago)"] = mutableListOf()

        validEntries.forEach { entry ->
            entry.avgTemp?.let { temp ->
                when {
                    temp < 0.0 -> tempBands["Ispod 0°C (Mraz)"]?.add(entry.bags)
                    temp <= 5.0 -> tempBands["0°C do 5°C (Hladno)"]?.add(entry.bags)
                    temp <= 10.0 -> tempBands["5°C do 10°C (Svježe)"]?.add(entry.bags)
                    else -> tempBands["Iznad 10°C (Blago)"]?.add(entry.bags)
                }
            }
        }

        val tempAnalysis = tempBands.map { (zone, bagsList) ->
            TempZoneStat(
                zoneName = zone,
                sampleCount = bagsList.size,
                avgBags = if (bagsList.isNotEmpty()) bagsList.average() else 0.0
            )
        }

        return IndustryMetrics(
            kgPerM2Season = kgPerM2Season,
            kgPerKwBoiler = kgPerKwBoiler,
            kmPerM2 = kmPerM2,
            kmPerDay = kmPerDay,
            avgBagsPerDay = avgBagsPerDay,
            activeDaysCount = activeDaysCount,
            totalKg = totalKg,
            totalCostKM = totalCostKM,
            tempZoneStats = tempAnalysis
        )
    }

    fun getTodayISO(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatNumber(value: Double, decimals: Int = 1): String {
        val pattern = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)
        val df = DecimalFormat(pattern)
        return df.format(value)
    }

    fun formatCurrency(value: Double, currency: String = "BAM"): String {
        val df = DecimalFormat("#,##0.00")
        return when (currency.uppercase()) {
            "EUR" -> "${df.format(value)} €"
            "DIN", "RSD" -> "${df.format(value)} din"
            else -> "${df.format(value)} KM"
        }
    }

    fun getCurrencySymbol(currency: String = "BAM"): String {
        return when (currency.uppercase()) {
            "EUR" -> "€"
            "DIN", "RSD" -> "din"
            else -> "KM"
        }
    }

    fun formatCurrencyKM(value: Double): String {
        return formatCurrency(value, "BAM")
    }

    fun getBagsLabel(bagsCount: Number, lang: String = "bs"): String {
        val count = kotlin.math.abs(kotlin.math.round(bagsCount.toDouble()).toLong())
        if (lang.lowercase() == "en") {
            return if (count == 1L) "bag" else "bags"
        }
        val rem100 = (count % 100).toInt()
        val rem10 = (count % 10).toInt()
        return if (rem100 in 11..14) {
            "vreća"
        } else when (rem10) {
            1 -> "vreća"
            2, 3, 4 -> "vreće"
            else -> "vreća"
        }
    }

    fun formatBagsWithUnit(bagsCount: Number, lang: String = "bs"): String {
        val count = kotlin.math.round(bagsCount.toDouble()).toLong()
        val label = getBagsLabel(count, lang)
        return "$count $label"
    }

    fun formatBagsCount(bagsCount: Number): String {
        return kotlin.math.round(bagsCount.toDouble()).toLong().toString()
    }

    fun formatDateHuman(isoDate: String, lang: String = "bs"): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = parser.parse(isoDate) ?: return isoDate
            val locale = when (lang.lowercase()) {
                "en" -> Locale.ENGLISH
                "sr" -> Locale("sr", "RS")
                "hr" -> Locale("hr", "HR")
                else -> Locale("bs", "BA")
            }
            val pattern = if (lang.lowercase() == "en") "MMM dd, yyyy" else "dd. MMM yyyy."
            val formatter = SimpleDateFormat(pattern, locale)
            formatter.format(date)
        } catch (_: Exception) {
            isoDate
        }
    }

    fun getDaysLoggedLabel(count: Int, lang: String = "bs"): String {
        if (lang.lowercase() == "en") {
            return if (count == 1) "day logged" else "days logged"
        }
        val rem100 = count % 100
        val rem10 = count % 10
        return if (rem100 in 11..14) {
            "dana unosa"
        } else when (rem10) {
            1 -> "dan unosa"
            2, 3, 4 -> "dana unosa"
            else -> "dana unosa"
        }
    }

    fun getTempZoneDisplayName(zoneName: String, lang: String): String {
        if (lang.lowercase() != "en") return zoneName
        return when {
            zoneName.contains("Mraz", ignoreCase = true) || zoneName.contains("Freezing", ignoreCase = true) -> "Below 0°C (Freezing)"
            zoneName.contains("Hladno", ignoreCase = true) || zoneName.contains("Cold", ignoreCase = true) -> "0°C to 5°C (Cold)"
            zoneName.contains("Svježe", ignoreCase = true) || zoneName.contains("Svjeze", ignoreCase = true) || zoneName.contains("Cool", ignoreCase = true) -> "5°C to 10°C (Cool)"
            zoneName.contains("Blago", ignoreCase = true) || zoneName.contains("Mild", ignoreCase = true) -> "Above 10°C (Mild)"
            else -> zoneName
        }
    }

    fun getInsulationLabel(insulation: String, lang: String): String {
        if (lang.lowercase() != "en") return insulation
        return when {
            insulation.contains("Bez", ignoreCase = true) || insulation.contains("No ", ignoreCase = true) -> "No insulation"
            insulation.contains("Djelimična", ignoreCase = true) || insulation.contains("Djelimicna", ignoreCase = true) || insulation.contains("Partial", ignoreCase = true) -> "Partial (5cm)"
            insulation.contains("Dobra", ignoreCase = true) || insulation.contains("Good", ignoreCase = true) -> "Good (10cm)"
            insulation.contains("Odlična", ignoreCase = true) || insulation.contains("Odlicna", ignoreCase = true) || insulation.contains("Excellent", ignoreCase = true) -> "Excellent (15cm+)"
            else -> insulation
        }
    }

    fun formatMonthName(yearMonth: String, lang: String = "bs"): String {
        return try {
            if (yearMonth.equals("Ostalo", ignoreCase = true) || yearMonth.equals("Other", ignoreCase = true)) {
                return if (lang.lowercase() == "en") "Other" else "Ostalo"
            }
            val parts = yearMonth.split("-")
            if (parts.size == 2) {
                val m = parts[1].toInt()
                val y = parts[0]
                val monthNames = when (lang.lowercase()) {
                    "en" -> listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )
                    else -> listOf(
                        "Januar", "Februar", "Mart", "April", "Maj", "Juni",
                        "Juli", "August", "Septembar", "Oktobar", "Novembar", "Decembar"
                    )
                }
                val mName = monthNames.getOrElse(m - 1) { "Month $m" }
                if (lang.lowercase() == "en") "$mName $y" else "$mName $y."
            } else yearMonth
        } catch (_: Exception) {
            yearMonth
        }
    }
}

data class InventorySummary(
    val totalPurchasedKg: Double,
    val totalPurchasedBags: Double,
    val totalPurchasedPallets: Double,
    val totalPurchasedCostKM: Double,
    val totalConsumedKg: Double,
    val totalConsumedBags: Double,
    val totalConsumedCostKM: Double,
    val remainingKg: Double,
    val remainingBags: Double,
    val remainingPallets: Double,
    val avgBags7Days: Double,
    val estimatedDaysLeft: Int?,
    val avgBagsPerDay: Double = avgBags7Days,
    val seasonDaysElapsed: Int = 0,
    val firstEntryDateISO: String? = null
)

data class IndustryMetrics(
    val kgPerM2Season: Double,
    val kgPerKwBoiler: Double,
    val kmPerM2: Double,
    val kmPerDay: Double,
    val avgBagsPerDay: Double,
    val activeDaysCount: Int,
    val totalKg: Double,
    val totalCostKM: Double,
    val tempZoneStats: List<TempZoneStat>
)

data class TempZoneStat(
    val zoneName: String,
    val sampleCount: Int,
    val avgBags: Double
)
