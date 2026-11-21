package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleEnd
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import java.text.NumberFormat
import java.util.Locale

data class ArenaAvatarOption(
    val index: Long,
    val title: String,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color,
)

val arenaAvatarOptions = listOf(
    ArenaAvatarOption(0L, "Vanguard", Icons.Rounded.Shield, NeonPurpleStart, NeonPurpleEnd),
    ArenaAvatarOption(1L, "Striker", Icons.Rounded.Bolt, Color(0xFFEA580C), Color(0xFFB45309)),
    ArenaAvatarOption(2L, "Marksman", Icons.Rounded.GpsFixed, Color(0xFF2563EB), Color(0xFF1E40AF)),
    ArenaAvatarOption(3L, "Champion", Icons.Rounded.EmojiEvents, AmberNotice, Color(0xFFD97706)),
)

@Composable
fun ArenaAvatarBadge(
    avatarIndex: Long,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val option = arenaAvatarOptions.getOrElse(avatarIndex.toInt()) { arenaAvatarOptions.first() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(option.gradientStart, option.gradientEnd),
                ),
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(MetallicSilver, NeonPurpleSoft),
                ),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = "${option.title} Avatar",
            tint = Color.White,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

@Composable
fun SubPageTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (() -> Unit)? = null,
) {
    Surface(
        color = ObsidianElevated,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("subpage_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MetallicSilver,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MetallicSilver,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MetallicSilverDim,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (trailingAction != null) {
                trailingAction()
            }
        }
    }
}

@Composable
fun ArenaFeedbackBanner(
    errorText: String?,
    successText: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isError = !errorText.isNullOrBlank()
    val message = if (isError) errorText else successText
    if (message.isNullOrBlank()) return

    val accent = if (isError) CrimsonAlert else EmeraldVerified
    val bg = if (isError) CrimsonSurface else EmeraldSurface
    val icon = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle

    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onDismiss),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isError) "Error" else "Success",
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MetallicSilver,
                    fontSize = 13.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss Message",
                    tint = MetallicSilverDim,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

fun formatIndianNumber(value: Long): String {
    return NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
}

fun openExternalHttpsUrl(context: Context, url: String) {
    if (!url.startsWith("https://")) {
        Toast.makeText(context, "Invalid link URL", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No browser app found to open $url", Toast.LENGTH_SHORT).show()
    }
}
