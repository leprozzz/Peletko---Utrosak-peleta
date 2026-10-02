package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pellet_purchases")
data class PelletPurchase(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateISO: String, // YYYY-MM-DD
    val pallets: Double, // Npr. 2.0, 2.5
    val bags: Double, // pallets * 70 ili direktan broj vreća
    val kg: Double, // bags * 15.0
    val totalPriceKM: Double, // Ukupno plaćeno u KM
    val pricePerPalletKM: Double, // totalPriceKM / pallets
    val pricePerKgKM: Double, // totalPriceKM / kg
    val supplier: String = "", // Proizvođač / Dobavljač peleta
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
