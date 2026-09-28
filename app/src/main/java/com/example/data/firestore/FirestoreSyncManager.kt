package com.example.data.firestore

import android.util.Log
import com.example.model.DayEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object FirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"
    private const val COLLECTION_NAME = "daily_diary_entries"

    fun syncDayEntry(entry: DayEntry, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val dataMap = hashMapOf(
                "date" to entry.date,
                "dateDisplay" to entry.dateDisplay,
                "total" to entry.total,
                "locked" to entry.locked,
                "notes" to entry.notes,
                "bankDepositAmount" to entry.bankDepositAmount,
                "bankDepositSlipUri" to (entry.bankDepositSlipUri ?: ""),
                "updatedAt" to entry.updatedAt,
                "items" to entry.items.map {
                    mapOf("name" to it.name, "amount" to it.amount)
                },
                "edits" to entry.edits.map {
                    mapOf(
                        "field" to it.field,
                        "oldValue" to it.oldValue,
                        "newValue" to it.newValue,
                        "time" to it.time
                    )
                }
            )

            firestore.collection(COLLECTION_NAME)
                .document(entry.date)
                .set(dataMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced entry ${entry.date} to Firestore")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync to Firestore: ${e.message}")
                    onComplete?.invoke(false)
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Firestore unavailable: ${e.message}")
            onComplete?.invoke(false)
        }
    }
}
