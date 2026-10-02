package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.AppSettings
import com.example.data.PelletEntry
import com.example.data.PelletPurchase
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    /**
     * Generiše formatiran CSV sa UTF-8 BOM bajtovima tako da Microsoft Excel i drugi
     * programi za tabele odmah prepoznaju naša slova (š, đ, č, ć, ž) bez deformacije.
     * Koristi tačka-zarez (;) kao standardni separator za evropske i regionalne Excel postavke.
     */
    fun generateSeasonCsv(
        season: String,
        settings: AppSettings,
        inventory: InventorySummary,
        metrics: IndustryMetrics,
        entries: List<PelletEntry>,
        purchases: List<PelletPurchase>
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM
        sb.append("\uFEFF")

        val todayStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val isAllSeasons = season.equals("ALL", ignoreCase = true)
        val seasonLabel = if (isAllSeasons) "Sve sezone (Cjelokupni period)" else "Sezona $season"

        // Filtriraj unose i nabavke po sezoni
        val filteredEntries = if (isAllSeasons) {
            entries.sortedBy { it.dateISO }
        } else {
            entries.filter { PelletCalculator.isDateInSeason(it.dateISO, season) }.sortedBy { it.dateISO }
        }

        val filteredPurchases = if (isAllSeasons) {
            purchases.sortedBy { it.dateISO }
        } else {
            purchases.filter { PelletCalculator.isDateInSeason(it.dateISO, season) }.sortedBy { it.dateISO }
        }

        val totalSeasonBags = filteredEntries.sumOf { it.bags }
        val totalSeasonKg = filteredEntries.sumOf { it.kg }
        val totalSeasonCostKM = filteredEntries.sumOf { it.costKM }

        // 1. ZAGLAVLJE DOKUMENTA
        sb.append("========================================================================\n")
        sb.append("PELETKO - IZVJEŠTAJ O POTROŠNJI PELETA I STANJU ZALIHA\n")
        sb.append("========================================================================\n")
        sb.append("Datum kreiranja izvještaja:;").append(todayStr).append("\n")
        sb.append("Odabrana sezona:;").append(seasonLabel).append("\n")
        sb.append("Grad / Lokacija:;").append(escapeCsv(settings.city)).append("\n")
        sb.append("Grejna površina:;").append(PelletCalculator.formatNumber(settings.heatingAreaM2, 0)).append(" m²\n")
        sb.append("Snaga kotla/peći:;").append(PelletCalculator.formatNumber(settings.boilerPowerKw, 0)).append(" kW\n")
        sb.append("Izolacija objekta:;").append(escapeCsv(settings.insulationLevel)).append("\n")
        sb.append("Klasa peleta:;").append(escapeCsv(settings.pelletClass)).append("\n")
        sb.append("Valuta:;").append(settings.currency).append("\n\n")

        // 2. REZIME POTROŠNJE ZA SEZONU
        sb.append("------------------------------------------------------------------------\n")
        sb.append("STATISTIČKI REZIME POTROŠNJE (").append(seasonLabel).append(")\n")
        sb.append("------------------------------------------------------------------------\n")
        sb.append("Ukupno utrošeno vreća peleta:;").append(PelletCalculator.formatNumber(totalSeasonBags, 1)).append(" vreća\n")
        sb.append("Ukupno utrošeno kilograma (kg):;").append(PelletCalculator.formatNumber(totalSeasonKg, 1)).append(" kg\n")
        sb.append("Ukupni trošak grijanja:;").append(PelletCalculator.formatNumber(totalSeasonCostKM, 2)).append(" ").append(settings.currency).append("\n")
        sb.append("Broj unosa (aktivno loženi dani):;").append(filteredEntries.size).append(" unosa\n")

        val daysCount = metrics.activeDaysCount
        sb.append("Dnevni prosjek potrošnje:;").append(PelletCalculator.formatNumber(metrics.avgBagsPerDay, 2)).append(" vreća/dan\n")
        sb.append("Dnevni prosječni trošak:;").append(PelletCalculator.formatNumber(metrics.kmPerDay, 2)).append(" ").append(settings.currency).append("/dan\n")
        sb.append("Specifična potrošnja po površini:;").append(PelletCalculator.formatNumber(metrics.kgPerM2Season, 2)).append(" kg/m²\n")
        sb.append("Specifična potrošnja po snazi:;").append(PelletCalculator.formatNumber(metrics.kgPerKwBoiler, 2)).append(" kg/kW\n")
        sb.append("Trošak po kvadratnom metru:;").append(PelletCalculator.formatNumber(metrics.kmPerM2, 2)).append(" ").append(settings.currency).append("/m²\n\n")

        // 3. STANJE MAGACINA (KUMULATIVNO)
        sb.append("------------------------------------------------------------------------\n")
        sb.append("TRENUTNO STANJE MAGACINA (ZALIHE PELETA)\n")
        sb.append("------------------------------------------------------------------------\n")
        sb.append("Ukupno kupljeno peleta u magacin:;").append(PelletCalculator.formatNumber(inventory.totalPurchasedBags, 0)).append(" vreća (")
            .append(PelletCalculator.formatNumber(inventory.totalPurchasedPallets, 2)).append(" paleta / ")
            .append(PelletCalculator.formatNumber(inventory.totalPurchasedKg, 0)).append(" kg)\n")
        sb.append("Ukupno potrošeno do sada:;").append(PelletCalculator.formatNumber(inventory.totalConsumedBags, 0)).append(" vreća (")
            .append(PelletCalculator.formatNumber(inventory.totalConsumedKg, 0)).append(" kg)\n")
        sb.append("TRENUTNO PREOSTALO NA STANJU:;").append(PelletCalculator.formatNumber(inventory.remainingBags, 1)).append(" vreća (")
            .append(PelletCalculator.formatNumber(inventory.remainingPallets, 2)).append(" paleta / ")
            .append(PelletCalculator.formatNumber(inventory.remainingKg, 0)).append(" kg)\n")

        val daysLeftStr = inventory.estimatedDaysLeft?.let { "$it dana" } ?: "N/A"
        sb.append("Procijenjeno trajanje zaliha:;").append(daysLeftStr).append("\n\n")

        // 4. DETALJNA TABELA DNEVNIH UNOSA
        sb.append("------------------------------------------------------------------------\n")
        sb.append("DETALJNI DNEVNI UNOSI POTROŠNJE PELETA\n")
        sb.append("------------------------------------------------------------------------\n")
        sb.append("Datum;Vreće;Kilogrami (kg);Iznos (").append(settings.currency).append(");Prosj. Temp (°C);Jutarnja (08h);Dnevna (13h);Večernja (20h);Napomena\n")

        for (e in filteredEntries) {
            val dateFmt = formatDateLocal(e.dateISO)
            val bagsStr = PelletCalculator.formatNumber(e.bags, 1)
            val kgStr = PelletCalculator.formatNumber(e.kg, 1)
            val costStr = PelletCalculator.formatNumber(e.costKM, 2)
            val tempStr = e.avgTemp?.let { PelletCalculator.formatNumber(it, 1) } ?: "-"
            val t08Str = e.t08?.let { PelletCalculator.formatNumber(it, 1) } ?: "-"
            val t13Str = e.t13?.let { PelletCalculator.formatNumber(it, 1) } ?: "-"
            val t20Str = e.t20?.let { PelletCalculator.formatNumber(it, 1) } ?: "-"
            val noteStr = escapeCsv(e.notes)

            sb.append(dateFmt).append(";")
                .append(bagsStr).append(";")
                .append(kgStr).append(";")
                .append(costStr).append(";")
                .append(tempStr).append(";")
                .append(t08Str).append(";")
                .append(t13Str).append(";")
                .append(t20Str).append(";")
                .append(noteStr).append("\n")
        }
        sb.append("\n")

        // 5. DETALJNA TABELA NABAVKI PELETA
        sb.append("------------------------------------------------------------------------\n")
        sb.append("ISTORIJA NABAVKI PELETA U MAGACIN\n")
        sb.append("------------------------------------------------------------------------\n")
        sb.append("Datum;Palete;Vreće;Kilogrami (kg);Cijena/paleti (").append(settings.currency).append(");Ukupno (").append(settings.currency).append(");Dobavljač;Napomena\n")

        for (p in filteredPurchases) {
            val dateFmt = formatDateLocal(p.dateISO)
            val palStr = PelletCalculator.formatNumber(p.pallets, 2)
            val bagsStr = PelletCalculator.formatNumber(p.bags, 0)
            val kgStr = PelletCalculator.formatNumber(p.kg, 0)
            val pppStr = PelletCalculator.formatNumber(p.pricePerPalletKM, 2)
            val totalStr = PelletCalculator.formatNumber(p.totalPriceKM, 2)
            val supplierStr = escapeCsv(p.supplier)
            val noteStr = escapeCsv(p.notes)

            sb.append(dateFmt).append(";")
                .append(palStr).append(";")
                .append(bagsStr).append(";")
                .append(kgStr).append(";")
                .append(pppStr).append(";")
                .append(totalStr).append(";")
                .append(supplierStr).append(";")
                .append(noteStr).append("\n")
        }

        return sb.toString()
    }

    /**
     * Dijeli CSV fajl putem standardnog Android Share dijaloga (Excel, Sheets, Gmail, Drive, WhatsApp).
     */
    fun shareCsv(context: Context, filename: String, csvContent: String) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, filename)
            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Peletko Izvještaj - $filename")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Podijeli ili otvori izvještaj u Excelu")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Greška pri kreiranju fajla: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Direktno upisuje CSV sadržaj u odabrani URI (Storage Access Framework).
     */
    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(csvContent.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun escapeCsv(text: String): String {
        if (text.isBlank()) return ""
        val clean = text.replace("\"", "\"\"")
        return if (clean.contains(";") || clean.contains("\n") || clean.contains("\"")) {
            "\"$clean\""
        } else {
            clean
        }
    }

    private fun formatDateLocal(isoDate: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfOut = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val d = sdfIn.parse(isoDate)
            if (d != null) sdfOut.format(d) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }
}
