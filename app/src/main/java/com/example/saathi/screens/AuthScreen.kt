package com.example.saathi.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.example.saathi.ui.theme.AccentPurple
import com.example.saathi.ui.theme.DarkBackground
import com.example.saathi.ui.theme.DarkSurface
import com.example.saathi.ui.theme.TextGrey
import com.example.saathi.viewmodel.LoginState
import com.example.saathi.viewmodel.LoginViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
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
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        if (isLogin) {
            LoginContent(
                onSignUpClick = { isLogin = false },
                onLoginSuccess = onAuthComplete,
                auth = auth,
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
        Log.d(TAG, "STEP 1: Credential received")

        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken
            Log.d(TAG, "STEP 3: Google ID Token extracted")

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
    onLoginSuccess: () -> Unit,
    auth: FirebaseAuth,
    onGoogleSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Welcome back \ud83d\udc4b",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Glad to see you again.\nLet's continue your journey.",
            color = TextGrey,
            textAlign = TextAlign.Center,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email address",
            icon = Icons.Outlined.Email
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            icon = Icons.Outlined.Lock,
            isPassword = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = AccentPurple)
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
                                onLoginSuccess()
                            } else {
                                val error = task.exception
                                val msg = when (error) {
                                    is FirebaseAuthException -> "Auth Error: ${error.errorCode} - ${error.message}"
                                    else -> "Error: ${error?.message ?: "Unknown error"}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        SocialLoginSection(onGoogleClick = onGoogleSignIn)
        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account? ", color = TextGrey)
            Text("Sign up", color = AccentPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSignUpClick() })
        }
    }
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

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Create account \ud83d\udc9c", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Start your journey with Saathi today.", color = TextGrey, textAlign = TextAlign.Center, fontSize = 15.sp)

        Spacer(modifier = Modifier.height(40.dp))

        AuthTextField(value = name, onValueChange = { name = it }, label = "Full name", icon = Icons.Outlined.Person, enabled = !hasSentOtp)
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(value = email, onValueChange = { email = it }, label = "Email address", icon = Icons.Outlined.Email, enabled = !hasSentOtp)
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Outlined.Lock, isPassword = true, enabled = !hasSentOtp)
        Spacer(modifier = Modifier.height(16.dp))
        AuthTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm password", icon = Icons.Outlined.Lock, isPassword = true, enabled = !hasSentOtp)

        if (hasSentOtp) {
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField(
                value = otp,
                onValueChange = { if (it.length <= 6) otp = it },
                label = "Enter 6-digit OTP",
                icon = Icons.Outlined.Lock,
                keyboardType = KeyboardType.Number
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (loginState is LoginState.Loading) {
            CircularProgressIndicator(color = AccentPurple)
        } else {
            if (!hasSentOtp) {
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!email.contains("@")) {
                            Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password != confirmPassword) {
                            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.sendOtp(email.trim())
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Verify Email (Send OTP)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
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
                                                        if (dbTask.isSuccessful) {
                                                            onSignUpSuccess()
                                                        } else {
                                                            Toast.makeText(context, "DB Sync Failed", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                            } else {
                                                Toast.makeText(context, "Signup failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "OTP Verification Failed", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Complete Signup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { viewModel.sendOtp(email.trim()) },
                    enabled = timerValue == 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (timerValue > 0) "Resend OTP in ${timerValue}s" else "Resend OTP",
                        color = if (timerValue > 0) Color.Gray else AccentPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        SocialLoginSection(onGoogleClick = onGoogleSignIn)
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account? ", color = TextGrey)
            Text("Login", color = AccentPurple, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onLoginClick() })
        }
    }
}

@Composable
fun ValidationItem(text: String, isValid: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = if (isValid) Color.Green else Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = if (isValid) Color.White else TextGrey, fontSize = 13.sp)
    }
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
        color = if (enabled) DarkSurface else DarkSurface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (enabled) TextGrey else TextGrey.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) Text(label, color = if (enabled) TextGrey else TextGrey.copy(alpha = 0.5f), fontSize = 15.sp)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = TextStyle(color = if (enabled) Color.White else Color.Gray, fontSize = 15.sp),
                    cursorBrush = SolidColor(AccentPurple),
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = null, tint = TextGrey)
                }
            }
        }
    }
}

@Composable
fun SocialLoginSection(onGoogleClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray.copy(alpha = 0.2f))
            Text("or continue with", color = TextGrey, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.Gray.copy(alpha = 0.2f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        GoogleSignInButton(onClick = onGoogleClick)
    }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.White.copy(alpha = 0.1f)
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
