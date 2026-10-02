package com.example.data

import kotlinx.coroutines.flow.Flow

class PelletRepository(private val pelletDao: PelletDao) {

    val allEntries: Flow<List<PelletEntry>> = pelletDao.getAllEntries()
    val allPurchases: Flow<List<PelletPurchase>> = pelletDao.getAllPurchases()
    val settings: Flow<AppSettings?> = pelletDao.getSettings()

    suspend fun getEntryByDateOnce(dateISO: String): PelletEntry? =
        pelletDao.getEntryByDateOnce(dateISO)

    suspend fun getAllEntriesOnce(): List<PelletEntry> =
        pelletDao.getAllEntriesOnce()

    suspend fun getSettingsOnce(): AppSettings? =
        pelletDao.getSettingsOnce()

    suspend fun insertEntry(entry: PelletEntry): Long =
        pelletDao.insertEntry(entry)

    suspend fun updateEntry(entry: PelletEntry) =
        pelletDao.updateEntry(entry)

    suspend fun deleteEntry(entry: PelletEntry) =
        pelletDao.deleteEntry(entry)

    suspend fun deleteEntryById(id: Long) =
        pelletDao.deleteEntryById(id)

    suspend fun insertPurchase(purchase: PelletPurchase): Long =
        pelletDao.insertPurchase(purchase)

    suspend fun updatePurchase(purchase: PelletPurchase) =
        pelletDao.updatePurchase(purchase)

    suspend fun deletePurchase(purchase: PelletPurchase) =
        pelletDao.deletePurchase(purchase)

    suspend fun deletePurchaseById(id: Long) =
        pelletDao.deletePurchaseById(id)

    suspend fun updateSettings(settings: AppSettings) =
        pelletDao.setSettings(settings)
}
