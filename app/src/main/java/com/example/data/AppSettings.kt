package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val heatingAreaM2: Double = 120.0,
    val boilerPowerKw: Double = 20.0,
    val insulationLevel: String = "Srednja (10cm)", // "Bez izolacije", "Djelimična (5cm)", "Dobra (10cm)", "Odlična (15cm+)"
    val city: String = "Sarajevo",
    val pelletBrand: String = "Medex", // npr. Medex, Drvoprodex, Fagus
    val pelletClass: String = "A1", // "A1", "A2", "Bez klase"
    val defaultPricePerPalletKM: Double = 525.0, // 7.50 KM po vreći (~0.50 KM/kg)
    val allowNegativeStock: Boolean = true,
    val isOnboarded: Boolean = false,
    val currentSeason: String = "2025/2026",
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val language: String = "bs", // "bs", "sr", "hr", "en"
    val currency: String = "BAM", // "BAM", "EUR", "DIN"
    val autoFetchWeather: Boolean = true
)
