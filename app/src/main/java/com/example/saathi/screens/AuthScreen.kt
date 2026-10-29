package com.example.saathi.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saathi.R
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
    val isDarkTheme = MaterialTheme.colorScheme.background != LightBackground

    val bgGradient = if (isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F0C1B),
                Color(0xFF18132B),
                Color(0xFF0F0C1B)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFAF8FF),
                Color(0xFFF2ECFC),
                Color(0xFFFAF8FF)
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {
        AuthBackgroundCanvas(isDarkTheme = isDarkTheme)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo Header
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                border = BorderStroke(2.dp, PrimaryPurple.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Saarthi AI",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented Tab Switcher (Login / Signup)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { isLogin = true },
                        color = if (isLogin) PrimaryPurple else Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Login",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isLogin) Color.White else SecondaryText
                            )
                        }
                    }

                    Surface(
                        onClick = { isLogin = false },
                        color = if (!isLogin) PrimaryPurple else Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Sign Up",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (!isLogin) Color.White else SecondaryText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLogin) {
                LoginContent(
                    viewModel = viewModel,
                    onSignUpClick = { isLogin = false },
                    onGoogleSignIn = {
                        scope.launch {
                            handleGoogleSignIn(context, auth, database, viewModel, onAuthComplete)
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
                            handleGoogleSignIn(context, auth, database, viewModel, onAuthComplete)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private suspend fun handleGoogleSignIn(
    context: android.content.Context,
    auth: FirebaseAuth,
    database: FirebaseDatabase,
    viewModel: LoginViewModel,
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
                viewModel.setSuccess()
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Google Sign-In failed", e)
        viewModel.setError("Google Sign-In failed: ${e.message}")
    }
}

@Composable
fun LoginContent(
    viewModel: LoginViewModel,
    onSignUpClick: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Email Address", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "you@example.com",
                icon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            Text("Password", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "••••••••",
                icon = Icons.Outlined.Lock,
                isPassword = true,
                keyboardType = KeyboardType.Password
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    Toast.makeText(context, "Password reset link feature coming soon", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Forgot password?", color = PrimaryPurple, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(28.dp))
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
                                    viewModel.setSuccess()
                                } else {
                                    viewModel.setError(task.exception?.message ?: "Login failed")
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign-In Button
            Surface(
                onClick = onGoogleSignIn,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Continue with Google 🌐", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Don't have an account? ", color = SecondaryText, fontSize = 14.sp)
        Text("Sign up", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onSignUpClick() })
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

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (!hasSentOtp) {
                Text("Full Name", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                AuthTextField(value = name, onValueChange = { name = it }, label = "Your Name", icon = Icons.Outlined.Person)
                Spacer(modifier = Modifier.height(14.dp))
                
                Text("Email Address", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                AuthTextField(value = email, onValueChange = { email = it }, label = "you@example.com", icon = Icons.Outlined.Email, keyboardType = KeyboardType.Email)
                Spacer(modifier = Modifier.height(14.dp))
                
                Text("Password", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                AuthTextField(value = password, onValueChange = { password = it }, label = "••••••••", icon = Icons.Outlined.Lock, isPassword = true, keyboardType = KeyboardType.Password)
                Spacer(modifier = Modifier.height(14.dp))
                
                Text("Confirm Password", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                AuthTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "••••••••", icon = Icons.Outlined.Lock, isPassword = true, keyboardType = KeyboardType.Password)
            } else {
                Text("Verify Your Email ✉️", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text("We've sent a 6-digit verification code to $email", color = SecondaryText, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(20.dp))
                
                AuthTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    label = "000000",
                    icon = Icons.Outlined.VpnKey,
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (loginState is LoginState.Loading) {
                Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(28.dp))
                }
            } else {
                Button(
                    onClick = {
                        if (!hasSentOtp) {
                            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
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
                                                            if (dbTask.isSuccessful) viewModel.setSuccess()
                                                        }
                                                } else {
                                                    viewModel.setError(task.exception?.message ?: "User creation failed")
                                                }
                                            }
                                    } else {
                                        viewModel.setError(verifyResponse.body()?.message ?: "Invalid OTP")
                                    }
                                } catch (e: Exception) {
                                    viewModel.setError(e.message ?: "Verification failed")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (hasSentOtp) "Complete Sign Up" else "Verify Email", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                if (hasSentOtp) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { viewModel.sendOtp(email.trim()) }, enabled = timerValue == 0, modifier = Modifier.fillMaxWidth()) {
                        Text(if (timerValue > 0) "Resend in ${timerValue}s" else "Resend Verification Code", color = PrimaryPurple, fontSize = 13.sp)
                    }
                }
            }

            if (!hasSentOtp) {
                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In Button
                Surface(
                    onClick = onGoogleSignIn,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Continue with Google 🌐", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Already have an account? ", color = SecondaryText, fontSize = 14.sp)
        Text("Login", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { onLoginClick() })
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
    val focusRequester = remember { FocusRequester() }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { focusRequester.requestFocus() }
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) Text(label, color = SecondaryText, fontSize = 14.sp)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    cursorBrush = SolidColor(PrimaryPurple),
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, contentDescription = null, tint = SecondaryText)
                }
            }
        }
    }
}

@Composable
fun AuthBackgroundCanvas(isDarkTheme: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "authBackground")
    
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale2"
    )

    val glowColor1 = PrimaryPurple.copy(alpha = if (isDarkTheme) 0.16f else 0.08f)
    val glowColor2 = Color(0xFF7C4DFF).copy(alpha = if (isDarkTheme) 0.12f else 0.05f)
    val glowColor3 = Color(0xFF00E5FF).copy(alpha = if (isDarkTheme) 0.06f else 0.03f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Top-Right Glowing Orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor1, Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.15f),
                radius = size.width * 0.7f * pulseScale1
            ),
            center = Offset(size.width * 0.85f, size.height * 0.15f),
            radius = size.width * 0.7f * pulseScale1
        )

        // Bottom-Left Glowing Orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor2, Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.82f),
                radius = size.width * 0.8f * pulseScale2
            ),
            center = Offset(size.width * 0.15f, size.height * 0.82f),
            radius = size.width * 0.8f * pulseScale2
        )

        // Center Ambient Accent Orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor3, Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.45f),
                radius = size.width * 0.5f * pulseScale1
            ),
            center = Offset(size.width * 0.5f, size.height * 0.45f),
            radius = size.width * 0.5f * pulseScale1
        )

        // Geometric Dot Matrix Grid Overlay
        val patternColor = (if (isDarkTheme) Color.White else Color(0xFF5B308C)).copy(alpha = 0.035f)
        val step = 40.dp.toPx()
        var x = 20.dp.toPx()
        while (x < size.width) {
            var y = 20.dp.toPx()
            while (y < size.height) {
                drawCircle(
                    color = patternColor,
                    radius = 1.5.dp.toPx(),
                    center = Offset(x, y)
                )
                y += step
            }
            x += step
        }
    }
}
