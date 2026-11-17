package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PlayerProfile
import com.example.data.model.Tournament
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.ClayAccentShadow
import com.example.ui.theme.ClaySurfaceBottom
import com.example.ui.theme.ClaySurfaceTop
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleEnd
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput
import kotlin.math.cos
import kotlin.math.sin

private data class ConfettiSpark(
    val xFraction: Float,
    val yOffset: Float,
    val sizeDp: Float,
    val speedFactor: Float,
    val color: Color,
    val baseAngle: Float,
)

private val celebrationSparks = listOf(
    ConfettiSpark(0.10f, 0.12f, 6f, 1.0f, EmeraldVerified, 15f),
    ConfettiSpark(0.22f, 0.48f, 5f, 1.3f, AmberNotice, 45f),
    ConfettiSpark(0.32f, 0.18f, 7f, 0.9f, NeonPurpleSoft, 70f),
    ConfettiSpark(0.46f, 0.08f, 5f, 1.15f, EmeraldVerified, 110f),
    ConfettiSpark(0.58f, 0.10f, 6f, 1.2f, AmberNotice, 25f),
    ConfettiSpark(0.70f, 0.24f, 7f, 0.85f, NeonPurpleStart, 55f),
    ConfettiSpark(0.82f, 0.42f, 5f, 1.25f, EmeraldVerified, 80f),
    ConfettiSpark(0.90f, 0.16f, 6f, 1.05f, AmberNotice, 130f),
    ConfettiSpark(0.16f, 0.72f, 4f, 1.1f, NeonPurpleSoft, 35f),
    ConfettiSpark(0.84f, 0.74f, 4.5f, 1.15f, NeonPurpleSoft, 95f),
)

