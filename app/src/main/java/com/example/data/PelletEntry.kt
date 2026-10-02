package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pellet_entries")
data class PelletEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateISO: String, // YYYY-MM-DD
    val bags: Double, // Npr. 1.0, 1.5, 2.0
    val kg: Double, // bags * 15.0
    val costKM: Double, // kg * cijenaPoKg
    val avgTemp: Double? = null, // Prosječna vanjska temperatura u °C
    val t08: Double? = null, // Temp u 08h
    val t13: Double? = null, // Temp u 13h
    val t20: Double? = null, // Temp u 20h
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
