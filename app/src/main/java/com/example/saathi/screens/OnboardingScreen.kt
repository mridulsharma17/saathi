package com.example.saathi.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.saathi.ui.theme.AccentPurple
import com.example.saathi.ui.theme.DarkBackground
import com.example.saathi.ui.theme.TextGrey
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val highlightedTitle: String,
    val description: String,
    val icon: ImageVector,
    val buttonText: String? = null,
    val showLogo: Boolean = false
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            title = "You are not alone.",
            highlightedTitle = "We are always\nhere for you.",
            description = "", // Left empty to match the cleaner look in first image
            icon = Icons.Default.Favorite,
            buttonText = "Get Started",
            showLogo = true
        ),
        OnboardingPage(
            title = "We listen.",
            highlightedTitle = "We don't judge.",
            description = "Speak freely.\nThis is your safe space.",
            icon = Icons.Default.Favorite,
            buttonText = "Next"
        ),
        OnboardingPage(
            title = "Every journey\nbegins with",
            highlightedTitle = "one conversation.",
            description = "Let's take the first step\ntogether.",
            icon = Icons.AutoMirrored.Filled.Chat,
            buttonText = "Let's Begin"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Background Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F102B),
                            DarkBackground
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { position ->
                OnboardingContent(pages[position])
            }

            // Bottom UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    Modifier.padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) AccentPurple else Color.Gray.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                        )
                    }
                }

                // Button
                val currentPage = pages[pagerState.currentPage]
                if (currentPage.buttonText != null) {
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
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = currentPage.buttonText,
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.CenterEnd),
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(56.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (page.showLogo) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Rahi",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = "💜",
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(64.dp))
        } else {
            Box(contentAlignment = Alignment.Center) {
                // Glow effect
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AccentPurple.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(AccentPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )
        
        Text(
            text = page.highlightedTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = AccentPurple,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        if (page.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "— 💜 —",
                color = AccentPurple.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = TextGrey,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}