@Composable
fun MatchJoinedCongratulationsDialog(
    tournament: Tournament,
    slotNumber: Long,
    profile: PlayerProfile,
    onViewRoomAndSlot: () -> Unit,
    onGoToMyMatches: () -> Unit,
    onDismiss: () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appeared = true
    }

    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "congrats_card_scale",
    )

    val trophyScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = 0.45f,
            stiffness = Spring.StiffnessLow,
        ),
        label = "congrats_trophy_scale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "congrats_confetti")
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confetti_phase",
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trophy_glow_pulse",
    )

    val isRoomOpen = tournament.roomUnlocked || tournament.status.equals("live", ignoreCase = true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val isCompactDialog = maxWidth < 360.dp
            val modalMaxHeight = (maxHeight * 0.92f).coerceAtLeast(320.dp)
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            EmeraldVerified.copy(alpha = 0.85f),
                            NeonPurpleStart.copy(alpha = 0.75f),
                            AmberNotice.copy(alpha = 0.7f),
                        ),
                    ),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .heightIn(max = modalMaxHeight)
                    .scale(cardScale)
                    .claySurface(
                        cornerRadius = 28.dp,
                        topColor = ClaySurfaceTop,
                        bottomColor = ClaySurfaceBottom,
                        shadowColor = ClayAccentShadow,
                        elevation = 18.dp,
                    )
                    .testTag("match_joined_congrats_dialog"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Top Celebration Hero Stage with Confetti + Glowing Trophy Crest
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1A1530),
                                        Color(0xFF10201E),
                                        ClaySurfaceTop,
                                    ),
                                ),
                            )
                            .padding(top = 14.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    ) {
                        // Animated floating confetti sparks
                        Canvas(
                            modifier = Modifier
                                .matchParentSize(),
                        ) {
                            val center = Offset(size.width / 2f, size.height * 0.48f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        EmeraldVerified.copy(alpha = 0.24f * pulseGlow),
                                        NeonPurpleStart.copy(alpha = 0.14f * pulseGlow),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = size.minDimension * 0.65f,
                                ),
                                radius = size.minDimension * 0.65f,
                                center = center,
                            )

                            celebrationSparks.forEachIndexed { idx, spark ->
                                val progress = (shimmerPhase * spark.speedFactor + idx * 0.13f) % 1f
                                val driftY = sin(progress * Math.PI * 2.0).toFloat() * 8.dp.toPx()
                                val driftX = cos(progress * Math.PI * 2.0).toFloat() * 5.dp.toPx()
                                val cx = size.width * spark.xFraction + driftX
                                val cy = size.height * spark.yOffset + driftY
                                val particleSize = spark.sizeDp.dp.toPx()
                                rotate(
                                    degrees = spark.baseAngle + shimmerPhase * 180f,
                                    pivot = Offset(cx, cy),
                                ) {
                                    if (idx % 2 == 0) {
                                        drawRect(
                                            color = spark.color.copy(alpha = 0.85f),
                                            topLeft = Offset(cx - particleSize / 2f, cy - particleSize / 3f),
                                            size = Size(particleSize, particleSize * 0.58f),
                                        )
                                    } else {
                                        drawCircle(
                                            color = spark.color.copy(alpha = 0.85f),
                                            radius = particleSize * 0.42f,
                                            center = Offset(cx, cy),
                                        )
                                    }
                                }
                            }
                        }

                        // Close Button
                        Surface(
                            color = ObsidianInput.copy(alpha = 0.88f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("congrats_close_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close Congratulations Popup",
                                    tint = MetallicSilver,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Entry Confirmed Pill
                            Surface(
                                color = EmeraldSurface,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.7f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = EmeraldVerified,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "ENTRY CONFIRMED",
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldVerified,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp,
                                        ),
                                    )
                                }
                            }

                            // Layered Trophy Crest + Overlapping Verified Seal
                            Box(
                                modifier = Modifier
                                    .size(if (isCompactDialog) 82.dp else 94.dp)
                                    .scale(trophyScale),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCompactDialog) 74.dp else 84.dp)
                                        .shadow(
                                            elevation = 16.dp,
                                            shape = CircleShape,
                                            ambientColor = EmeraldVerified,
                                            spotColor = NeonPurpleStart,
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    NeonPurpleStart,
                                                    EmeraldVerified,
                                                ),
                                            ),
                                        )
                                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = "Match Joined Trophy",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isCompactDialog) 38.dp else 44.dp),
                                    )
                                }

                                Surface(
                                    color = EmeraldVerified,
                                    shape = CircleShape,
                                    border = BorderStroke(2.dp, ClaySurfaceTop),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = (-2).dp, y = (-2).dp)
                                        .size(28.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Verified,
                                            contentDescription = "Slot Verified",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }

                            // Headline & Subheadline
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "CONGRATULATIONS!",
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = if (isCompactDialog) 19.sp else 22.sp,
                                        letterSpacing = 0.6.sp,
                                    ),
                                )
                                Text(
                                    text = "Your lobby seat is officially locked in TournaX!",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MetallicSilver,
                                    ),
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // Tactical Match Ticket Card + Action Buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            color = ObsidianInput,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        TournaXBadge(
                                            text = "${tournament.mode} • ${tournament.mapName.ifBlank { "Bermuda" }}",
                                            accentColor = NeonPurpleSoft,
                                            backgroundColor = NeonPurpleGlow,
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(NeonPurpleStart, EmeraldVerified),
                                                ),
                                            )
                                            .testTag("congrats_slot_badge"),
                                    ) {
                                        Text(
                                            text = "SLOT #$slotNumber",
                                            maxLines = 1,
                                            softWrap = false,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp,
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = tournament.title,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                        ),
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            tint = NeonPurpleSoft,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = tournament.dateTimeLabel.ifBlank { "Schedule TBA" },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                                        )
                                    }
                                }

                                HorizontalDivider(color = ObsidianBorder)

                                // Player IGN & Entry/Prize Summary
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        color = ObsidianElevated,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ObsidianBorder),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "CAPTAIN",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MetallicSilverDim,
                                                    fontSize = 9.sp,
                                                ),
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = profile.freeFireIgn.ifBlank { profile.fullName },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = MetallicSilver,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            )
                                        }
                                    }
                                    Surface(
                                        color = ObsidianElevated,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, ObsidianBorder),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "PRIZE POOL",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MetallicSilverDim,
                                                    fontSize = 9.sp,
                                                ),
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${tournament.prizePool} Pts",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    color = AmberNotice,
                                                    fontWeight = FontWeight.ExtraBold,
                                                ),
                                            )
                                        }
                                    }
                                }

                                // Room ID Status Pill
                                Surface(
                                    color = if (isRoomOpen) EmeraldSurface else AmberSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isRoomOpen) EmeraldVerified.copy(alpha = 0.55f) else AmberNotice.copy(alpha = 0.5f),
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (isRoomOpen) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = if (isRoomOpen) EmeraldVerified else AmberNotice,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = if (isRoomOpen) {
                                                "Room ID & Password are open! Tap View Room below."
                                            } else {
                                                "Room ID & Password unlock 15 mins before match start."
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isRoomOpen) EmeraldVerified else AmberNotice,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons
                        TournaXGradientButton(
                            text = "View Room & Slot →",
                            leadingIcon = Icons.Rounded.CheckCircle,
                            onClick = onViewRoomAndSlot,
                            testTag = "congrats_view_room_button",
                        )

                        OutlinedButton(
                            onClick = onGoToMyMatches,
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicSilver),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("congrats_my_matches_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = NeonPurpleSoft,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open My Tournaments",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
