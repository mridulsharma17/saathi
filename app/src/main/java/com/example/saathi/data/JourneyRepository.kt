package com.example.saathi.data

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class JourneyRepository {
    private val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
    private val usersRef = database.getReference("users")

    suspend fun saveJourneyMemory(userId: String, memory: JourneyMemory): Boolean {
        return try {
            val journeyRef = usersRef.child(userId).child("journey")
            val newMemoryRef = if (memory.id.isEmpty()) journeyRef.push() else journeyRef.child(memory.id)
            val finalMemory = memory.copy(id = newMemoryRef.key ?: "")
            newMemoryRef.setValue(finalMemory).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getJourneyMemories(userId: String): List<JourneyMemory> {
        return try {
            val snapshot = usersRef.child(userId).child("journey").get().await()
            snapshot.children.mapNotNull { it.getValue(JourneyMemory::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
