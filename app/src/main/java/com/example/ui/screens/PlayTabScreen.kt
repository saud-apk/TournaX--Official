package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PlayerProfile
import com.example.data.model.Tournament
import com.example.data.model.TournamentParticipant
import com.example.data.model.TournamentRegistration
import com.example.data.model.TournamentResultEntry
import com.example.ui.components.MatchJoinedCongratulationsDialog
import com.example.ui.components.TournaXBadge
import com.example.ui.components.TournaXCard
import com.example.ui.components.TournamentDetailDialog
import com.example.ui.components.drawableForModeString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSurface
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
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput
import java.util.Locale

private enum class ArenaModeCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val drawableRes: Int,
) {
    ALL(
        id = "all",
        title = "ALL MATCHES",
        subtitle = "Every Live & Upcoming Lobby",
        drawableRes = R.drawable.mode_br,
    ),
    BR(
        id = "br",
        title = "BR",
        subtitle = "Battle Royale",
        drawableRes = R.drawable.mode_br,
    ),
    CS(
        id = "cs",
        title = "CS",
        subtitle = "Clash Squad",
        drawableRes = R.drawable.mode_cs,
    ),
    LONE_WOLF(
        id = "lone_wolf",
        title = "LONE WOLF",
        subtitle = "1v1 Showdown",
        drawableRes = R.drawable.mode_lone_wolf,
    ),
    FREE_MATCHES(
        id = "free_matches",
        title = "FREE MATCHES",
        subtitle = "Zero Entry Fee",
        drawableRes = R.drawable.mode_free_matches,
    ),
}

private val csModeRegex = Regex("\\b(cs|clash\\s*squad|4v4)\\b")
private val lwModeRegex = Regex("\\b(lw|lone\\s*wolf|1v1)\\b")
private val brModeRegex = Regex("\\b(br|battle\\s*royale)\\b")

private fun matchesCategory(tournament: Tournament, category: ArenaModeCategory): Boolean {
    if (category == ArenaModeCategory.ALL) return true
    if (category == ArenaModeCategory.FREE_MATCHES) return tournament.entryFee <= 0L
    val m = tournament.mode.trim().lowercase(Locale.US)
    return when (category) {
        ArenaModeCategory.ALL -> true
        ArenaModeCategory.FREE_MATCHES -> tournament.entryFee <= 0L
        ArenaModeCategory.CS -> csModeRegex.containsMatchIn(m)
        ArenaModeCategory.LONE_WOLF -> lwModeRegex.containsMatchIn(m)
        ArenaModeCategory.BR -> brModeRegex.containsMatchIn(m) ||
            (!csModeRegex.containsMatchIn(m) && !lwModeRegex.containsMatchIn(m))
    }
}

private val statusFilters = listOf(
    "upcoming" to "Upcoming",
    "live" to "Live Now",
    "completed" to "Completed",
)

private fun formatFiltersForCategory(category: ArenaModeCategory): List<String> = when (category) {
    ArenaModeCategory.BR -> listOf("All", "Solo", "Duo", "Squad")
    ArenaModeCategory.CS -> listOf("All", "4v4", "2v2", "1v1", "Squad")
    ArenaModeCategory.LONE_WOLF -> listOf("All", "1v1", "2v2")
    ArenaModeCategory.FREE_MATCHES, ArenaModeCategory.ALL -> listOf("All", "Solo", "Duo", "Squad", "4v4", "1v1")
}

private fun matchesFormatFilter(mode: String, selectedFormat: String): Boolean {
    if (selectedFormat == "All") return true
    val normalized = mode.trim()
        .lowercase(Locale.US)
        .replace(Regex("\\s*vs\\s*"), "v")
        .replace(Regex("\\s+"), " ")
    val cleanTarget = selectedFormat.trim().lowercase(Locale.US)
    return when (cleanTarget) {
        "solo" -> Regex("\\bsolo\\b").containsMatchIn(normalized)
        "duo" -> Regex("\\bduo\\b").containsMatchIn(normalized)
        "1v1" -> Regex("\\b1v1\\b").containsMatchIn(normalized) ||
            (Regex("\\b(lw|lone\\s*wolf)\\b").containsMatchIn(normalized) && !Regex("\\b2v2\\b").containsMatchIn(normalized))
        "2v2" -> Regex("\\b2v2\\b").containsMatchIn(normalized)
        "4v4" -> Regex("\\b4v4\\b").containsMatchIn(normalized)
        "squad" -> !Regex("\\b(1v1|2v2|solo|duo)\\b").containsMatchIn(normalized) &&
            (Regex("\\bsquad\\b").containsMatchIn(normalized.replace("clash squad", " ")) ||
                Regex("\\b(4v4|clash squad)\\b").containsMatchIn(normalized))
        else -> normalized.contains(cleanTarget)
    }
}

