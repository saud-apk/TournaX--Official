package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PlayerProfile
import com.example.ui.components.TournaXAvatarImage
import com.example.ui.components.TournaXCard
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput

@Composable
fun LeaderboardScreen(
    entries: List<LeaderboardEntry>,
    profile: PlayerProfile,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val top15 = entries.take(15)
    val first = top15.getOrNull(0)
    val second = top15.getOrNull(1)
    val third = top15.getOrNull(2)

    val myIndex = entries.indexOfFirst { it.id == profile.userId }
    val estimatedRank = if (myIndex >= 0) {
        myIndex + 1
    } else {
        entries.count { it.kills > profile.totalKills } + 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        SubPageHeader(
            title = "ARENA LEADERBOARD",
            subtitle = "Live verified TournaX players",
            onBack = onBack,
            backTag = "leaderboard_back",
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (top15.isEmpty()) {
                item {
                    TournaXCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = NeonPurpleSoft,
                                modifier = Modifier.size(34.dp),
                            )
                            Text(
                                text = "Live Leaderboard Syncing",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = "Play matches and verify results to climb the real TournaX rankings.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                            )
                        }
                    }
                }
            }

            if (top15.size >= 3) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        PodiumCard(
                            rank = "#2",
                            entry = second,
                            accentColor = MetallicSilver,
                            topPadding = 16.dp,
                            modifier = Modifier.weight(1f),
                        )
                        PodiumCard(
                            rank = "#1",
                            entry = first,
                            accentColor = AmberNotice,
                            topPadding = 0.dp,
                            modifier = Modifier.weight(1.1f),
                        )
                        PodiumCard(
                            rank = "#3",
                            entry = third,
                            accentColor = Color(0xFFCD7F32),
                            topPadding = 22.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            itemsIndexed(top15, key = { index, item -> item.id.ifBlank { "$index" } }) { index, entry ->
                val isCurrentUser = entry.id == profile.userId
                TournaXCard(
                    borderColor = if (isCurrentUser) NeonPurpleStart else ObsidianBorder,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = when (index) {
                                    0 -> AmberNotice
                                    1 -> MetallicSilver
                                    2 -> Color(0xFFCD7F32)
                                    else -> NeonPurpleSoft
                                },
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.width(34.dp),
                        )

                        TournaXAvatarImage(
                            avatarIndex = entry.avatarIndex,
                            size = 38.dp,
                            contentDescription = entry.ign,
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = entry.ign,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MetallicSilver,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (isCurrentUser) {
                                    Text(
                                        text = "YOU",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = NeonPurpleSoft,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                    )
                                }
                            }
                            Text(
                                text = "${entry.booyahs} Booyahs • ${entry.winningsInr} Pts Won",
                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${entry.kills}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = NeonPurpleSoft,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = "KILLS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                    fontSize = 9.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }

        // Sticky "Your Rank" bar pinned at bottom
        Surface(
            color = ObsidianElevated,
            border = BorderStroke(1.dp, NeonPurpleStart.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
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
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MilitaryTech,
                        contentDescription = null,
                        tint = AmberNotice,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YOUR RANK • #$estimatedRank",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = NeonPurpleSoft,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = profile.freeFireIgn.ifBlank { profile.fullName },
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${profile.totalKills} Kills",
                        style = MaterialTheme.typography.labelLarge.copy(color = MetallicSilver),
                    )
                    Text(
                        text = "${profile.booyahs} Booyahs",
                        style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumCard(
    rank: String,
    entry: LeaderboardEntry?,
    accentColor: Color,
    topPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f)),
        modifier = modifier.padding(top = topPadding),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (entry != null) {
                TournaXAvatarImage(
                    avatarIndex = entry.avatarIndex,
                    size = 34.dp,
                    contentDescription = entry.ign,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = rank,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = rank,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = entry?.ign ?: "—",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MetallicSilver,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${entry?.kills ?: 0} Kills",
                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
            )
        }
    }
}
