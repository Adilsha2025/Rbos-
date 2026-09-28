package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.CreditEntry

@Entity(tableName = "credit_entries")
data class CreditEntryEntity(
    @PrimaryKey
    val id: String,
    val personName: String,
    val amount: Long,
    val phone: String = "",
    val startDate: String,
    val notes: String = "",
    val isPaid: Boolean = false,
    val paidDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): CreditEntry {
        return CreditEntry(
            id = id,
            personName = personName,
            amount = amount,
            phone = phone,
            startDate = startDate,
            notes = notes,
            isPaid = isPaid,
            paidDate = paidDate,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(entry: CreditEntry): CreditEntryEntity {
            return CreditEntryEntity(
                id = entry.id,
                personName = entry.personName,
                amount = entry.amount,
                phone = entry.phone,
                startDate = entry.startDate,
                notes = entry.notes,
                isPaid = entry.isPaid,
                paidDate = entry.paidDate,
                createdAt = entry.createdAt
            )
        }
    }
}
