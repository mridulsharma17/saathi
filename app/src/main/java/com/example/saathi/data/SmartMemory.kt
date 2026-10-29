package com.example.saathi.data

data class SmartMemory(
    val id: String = "",
    val entityId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val importance: Int = 5
)

data class EntityInfo(
    val id: String = "",
    val primaryName: String = "",
    val aliases: List<String> = emptyList(),
    val relations: List<String> = emptyList()
)

data class EntityExtractionResult(
    val name: String? = null,
    val relation: String? = null,
    val otherNames: List<String> = emptyList()
)

data class SmartMemoryUpdate(
    val content: String = "",
    val importance: Int = 5
)