@Composable
fun PlayTabScreen(
    profile: PlayerProfile,
    tournaments: List<Tournament>,
    registrations: List<TournamentRegistration>,
    isBusy: Boolean,
    onJoinTournament: (Tournament, List<String>, (Long) -> Unit) -> Unit,
    onGoToWallet: () -> Unit,
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenMyTournaments: () -> Unit = {},
    observeMatchResults: (String) -> StateFlow<List<TournamentResultEntry>> = { MutableStateFlow(emptyList()) },
    observeMatchParticipants: (String) -> StateFlow<List<TournamentParticipant>> = { MutableStateFlow(emptyList()) },
    onEnsureOwnParticipantSynced: (String, TournamentRegistration?) -> Unit = { _, _ -> },
) {
    var activeCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeCategory = ArenaModeCategory.entries.firstOrNull { it.id == activeCategoryId }
    var selectedStatus by rememberSaveable { mutableStateOf("upcoming") }
    var selectedFormat by rememberSaveable { mutableStateOf("All") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var activeTournament by remember { mutableStateOf<Tournament?>(null) }
    var joinedCelebration by remember { mutableStateOf<Pair<Tournament, Long>?>(null) }

    val depositPts = profile.safeDepositPoints
    val winningPts = profile.safeWinningPoints

    val visibleTournaments = remember(tournaments) {
        tournaments.filterNot { it.status.equals("cancelled", ignoreCase = true) }
    }

    val regByTournamentId = remember(registrations) {
        registrations.associateBy { it.tournamentId }
    }

    val totalLiveCount = remember(visibleTournaments) {
        visibleTournaments.count { it.status.equals("live", ignoreCase = true) }
    }
    val totalUpcomingCount = remember(visibleTournaments) {
        visibleTournaments.count { it.status.equals("upcoming", ignoreCase = true) }
    }

    val featuredMatch = remember(visibleTournaments) {
        visibleTournaments.firstOrNull { it.status.equals("live", ignoreCase = true) }
            ?: visibleTournaments.firstOrNull { it.status.equals("upcoming", ignoreCase = true) }
    }

    val openCategory: (ArenaModeCategory) -> Unit = { category ->
        val modeItems = visibleTournaments.filter { matchesCategory(it, category) }
        val hasLive = modeItems.any { it.status.equals("live", ignoreCase = true) }
        val hasUpcoming = modeItems.any { it.status.equals("upcoming", ignoreCase = true) }
        selectedStatus = when {
            hasLive && !hasUpcoming -> "live"
            else -> "upcoming"
        }
        selectedFormat = "All"
        activeCategoryId = category.id
    }

    if (activeCategory != null) {
        BackHandler {
            activeCategoryId = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        val currentCategory = activeCategory
        if (currentCategory == null) {
            // PRO ESPORTS MODE HUB
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header with Arena Brand + Live/Upcoming Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TOURNAMENTS",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = MetallicSilver,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )
                        Text(
                            text = "Verified Free Fire Mobile Lobbies",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                        )
                    }

                    LiveHeaderBadge(
                        liveCount = totalLiveCount,
                        upcomingCount = totalUpcomingCount,
                    )
                }

                // Quick Command Strip: Wallet Points + My Joined Slots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onGoToWallet)
                            .testTag("play_wallet_points_chip"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = "Wallet Points",
                                tint = NeonPurpleStart,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = "DEPOSIT • WINNING",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MetallicSilverDim,
                                        fontSize = 9.sp,
                                    ),
                                )
                                Text(
                                    text = "$depositPts • $winningPts Pts",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    ),
                                )
                            }
                        }
                    }

                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenMyTournaments)
                            .testTag("play_my_slots_chip"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ConfirmationNumber,
                                contentDescription = "My Joined Matches",
                                tint = MetallicSilverDim,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text(
                                    text = "MY JOINED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MetallicSilverDim,
                                        fontSize = 9.sp,
                                    ),
                                )
                                Text(
                                    text = "${registrations.size} Matches",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    ),
                                )
                            }
                        }
                    }
                }

                // Real-time Spotlight Match Banner OR Clean Live Sync Strip
                if (featuredMatch != null) {
                    FeaturedTournamentBanner(
                        tournament = featuredMatch,
                        isJoined = regByTournamentId.containsKey(featuredMatch.id),
                        onClick = { activeTournament = featuredMatch },
                    )
                } else {
                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                color = ObsidianElevated,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SportsEsports,
                                    contentDescription = null,
                                    tint = NeonPurpleStart,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LIVE LOBBY SYNC",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(
                                    text = "Official matches scheduled by Admin appear here in real time.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SELECT MODE",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MetallicSilverDim,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp,
                        ),
                    )
                    Text(
                        text = "${visibleTournaments.size} Lobbies",
                        style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                    )
                }

                // Row 1: BR & CS Pro Photo Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ModeSquareCard(
                        category = ArenaModeCategory.BR,
                        matches = remember(visibleTournaments) {
                            visibleTournaments.filter { matchesCategory(it, ArenaModeCategory.BR) }
                        },
                        onClick = { openCategory(ArenaModeCategory.BR) },
                        modifier = Modifier.weight(1f),
                    )
                    ModeSquareCard(
                        category = ArenaModeCategory.CS,
                        matches = remember(visibleTournaments) {
                            visibleTournaments.filter { matchesCategory(it, ArenaModeCategory.CS) }
                        },
                        onClick = { openCategory(ArenaModeCategory.CS) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Row 2: Lone Wolf & Free Matches Square Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ModeSquareCard(
                        category = ArenaModeCategory.LONE_WOLF,
                        matches = remember(visibleTournaments) {
                            visibleTournaments.filter { matchesCategory(it, ArenaModeCategory.LONE_WOLF) }
                        },
                        onClick = { openCategory(ArenaModeCategory.LONE_WOLF) },
                        modifier = Modifier.weight(1f),
                    )
                    ModeSquareCard(
                        category = ArenaModeCategory.FREE_MATCHES,
                        matches = remember(visibleTournaments) {
                            visibleTournaments.filter { matchesCategory(it, ArenaModeCategory.FREE_MATCHES) }
                        },
                        onClick = { openCategory(ArenaModeCategory.FREE_MATCHES) },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Row 3: All Lobbies Wide Hub Card
                AllMatchesHubCard(
                    tournaments = visibleTournaments,
                    onClick = { openCategory(ArenaModeCategory.ALL) },
                    modifier = Modifier.fillMaxWidth(),
                )

                // Pro Fair-Play Guarantee Strip
                Surface(
                    color = ObsidianCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = EmeraldVerified,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Mobile Only • Atomic Slot Lock • Verified Room Credentials",
                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        } else {
            val categoryTournaments = remember(visibleTournaments, currentCategory) {
                visibleTournaments.filter { matchesCategory(it, currentCategory) }
            }
            val upcomingCount = remember(categoryTournaments) {
                categoryTournaments.count { it.status.equals("upcoming", ignoreCase = true) }
            }
            val liveCount = remember(categoryTournaments) {
                categoryTournaments.count { it.status.equals("live", ignoreCase = true) }
            }
            val completedCount = remember(categoryTournaments) {
                categoryTournaments.count { it.status.equals("completed", ignoreCase = true) }
            }
            val availableFormats = remember(currentCategory) {
                formatFiltersForCategory(currentCategory)
            }
            val effectiveSelectedFormat = if (selectedFormat in availableFormats) selectedFormat else "All"
            val filteredCategoryMatches = remember(categoryTournaments, selectedStatus, effectiveSelectedFormat, searchQuery) {
                val q = searchQuery.trim()
                categoryTournaments.filter { item ->
                    val statusOk = item.status.equals(selectedStatus, ignoreCase = true)
                    val formatOk = matchesFormatFilter(item.mode, effectiveSelectedFormat)
                    val searchOk = q.isEmpty() ||
                        item.title.contains(q, ignoreCase = true) ||
                        item.mapName.contains(q, ignoreCase = true) ||
                        item.mode.contains(q, ignoreCase = true)
                    statusOk && formatOk && searchOk
                }
            }

            // INSIDE SELECTED MODE -> Pro Matches Browser
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        IconButton(
                            onClick = { activeCategoryId = null },
                            modifier = Modifier.testTag("mode_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to Modes",
                                tint = MetallicSilver,
                            )
                        }
                        Image(
                            painter = painterResource(id = currentCategory.drawableRes),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, ObsidianBorderBright, RoundedCornerShape(8.dp)),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentCategory.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                ),
                            )
                            Text(
                                text = currentCategory.subtitle,
                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                            )
                        }
                    }

                    Surface(
                        color = ObsidianCard,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ObsidianBorderBright),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onGoToWallet),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = "Points",
                                tint = NeonPurpleStart,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Dep $depositPts • Win $winningPts",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }

                // Status Filter Pills with real-time counts
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianCard)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    statusFilters.forEach { (key, label) ->
                        val isSelected = selectedStatus == key
                        val count = when (key) {
                            "live" -> liveCount
                            "completed" -> completedCount
                            else -> upcomingCount
                        }
                        Surface(
                            color = if (isSelected) ObsidianElevated else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected) BorderStroke(1.dp, ObsidianBorderBright) else null,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedStatus = key }
                                .testTag("status_pill_$key"),
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "$label ($count)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = when {
                                            isSelected && key == "live" && liveCount > 0 -> CrimsonAlert
                                            isSelected -> MetallicSilver
                                            else -> MetallicSilverDim
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    ),
                                )
                            }
                        }
                    }
                }

                // Team Format Sub-Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    availableFormats.forEach { format ->
                        val active = effectiveSelectedFormat == format
                        Surface(
                            color = if (active) NeonPurpleGlow else ObsidianCard,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (active) NeonPurpleStart else ObsidianBorder,
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedFormat = format },
                        ) {
                            Text(
                                text = format,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (active) NeonPurpleSoft else MetallicSilverDim,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                if (categoryTournaments.size > 1) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Search lobby name or map (Bermuda, Purgatory...)",
                                style = MaterialTheme.typography.bodySmall.copy(color = MetallicSilverDim),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search Tournaments",
                                tint = NeonPurpleSoft,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ObsidianCard,
                            unfocusedContainerColor = ObsidianInput,
                            focusedBorderColor = NeonPurpleStart,
                            unfocusedBorderColor = ObsidianBorder,
                            focusedTextColor = MetallicSilver,
                            unfocusedTextColor = MetallicSilver,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("play_search_input"),
                    )
                }
            }

            if (filteredCategoryMatches.isEmpty()) {
                val statusTitle = statusFilters.firstOrNull { it.first == selectedStatus }?.second ?: "Upcoming"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(id = currentCategory.drawableRes),
                        contentDescription = currentCategory.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, ObsidianBorderBright, RoundedCornerShape(12.dp)),
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No $statusTitle ${currentCategory.title}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MetallicSilver,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Official ${currentCategory.subtitle} matches will sync here automatically.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("play_matches_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(filteredCategoryMatches, key = { it.id }) { tournament ->
                        val reg = regByTournamentId[tournament.id]
                        TournamentMatchCard(
                            tournament = tournament,
                            registration = reg,
                            onOpenDetail = { activeTournament = tournament },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }

    val selectedDetail = activeTournament?.let { current ->
        tournaments.find { it.id == current.id } ?: current
    }
    if (selectedDetail != null) {
        val regForDetail = regByTournamentId[selectedDetail.id]
        val matchResults by remember(selectedDetail.id) {
            observeMatchResults(selectedDetail.id)
        }.collectAsStateWithLifecycle()
        val matchParticipants by remember(selectedDetail.id) {
            observeMatchParticipants(selectedDetail.id)
        }.collectAsStateWithLifecycle()

        androidx.compose.runtime.LaunchedEffect(selectedDetail.id, regForDetail?.slotNumber) {
            if (regForDetail != null) {
                onEnsureOwnParticipantSynced(selectedDetail.id, regForDetail)
            }
        }

        TournamentDetailDialog(
            tournament = selectedDetail,
            registration = regForDetail,
            profile = profile,
            isBusy = isBusy,
            onDismiss = { activeTournament = null },
            onJoin = { teammateUids ->
                onJoinTournament(selectedDetail, teammateUids) { confirmedSlot ->
                    joinedCelebration = selectedDetail to confirmedSlot
                }
            },
            onGoToWallet = {
                activeTournament = null
                onGoToWallet()
            },
            onCopied = onCopied,
            results = matchResults,
            participants = matchParticipants,
        )
    }

    joinedCelebration?.let { (joinedMatch, slotNum) ->
        val latestMatch = tournaments.find { it.id == joinedMatch.id } ?: joinedMatch
        MatchJoinedCongratulationsDialog(
            tournament = latestMatch,
            slotNumber = slotNum,
            profile = profile,
            onViewRoomAndSlot = {
                joinedCelebration = null
            },
            onGoToMyMatches = {
                joinedCelebration = null
                activeTournament = null
                onOpenMyTournaments()
            },
            onDismiss = {
                joinedCelebration = null
            },
        )
    }
}

