package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerProfile
import com.example.ui.components.TournaXBadge
import com.example.ui.components.TournaXCard
import com.example.ui.components.TournaXWordmark
import com.example.ui.components.claySurface
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianInput
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun AccountRestrictedScreen(
    profile: PlayerProfile,
    nowMillis: Long,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isPermanentBan = profile.isBanned
    val accentColor = if (isPermanentBan) CrimsonAlert else AmberNotice
    val surfaceTint = if (isPermanentBan) CrimsonSurface else AmberSurface
    val badgeText = if (isPermanentBan) "ACCOUNT BANNED" else "ACCOUNT SUSPENDED"
    val headlineText = if (isPermanentBan) {
        "Arena Access Permanently Locked"
    } else {
        "Account Temporarily Suspended"
    }
    val subText = if (isPermanentBan) {
        "Aapka TournaX account Admin team dwara permanently ban kar diya gaya hai. Matches, wallet, aur UID actions locked hain."
    } else {
        "Aapka account Fair-Play review ke under temporarily suspend hai. Timer khatam hone ya Admin unban karne par access automatically restore ho jayega."
    }

    val suspendedUntilMillis = profile.suspendedUntil?.toDate()?.time
    val remainingMillis = remember(suspendedUntilMillis, nowMillis) {
        if (suspendedUntilMillis != null) {
            (suspendedUntilMillis - nowMillis).coerceAtLeast(0L)
        } else {
            null
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    }
    val bannedAtLabel = remember(profile.bannedAt) {
        profile.bannedAt?.toDate()?.let { dateFormatter.format(it) } ?: "Active Moderation"
    }
    val untilLabel = remember(suspendedUntilMillis) {
        if (suspendedUntilMillis != null) {
            dateFormatter.format(Date(suspendedUntilMillis))
        } else {
            "Until Admin Review"
        }
    }

    val reasonText = profile.banReason.trim().ifBlank {
        if (isPermanentBan) {
            "Violation of TournaX Fair-Play / Anti-Cheat Rules."
        } else {
            "Temporary Fair-Play / Match Verification Hold by Admin."
        }
    }
    val issuedByText = profile.bannedBy.trim().ifBlank { "TournaX Admin Desk" }

    Scaffold(
        containerColor = ObsidianBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            TournaXWordmark(fontSize = 24.sp)
            Spacer(modifier = Modifier.height(18.dp))

            // 3D Clay Status Hero Card
            TournaXCard(
                borderColor = accentColor.copy(alpha = 0.55f),
                modifier = Modifier.testTag("restricted_status_card"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(surfaceTint, accentColor.copy(alpha = 0.10f)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPermanentBan) Icons.Filled.Block else Icons.Filled.HourglassBottom,
                            contentDescription = badgeText,
                            tint = accentColor,
                            modifier = Modifier.size(38.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TournaXBadge(
                        text = badgeText,
                        accentColor = accentColor,
                        backgroundColor = surfaceTint,
                        modifier = Modifier.testTag("restricted_status_badge"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = headlineText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MetallicSilver,
                            textAlign = TextAlign.Center,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MetallicSilverDim,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        ),
                    )

                    if (!isPermanentBan) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = surfaceTint,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("suspension_countdown_box"),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = if (remainingMillis != null) "UNLOCKS IN" else "SUSPENSION DURATION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = accentColor,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (remainingMillis != null) {
                                        formatRemainingDuration(remainingMillis)
                                    } else {
                                        "Manual Admin Review"
                                    },
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = ChakraPetchFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MetallicSilver,
                                    ),
                                    modifier = Modifier.testTag("suspension_countdown_text"),
                                )
                                if (remainingMillis != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Ends: $untilLabel",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MetallicSilverDim,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reason & Moderation Details Clay Card
            TournaXCard(borderColor = ObsidianBorderBright) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Moderation Reason",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MODERATION REASON",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ChakraPetchFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MetallicSilver,
                                letterSpacing = 0.8.sp,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = ObsidianInput,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = reasonText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier
                                .padding(14.dp)
                                .testTag("restricted_reason_text"),
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = ObsidianBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    ModerationDetailRow(
                        label = "Player IGN",
                        value = profile.freeFireIgn.ifBlank { profile.fullName.ifBlank { "Registered Player" } },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModerationDetailRow(
                        label = "Locked FF UID",
                        value = profile.freeFireUid.ifBlank { "Locked" },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModerationDetailRow(
                        label = "Action Taken",
                        value = bannedAtLabel,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModerationDetailRow(
                        label = "Issued By",
                        value = issuedByText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Appeal & Sign Out Clay Card
            TournaXCard(borderColor = ObsidianBorder) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.SupportAgent,
                            contentDescription = "Appeal Support",
                            tint = EmeraldVerified,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEED REVIEW OR APPEAL?",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ChakraPetchFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MetallicSilver,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Agar aapko lagta hai yeh action galti se hua hai, toh apni Free Fire UID (${profile.freeFireUid}) ke saath Admin Desk par appeal bhejein.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MetallicSilverDim,
                            lineHeight = 18.sp,
                        ),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val appealSummary = buildString {
                        append("TournaX Account Appeal\n")
                        append("Status: ${profile.accountStatus.uppercase(Locale.US)}\n")
                        append("IGN: ${profile.freeFireIgn}\n")
                        append("FF UID: ${profile.freeFireUid}\n")
                        append("User ID: ${profile.userId}\n")
                        append("Reason shown: $reasonText")
                    }

                    Button(
                        onClick = {
                            val waUri = "${TournaXSupportContacts.WHATSAPP_URI}?text=${Uri.encode(appealSummary)}"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(waUri)))
                            } catch (_: ActivityNotFoundException) {
                                clipboard.setText(AnnotatedString(TournaXSupportContacts.WHATSAPP_DISPLAY))
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldVerified,
                            contentColor = Color.Black,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("appeal_whatsapp_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SupportAgent,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appeal on WhatsApp",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val emailUri = Uri.parse(
                                "mailto:${TournaXSupportContacts.SUPPORT_EMAIL}" +
                                    "?subject=${Uri.encode("Account Appeal - UID ${profile.freeFireUid}")}" +
                                    "&body=${Uri.encode(appealSummary)}",
                            )
                            try {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, emailUri))
                            } catch (_: ActivityNotFoundException) {
                                clipboard.setText(AnnotatedString(TournaXSupportContacts.SUPPORT_EMAIL))
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, ObsidianBorderBright),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MetallicSilver,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("appeal_email_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appeal via Email",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, CrimsonAlert.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CrimsonAlert,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .claySurface(cornerRadius = 16.dp, elevation = 4.dp)
                            .testTag("restricted_sign_out_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ModerationDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(color = MetallicSilverDim),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MetallicSilver,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

internal fun formatRemainingDuration(remainingMillis: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis).coerceAtLeast(0L)
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        days > 0L -> "${days}d ${hours}h ${minutes}m"
        hours > 0L -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0L -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
