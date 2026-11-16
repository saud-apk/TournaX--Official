package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.PeopleAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.PlayerProfile
import com.example.data.model.Tournament
import com.example.data.model.TournamentParticipant
import com.example.data.model.TournamentRegistration
import com.example.data.model.TournamentResultEntry
import com.example.ui.screens.CopyFieldRow
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.ClayAccentShadow
import com.example.ui.theme.ClayDropShadow
import com.example.ui.theme.ClaySurfaceBottom
import com.example.ui.theme.ClaySurfaceTop
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

fun drawableForModeString(mode: String): Int {
    val m = mode.trim().lowercase(Locale.US)
    return when {
        m == "cs" || m.startsWith("cs ") || m.contains("clash") -> R.drawable.mode_cs
        m == "lw" || m.contains("lone") || m.contains("1v1") -> R.drawable.mode_lone_wolf
        else -> R.drawable.mode_br
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TournamentDetailDialog(
    tournament: Tournament,
    registration: TournamentRegistration?,
    profile: PlayerProfile,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onJoin: (List<String>) -> Unit,
    onGoToWallet: () -> Unit,
    onCopied: (String) -> Unit,
    results: List<TournamentResultEntry> = emptyList(),
    participants: List<TournamentParticipant> = emptyList(),
) {
    val clipboard = LocalClipboardManager.current
    val modeArtwork = remember(tournament.mode) { drawableForModeString(tournament.mode) }
    var selectedSection by remember(tournament.id) { mutableIntStateOf(0) }
    var memberSearchQuery by remember(tournament.id) { mutableStateOf("") }

    val teammateCount = remember(tournament.mode) {
        when {
            tournament.mode.contains("Squad", ignoreCase = true) ||
                tournament.mode.contains("4v4", ignoreCase = true) -> 3
            tournament.mode.contains("Duo", ignoreCase = true) ||
                tournament.mode.contains("2v2", ignoreCase = true) -> 1
            else -> 0
        }
    }

    val teammateInputs = remember(tournament.id) {
        mutableStateListOf<String>().apply {
            repeat(teammateCount) { index ->
                add(profile.squadUids.getOrNull(index + 1).orEmpty())
            }
        }
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isLive = tournament.status.equals("live", ignoreCase = true)
    val isCompleted = tournament.status.equals("completed", ignoreCase = true)
    val isRoomOpen = tournament.roomUnlocked || isLive
    val remainingSlots = (tournament.maxSlots - tournament.filledSlots).coerceAtLeast(0L)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val maxDialogHeight = (maxHeight * 0.92f).coerceAtLeast(360.dp)
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.2.dp, ObsidianBorderBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .heightIn(max = maxDialogHeight)
                    .claySurface(cornerRadius = 26.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Clean Tactical Match Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(132.dp),
                    ) {
                        Image(
                            painter = painterResource(id = modeArtwork),
                            contentDescription = tournament.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x990A0C10),
                                            Color(0xE0141821),
                                            ObsidianCard,
                                        ),
                                    ),
                                ),
                        )

                        // Top badges & close button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                            ) {
                                when {
                                    isLive -> TournaXBadge(
                                        text = "● LIVE NOW",
                                        accentColor = CrimsonAlert,
                                        backgroundColor = Color(0xDD1F1215),
                                    )
                                    isCompleted -> TournaXBadge(
                                        text = "COMPLETED",
                                        accentColor = MetallicSilverDim,
                                        backgroundColor = Color(0xDD171B24),
                                    )
                                    else -> TournaXBadge(
                                        text = "UPCOMING",
                                        accentColor = EmeraldVerified,
                                        backgroundColor = Color(0xDD101D1B),
                                    )
                                }
                                TournaXBadge(
                                    text = "${tournament.mode} • ${tournament.mapName}",
                                    accentColor = MetallicSilver,
                                    backgroundColor = Color(0xDD1E2330),
                                )
                            }

                            Surface(
                                color = Color(0xDD171B24),
                                shape = CircleShape,
                                border = BorderStroke(1.2.dp, ObsidianBorderBright),
                                shadowElevation = 4.dp,
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .testTag("close_tournament_dialog"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close Match Modal",
                                        tint = MetallicSilver,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }

                        // Title & schedule bottom-aligned in banner
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = tournament.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                ),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = NeonPurpleSoft,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = tournament.dateTimeLabel.ifBlank { "Schedule TBA" },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // 2-Option Segmented Clay Toggle: Description | Joined Members
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ObsidianInput)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MatchDetailTabPill(
                            title = "Description",
                            icon = Icons.Rounded.Description,
                            selected = selectedSection == 0,
                            onClick = { selectedSection = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("match_tab_description"),
                        )
                        MatchDetailTabPill(
                            title = "Joined (${participants.size})",
                            icon = Icons.Rounded.PeopleAlt,
                            selected = selectedSection == 1,
                            onClick = { selectedSection = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("match_tab_members"),
                        )
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (selectedSection == 1) {
                            JoinedMembersSection(
                                tournament = tournament,
                                participants = participants,
                                currentUserId = profile.userId,
                                searchQuery = memberSearchQuery,
                                onSearchQueryChange = { memberSearchQuery = it },
                                onSwitchToDescriptionToJoin = { selectedSection = 0 },
                            )
                        } else {
                            // OPTION 1: BEAUTIFUL COMPLETE TOURNAMENT DESCRIPTION
                            // Live Fill Progress Card
                            val fillFraction = if (tournament.maxSlots > 0L) {
                                (tournament.filledSlots.toFloat() / tournament.maxSlots.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .claySurface(
                                        cornerRadius = 18.dp,
                                        topColor = ClaySurfaceTop,
                                        bottomColor = ClaySurfaceBottom,
                                        elevation = 6.dp,
                                    )
                                    .padding(14.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "LOBBY FILL STATUS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MetallicSilverDim,
                                                letterSpacing = 0.8.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                        Text(
                                            text = if (remainingSlots == 0L) {
                                                "LOBBY FULL"
                                            } else {
                                                "${tournament.filledSlots}/${tournament.maxSlots} Joined • $remainingSlots Left"
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (remainingSlots <= 3L) CrimsonAlert else EmeraldVerified,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(ObsidianInput)
                                            .border(1.dp, ObsidianBorder, RoundedCornerShape(999.dp)),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fillFraction.coerceAtLeast(0.03f))
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(NeonPurpleStart, EmeraldVerified),
                                                    ),
                                                ),
                                        )
                                    }
                                }
                            }

                            // 4-Metric Pre-Match Telemetry Strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                LobbyStatTile(
                                    label = "PRIZE POOL",
                                    value = "${tournament.prizePool} Pts",
                                    valueColor = AmberNotice,
                                    modifier = Modifier.weight(1f),
                                )
                                LobbyStatTile(
                                    label = "PER KILL",
                                    value = if (tournament.perKill > 0L) "${tournament.perKill} Pts" else "Booyah",
                                    valueColor = EmeraldVerified,
                                    modifier = Modifier.weight(1f),
                                )
                                LobbyStatTile(
                                    label = "ENTRY",
                                    value = if (tournament.entryFee == 0L) "FREE" else "${tournament.entryFee} Pts",
                                    valueColor = if (tournament.entryFee == 0L) EmeraldVerified else NeonPurpleSoft,
                                    modifier = Modifier.weight(1f),
                                )
                                LobbyStatTile(
                                    label = "SLOTS",
                                    value = "${tournament.filledSlots}/${tournament.maxSlots}",
                                    valueColor = if (remainingSlots <= 3L) CrimsonAlert else MetallicSilver,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            // Full Match Specifications Blueprint Grid
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .claySurface(
                                        cornerRadius = 18.dp,
                                        topColor = ClaySurfaceTop,
                                        bottomColor = ClaySurfaceBottom,
                                        elevation = 6.dp,
                                    )
                                    .padding(14.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Map,
                                            contentDescription = null,
                                            tint = NeonPurpleSoft,
                                            modifier = Modifier.size(17.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "TOURNAMENT OVERVIEW",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = MetallicSilver,
                                                letterSpacing = 0.6.sp,
                                            ),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        MatchSpecPill(
                                            label = "GAME MODE",
                                            value = tournament.mode,
                                            modifier = Modifier.weight(1f),
                                        )
                                        MatchSpecPill(
                                            label = "ARENA MAP",
                                            value = tournament.mapName.ifBlank { "Bermuda" },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        MatchSpecPill(
                                            label = "LOBBY FORMAT",
                                            value = "${tournament.maxSlots} ${tournament.slotUnitLabel}",
                                            modifier = Modifier.weight(1f),
                                        )
                                        MatchSpecPill(
                                            label = "ROOM ID DROP",
                                            value = if (isRoomOpen) "Unlocked Now ✓" else "15m Before Match",
                                            valueColor = if (isRoomOpen) EmeraldVerified else AmberNotice,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }

                            // Podium Prize Distribution Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .claySurface(
                                        cornerRadius = 18.dp,
                                        topColor = ClaySurfaceTop,
                                        bottomColor = ClaySurfaceBottom,
                                        elevation = 6.dp,
                                    ),
                            ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = AmberNotice,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PRIZE BREAKDOWN",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = MetallicSilver,
                                                letterSpacing = 0.6.sp,
                                            ),
                                        )
                                    }
                                    Text(
                                        text = "Total ${tournament.prizePool} Pts",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = AmberNotice,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    PodiumPrizeCard(
                                        rankLabel = "1ST PLACE",
                                        prizeText = "${tournament.prizeFirst} Pts",
                                        accent = AmberNotice,
                                        bgColor = AmberSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    PodiumPrizeCard(
                                        rankLabel = "2ND PLACE",
                                        prizeText = "${tournament.prizeSecond} Pts",
                                        accent = MetallicSilver,
                                        bgColor = ObsidianElevated,
                                        modifier = Modifier.weight(1f),
                                    )
                                    PodiumPrizeCard(
                                        rankLabel = "3RD PLACE",
                                        prizeText = "${tournament.prizeThird} Pts",
                                        accent = Color(0xFFFB923C),
                                        bgColor = ObsidianElevated,
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                if (tournament.perKill > 0L) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(EmeraldSurface)
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MilitaryTech,
                                            contentDescription = null,
                                            tint = EmeraldVerified,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Bonus: +${tournament.perKill} Pts per confirmed kill",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EmeraldVerified,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        // Pre-Match Fair Play & Rules Brief
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .claySurface(
                                    cornerRadius = 18.dp,
                                    topColor = ClaySurfaceTop,
                                    bottomColor = ClaySurfaceBottom,
                                    elevation = 6.dp,
                                ),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = NeonPurpleSoft,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "MATCH RULES & FAIR PLAY",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = MetallicSilver,
                                            letterSpacing = 0.6.sp,
                                        ),
                                    )
                                }
                                Text(
                                    text = tournament.rulesSummary.ifBlank {
                                        "Mobile devices only. No emulators, teaming, or third-party scripts allowed."
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                                )
                                RuleBullet("Join custom room 10 mins early matching your exact slot #.")
                                RuleBullet("Upload end-match result screenshot in My Tournaments for kill & rank points.")
                            }
                        }

                        // Room Credentials Vault OR Registration Form
                        if (registration != null) {
                            Surface(
                                color = ObsidianElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.2.dp, EmeraldVerified.copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldVerified,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Slot #${registration.slotNumber} Confirmed",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = EmeraldVerified,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            )
                                        }
                                        TournaXBadge(
                                            text = if (isRoomOpen) "ROOM OPEN" else "LOCKED",
                                            accentColor = if (isRoomOpen) EmeraldVerified else AmberNotice,
                                            backgroundColor = if (isRoomOpen) EmeraldSurface else AmberSurface,
                                        )
                                    }

                                    Text(
                                        text = "Registered Captain: ${registration.captainIgn} (UID: ${registration.captainUid})",
                                        style = MaterialTheme.typography.labelMedium.copy(color = MetallicSilver),
                                    )
                                    if (registration.teammateUids.isNotEmpty()) {
                                        Text(
                                            text = "Teammates: ${registration.teammateUids.joinToString(", ")}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                                        )
                                    }

                                    if (isRoomOpen) {
                                        CopyFieldRow(
                                            label = "Room ID",
                                            value = tournament.roomId.ifBlank { "Updating..." },
                                            onCopy = {
                                                if (tournament.roomId.isNotBlank()) {
                                                    clipboard.setText(AnnotatedString(tournament.roomId))
                                                    onCopied("Room ID copied!")
                                                }
                                            },
                                        )
                                        CopyFieldRow(
                                            label = "Password",
                                            value = tournament.roomPassword.ifBlank { "No Password" },
                                            onCopy = {
                                                if (tournament.roomPassword.isNotBlank()) {
                                                    clipboard.setText(AnnotatedString(tournament.roomPassword))
                                                    onCopied("Room Password copied!")
                                                }
                                            },
                                        )
                                        Text(
                                            text = "Open Free Fire → Custom Room → Enter Room ID & Password and take your confirmed Slot #${registration.slotNumber}.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldVerified),
                                        )
                                    } else {
                                        Surface(
                                            color = ObsidianInput,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, ObsidianBorder),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lock,
                                                    contentDescription = "Room Credentials Locked",
                                                    tint = AmberNotice,
                                                    modifier = Modifier.size(22.dp),
                                                )
                                                Column {
                                                    Text(
                                                        text = "Room ID: ••••••••   •   Pass: ••••••",
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            color = MetallicSilver,
                                                            fontWeight = FontWeight.Bold,
                                                        ),
                                                    )
                                                    Text(
                                                        text = "Room unlocks 15 mins before match. You will receive a notification in TournaX when credentials go live.",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = AmberNotice),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (isCompleted || results.isNotEmpty()) {
                                MatchPointsTableSection(
                                    tournament = tournament,
                                    results = results,
                                    currentUserId = profile.userId,
                                    currentFreeFireUid = profile.freeFireUid,
                                )
                            }
                        } else if (!isCompleted && tournament.filledSlots < tournament.maxSlots) {
                            // Join Registration Box
                            Surface(
                                color = ObsidianInput,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.SportsEsports,
                                            contentDescription = null,
                                            tint = NeonPurpleSoft,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CAPTAIN / PLAYER REGISTRATION (AUTO-FILLED)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = NeonPurpleSoft,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                    Text(
                                        text = "${profile.freeFireIgn} • UID: ${profile.freeFireUid}",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MetallicSilver,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                }
                            }

                            teammateInputs.forEachIndexed { idx, value ->
                                TournaXTextField(
                                    value = value,
                                    onValueChange = { next ->
                                        teammateInputs[idx] = next.filter(Char::isDigit).take(12)
                                        validationError = null
                                    },
                                    label = "Teammate ${idx + 1} Free Fire UID (Optional)",
                                    placeholder = "8 to 12 digit UID",
                                    leadingIcon = Icons.Rounded.Group,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    testTag = "teammate_uid_input_$idx",
                                )
                            }

                            val depositPts = profile.walletDeposit + profile.points
                            val totalPlayablePts = depositPts + profile.walletWinning
                            val hasEnoughPoints = totalPlayablePts >= tournament.entryFee

                            Surface(
                                color = if (hasEnoughPoints) ObsidianElevated else CrimsonSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (hasEnoughPoints) NeonPurpleStart.copy(alpha = 0.55f) else CrimsonAlert.copy(alpha = 0.7f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "ENTRY FEE",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                                        )
                                        Text(
                                            text = if (tournament.entryFee == 0L) "FREE" else "${tournament.entryFee} Pts",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = if (tournament.entryFee == 0L) EmeraldVerified else NeonPurpleSoft,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = if (hasEnoughPoints) EmeraldVerified else CrimsonAlert,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "YOUR WALLET POINTS",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                                            )
                                        }
                                        Text(
                                            text = "$totalPlayablePts Pts",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                color = if (hasEnoughPoints) EmeraldVerified else CrimsonAlert,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                    Text(
                                        text = "Deposit: $depositPts Pts • Winning: ${profile.walletWinning} Pts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MetallicSilverDim,
                                            fontSize = 10.sp,
                                        ),
                                    )
                                    if (!hasEnoughPoints && tournament.entryFee > 0L) {
                                        Text(
                                            text = "Need ${tournament.entryFee - totalPlayablePts} more Pts! Add points in Wallet.",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = CrimsonAlert,
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        )
                                    }
                                }
                            }

                            if (validationError != null) {
                                Text(
                                    text = validationError!!,
                                    style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert),
                                )
                            }

                            if (!hasEnoughPoints && tournament.entryFee > 0L) {
                                TournaXGradientButton(
                                    text = "Add Points in Wallet →",
                                    leadingIcon = Icons.Rounded.AccountBalanceWallet,
                                    onClick = onGoToWallet,
                                    testTag = "go_to_wallet_for_points_button",
                                )
                            } else {
                                TournaXGradientButton(
                                    text = if (tournament.entryFee > 0L) {
                                        "Pay ${tournament.entryFee} Pts & Join Slot →"
                                    } else {
                                        "Confirm & Join Free Slot →"
                                    },
                                    leadingIcon = Icons.Rounded.VerifiedUser,
                                    isLoading = isBusy,
                                    onClick = {
                                        val badUid = teammateInputs.firstOrNull {
                                            it.isNotBlank() && (it.length !in 8..12)
                                        }
                                        if (badUid != null) {
                                            validationError = "Teammate UIDs must be 8 to 12 digits if entered."
                                        } else {
                                            onJoin(teammateInputs.toList())
                                        }
                                    },
                                    testTag = "confirm_join_tournament_button",
                                )
                            }
                        } else if (isCompleted || results.isNotEmpty()) {
                            MatchPointsTableSection(
                                tournament = tournament,
                                results = results,
                                currentUserId = profile.userId,
                                currentFreeFireUid = profile.freeFireUid,
                            )
                        } else {
                            Surface(
                                color = ObsidianInput,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LockOpen,
                                        contentDescription = null,
                                        tint = MetallicSilverDim,
                                    )
                                    Text(
                                        text = "All ${tournament.maxSlots} slots are currently filled. Watch for the next match!",
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
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

@Composable
private fun MatchDetailTabPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = pillShape,
                            ambientColor = ClayAccentShadow,
                            spotColor = ClayAccentShadow,
                        )
                        .clip(pillShape)
                        .background(
                            brush = Brush.verticalGradient(colors = listOf(NeonPurpleStart, NeonPurpleEnd)),
                            shape = pillShape,
                        )
                } else {
                    Modifier.clip(pillShape)
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else MetallicSilverDim,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (selected) Color.White else MetallicSilverDim,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                ),
            )
        }
    }
}

