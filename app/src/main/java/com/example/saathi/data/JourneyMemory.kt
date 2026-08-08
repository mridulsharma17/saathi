package com.example.saathi.data

data class JourneyMemory(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val importance: Int = 0,
    val createdAt: Long = 0,
    val sourceMessage: String = "",
    val aiGenerated: Boolean = true
)
