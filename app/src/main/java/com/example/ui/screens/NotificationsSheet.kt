package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArenaNotification
import com.example.ui.components.TournaXBadge
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationsSheet(
    notifications: List<ArenaNotification>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: (List<String>) -> Unit,
    onLinkError: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault())
    }
    val unreadIds = notifications.filter { !it.isRead }.map { it.id }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianCard,
        contentColor = MetallicSilver,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val maxListHeight = (maxHeight * 0.62f).coerceIn(220.dp, 460.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Campaign,
                        contentDescription = null,
                        tint = NeonPurpleSoft,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Notifications",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = if (unreadIds.isEmpty()) {
                                "Live Admin Broadcasts • All caught up"
                            } else {
                                "Live Admin Broadcasts • ${unreadIds.size} unread"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                        )
                    }
                }

                if (unreadIds.isNotEmpty()) {
                    TextButton(
                        onClick = { onMarkAllRead(unreadIds) },
                        modifier = Modifier.testTag("mark_all_notifications_read"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = null,
                            tint = NeonPurpleSoft,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mark All Read",
                            color = NeonPurpleSoft,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            HorizontalDivider(color = ObsidianBorder)

            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsNone,
                        contentDescription = null,
                        tint = NeonPurpleSoft,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Admin Broadcasts Yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MetallicSilver,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Official room updates, schedule alerts, and announcements from the TournaX Admin Panel appear here in real time.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        val isHighPriority = notif.priority.equals("high", ignoreCase = true)
                        val isTargeted = notif.targetUserId.isNotBlank() &&
                            !notif.targetUserId.equals("all", ignoreCase = true)
                        val timeText = notif.createdAt?.toDate()?.let { dateFormatter.format(it) }.orEmpty()

                        Surface(
                            color = if (notif.isRead) ObsidianInput else ObsidianElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    !notif.isRead && isHighPriority -> AmberNotice.copy(alpha = 0.7f)
                                    !notif.isRead -> NeonPurpleSoft.copy(alpha = 0.55f)
                                    else -> ObsidianBorder
                                },
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (!notif.isRead) onMarkRead(notif.id)
                                }
                                .testTag("notification_card_${notif.id}"),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                if (!notif.isRead) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 5.dp, end = 10.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CrimsonAlert),
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        TournaXBadge(
                                            text = notif.category.ifBlank { "admin" }.uppercase(Locale.US),
                                        )
                                        if (isHighPriority) {
                                            TournaXBadge(
                                                text = "URGENT",
                                                accentColor = AmberNotice,
                                                backgroundColor = AmberSurface,
                                            )
                                        }
                                        if (isTargeted) {
                                            TournaXBadge(
                                                text = "FOR YOU",
                                                accentColor = NeonPurpleSoft,
                                                backgroundColor = NeonPurpleGlow,
                                            )
                                        }
                                        if (timeText.isNotEmpty()) {
                                            Text(
                                                text = timeText,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MetallicSilverDim,
                                                    fontSize = 10.sp,
                                                ),
                                            )
                                        }
                                    }

                                    Text(
                                        text = notif.title,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = MetallicSilver,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )

                                    Text(
                                        text = notif.body,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MetallicSilverDim,
                                        ),
                                    )

                                    if (notif.linkUrl.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                if (!notif.isRead) onMarkRead(notif.id)
                                                try {
                                                    val rawUrl = notif.linkUrl.trim()
                                                    val normalizedUrl = if (
                                                        rawUrl.contains("://") ||
                                                        rawUrl.startsWith("mailto:", ignoreCase = true) ||
                                                        rawUrl.startsWith("tel:", ignoreCase = true)
                                                    ) {
                                                        rawUrl
                                                    } else {
                                                        "https://$rawUrl"
                                                    }
                                                    uriHandler.openUri(normalizedUrl)
                                                } catch (_: Exception) {
                                                    onLinkError("Unable to open notification link.")
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("notification_link_${notif.id}"),
                                        ) {
                                            Text(
                                                text = "Open Link",
                                                color = NeonPurpleSoft,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                                contentDescription = "Open notification link",
                                                tint = NeonPurpleSoft,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
