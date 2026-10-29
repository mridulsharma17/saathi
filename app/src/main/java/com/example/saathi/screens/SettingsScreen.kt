package com.example.saathi.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.data.LocalChatRepository
import com.example.saathi.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onMemoriesClick: () -> Unit
) {
    var showAiDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val localChatRepo = remember { LocalChatRepository(context) }
    var storageMode by remember { mutableStateOf(localChatRepo.getStorageMode()) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ProfileHeader()
        }

        item {
            Text(
                text = "APP PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryText,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            SettingsSection(
                items = listOf(
                    SettingsItemData("AI Companion", "Rahi 💜", Icons.Default.SmartToy, onClick = { showAiDialog = true }),
                    SettingsItemData("Appearance", if (isDarkTheme) "Dark Mode" else "Light Mode", Icons.Default.Palette, onClick = onThemeToggle),
                    SettingsItemData("Chat Storage", if (storageMode == "cloud") "Cloud (Firebase)" else "Device Only (Local)", Icons.Default.CloudQueue, onClick = { showStorageDialog = true })
                )
            )
        }

        item {
            Text(
                text = "PRIVACY & ABOUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryText,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            val isUnrestricted = com.example.saathi.util.BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            SettingsSection(
                items = listOf(
                    SettingsItemData(
                        "Background Execution",
                        if (isUnrestricted) "Allowed ✅" else "Tap to Allow ⚡",
                        Icons.Default.BatteryChargingFull,
                        onClick = {
                            if (!isUnrestricted) {
                                com.example.saathi.util.BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                            } else {
                                Toast.makeText(context, "Unrestricted background execution is enabled!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ),
                    SettingsItemData("Privacy & Data", "Protected", Icons.Default.Lock, onClick = { showPrivacyDialog = true }),
                    SettingsItemData("About Saarthi AI", "v2.0.1", Icons.Default.Info, onClick = { showAboutDialog = true })
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                onClick = { showSignOutDialog = true },
                color = if (isDarkTheme) Color(0xFF3B151A) else Color(0xFFFFEBEE),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Sign Out", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saarthi AI Companion • Build 2026",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = SecondaryText,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Storage Location Dialog
    if (showStorageDialog) {
        AlertDialog(
            onDismissRequest = { showStorageDialog = false },
            title = { Text("Chat Storage Preference", fontWeight = FontWeight.Bold, color = onSurfaceColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Choose where new chat messages are saved. Your chat history from both Cloud and Local Storage will always be combined and displayed in correct order.",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Surface(
                        onClick = {
                            storageMode = "cloud"
                            localChatRepo.setStorageMode("cloud")
                            showStorageDialog = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (storageMode == "cloud") PrimaryPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (storageMode == "cloud") PrimaryPurple else Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Cloud Storage (Firebase)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = onSurfaceColor)
                                Text("Syncs chat history across devices", fontSize = 11.sp, color = SecondaryText)
                            }
                        }
                    }

                    Surface(
                        onClick = {
                            storageMode = "local"
                            localChatRepo.setStorageMode("local")
                            showStorageDialog = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (storageMode == "local") PrimaryPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (storageMode == "local") PrimaryPurple else Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Device Only (Local Storage)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = onSurfaceColor)
                                Text("Maximum privacy. Saved strictly on device", fontSize = 11.sp, color = SecondaryText)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStorageDialog = false }) {
                    Text("Close", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            FirebaseAuth.getInstance().signOut()
                            val prefs = context.getSharedPreferences("saathi_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().clear().apply()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showSignOutDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = SecondaryText, fontWeight = FontWeight.Medium)
                    }
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = onSurfaceColor)
                }
            },
            text = {
                Text(
                    "Are you sure you want to log out of Saathi?",
                    textAlign = TextAlign.Center,
                    color = SecondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI Companion: Rahi 👋", fontWeight = FontWeight.Bold, color = onSurfaceColor) },
            text = { 
                Text(
                    "Rahi is your empathetic, witty AI friend. Rahi remembers your personal details, life goals, and major milestones over time using intelligent memory lookup.",
                    color = SecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ) 
            },
            confirmButton = { TextButton(onClick = { showAiDialog = false }) { Text("Got It", color = PrimaryPurple, fontWeight = FontWeight.Bold) } },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Security 🔒", fontWeight = FontWeight.Bold, color = onSurfaceColor) },
            text = { 
                Text(
                    "Your conversation data and smart memories can be saved on Cloud or strictly on Device Local Storage based on your preference.",
                    color = SecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ) 
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("Done", color = PrimaryPurple, fontWeight = FontWeight.Bold) } },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Saarthi AI 💜", fontWeight = FontWeight.Bold, color = onSurfaceColor) },
            text = { 
                Text(
                    "Saarthi AI is an AI companion application designed for genuine conversation, memory preservation, and emotional support.\n\nVersion 2.0.1\nEngineered with Jetpack Compose & Firebase.",
                    color = SecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ) 
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close", color = PrimaryPurple, fontWeight = FontWeight.Bold) } },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun ProfileHeader() {
    val user = FirebaseAuth.getInstance().currentUser
    val userName = remember(user) {
        user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "User"
    }
    val userEmail = user?.email ?: "saathi.user@example.com"
    val initial = userName.firstOrNull()?.uppercase() ?: "U"

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryPurple, Color(0xFF7C4DFF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = userEmail,
                fontSize = 13.sp,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = PrimaryPurple.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saarthi AI Member 💜",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(items: List<SettingsItemData>) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Column {
            items.forEachIndexed { index, item ->
                SettingsItem(item)
                if (index < (items.size - 1)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = DividerColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItem(item: SettingsItemData) {
    Surface(
        onClick = item.onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )

            if (item.value != null) {
                Text(
                    text = item.value,
                    color = SecondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

data class SettingsItemData(
    val title: String,
    val value: String?,
    val icon: ImageVector,
    val onClick: () -> Unit
)
