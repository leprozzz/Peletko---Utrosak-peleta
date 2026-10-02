package com.example

import com.example.data.AppSettings
import com.example.data.PelletEntry
import com.example.data.PelletPurchase
import com.example.util.PelletCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPelletConstantsAndFormulas() {
        // 1 vreća = 15 kg
        assertEquals(15.0, PelletCalculator.computeKgFromBags(1.0), 0.001)
        assertEquals(30.0, PelletCalculator.computeKgFromBags(2.0), 0.001)
        assertEquals(37.5, PelletCalculator.computeKgFromBags(2.5), 0.001)

        // 1 paleta = 70 vreća = 1050 kg
        assertEquals(70.0, PelletCalculator.BAGS_PER_PALLET, 0.001)
        assertEquals(1050.0, PelletCalculator.KG_PER_PALLET, 0.001)
        assertEquals(2.0, PelletCalculator.computeBagsFromKg(30.0), 0.001)
    }

    @Test
    fun testWeightedAveragePrice() {
        // Nabavka 1: 1 paleta (70 vreća = 1050 kg), plaćeno 500 KM -> 500 / 1050 = ~0.476 KM/kg
        // Nabavka 2: 2 palete (140 vreća = 2100 kg), plaćeno 1100 KM -> ukupno 1600 KM / 3150 kg
        val p1 = PelletPurchase(
            id = 1,
            dateISO = "2024-10-01",
            pallets = 1.0,
            bags = 70.0,
            kg = 1050.0,
            totalPriceKM = 500.0,
            pricePerPalletKM = 500.0,
            pricePerKgKM = 500.0 / 1050.0
        )
        val p2 = PelletPurchase(
            id = 2,
            dateISO = "2024-11-01",
            pallets = 2.0,
            bags = 140.0,
            kg = 2100.0,
            totalPriceKM = 1100.0,
            pricePerPalletKM = 550.0,
            pricePerKgKM = 1100.0 / 2100.0
        )

        val weighted = PelletCalculator.computeWeightedAvgPricePerKg(listOf(p1, p2))
        assertEquals(1600.0 / 3150.0, weighted, 0.0001)
    }

    @Test
    fun testInventorySummaryAndMetrics() {
        val purchases = listOf(
            PelletPurchase(
                id = 1,
                dateISO = "2025-10-01",
                pallets = 2.0,
                bags = 140.0,
                kg = 2100.0,
                totalPriceKM = 1000.0,
                pricePerPalletKM = 500.0,
                pricePerKgKM = 1000.0 / 2100.0
            )
        )

        val entries = listOf(
            PelletEntry(
                id = 1,
                dateISO = "2025-10-02",
                bags = 2.0,
                kg = 30.0,
                costKM = 14.28,
                avgTemp = 2.0
            ),
            PelletEntry(
                id = 2,
                dateISO = "2025-10-03",
                bags = 1.5,
                kg = 22.5,
                costKM = 10.71,
                avgTemp = -1.0
            )
        )

        val inv = PelletCalculator.computeInventory(purchases, entries)
        assertEquals(2100.0 - 52.5, inv.remainingKg, 0.001)
        assertEquals(140.0 - 3.5, inv.remainingBags, 0.001)
        assertTrue(inv.estimatedDaysLeft != null && inv.estimatedDaysLeft!! > 50)

        val settings = AppSettings(
            heatingAreaM2 = 100.0,
            boilerPowerKw = 20.0
        )
        val metrics = PelletCalculator.computeMetrics(entries, settings)
        assertEquals(52.5 / 100.0, metrics.kgPerM2Season, 0.001)
        assertEquals(52.5 / 20.0, metrics.kgPerKwBoiler, 0.001)
        assertEquals(24.99 / 100.0, metrics.kmPerM2, 0.01)
    }

    @Test
    fun testSeasonAverageForecast() {
        val purchases = listOf(
            PelletPurchase(
                id = 1,
                dateISO = "2024-10-01",
                pallets = 2.0,
                bags = 140.0,
                kg = 2100.0,
                totalPriceKM = 1000.0,
                pricePerPalletKM = 500.0,
                pricePerKgKM = 1000.0 / 2100.0
            )
        )
        // 10 unosa po 3 vreće (ukupno 30 vreća) u periodu od 30 dana (od 10.10.2024 do 08.11.2024)
        val entries = mutableListOf<PelletEntry>()
        for (i in 0 until 10) {
            val day = 10 + (i * 3) // dani: 10, 13, 16, 19, 22, 25, 28, 31. oktobar, 3, 6. novembar
            val dateStr = if (day <= 31) String.format("2024-10-%02d", day) else String.format("2024-11-%02d", day - 31)
            entries.add(
                PelletEntry(
                    id = (i + 1).toLong(),
                    dateISO = dateStr,
                    bags = 3.0,
                    kg = 45.0,
                    costKM = 21.4
                )
            )
        }

        val settings = AppSettings(currentSeason = "2024/2025")
        // Danas je 30. dan (08.11.2024) od početka (10.10.2024)
        val todayISO = "2024-11-08"
        val inv = PelletCalculator.computeInventory(purchases, entries, settings, todayISO)

        assertEquals(110.0, inv.remainingBags, 0.001) // 140 - 30 = 110 vreća preostalo
        assertEquals(30, inv.seasonDaysElapsed)
        assertEquals(1.0, inv.avgBagsPerDay, 0.001) // 30 vreća / 30 dana = 1.0 vreća/dan
        assertEquals(110, inv.estimatedDaysLeft) // 110 vreća / 1.0 = 110 dana
    }

    @Test
    fun testDecemberJanuaryTrendForecast() {
        val purchases = listOf(
            PelletPurchase(
                id = 1,
                dateISO = "2024-11-25",
                pallets = 3.0,
                bags = 210.0,
                kg = 3150.0,
                totalPriceKM = 1680.0, // 8 KM po vreći
                pricePerPalletKM = 560.0,
                pricePerKgKM = 1680.0 / 3150.0
            )
        )

        val entries = mutableListOf<PelletEntry>()
        // Decembar: 45 vreća kroz 31 dan (od 2024-12-01 do 2024-12-31)
        // 15 unosa po 3 vreće
        for (i in 0 until 15) {
            val day = 1 + (i * 2)
            entries.add(
                PelletEntry(
                    id = (i + 1).toLong(),
                    dateISO = String.format("2024-12-%02d", day),
                    bags = 3.0,
                    kg = 45.0,
                    costKM = 24.0 // 3 * 8 KM
                )
            )
        }
        // Januar: 30 vreća kroz 31 dan (od 2025-01-01 do 2025-01-31)
        // 15 unosa koji pokrivaju do 31. januara
        for (i in 0 until 15) {
            val day = if (i == 14) 31 else 1 + (i * 2)
            entries.add(
                PelletEntry(
                    id = (i + 16).toLong(),
                    dateISO = String.format("2025-01-%02d", day),
                    bags = 2.0,
                    kg = 30.0,
                    costKM = 16.0 // 2 * 8 KM
                )
            )
        }

        val settings = AppSettings(
            currentSeason = "2024/2025",
            heatingAreaM2 = 100.0,
            boilerPowerKw = 20.0
        )

        // Scenarij A: Kraj januara (31.01.2025)
        val invJan31 = PelletCalculator.computeInventory(purchases, entries, settings, todayISO = "2025-01-31")
        assertEquals(210.0 - 75.0, invJan31.remainingBags, 0.001) // 135 vreća preostalo
        assertEquals(62, invJan31.seasonDaysElapsed) // 31 dec + 31 jan = 62 dana
        assertEquals(75.0 / 62.0, invJan31.avgBagsPerDay, 0.01) // ~1.21 vreća/dan
        assertEquals((135.0 / (75.0 / 62.0)).toInt(), invJan31.estimatedDaysLeft) // ~111 dana

        // Scenarij B: 1. februar (63. dan od početka)
        val invFeb1 = PelletCalculator.computeInventory(purchases, entries, settings, todayISO = "2025-02-01")
        assertEquals(63, invFeb1.seasonDaysElapsed)
        assertEquals(75.0 / 63.0, invFeb1.avgBagsPerDay, 0.01) // ~1.19 vreća/dan

        // Scenarij C: Korisnik testira podatke na uređaju čiji je sat u oktobru (danas znatno kasnije)
        // Aplikacija ne smije dijeliti sa stotinama dana pauze, nego sa 62 dana aktivnog perioda!
        val invLater = PelletCalculator.computeInventory(purchases, entries, settings, todayISO = "2026-10-01")
        assertEquals(62, invLater.seasonDaysElapsed)
        assertEquals(75.0 / 62.0, invLater.avgBagsPerDay, 0.01) // ~1.21 vreća/dan

        // Provjera detaljne statistike: 600 KM / 62 dana = 9.68 KM/dan (odnosno 8 KM * 1.21 vreća/dan)
        val metrics = PelletCalculator.computeMetrics(entries, settings)
        assertEquals(600.0 / 62.0, metrics.kmPerDay, 0.01) // ~9.68 KM/dan
        assertEquals(75.0 / 62.0, metrics.avgBagsPerDay, 0.01) // ~1.21 vreća/dan
    }

    @Test
    fun testCurrencyFormatting() {
        assertEquals("15.00 KM", PelletCalculator.formatCurrency(15.0, "BAM"))
        assertEquals("15.00 €", PelletCalculator.formatCurrency(15.0, "EUR"))
        assertEquals("15.00 din", PelletCalculator.formatCurrency(15.0, "DIN"))
        assertEquals("€", PelletCalculator.getCurrencySymbol("EUR"))
        assertEquals("KM", PelletCalculator.getCurrencySymbol("BAM"))
        assertEquals("din", PelletCalculator.getCurrencySymbol("DIN"))
    }

    @Test
    fun testLanguageDictionary() {
        assertEquals("Peletko", com.example.util.AppStrings.get("app_title", "bs"))
        assertEquals("Peletko", com.example.util.AppStrings.get("app_title", "en"))
        assertEquals("Spremi", com.example.util.AppStrings.get("save", "hr"))
        assertEquals("Sačuvaj", com.example.util.AppStrings.get("save", "sr"))
    }

    @Test
    fun testBoilerPowerLimitsAndSeasonDeclaration() {
        // 5-15 kW: 3 vreće
        assertEquals(3, PelletCalculator.getMaxDailyBagsForBoiler(5.0))
        assertEquals(3, PelletCalculator.getMaxDailyBagsForBoiler(15.0))

        // 16-25 kW: 5 vreća
        assertEquals(5, PelletCalculator.getMaxDailyBagsForBoiler(16.0))
        assertEquals(5, PelletCalculator.getMaxDailyBagsForBoiler(25.0))

        // 26-45 kW: 7 vreća
        assertEquals(7, PelletCalculator.getMaxDailyBagsForBoiler(30.0))
        assertEquals(7, PelletCalculator.getMaxDailyBagsForBoiler(45.0))

        // 46-70 kW: 12 vreća
        assertEquals(12, PelletCalculator.getMaxDailyBagsForBoiler(50.0))
        assertEquals(12, PelletCalculator.getMaxDailyBagsForBoiler(70.0))

        // 71+ kW: 30 vreća
        assertEquals(30, PelletCalculator.getMaxDailyBagsForBoiler(75.0))
        assertEquals(30, PelletCalculator.getMaxDailyBagsForBoiler(100.0))

        // Deklarisana sezona: ako se unese 2026 -> 2026/2027, ako 2027 -> 2027/2028
        assertEquals("2026/2027", PelletCalculator.computeSeason("2026"))
        assertEquals("2026/2027", PelletCalculator.computeSeason("2026-03-12"))
        assertEquals("2027/2028", PelletCalculator.computeSeason("2027"))
        assertEquals("2027/2028", PelletCalculator.computeSeason("2027-11-20"))
    }

    @Test
    fun testBagsGrammarAndFormatting() {
        assertEquals("1 vreća", PelletCalculator.formatBagsWithUnit(1, "bs"))
        assertEquals("2 vreće", PelletCalculator.formatBagsWithUnit(2, "bs"))
        assertEquals("3 vreće", PelletCalculator.formatBagsWithUnit(3, "bs"))
        assertEquals("4 vreće", PelletCalculator.formatBagsWithUnit(4, "bs"))
        assertEquals("5 vreća", PelletCalculator.formatBagsWithUnit(5, "bs"))
        assertEquals("10 vreća", PelletCalculator.formatBagsWithUnit(10, "bs"))
        assertEquals("11 vreća", PelletCalculator.formatBagsWithUnit(11, "bs"))
        assertEquals("12 vreća", PelletCalculator.formatBagsWithUnit(12, "bs"))
        assertEquals("14 vreća", PelletCalculator.formatBagsWithUnit(14, "bs"))
        assertEquals("21 vreća", PelletCalculator.formatBagsWithUnit(21, "bs"))
        assertEquals("22 vreće", PelletCalculator.formatBagsWithUnit(22, "bs"))
        assertEquals("24 vreće", PelletCalculator.formatBagsWithUnit(24, "bs"))
        assertEquals("100 vreća", PelletCalculator.formatBagsWithUnit(100, "bs"))
        assertEquals("101 vreća", PelletCalculator.formatBagsWithUnit(101, "bs"))
        assertEquals("200 vreća", PelletCalculator.formatBagsWithUnit(200, "bs"))
        assertEquals("204 vreće", PelletCalculator.formatBagsWithUnit(204, "bs"))

        // Cijeli brojevi bez decimala
        assertEquals("204", PelletCalculator.formatBagsCount(204.0))
        assertEquals("100", PelletCalculator.formatBagsCount(100.0))
    }
}
