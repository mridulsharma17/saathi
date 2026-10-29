package com.example.saathi.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.R
import com.example.saathi.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val buttonText: String,
    val showLogo: Boolean = false
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "Meet Rahi 👋",
            subtitle = "Your AI Companion",
            description = "A witty, supportive, and non-judgmental friend. Always here to listen, chat, and keep you company.",
            icon = Icons.Default.AutoAwesome,
            buttonText = "Get Started",
            showLogo = true
        ),
        OnboardingPage(
            title = "Smart Memory 🧠",
            subtitle = "Contextual Understanding",
            description = "Rahi remembers details about your life, goals, and friends over time so every conversation feels personal.",
            icon = Icons.Default.Psychology,
            buttonText = "Continue"
        ),
        OnboardingPage(
            title = "Life Journey 🌟",
            subtitle = "Automatic Timeline",
            description = "Share your achievements, career wins, and milestones. They automatically land on your visual journey timeline.",
            icon = Icons.Default.Timeline,
            buttonText = "Start Exploring"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val isDarkTheme = MaterialTheme.colorScheme.background != LightBackground
    
    // Background Breathing Glow Animation
    val infiniteTransition = rememberInfiniteTransition(label = "onboardingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

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

    val glowColor1 = PrimaryPurple.copy(alpha = if (isDarkTheme) 0.12f else 0.06f)
    val glowColor2 = Color(0xFF7C4DFF).copy(alpha = if (isDarkTheme) 0.08f else 0.04f)

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
        ) {
            // Ambient Glowing Background Circles
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor1, Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = size.width * 0.7f * pulseScale
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = size.width * 0.7f * pulseScale
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor2, Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.7f),
                        radius = size.width * 0.8f * pulseScale
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.7f),
                    radius = size.width * 0.8f * pulseScale
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar with Skip Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (pagerState.currentPage < pages.size - 1) {
                        TextButton(onClick = onFinished) {
                            Text(
                                text = "Skip",
                                color = SecondaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { position ->
                    OnboardingContent(pages[position])
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Indicator Dots
                    Row(
                        modifier = Modifier.padding(bottom = 28.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(pages.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            val width = if (isSelected) 28.dp else 8.dp
                            val color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(width, 8.dp)
                            )
                        }
                    }

                    // Primary Button
                    val currentPage = pages[pagerState.currentPage]
                    Button(
                        onClick = {
                            if (pagerState.currentPage < pages.size - 1) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onFinished()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentPage.buttonText,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "By continuing, you agree to our Terms & Privacy Policy.",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingContent(page: OnboardingPage) {
    val floatTransition = rememberInfiniteTransition(label = "badgeFloat")
    val floatOffset by floatTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (page.showLogo) {
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .offset(y = floatOffset.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryPurple.copy(alpha = 0.25f))
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
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Saarthi AI",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryPurple,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(36.dp))
        } else {
            Surface(
                modifier = Modifier
                    .size(112.dp)
                    .offset(y = floatOffset.dp),
                shape = CircleShape,
                color = PrimaryPurple.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(36.dp))
        }

        Surface(
            color = PrimaryPurple.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = page.subtitle.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
