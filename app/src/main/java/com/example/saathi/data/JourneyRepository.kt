package com.example.saathi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.Locale

class JourneyRepository {
    private val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
    private val usersRef = database.getReference("users")

    private fun getUserKeys(userId: String): List<String> {
        val keys = mutableListOf<String>()
        if (userId.isNotBlank()) keys.add(userId)
        
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email
        if (!email.isNullOrBlank()) {
            val sanitizedEmail = email.replace(".", ",")
            if (!keys.contains(sanitizedEmail)) {
                keys.add(sanitizedEmail)
            }
        }
        return keys
    }

    fun normalizeCategory(category: String): String {
        val trimmed = category.lowercase(Locale.getDefault()).trim()
        return when {
            trimmed.contains("education") || trimmed.contains("academic") || trimmed.contains("study") || trimmed.contains("exam") || trimmed.contains("college") || trimmed.contains("grade") || trimmed.contains("school") -> "education"
            trimmed.contains("career") || trimmed.contains("job") || trimmed.contains("work") || trimmed.contains("placement") || trimmed.contains("internship") || trimmed.contains("office") -> "career"
            trimmed.contains("relation") || trimmed.contains("family") || trimmed.contains("love") || trimmed.contains("friend") || trimmed.contains("marriage") -> "relationship"
            trimmed.contains("achieve") || trimmed.contains("win") || trimmed.contains("goal") || trimmed.contains("award") || trimmed.contains("prize") -> "achievement"
            trimmed.contains("travel") || trimmed.contains("trip") || trimmed.contains("vacation") || trimmed.contains("tour") -> "travel"
            trimmed.contains("milestone") || trimmed.contains("life") -> "milestone"
            else -> if (trimmed.isBlank()) "general" else trimmed
        }
    }

    suspend fun saveJourneyMemory(userId: String, memory: JourneyMemory): Boolean {
        return try {
            val existingMemories = getJourneyMemories(userId)
            
            val newTitleNorm = memory.title.lowercase(Locale.getDefault()).trim()
            val newSourceNorm = memory.sourceMessage.lowercase(Locale.getDefault()).trim()

            // Duplicate Guard: Check if the same achievement/title or source message is already saved
            val isDuplicate = existingMemories.any { existing ->
                val exTitleNorm = existing.title.lowercase(Locale.getDefault()).trim()
                val exSourceNorm = existing.sourceMessage.lowercase(Locale.getDefault()).trim()

                exTitleNorm == newTitleNorm ||
                (newSourceNorm.isNotBlank() && exSourceNorm == newSourceNorm) ||
                (exTitleNorm.contains(newTitleNorm) && newTitleNorm.length > 5) ||
                (newTitleNorm.contains(exTitleNorm) && exTitleNorm.length > 5)
            }

            if (isDuplicate) {
                // Already saved in Journey! Prevent duplicate entry.
                return true
            }

            val journeyType = normalizeCategory(memory.category)
            val userKeys = getUserKeys(userId)
            val primaryKey = userKeys.firstOrNull() ?: userId

            val typeRef = usersRef.child(primaryKey).child("journey").child(journeyType)
            val newMemoryRef = if (memory.id.isEmpty()) typeRef.push() else typeRef.child(memory.id)
            val finalMemory = memory.copy(
                id = newMemoryRef.key ?: "",
                category = journeyType
            )
            newMemoryRef.setValue(finalMemory).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getJourneyMemories(userId: String): List<JourneyMemory> {
        val userKeys = getUserKeys(userId)
        val allMemories = mutableListOf<JourneyMemory>()

        for (userKey in userKeys) {
            try {
                val snapshot = usersRef.child(userKey).child("journey").get().await()
                if (snapshot.exists()) {
                    snapshot.children.forEach { typeChild ->
                        if (typeChild.hasChildren()) {
                            typeChild.children.forEach { memoryChild ->
                                val memory = memoryChild.getValue(JourneyMemory::class.java)
                                if (memory != null && memory.title.isNotBlank()) {
                                    allMemories.add(memory)
                                }
                            }
                        } else {
                            val memory = typeChild.getValue(JourneyMemory::class.java)
                            if (memory != null && memory.title.isNotBlank()) {
                                allMemories.add(memory)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue checking next key
            }
        }

        return allMemories.distinctBy { if (it.id.isNotEmpty()) it.id else "${it.title}_${it.createdAt}" }
            .sortedWith(
                compareByDescending<JourneyMemory> { it.importance }
                    .thenByDescending { it.createdAt }
            )
    }

    suspend fun getJourneyMemoriesByType(userId: String, journeyType: String): List<JourneyMemory> {
        val allMemories = getJourneyMemories(userId)
        val normalizedType = normalizeCategory(journeyType)
        
        return allMemories.filter { 
            normalizeCategory(it.category) == normalizedType || normalizedType == "all"
        }
    }
}
