package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PlayerProfile
import com.example.data.model.Tournament
import com.example.data.model.TournamentResultEntry
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.ClaySurfaceBottom
import com.example.ui.theme.ClaySurfaceTop
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput
import com.example.ui.theme.SlateSubtle

@Composable
fun MatchPointsTableDialog(
    tournament: Tournament,
    results: List<TournamentResultEntry>,
    profile: PlayerProfile,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .testTag("points_table_dialog"),
            contentAlignment = Alignment.Center,
        ) {
            val maxDialogHeight = (maxHeight * 0.92f).coerceAtLeast(360.dp)
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.2.dp, ObsidianBorderBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .heightIn(max = maxDialogHeight)
                    .claySurface(
                        cornerRadius = 22.dp,
                        topColor = ClaySurfaceTop,
                        bottomColor = ClaySurfaceBottom,
                        elevation = 12.dp,
                    ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Clean Header Bar
                    Surface(
                        color = ObsidianElevated,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Surface(
                                    color = NeonPurpleGlow,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, NeonPurpleStart.copy(alpha = 0.45f)),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Leaderboard,
                                        contentDescription = "Points Table",
                                        tint = NeonPurpleSoft,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(20.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        text = "POINTS TABLE",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MetallicSilver,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                        ),
                                        maxLines = 1,
                                    )
                                    Text(
                                        text = "${tournament.title} • ${tournament.mode}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MetallicSilverDim,
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("points_table_close_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close Points Table",
                                    tint = MetallicSilverDim,
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MatchPointsTableSection(
                            tournament = tournament,
                            results = results,
                            currentUserId = profile.userId,
                            currentFreeFireUid = profile.freeFireUid,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchPointsTableSection(
    tournament: Tournament,
    results: List<TournamentResultEntry>,
    currentUserId: String,
    currentFreeFireUid: String,
    modifier: Modifier = Modifier,
) {
    val myStanding = results.firstOrNull { entry ->
        (entry.userId.isNotBlank() && entry.userId == currentUserId) ||
            (entry.freeFireUid.isNotBlank() && entry.freeFireUid == currentFreeFireUid)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (myStanding != null) {
            val myRank = if (myStanding.rank > 0L) myStanding.rank else (results.indexOf(myStanding) + 1).toLong()
            Surface(
                color = if (myStanding.prizeWon > 0L) EmeraldSurface else ObsidianElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (myStanding.prizeWon > 0L) EmeraldVerified.copy(alpha = 0.6f) else NeonPurpleStart.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("points_table_user_summary"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = if (myStanding.prizeWon > 0L) EmeraldVerified else AmberNotice,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "YOUR MATCH RESULT • RANK #$myRank" + if (myStanding.isMvp) " (MVP)" else "",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                            )
                            Text(
                                text = if (myStanding.prizeWon > 0L) {
                                    "${myStanding.kills} Kills • Credited to Withdrawable Winning Wallet"
                                } else {
                                    "${myStanding.kills} Kills • Official Admin-Verified Standing"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (myStanding.prizeWon > 0L) EmeraldVerified else MetallicSilverDim,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                    }
                    Text(
                        text = if (myStanding.prizeWon > 0L) "+${myStanding.prizeWon} Pts" else "0 Pts",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (myStanding.prizeWon > 0L) EmeraldVerified else MetallicSilverDim,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    )
                }
            }
        }
        // Section Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "OFFICIAL STANDINGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MetallicSilverDim,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                ),
            )
            if (results.isNotEmpty()) {
                TournaXBadge(
                    text = "ADMIN VERIFIED",
                    accentColor = EmeraldVerified,
                    backgroundColor = EmeraldSurface,
                )
            } else {
                TournaXBadge(
                    text = "PENDING PUBLISH",
                    accentColor = AmberNotice,
                    backgroundColor = AmberSurface,
                )
            }
        }

        if (results.isEmpty()) {
            Surface(
                color = ObsidianInput,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("points_table_pending_card"),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = AmberNotice,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Points Table Awaited",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "Host is finalizing match standings (${tournament.mode}) from the Admin Panel.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MetallicSilverDim),
                        )
                    }
                }
            }
        } else {
            // Strictly 4-Column Points Table: NO. | PLAYER NAME | KILLS | WINNING
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .claySurface(
                        cornerRadius = 16.dp,
                        topColor = ClaySurfaceTop,
                        bottomColor = ClaySurfaceBottom,
                        elevation = 6.dp,
                    ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianElevated)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TableHeaderCell("NO.", Modifier.width(38.dp), TextAlign.Start)
                        TableHeaderCell("PLAYER NAME", Modifier.weight(1f), TextAlign.Start)
                        TableHeaderCell("KILLS", Modifier.width(50.dp), TextAlign.Center)
                        TableHeaderCell("WINNING", Modifier.width(74.dp), TextAlign.End)
                    }
                    HorizontalDivider(color = ObsidianBorder)

                    results.forEachIndexed { idx, entry ->
                        val rankDisplay = if (entry.rank > 0L) entry.rank else (idx + 1).toLong()
                        val isMe = (entry.userId.isNotBlank() && entry.userId == currentUserId) ||
                            (entry.freeFireUid.isNotBlank() && entry.freeFireUid == currentFreeFireUid)
                        val rankColor = when (rankDisplay) {
                            1L -> AmberNotice
                            2L -> MetallicSilver
                            3L -> NeonPurpleSoft
                            else -> MetallicSilverDim
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isMe) NeonPurpleGlow else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 11.dp)
                                .testTag("points_table_row_${entry.id.ifBlank { rankDisplay.toString() }}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "#$rankDisplay",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = rankColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                ),
                                modifier = Modifier.width(38.dp),
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(
                                    text = formatEntryLabel(entry),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MetallicSilver,
                                        fontWeight = if (isMe || rankDisplay <= 3L) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (isMe) {
                                    Surface(
                                        color = NeonPurpleStart.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp),
                                    ) {
                                        Text(
                                            text = "YOU",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = NeonPurpleSoft,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        )
                                    }
                                }
                            }

                            TableValueCell(
                                text = "${entry.kills}",
                                color = MetallicSilver,
                                modifier = Modifier.width(50.dp),
                            )

                            Text(
                                text = if (entry.prizeWon > 0L) "${entry.prizeWon} Pts" else "0 Pts",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (entry.prizeWon > 0L) EmeraldVerified else SlateSubtle,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                ),
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(74.dp),
                            )
                        }

                        if (idx < results.lastIndex) {
                            HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            color = SlateSubtle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
        ),
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Composable
private fun TableValueCell(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        ),
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

private fun formatEntryLabel(entry: TournamentResultEntry): String {
    return if (entry.squadTag.isNotBlank()) {
        "[${entry.squadTag}] ${entry.playerOrTeamName}"
    } else {
        entry.playerOrTeamName
    }
}
