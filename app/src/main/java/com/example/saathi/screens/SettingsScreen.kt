package com.example.saathi.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.ui.theme.DarkBackground
import com.example.saathi.ui.theme.DarkSurface
import com.example.saathi.ui.theme.AccentPurple
import com.example.saathi.ui.theme.TextGrey
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) DarkBackground else Color.White)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ProfileCard(isDarkTheme)
        }

        item {
            SettingsSection(
                items = listOf(
                    SettingsItemData("AI Companion", "Rahi", Icons.Default.SmartToy, onClick = { showAiDialog = true }),
                    SettingsItemData("Appearance", if (isDarkTheme) "Dark" else "Light", Icons.Default.Palette, onClick = onThemeToggle)
                ),
                isDarkTheme = isDarkTheme
            )
        }

        item {
            SettingsSection(
                items = listOf(
                    SettingsItemData("Privacy & Data", null, Icons.Default.Lock, onClick = { showPrivacyDialog = true }),
                    SettingsItemData("Export Journey", null, Icons.Default.Download, onClick = { 
                        Toast.makeText(context, "Exporting your journey data...", Toast.LENGTH_SHORT).show()
                    }),
                    SettingsItemData("About Saathi", null, Icons.Default.Info, onClick = { showAboutDialog = true })
                ),
                isDarkTheme = isDarkTheme
            )
        }

        item {
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign Out", color = Color.Red, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out of Saathi?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        FirebaseAuth.getInstance().signOut()
                        val prefs = context.getSharedPreferences("saathi_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        onLogout()
                    }
                ) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = if (isDarkTheme) Color.White else Color.Black)
                }
            },
            containerColor = if (isDarkTheme) DarkSurface else Color.White,
            titleContentColor = if (isDarkTheme) Color.White else Color.Black,
            textContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
        )
    }

    // AI Companion Dialog
    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI Companion: Rahi", fontWeight = FontWeight.Bold) },
            text = {
                Text("Rahi is powered by advanced AI models including Google's Gemini and OpenRouter. Rahi acts as a close Indian friend who remembers your life events and provides witty, honest, and caring support.")
            },
            confirmButton = {
                TextButton(onClick = { showAiDialog = false }) { Text("Close", color = AccentPurple) }
            },
            containerColor = if (isDarkTheme) DarkSurface else Color.White,
            titleContentColor = if (isDarkTheme) Color.White else Color.Black,
            textContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Data", fontWeight = FontWeight.Bold) },
            text = {
                Text("Your data is secured using Firebase Authentication. Chats and Journey memories are stored in an encrypted database. We use a rolling context summary (memory) to maintain conversation continuity while protecting your privacy.")
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Understood", color = AccentPurple) }
            },
            containerColor = if (isDarkTheme) DarkSurface else Color.White,
            titleContentColor = if (isDarkTheme) Color.White else Color.Black,
            textContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Saathi", fontWeight = FontWeight.Bold) },
            text = {
                Text("Saathi is your AI-powered companion journey. Designed to be your best friend who never forgets your special moments.")
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK", color = AccentPurple) }
            },
            containerColor = if (isDarkTheme) DarkSurface else Color.White,
            titleContentColor = if (isDarkTheme) Color.White else Color.Black,
            textContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ProfileCard(isDarkTheme: Boolean) {
    Surface(
        color = if (isDarkTheme) DarkSurface else Color(0xFFF5F5F5),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AccentPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Mridul", 
                style = MaterialTheme.typography.headlineSmall, 
                color = if (isDarkTheme) Color.White else Color.Black,
                fontWeight = FontWeight.Bold
            )
            Text("Keep growing, keep going.", color = TextGrey, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class SettingsItemData(
    val title: String,
    val value: String?,
    val icon: ImageVector,
    val hasSwitch: Boolean = false,
    val onClick: () -> Unit = {}
)

@Composable
fun SettingsSection(items: List<SettingsItemData>, isDarkTheme: Boolean) {
    Surface(
        color = if (isDarkTheme) DarkSurface else Color(0xFFF5F5F5),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            items.forEachIndexed { index, item ->
                SettingsItem(item, isDarkTheme)
                if (index < (items.size - 1)) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun SettingsItem(item: SettingsItemData, isDarkTheme: Boolean) {
    Surface(
        onClick = item.onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(item.icon, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(item.title, color = if (isDarkTheme) Color.White else Color.Black, modifier = Modifier.weight(1f))
            if (item.value != null) {
                Text(item.value, color = TextGrey, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (item.hasSwitch) {
                Switch(
                    checked = true,
                    onCheckedChange = { },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentPurple)
                )
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextGrey)
            }
        }
    }
}
