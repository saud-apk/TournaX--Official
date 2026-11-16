package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TournaXBadge
import com.example.ui.components.TournaXEmblem
import com.example.ui.components.TournaXWordmark
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleEnd
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianInput
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
) {
    var startProgress by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startProgress = true
        delay(1900L)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emblem_scale",
    )

    val progress by animateFloatAsState(
        targetValue = if (startProgress) 1f else 0.08f,
        animationSpec = tween(durationMillis = 1750, easing = FastOutSlowInEasing),
        label = "arena_progress",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.scale(pulseScale)) {
                    TournaXEmblem(size = 108.dp)
                }
                Spacer(modifier = Modifier.height(20.dp))
                TournaXWordmark(fontSize = 36.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "PLAY  •  COMPETE  •  WIN",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MetallicSilverDim,
                        letterSpacing = 2.6.sp,
                        fontSize = 12.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(14.dp))
                TournaXBadge(text = "INDIA FREE FIRE ESPORTS ARENA")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Initializing Arena...",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MetallicSilverDim,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(ObsidianInput),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(NeonPurpleStart, NeonPurpleEnd),
                                ),
                            ),
                    )
                }
            }
        }
    }
}
