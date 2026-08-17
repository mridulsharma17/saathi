package com.example.saathi.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saathi.data.OtpRepository
import com.example.saathi.ui.theme.*
import com.example.saathi.viewmodel.LoginState
import com.example.saathi.viewmodel.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthScreen"
private const val WEB_CLIENT_ID = "54648344525-er4dh927v9q9lmvsdj3jh813vmu425e6.apps.googleusercontent.com"

@Composable
fun AuthScreen(onAuthComplete: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val viewModel: LoginViewModel = viewModel()
    val scrollState = rememberScrollState()

    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val prefs = context.getSharedPreferences("saathi_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_logged_in", true).apply()
            onAuthComplete()
        }
        if (loginState is LoginState.Error) {
            Toast.makeText(context, (loginState as LoginState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Logo matching Image 2
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            color = PrimaryPurple
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = if (isLogin) "Welcome back 👋" else "Create account ✨",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        Text(
            text = if (isLogin) "Glad to see you again. Let's continue your journey." else "Start your journey with Saathi today.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (isLogin) {
            LoginContent(
                onSignUpClick = { isLogin = false },
                onGoogleSignIn = {
                    scope.launch {
                        handleGoogleSignIn(context, auth, database, onAuthComplete)
                    }
                }
            )
        } else {
            SignUpContent(
                onLoginClick = { isLogin = true },
                onSignUpSuccess = onAuthComplete,
                auth = auth,
                database = database,
                viewModel = viewModel,
                onGoogleSignIn = {
                    scope.launch {
                        handleGoogleSignIn(context, auth, database, onAuthComplete)
                    }
                }
            )
        }
    }
}

private suspend fun handleGoogleSignIn(
    context: android.content.Context,
    auth: FirebaseAuth,
    database: FirebaseDatabase,
    onAuthComplete: () -> Unit
) {
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(WEB_CLIENT_ID)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user

            if (user != null) {
                val sanitizedEmail = user.email?.replace(".", ",") ?: user.uid
                val userMap = mapOf(
                    "name" to (user.displayName ?: "User"),
                    "email" to (user.email ?: ""),
                    "uid" to user.uid
                )
                database.getReference("users").child(sanitizedEmail).updateChildren(userMap).await()
                onAuthComplete()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        Toast.makeText(context, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun LoginContent(
    onSignUpClick: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Email address", fontWeight = FontWeight.Medium, color = PrimaryText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "you@example.com",
                icon = Icons.Outlined.Email
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text("Password", fontWeight = FontWeight.Medium, color = PrimaryText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "........",
                icon = Icons.Outlined.Lock,
                isPassword = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { }) {
                    Text("Forgot password?", color = PrimaryPurple, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            } else {
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    // Success handled in AuthScreen
                                } else {
                                    Toast.makeText(context, "Login failed", Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    
    // Google Button matching Image 2
    Surface(
        onClick = onGoogleSignIn,
        color = Color(0xFFE8EAF6),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Continue with Google", color = PrimaryPurple, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Don't have an account? ", color = SecondaryText)
        Text("Sign up", color = PrimaryPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSignUpClick() })
    }
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun SignUpContent(
    onLoginClick: () -> Unit,
    onSignUpSuccess: () -> Unit,
    auth: FirebaseAuth,
    database: FirebaseDatabase,
    viewModel: LoginViewModel,
    onGoogleSignIn: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    
    val loginState by viewModel.loginState.collectAsState()
    val timerValue by viewModel.timerValue.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasSentOtp by remember { mutableStateOf(false) }
    if (loginState is LoginState.OtpSent) {
        hasSentOtp = true
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (!hasSentOtp) {
                Text("Full name", fontWeight = FontWeight.Medium, color = PrimaryText, fontSize = 14.sp)
                AuthTextField(value = name, onValueChange = { name = it }, label = "Your Name", icon = Icons.Outlined.Person)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Email address", fontWeight = FontWeight.Medium, color = PrimaryText, fontSize = 14.sp)
                AuthTextField(value = email, onValueChange = { email = it }, label = "you@example.com", icon = Icons.Outlined.Email)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Password", fontWeight = FontWeight.Medium, color = PrimaryText, fontSize = 14.sp)
                AuthTextField(value = password, onValueChange = { password = it }, label = "........", icon = Icons.Outlined.Lock, isPassword = true)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Confirm password", fontWeight = FontWeight.Medium, color = PrimaryText, fontSize = 14.sp)
                AuthTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "........", icon = Icons.Outlined.Lock, isPassword = true)
            } else {
                Text("Verify your email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("We've sent a 6-digit code to $email", color = SecondaryText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                
                AuthTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    label = "000000",
                    icon = Icons.Outlined.VpnKey,
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (loginState is LoginState.Loading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            } else {
                Button(
                    onClick = {
                        if (!hasSentOtp) {
                            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.sendOtp(email.trim())
                        } else {
                            if (otp.length != 6) {
                                Toast.makeText(context, "Please enter a 6-digit OTP", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                try {
                                    val verifyResponse = OtpRepository().verifyOtp(email.trim(), otp)
                                    if (verifyResponse.isSuccessful && verifyResponse.body()?.success == true) {
                                        auth.createUserWithEmailAndPassword(email.trim(), password)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val sanitizedEmail = email.trim().replace(".", ",")
                                                    val userMap = mapOf(
                                                        "name" to name,
                                                        "email" to email.trim(),
                                                        "uid" to (task.result.user?.uid ?: "")
                                                    )
                                                    database.getReference("users").child(sanitizedEmail)
                                                        .setValue(userMap)
                                                        .addOnCompleteListener { dbTask ->
                                                            if (dbTask.isSuccessful) onSignUpSuccess()
                                                        }
                                                }
                                            }
                                    }
                                } catch (e: Exception) { }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (hasSentOtp) "Complete Signup" else "Verify Email", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                if (hasSentOtp) {
                    TextButton(onClick = { viewModel.sendOtp(email.trim()) }, enabled = timerValue == 0) {
                        Text(if (timerValue > 0) "Resend in ${timerValue}s" else "Resend OTP", color = PrimaryPurple)
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    
    Surface(
        onClick = onGoogleSignIn,
        color = Color(0xFFE8EAF6),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Continue with Google", color = PrimaryPurple, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(32.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Already have an account? ", color = SecondaryText)
        Text("Login", color = PrimaryPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onLoginClick() })
    }
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var passwordVisible by remember { mutableStateOf(false) }
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) Text(label, color = Color(0xFF94A3B8), fontSize = 15.sp)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = TextStyle(color = PrimaryText, fontSize = 15.sp),
                    cursorBrush = SolidColor(PrimaryPurple),
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = null, tint = Color(0xFF94A3B8))
                }
            }
        }
    }
}
