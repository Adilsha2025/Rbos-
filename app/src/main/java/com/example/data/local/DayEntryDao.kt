package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DayEntryDao {

    @Query("SELECT * FROM day_entries WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllDays(): Flow<List<DayEntryEntity>>

    @Query("SELECT * FROM day_entries WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedDays(): Flow<List<DayEntryEntity>>

    @Query("SELECT * FROM day_entries WHERE date = :date AND isDeleted = 0 LIMIT 1")
    suspend fun getDay(date: String): DayEntryEntity?

    @Query("SELECT * FROM day_entries WHERE date = :date LIMIT 1")
    suspend fun getDayIncludingDeleted(date: String): DayEntryEntity?

    @Query("SELECT * FROM day_entries WHERE date < :date AND isDeleted = 0 ORDER BY date DESC LIMIT 1")
    suspend fun getPreviousSavedDay(date: String): DayEntryEntity?

    @Query("SELECT * FROM day_entries WHERE isDeleted = 0 ORDER BY date DESC LIMIT 1")
    suspend fun getMostRecentDay(): DayEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: DayEntryEntity)

    @Query("UPDATE day_entries SET isDeleted = 1, deletedAt = :timestamp WHERE date = :date")
    suspend fun softDeleteDay(date: String, timestamp: Long)

    @Query("UPDATE day_entries SET isDeleted = 0, deletedAt = NULL WHERE date = :date")
    suspend fun restoreDay(date: String)

    @Query("DELETE FROM day_entries WHERE date = :date")
    suspend fun permanentlyDeleteDay(date: String)

    @Query("DELETE FROM day_entries WHERE isDeleted = 1 AND deletedAt < :cutoffTimestamp")
    suspend fun purgeOldDeletedDays(cutoffTimestamp: Long)

    @Query("DELETE FROM day_entries WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Query("DELETE FROM day_entries")
    suspend fun deleteAll()
}
