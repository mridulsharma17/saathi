package com.example.saathi.data

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.*

class SmartMemoryRepository {
    private val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
    private val usersRef = database.getReference("users")

    // Normalization helper
    fun normalizeName(name: String): String {
        return name.lowercase(Locale.getDefault()).trim()
    }

    // nameMap: name/alias -> entityId
    suspend fun getEntityIdByName(userId: String, name: String): String? {
        val normalized = normalizeName(name)
        if (normalized.isEmpty()) return null
        return try {
            val snapshot = usersRef.child(userId)
                .child("smartMemory")
                .child("nameMap")
                .child(normalized)
                .get().await()
            snapshot.value?.toString()
        } catch (e: Exception) {
            null
        }
    }

    // relationshipMap: relation -> list of entityIds
    suspend fun getEntityIdsByRelation(userId: String, relation: String): List<String> {
        val normalized = normalizeName(relation)
        if (normalized.isEmpty()) return emptyList()
        return try {
            val snapshot = usersRef.child(userId)
                .child("smartMemory")
                .child("relationshipMap")
                .child(normalized)
                .get().await()
            
            if (snapshot.exists()) {
                snapshot.children.mapNotNull { it.value?.toString() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Add a relationship mapping to an entity
    suspend fun addRelation(userId: String, entityId: String, relation: String) {
        val normalized = normalizeName(relation)
        if (normalized.isEmpty()) return
        
        try {
            // Add entityId under relationshipMap/relation
            val relRef = usersRef.child(userId).child("smartMemory").child("relationshipMap").child(normalized)
            val snapshot = relRef.get().await()
            val currentList = snapshot.children.mapNotNull { it.value?.toString() }.toMutableList()
            
            if (!currentList.contains(entityId)) {
                currentList.add(entityId)
                relRef.setValue(currentList).await()
            }

            // Update relations list in EntityInfo
            val infoRef = usersRef.child(userId).child("smartMemory").child("entities").child(entityId)
            val infoSnapshot = infoRef.get().await()
            val info = infoSnapshot.getValue(EntityInfo::class.java)
            if (info != null && !info.relations.contains(normalized)) {
                val newRelations = info.relations + normalized
                infoRef.child("relations").setValue(newRelations).await()
            }
        } catch (e: Exception) {
            // Handle or log error
        }
    }

    // memoryMap: entityId -> memories[]
    suspend fun getMemoriesByEntityId(userId: String, entityId: String): List<SmartMemory> {
        return try {
            val snapshot = usersRef.child(userId)
                .child("smartMemory")
                .child("memoryMap")
                .child(entityId)
                .get().await()
            snapshot.children.mapNotNull { it.getValue(SmartMemory::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Add a new entity with optional relation
    suspend fun createEntity(userId: String, primaryName: String, relation: String? = null): String {
        val entityId = "ent_" + UUID.randomUUID().toString().substring(0, 8)
        val normalized = normalizeName(primaryName)
        
        // Map name to ID
        usersRef.child(userId).child("smartMemory").child("nameMap").child(normalized).setValue(entityId).await()
        
        val relationsList = if (!relation.isNullOrBlank()) listOf(normalizeName(relation)) else emptyList()
        
        // Save entity info
        val entityInfo = EntityInfo(
            id = entityId,
            primaryName = primaryName,
            aliases = listOf(normalized),
            relations = relationsList
        )
        usersRef.child(userId).child("smartMemory").child("entities").child(entityId).setValue(entityInfo).await()
        
        if (!relation.isNullOrBlank()) {
            addRelation(userId, entityId, relation)
        }
        
        return entityId
    }

    // Add an alias to an existing entity
    suspend fun addAlias(userId: String, entityId: String, alias: String) {
        val normalized = normalizeName(alias)
        if (normalized.isEmpty()) return
        
        usersRef.child(userId).child("smartMemory").child("nameMap").child(normalized).setValue(entityId).await()
        
        // Update aliases list in entity info
        val infoRef = usersRef.child(userId).child("smartMemory").child("entities").child(entityId)
        val snapshot = infoRef.get().await()
        val info = snapshot.getValue(EntityInfo::class.java)
        if (info != null && !info.aliases.contains(normalized)) {
            val newAliases = info.aliases + normalized
            infoRef.child("aliases").setValue(newAliases).await()
        }
    }

    // Save a memory to an entity's bucket
    suspend fun saveMemory(userId: String, entityId: String, content: String, importance: Int = 5) {
        val memoryRef = usersRef.child(userId).child("smartMemory").child("memoryMap").child(entityId).push()
        val memory = SmartMemory(
            id = memoryRef.key ?: "",
            entityId = entityId,
            content = content,
            timestamp = System.currentTimeMillis(),
            importance = importance
        )
        memoryRef.setValue(memory).await()
    }

    // Fetch entity info
    suspend fun getEntityInfo(userId: String, entityId: String): EntityInfo? {
        return try {
            val snapshot = usersRef.child(userId).child("smartMemory").child("entities").child(entityId).get().await()
            snapshot.getValue(EntityInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Fetch all entities (for UI)
    suspend fun getAllEntities(userId: String): List<EntityInfo> {
        return try {
            val snapshot = usersRef.child(userId).child("smartMemory").child("entities").get().await()
            snapshot.children.mapNotNull { it.getValue(EntityInfo::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Fetch name map
    suspend fun getNameMap(userId: String): Map<String, String> {
        return try {
            val snapshot = usersRef.child(userId).child("smartMemory").child("nameMap").get().await()
            val map = mutableMapOf<String, String>()
            snapshot.children.forEach { child ->
                map[child.key ?: ""] = child.value?.toString() ?: ""
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
