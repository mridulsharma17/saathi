package com.example.saathi.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.data.EntityInfo
import com.example.saathi.data.SmartMemory
import com.example.saathi.data.SmartMemoryRepository
import com.example.saathi.ui.theme.PrimaryPurple
import com.example.saathi.ui.theme.SecondaryText
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMemoriesScreen(onBack: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return
    val repository = remember { SmartMemoryRepository() }
    
    var entities by remember { mutableStateOf(listOf<EntityInfo>()) }
    var selectedEntity by remember { mutableStateOf<EntityInfo?>(null) }
    var entityMemories by remember { mutableStateOf(listOf<SmartMemory>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        entities = repository.getAllEntities(userId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedEntity == null) "Memories" else selectedEntity!!.primaryName) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedEntity != null) selectedEntity = null else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPurple)
            } else if (selectedEntity == null) {
                EntityList(entities) { entity ->
                    selectedEntity = entity
                    isLoading = true
                    // Load memories for selected entity
                    // In a real app, this would be a separate LaunchedEffect or state update
                }
            } else {
                // If we are here, an entity is selected. Let's load its memories if not loaded.
                LaunchedEffect(selectedEntity) {
                    entityMemories = repository.getMemoriesByEntityId(userId, selectedEntity!!.id)
                    isLoading = false
                }
                
                if (!isLoading) {
                    MemoryList(entityMemories)
                } else {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryPurple)
                }
            }
        }
    }
}

@Composable
fun EntityList(entities: List<EntityInfo>, onEntityClick: (EntityInfo) -> Unit) {
    if (entities.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No entity memories found yet.", color = SecondaryText)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(entities) { entity ->
                Surface(
                    onClick = { onEntityClick(entity) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryPurple)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(entity.primaryName, fontWeight = FontWeight.Bold)
                            Text("Aliases: ${entity.aliases.joinToString(", ")}", fontSize = 12.sp, color = SecondaryText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryList(memories: List<SmartMemory>) {
    if (memories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No specific memories for this person.", color = SecondaryText)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(memories.sortedByDescending { it.timestamp }) { memory ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(memory.timestamp)),
                            fontSize = 11.sp,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(memory.content, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
