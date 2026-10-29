package com.example.saathi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.saathi.screens.ApiTestScreen
import com.example.saathi.screens.AuthScreen
import com.example.saathi.screens.OnboardingScreen
import com.example.saathi.ui.theme.PrimaryPurple
import com.example.saathi.ui.theme.SaathiTheme
import com.example.saathi.ui.theme.SecondaryText
import com.example.saathi.util.BatteryOptimizationHelper
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val auth = FirebaseAuth.getInstance()
        val prefs = getSharedPreferences("saathi_prefs", MODE_PRIVATE)
        val isOtpLoggedIn = prefs.getBoolean("is_logged_in", false)

        val isLoggedIn = auth.currentUser != null || isOtpLoggedIn
        val startStage = if (isLoggedIn) "main" else "onboarding"

        enableEdgeToEdge()
        setContent {
            val isDarkThemePref = prefs.getBoolean("is_dark_theme", true)
            var isDarkTheme by remember { mutableStateOf(isDarkThemePref) }
            
            SaathiTheme(darkTheme = isDarkTheme) {
                var currentStage by remember { mutableStateOf(startStage) }

                // Call handlers
                NotificationPermissionHandler()

                if (currentStage == "main") {
                    BatteryOptimizationHandler()
                }

                when (currentStage) {
                    "test" -> ApiTestScreen()
                    "onboarding" -> OnboardingScreen(onFinished = { 
                        prefs.edit().putBoolean("onboarding_finished", true).apply()
                        currentStage = "auth" 
                    })
                    "auth" -> AuthScreen(onAuthComplete = { currentStage = "main" })
                    "main" -> MainScreen(
                        onLogout = { 
                            prefs.edit().clear().apply()
                            currentStage = "onboarding" 
                        },
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { 
                            val newValue = !isDarkTheme
                            isDarkTheme = newValue
                            prefs.edit().putBoolean("is_dark_theme", newValue).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryOptimizationHandler() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("saathi_prefs", android.content.Context.MODE_PRIVATE) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val isUnrestricted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        val promptDismissed = prefs.getBoolean("battery_prompt_dismissed", false)

        if (!isUnrestricted && !promptDismissed) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                prefs.edit().putBoolean("battery_prompt_dismissed", true).apply()
                showDialog = false
            },
            title = {
                Text(
                    text = "Enable Background Check-ins ⚡",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "To allow Rahi to check in on you and send notifications when the app is closed, please allow unrestricted background execution.",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit().putBoolean("battery_prompt_dismissed", true).apply()
                        showDialog = false
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Allow Access", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        prefs.edit().putBoolean("battery_prompt_dismissed", true).apply()
                        showDialog = false
                    }
                ) {
                    Text("Not Now", color = SecondaryText)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
