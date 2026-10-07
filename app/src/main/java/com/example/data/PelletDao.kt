package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PelletDao {
    // --- Unosi potrošnje (Entries) ---
    @Query("SELECT * FROM pellet_entries ORDER BY dateISO DESC, id DESC")
    fun getAllEntries(): Flow<List<PelletEntry>>

    @Query("SELECT * FROM pellet_entries ORDER BY dateISO DESC, id DESC")
    suspend fun getAllEntriesOnce(): List<PelletEntry>

    @Query("SELECT * FROM pellet_entries WHERE dateISO = :dateISO LIMIT 1")
    fun getEntryByDate(dateISO: String): Flow<PelletEntry?>

    @Query("SELECT * FROM pellet_entries WHERE dateISO = :dateISO LIMIT 1")
    suspend fun getEntryByDateOnce(dateISO: String): PelletEntry?

    @Query("SELECT * FROM pellet_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): PelletEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PelletEntry): Long

    @Update
    suspend fun updateEntry(entry: PelletEntry)

    @Delete
    suspend fun deleteEntry(entry: PelletEntry)

    @Query("DELETE FROM pellet_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    // --- Nabavka / Kupovina (Purchases) ---
    @Query("SELECT * FROM pellet_purchases ORDER BY dateISO DESC, id DESC")
    fun getAllPurchases(): Flow<List<PelletPurchase>>

    @Query("SELECT * FROM pellet_purchases ORDER BY dateISO DESC, id DESC")
    suspend fun getAllPurchasesOnce(): List<PelletPurchase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PelletPurchase): Long

    @Update
    suspend fun updatePurchase(purchase: PelletPurchase)

    @Delete
    suspend fun deletePurchase(purchase: PelletPurchase)

    @Query("DELETE FROM pellet_purchases WHERE id = :id")
    suspend fun deletePurchaseById(id: Long)

    // --- Postavke (Settings) ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSettings(settings: AppSettings)
}
