package com.example.saathi.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.data.JourneyMemory
import com.example.saathi.data.JourneyRepository
import com.example.saathi.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JourneyScreen() {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val journeyRepo = remember { JourneyRepository() }
    
    var allMemories by remember { mutableStateOf(listOf<JourneyMemory>()) }
    var selectedCategory by remember { mutableStateOf("All") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        allMemories = journeyRepo.getJourneyMemories(userId)
        isLoading = false
    }

    val filteredMemories = remember(allMemories, selectedCategory) {
        if (selectedCategory.equals("All", ignoreCase = true)) {
            allMemories
        } else {
            allMemories.filter { 
                journeyRepo.normalizeCategory(it.category).contains(journeyRepo.normalizeCategory(selectedCategory)) ||
                journeyRepo.normalizeCategory(selectedCategory).contains(journeyRepo.normalizeCategory(it.category))
            }
        }.sortedWith(
            compareByDescending<JourneyMemory> { it.importance }
                .thenByDescending { it.createdAt }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryPurple)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // Header Hero Section
                item {
                    JourneyHeader(memoryCount = allMemories.size)
                    Spacer(modifier = Modifier.height(16.dp))
                    CategoryFilterRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (filteredMemories.isEmpty()) {
                    item {
                        JourneyEmptyState(selectedCategory = selectedCategory)
                    }
                } else {
                    itemsIndexed(filteredMemories) { index, memory ->
                        TimelineItem(
                            memory = memory,
                            isLast = index == filteredMemories.size - 1
                        )
                    }

                    item {
                        WhatNextItem()
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf("All", "Education", "Career", "Relationship", "Achievement", "Travel")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { cat ->
            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
            Surface(
                onClick = { onCategorySelected(cat) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    text = cat,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
fun JourneyHeader(memoryCount: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(PrimaryPurple.copy(alpha = 0.3f), PrimaryPurple.copy(alpha = 0.1f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌟", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Life Journey",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = PrimaryPurple.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "$memoryCount Memory${if (memoryCount != 1) "s" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your milestones, achievements, and life events sorted by importance. Automatically recorded as you chat with Rahi.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun TimelineItem(memory: JourneyMemory, isLast: Boolean) {
    val categoryConfig = getCategoryConfig(memory.category)

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline Column (Node + Line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(categoryConfig.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryConfig.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(DividerColor)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content Card
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(memory.createdAt)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryText,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Importance Rating Badge
                Surface(
                    color = categoryConfig.color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Importance ${memory.importance}/10",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryConfig.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 1.5.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    categoryConfig.color.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = categoryConfig.color.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = memory.category.replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryConfig.color,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (memory.importance > 7) {
                            Surface(
                                color = Color(0xFFFFF8E1),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "MILESTONE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFA000)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = memory.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    Text(
                        text = memory.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        lineHeight = 20.sp
                    )

                    if (memory.sourceMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💬 \"${memory.sourceMessage}\"",
                                    fontSize = 11.sp,
                                    color = SecondaryText,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JourneyEmptyState(selectedCategory: String = "All") {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (selectedCategory.equals("All", ignoreCase = true)) "No Journey Entries Yet" else "No $selectedCategory Memories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Talk to Rahi about your achievements, job news, career wins, travels, or life goals. They will automatically be sorted by importance and recorded on your timeline!",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun WhatNextItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("•", color = SecondaryText, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text("What's Next?", color = SecondaryText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

data class CategoryConfig(
    val color: Color,
    val icon: ImageVector
)

private fun getCategoryConfig(category: String): CategoryConfig {
    val cat = category.lowercase().trim()
    return when {
        cat.contains("education") || cat.contains("academic") || cat.contains("study") || cat.contains("grade") || cat.contains("exam") || cat.contains("school") -> CategoryConfig(Color(0xFF0288D1), Icons.Default.School)
        cat.contains("career") || cat.contains("job") || cat.contains("work") -> CategoryConfig(Color(0xFF1E88E5), Icons.Default.Work)
        cat.contains("relation") || cat.contains("family") || cat.contains("love") -> CategoryConfig(Color(0xFFE91E63), Icons.Default.Favorite)
        cat.contains("achieve") || cat.contains("win") || cat.contains("goal") -> CategoryConfig(Color(0xFFFFA000), Icons.Default.WorkspacePremium)
        cat.contains("travel") || cat.contains("trip") || cat.contains("milestone") -> CategoryConfig(Color(0xFF7C4DFF), Icons.Default.Celebration)
        else -> CategoryConfig(Color(0xFF00897B), Icons.Default.AutoAwesome)
    }
}