@Composable
private fun MatchSpecPill(
    label: String,
    value: String,
    valueColor: Color = MetallicSilver,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianInput,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MetallicSilverDim,
                    fontSize = 9.sp,
                    letterSpacing = 0.6.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = valueColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun JoinedMembersSection(
    tournament: Tournament,
    participants: List<TournamentParticipant>,
    currentUserId: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSwitchToDescriptionToJoin: () -> Unit,
) {
    val filteredParticipants = remember(participants, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            participants
        } else {
            participants.filter { p ->
                p.captainIgn.contains(q, ignoreCase = true) ||
                    p.captainUid.contains(q, ignoreCase = true) ||
                    p.squadTag.contains(q, ignoreCase = true) ||
                    p.slotNumber.toString() == q
            }
        }
    }

    // Header Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .claySurface(
                cornerRadius = 18.dp,
                topColor = ClaySurfaceTop,
                bottomColor = ClaySurfaceBottom,
                elevation = 6.dp,
            )
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PeopleAlt,
                        contentDescription = null,
                        tint = NeonPurpleSoft,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "REAL JOINED MEMBERS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MetallicSilver,
                            letterSpacing = 0.6.sp,
                        ),
                    )
                }
                TournaXBadge(
                    text = "${participants.size} / ${tournament.maxSlots} LIVE",
                    accentColor = EmeraldVerified,
                    backgroundColor = EmeraldSurface,
                )
            }
            Text(
                text = "100% live verified player roster from Firestore. Every slot below is a real Free Fire player joined in this match.",
                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
            )
        }
    }

    if (participants.size > 3) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = "Search by IGN, FF UID, Squad Tag or Slot #...",
                    style = MaterialTheme.typography.bodySmall.copy(color = MetallicSilverDim),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search Joined Members",
                    tint = NeonPurpleSoft,
                    modifier = Modifier.size(18.dp),
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ClaySurfaceTop,
                unfocusedContainerColor = ObsidianInput,
                focusedBorderColor = NeonPurpleStart,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = MetallicSilver,
                unfocusedTextColor = MetallicSilver,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("joined_members_search_input"),
        )
    }

    if (filteredParticipants.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .claySurface(
                    cornerRadius = 18.dp,
                    topColor = ClaySurfaceTop,
                    bottomColor = ClaySurfaceBottom,
                    elevation = 6.dp,
                )
                .padding(22.dp)
                .testTag("joined_members_empty_state"),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(NeonPurpleGlow)
                        .border(1.dp, NeonPurpleStart.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Group,
                        contentDescription = null,
                        tint = NeonPurpleSoft,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    text = if (participants.isEmpty()) {
                        "No Members Joined Yet"
                    } else {
                        "No Matching Player Found"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = if (participants.isEmpty()) {
                        "This lobby is freshly listed. Real players will appear here automatically as soon as they claim a slot!"
                    } else {
                        "Try searching with a different IGN, Free Fire UID, or Slot number."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = MetallicSilverDim),
                    textAlign = TextAlign.Center,
                )
                if (participants.isEmpty() && !tournament.status.equals("completed", ignoreCase = true) && tournament.filledSlots < tournament.maxSlots) {
                    Surface(
                        color = NeonPurpleStart,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onSwitchToDescriptionToJoin),
                    ) {
                        Text(
                            text = "View Details & Join Slot #1 →",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredParticipants.forEach { member ->
                val isCurrentUser = member.userId == currentUserId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .claySurface(
                            cornerRadius = 18.dp,
                            topColor = if (isCurrentUser) ObsidianElevated else ClaySurfaceTop,
                            bottomColor = ClaySurfaceBottom,
                            shadowColor = ClayDropShadow,
                            elevation = 6.dp,
                        )
                        .then(
                            if (isCurrentUser) {
                                Modifier.border(
                                    1.2.dp,
                                    EmeraldVerified.copy(alpha = 0.65f),
                                    RoundedCornerShape(18.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(12.dp)
                        .testTag("joined_member_row_${member.id}"),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        // Slot Badge
                        Surface(
                            color = if (isCurrentUser) EmeraldSurface else NeonPurpleGlow,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentUser) EmeraldVerified.copy(alpha = 0.5f) else NeonPurpleStart.copy(alpha = 0.45f),
                            ),
                        ) {
                            Text(
                                text = "#${member.slotNumber}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (isCurrentUser) EmeraldVerified else NeonPurpleSoft,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                ),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            )
                        }

                        ArenaAvatarBadge(
                            avatarIndex = member.avatarIndex,
                            size = 40.dp,
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = member.captainIgn,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                if (member.squadTag.isNotBlank()) {
                                    TournaXBadge(
                                        text = member.squadTag,
                                        accentColor = NeonPurpleSoft,
                                        backgroundColor = NeonPurpleGlow,
                                    )
                                }
                                if (isCurrentUser) {
                                    TournaXBadge(
                                        text = "YOU",
                                        accentColor = EmeraldVerified,
                                        backgroundColor = EmeraldSurface,
                                    )
                                }
                            }
                            Text(
                                text = "FF UID: ${member.captainUid}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                ),
                            )
                            if (member.teammateUids.isNotEmpty()) {
                                Text(
                                    text = "Teammates: ${member.teammateUids.joinToString(" • ")}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonPurpleSoft,
                                        fontSize = 10.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyStatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianInput,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MetallicSilverDim,
                    fontSize = 9.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = valueColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PodiumPrizeCard(
    rankLabel: String,
    prizeText: String,
    accent: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = rankLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                ),
            )
            Text(
                text = prizeText,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                ),
            )
        }
    }
}

@Composable
private fun RuleBullet(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(NeonPurpleSoft),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(color = MetallicSilverDim),
        )
    }
}
