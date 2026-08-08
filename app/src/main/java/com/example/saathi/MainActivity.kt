package com.example.saathi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.saathi.screens.AuthScreen
import com.example.saathi.screens.OnboardingScreen
import com.example.saathi.screens.ApiTestScreen
import com.example.saathi.ui.theme.SaathiTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val auth = FirebaseAuth.getInstance()
        val prefs = getSharedPreferences("saathi_prefs", MODE_PRIVATE)
        val onboardingFinished = prefs.getBoolean("onboarding_finished", false)
        val isOtpLoggedIn = prefs.getBoolean("is_logged_in", false)

        val startStage = when {
            auth.currentUser != null || isOtpLoggedIn -> "main"
            onboardingFinished -> "auth"
            else -> "onboarding"
        }
//        val startStage="test"

        enableEdgeToEdge()
        setContent {
            val isDarkThemePref = prefs.getBoolean("is_dark_theme", true)
            var isDarkTheme by remember { mutableStateOf(isDarkThemePref) }
            
            SaathiTheme(darkTheme = isDarkTheme) {
                var currentStage by remember { mutableStateOf(startStage) }

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