@Composable
private fun FeaturedTournamentBanner(
    tournament: Tournament,
    isJoined: Boolean,
    onClick: () -> Unit,
) {
    val artwork = remember(tournament.mode) { drawableForModeString(tournament.mode) }
    val isLive = tournament.status.equals("live", ignoreCase = true)
    val remaining = (tournament.maxSlots - tournament.filledSlots).coerceAtLeast(0L)

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLive) CrimsonAlert.copy(alpha = 0.65f) else ObsidianBorderBright,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("featured_tournament_banner"),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = artwork),
                contentDescription = tournament.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.20f),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ObsidianCard.copy(alpha = 0.96f),
                                ObsidianCard.copy(alpha = 0.88f),
                                ObsidianElevated.copy(alpha = 0.78f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TournaXBadge(
                        text = if (isLive) "● LIVE NOW" else "NEXT MATCH",
                        accentColor = if (isLive) CrimsonAlert else NeonPurpleSoft,
                        backgroundColor = if (isLive) CrimsonSurface else NeonPurpleGlow,
                    )
                    Text(
                        text = "${tournament.mode} • ${tournament.mapName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MetallicSilverDim,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = tournament.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MetallicSilver,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                    Text(
                        text = tournament.dateTimeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "PRIZE",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                    fontSize = 9.sp,
                                ),
                            )
                            Text(
                                text = "${tournament.prizePool} Pts",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = AmberNotice,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "ENTRY",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                    fontSize = 9.sp,
                                ),
                            )
                            Text(
                                text = if (tournament.entryFee == 0L) "FREE" else "${tournament.entryFee} Pts",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (tournament.entryFee == 0L) EmeraldVerified else MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "SLOTS",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                    fontSize = 9.sp,
                                ),
                            )
                            Text(
                                text = "$remaining/${tournament.maxSlots}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }

                    Surface(
                        color = if (isJoined) EmeraldVerified else NeonPurpleStart,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = if (isJoined) "Joined ✓" else "Enter →",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSquareCard(
    category: ArenaModeCategory,
    matches: List<Tournament>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val liveCount = remember(matches) {
        matches.count { it.status.equals("live", ignoreCase = true) }
    }
    val upcomingCount = remember(matches) {
        matches.count { it.status.equals("upcoming", ignoreCase = true) }
    }

    val pulseAlpha = if (liveCount > 0) {
        val transition = rememberInfiniteTransition(label = "live_dot_${category.id}")
        val animated by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 750),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "live_alpha_${category.id}",
        )
        animated
    } else {
        1f
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardAspect = if (maxWidth > 260.dp) 1.32f else 1f
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (liveCount > 0) CrimsonAlert.copy(alpha = 0.7f) else ObsidianBorderBright,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(cardAspect)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick)
                    .testTag("mode_card_${category.id}"),
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = category.drawableRes),
                    contentDescription = "${category.title} ${category.subtitle}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0x440D0F13),
                                    Color(0xEB0D0F13),
                                ),
                            ),
                        ),
                )

                val badgeBg = when {
                    liveCount > 0 -> Color(0xE01F1215)
                    else -> ObsidianCard.copy(alpha = 0.90f)
                }
                val badgeBorder = when {
                    liveCount > 0 -> CrimsonAlert.copy(alpha = 0.8f)
                    else -> ObsidianBorderBright
                }
                val badgeText = when {
                    liveCount > 0 -> "$liveCount LIVE"
                    upcomingCount > 0 -> "$upcomingCount OPEN"
                    matches.isNotEmpty() -> "${matches.size} DONE"
                    else -> "0 OPEN"
                }
                val badgeTextColor = when {
                    liveCount > 0 -> Color.White
                    upcomingCount > 0 -> MetallicSilver
                    else -> MetallicSilverDim
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, badgeBorder),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .testTag("mode_live_badge_${category.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (liveCount > 0 || upcomingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .alpha(pulseAlpha)
                                    .clip(CircleShape)
                                    .background(if (liveCount > 0) CrimsonAlert else EmeraldVerified),
                            )
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = badgeTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(11.dp),
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        ),
                    )
                    Text(
                        text = category.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MetallicSilverDim,
                            fontSize = 10.sp,
                        ),
                    )
                }
            }
            }
        }

        Text(
            text = when {
                liveCount > 0 && upcomingCount > 0 -> "$liveCount Live • $upcomingCount Upcoming"
                liveCount > 0 -> "$liveCount Match Live"
                upcomingCount > 0 -> "$upcomingCount Upcoming"
                else -> "Open Lobby →"
            },
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (liveCount > 0) CrimsonAlert else MetallicSilverDim,
            ),
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun AllMatchesHubCard(
    tournaments: List<Tournament>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeCount = remember(tournaments) {
        tournaments.count {
            it.status.equals("live", ignoreCase = true) ||
                it.status.equals("upcoming", ignoreCase = true)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardHeight = if (maxWidth > 520.dp) 144.dp else 118.dp
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ObsidianBorderBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick)
                    .testTag("mode_card_all"),
            ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.banner_championship),
                    contentDescription = "All Matches Hub",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.30f),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x660D0F13),
                                    Color(0xD9151820),
                                    Color(0xF20D0F13),
                                ),
                            ),
                        ),
                )

                Surface(
                    color = ObsidianCard.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ObsidianBorderBright),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                ) {
                    Text(
                        text = "$activeCount ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (activeCount > 0) EmeraldVerified else MetallicSilverDim,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                ) {
                    Text(
                        text = "ALL LOBBIES",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                        ),
                    )
                    Text(
                        text = "BR • CS • Lone Wolf • Free Matches",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MetallicSilverDim,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
            }
        }

        Text(
            text = "All Modes →",
            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun LiveHeaderBadge(
    liveCount: Int,
    upcomingCount: Int,
) {
    when {
        liveCount > 0 -> TournaXBadge(
            text = "$liveCount LIVE NOW",
            accentColor = CrimsonAlert,
            backgroundColor = CrimsonSurface,
        )
        upcomingCount > 0 -> TournaXBadge(
            text = "$upcomingCount OPEN",
            accentColor = NeonPurpleSoft,
            backgroundColor = NeonPurpleGlow,
        )
        else -> TournaXBadge(
            text = "0 LIVE",
            accentColor = MetallicSilverDim,
            backgroundColor = ObsidianInput,
        )
    }
}

@Composable
private fun TournamentMatchCard(
    tournament: Tournament,
    registration: TournamentRegistration?,
    onOpenDetail: () -> Unit,
) {
    val fillRatio = if (tournament.maxSlots > 0L) {
        (tournament.filledSlots.toFloat() / tournament.maxSlots.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val remaining = (tournament.maxSlots - tournament.filledSlots).coerceAtLeast(0L)
    val isFull = tournament.filledSlots >= tournament.maxSlots
    val isJoined = registration != null
    val isLive = tournament.status.equals("live", ignoreCase = true)
    val isCompleted = tournament.status.equals("completed", ignoreCase = true)
    val isCancelled = tournament.status.equals("cancelled", ignoreCase = true)
    val modeArtwork = remember(tournament.mode) { drawableForModeString(tournament.mode) }

    TournaXCard(
        borderColor = when {
            isCancelled -> CrimsonAlert.copy(alpha = 0.55f)
            isJoined -> EmeraldVerified.copy(alpha = 0.65f)
            isLive -> CrimsonAlert.copy(alpha = 0.65f)
            else -> ObsidianBorderBright
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenDetail)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Minimal Pro Header: Mode Thumbnail + Format/Map + Single Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Image(
                        painter = painterResource(id = modeArtwork),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, ObsidianBorderBright, RoundedCornerShape(6.dp)),
                    )
                    Text(
                        text = "${tournament.mode.uppercase()} • ${tournament.mapName.uppercase()}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MetallicSilverDim,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    isCancelled -> TournaXBadge(
                        text = "CANCELLED",
                        accentColor = CrimsonAlert,
                        backgroundColor = CrimsonSurface,
                    )
                    isJoined -> TournaXBadge(
                        text = "SLOT #${registration!!.slotNumber}",
                        accentColor = EmeraldVerified,
                        backgroundColor = EmeraldSurface,
                    )
                    isLive -> TournaXBadge(
                        text = "● LIVE",
                        accentColor = CrimsonAlert,
                        backgroundColor = CrimsonSurface,
                    )
                    isCompleted -> TournaXBadge(
                        text = "COMPLETED",
                        accentColor = MetallicSilverDim,
                        backgroundColor = ObsidianInput,
                    )
                    else -> TournaXBadge(
                        text = if (fillRatio >= 0.75f && !isFull) "FILLING FAST" else "OPEN",
                        accentColor = if (fillRatio >= 0.75f && !isFull) AmberNotice else NeonPurpleSoft,
                        backgroundColor = if (fillRatio >= 0.75f && !isFull) AmberSurface else NeonPurpleGlow,
                    )
                }
            }

            // 2. Match Title & Schedule
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = tournament.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MetallicSilverDim,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tournament.dateTimeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                        )
                    }
                    if (isJoined && (tournament.roomUnlocked || isLive)) {
                        Text(
                            text = "ROOM LIVE ✓",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EmeraldVerified,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                        )
                    }
                }
            }

            // 3. Clean 3-Column Tournament Ledger + Podium Breakdown Preview
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianInput)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PRIZE POOL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MetallicSilverDim,
                                fontSize = 9.sp,
                            ),
                        )
                        Text(
                            text = "${tournament.prizePool} Pts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AmberNotice,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(ObsidianBorder),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "PER KILL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MetallicSilverDim,
                                fontSize = 9.sp,
                            ),
                        )
                        Text(
                            text = if (tournament.perKill > 0L) "${tournament.perKill} Pts" else "Booyah",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(26.dp)
                            .background(ObsidianBorder),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "ENTRY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MetallicSilverDim,
                                fontSize = 9.sp,
                            ),
                        )
                        Text(
                            text = if (tournament.entryFee == 0L) "FREE" else "${tournament.entryFee} Pts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (tournament.entryFee == 0L) EmeraldVerified else MetallicSilver,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            ),
                        )
                    }
                }

                if (tournament.prizeFirst > 0L) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianElevated)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "1st: ${tournament.prizeFirst} Pts",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberNotice,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                        )
                        if (tournament.prizeSecond > 0L) {
                            Text(
                                text = "• 2nd: ${tournament.prizeSecond} Pts",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilver,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                        if (tournament.prizeThird > 0L) {
                            Text(
                                text = "• 3rd: ${tournament.prizeThird} Pts",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                    fontSize = 10.sp,
                                ),
                            )
                        }
                    }
                }
            }

            // 4. Sleek Slots Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${tournament.filledSlots}/${tournament.maxSlots} ${tournament.slotUnitLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                    )
                    Text(
                        text = when {
                            isFull -> "FULL"
                            remaining <= 5L -> "ONLY $remaining LEFT"
                            else -> "$remaining left"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (remaining <= 5L) CrimsonAlert else MetallicSilver,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                LinearProgressIndicator(
                    progress = { fillRatio },
                    color = if (remaining <= 5L) CrimsonAlert else NeonPurpleStart,
                    trackColor = ObsidianInput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }

            // 5. Minimal Action Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = if (isJoined) Icons.Rounded.CheckCircle else Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = if (isJoined) EmeraldVerified else MetallicSilverDim,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = if (isJoined) "Slot Confirmed" else "Prizes & Rules",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isJoined) EmeraldVerified else MetallicSilverDim,
                        ),
                    )
                }

                val buttonLabel = when {
                    isCancelled -> "Cancelled"
                    isCompleted -> "Points Table"
                    isJoined -> "View Room →"
                    isFull -> "Lobby Full"
                    else -> "Join Match"
                }
                val ctaBg = when {
                    isCancelled || isFull || isCompleted -> ObsidianElevated
                    isJoined -> EmeraldVerified
                    else -> NeonPurpleStart
                }
                val ctaTextColor = if (isCancelled || isFull || isCompleted) MetallicSilverDim else Color.White

                Surface(
                    color = ctaBg,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isCancelled || isFull || isCompleted) BorderStroke(1.dp, ObsidianBorderBright) else null,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenDetail)
                        .testTag("tournament_cta_${tournament.id}"),
                ) {
                    Text(
                        text = buttonLabel,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = ctaTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CopyFieldRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianInput)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MetallicSilver,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy $label",
                tint = NeonPurpleSoft,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
