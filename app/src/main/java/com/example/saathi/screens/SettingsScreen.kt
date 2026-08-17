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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var showAiDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            Spacer(modifier = Modifier.height(16.dp))
            ProfileHeader(isDarkTheme)
        }

        item {
            SettingsSection(
                items = listOf(
                    SettingsItemData("AI Companion", null, Icons.Default.SmartToy, onClick = { showAiDialog = true }),
                    SettingsItemData("Appearance", null, Icons.Default.Palette, onClick = onThemeToggle)
                )
            )
        }

        item {
            SettingsSection(
                items = listOf(
                    SettingsItemData("Privacy & Data", null, Icons.Default.Lock, onClick = { showPrivacyDialog = true }),
                    SettingsItemData("Export Journey", null, Icons.Default.Download, onClick = { 
                        Toast.makeText(context, "Exporting data...", Toast.LENGTH_SHORT).show()
                    }),
                    SettingsItemData("About Saathi", null, Icons.Default.Info, onClick = { showAboutDialog = true })
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                onClick = { showSignOutDialog = true },
                color = if (isDarkTheme) Color.Red.copy(alpha = 0.1f) else Color(0xFFFFEBEE),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version 2.0.1 (${if (isDarkTheme) "Dark" else "Light"})",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = SecondaryText,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Button(
                        onClick = {
                            showSignOutDialog = false
                            FirebaseAuth.getInstance().signOut()
                            val prefs = context.getSharedPreferences("saathi_prefs", android.content.Context.MODE_PRIVATE)
                            prefs.edit().clear().apply()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        onClick = { showSignOutDialog = false },
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color(0xFFEEEEEE),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Cancel",
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceColor
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            },
            text = {
                Text(
                    "Are you sure you want to log out of Saathi? We'll be here whenever you're ready to return.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(28.dp)
        )
    }

    // Reuse other dialogs with theme colors...
    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI Companion: Rahi", fontWeight = FontWeight.Bold) },
            text = { Text("Rahi is your intelligent friend, powered by Gemini and OpenRouter.") },
            confirmButton = { TextButton(onClick = { showAiDialog = false }) { Text("Close", color = PrimaryPurple) } },
            containerColor = surfaceColor
        )
    }
}

@Composable
fun ProfileHeader(isDarkTheme: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                // Image placeholder
            }
            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.padding(6.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Mridul", 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text("Joined Journey today", color = SecondaryText, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsSection(items: List<SettingsItemData>) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Column {
            items.forEachIndexed { index, item ->
                SettingsItem(item)
                if (index < (items.size - 1)) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = DividerColor)
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(item.title, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

data class SettingsItemData(
    val title: String,
    val value: String?,
    val icon: ImageVector,
    val onClick: () -> Unit
)
