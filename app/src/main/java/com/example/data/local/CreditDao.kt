package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditDao {

    @Query("SELECT * FROM credit_entries ORDER BY isPaid ASC, createdAt DESC")
    fun getAllCreditEntries(): Flow<List<CreditEntryEntity>>

    @Query("SELECT * FROM credit_entries WHERE isPaid = 0 ORDER BY createdAt DESC")
    fun getPendingCreditEntries(): Flow<List<CreditEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: CreditEntryEntity)

    @Query("DELETE FROM credit_entries WHERE id = :id")
    suspend fun deleteCreditEntry(id: String)

    @Query("UPDATE credit_entries SET isPaid = :isPaid, paidDate = :paidDate WHERE id = :id")
    suspend fun updatePaidStatus(id: String, isPaid: Boolean, paidDate: String?)

    @Query("DELETE FROM credit_entries")
    suspend fun deleteAll()
}
