package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.model.PlayerProfile
import com.example.data.model.Tournament
import com.example.data.model.TournamentRegistration
import com.example.data.model.TournamentResultEntry
import com.example.ui.components.MatchPointsTableDialog
import com.example.ui.components.TournaXBadge
import com.example.ui.components.TournaXCard
import com.example.ui.components.TournaXGradientButton
import com.example.ui.components.TournaXTextField
import com.example.ui.components.drawableForModeString
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.EmeraldVerified
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
fun MyTournamentsScreen(
    tournaments: List<Tournament>,
    registrations: List<TournamentRegistration>,
    isBusy: Boolean,
    onSubmitProof: (tournamentId: String, kills: Long, uri: String, onDone: () -> Unit) -> Unit,
    onGoToPlayTab: () -> Unit,
    onCopied: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    profile: PlayerProfile = PlayerProfile(),
    observeMatchResults: (String) -> StateFlow<List<TournamentResultEntry>> = { MutableStateFlow(emptyList()) },
) {
    BackHandler(onBack = onBack)

    var selectedSegment by remember { mutableStateOf(0) } // 0 = Joined Upcoming, 1 = Past Results
    var uploadTarget by remember { mutableStateOf<TournamentRegistration?>(null) }
    var pointsTableTarget by remember { mutableStateOf<Tournament?>(null) }
    val clipboard = LocalClipboardManager.current

    val tournamentMap = remember(tournaments) { tournaments.associateBy { it.id } }
    val liveUnlockedCount = remember(registrations, tournamentMap) {
        registrations.count { reg ->
            val linked = tournamentMap[reg.tournamentId]
            linked?.roomUnlocked == true || linked?.status.equals("live", ignoreCase = true)
        }
    }
    val proofsSubmittedCount = remember(registrations) {
        registrations.count { it.screenshotSubmitted }
    }
    val displayed = remember(registrations, tournamentMap, selectedSegment) {
        registrations.filter { reg ->
            val linkedStatus = tournamentMap[reg.tournamentId]?.status ?: "upcoming"
            if (selectedSegment == 0) {
                linkedStatus != "completed"
            } else {
                linkedStatus == "completed" || reg.screenshotSubmitted
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 860.dp),
        ) {
            SubPageHeader(
                title = "MY TOURNAMENTS",
                subtitle = "Joined Free Fire slots, room credentials & proof",
                onBack = onBack,
                backTag = "my_tournaments_back",
            )

        // Pro Player Match Telemetry Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MyMatchSummaryTile("JOINED", "${registrations.size}", NeonPurpleSoft, Modifier.weight(1f))
            MyMatchSummaryTile("ROOMS OPEN", "$liveUnlockedCount", EmeraldVerified, Modifier.weight(1f))
            MyMatchSummaryTile("PROOFS SENT", "$proofsSubmittedCount", AmberNotice, Modifier.weight(1f))
        }

        // Segment Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianCard)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf("Joined Upcoming", "Past Results").forEachIndexed { idx, label ->
                val isSelected = selectedSegment == idx
                Surface(
                    color = if (isSelected) ObsidianElevated else ObsidianCard,
                    shape = RoundedCornerShape(10.dp),
                    border = if (isSelected) BorderStroke(1.dp, ObsidianBorderBright) else null,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSegment = idx }
                        .testTag("my_tournaments_tab_$idx"),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (isSelected) NeonPurpleSoft else MetallicSilverDim,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (displayed.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = NeonPurpleSoft,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (selectedSegment == 0) "No upcoming lobbies joined yet" else "No past match results yet",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Head to the Play tab to join an upcoming BR, CS, or Lone Wolf tournament.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                TournaXGradientButton(
                    text = "Browse Matches in Play →",
                    onClick = onGoToPlayTab,
                    testTag = "empty_my_tournaments_play_cta",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(displayed, key = { it.tournamentId }) { reg ->
                    val linkedTournament = tournamentMap[reg.tournamentId]
                    val unlocked = linkedTournament?.roomUnlocked == true ||
                        linkedTournament?.status.equals("live", ignoreCase = true)
                    val modeArt = remember(reg.mode) { drawableForModeString(reg.mode) }
                    TournaXCard(
                        borderColor = if (unlocked) EmeraldVerified.copy(alpha = 0.7f) else ObsidianBorderBright,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Image(
                                        painter = painterResource(id = modeArt),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, NeonPurpleStart.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
                                    )
                                    TournaXBadge(text = "SLOT #${reg.slotNumber}")
                                }
                                TournaXBadge(
                                    text = reg.resultStatus.uppercase(),
                                    accentColor = if (reg.screenshotSubmitted) EmeraldVerified else AmberNotice,
                                    backgroundColor = if (reg.screenshotSubmitted) EmeraldSurface else AmberSurface,
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = reg.tournamentTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                )
                                Text(
                                    text = "${reg.mode} • ${reg.mapName} • ${reg.dateTimeLabel}",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                                )
                            }

                            if (linkedTournament != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianInput)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "Prize: ${linkedTournament.prizePool} Pts",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AmberNotice,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                    Text(
                                        text = if (linkedTournament.perKill > 0L) {
                                            "Per Kill: ${linkedTournament.perKill} Pts"
                                        } else {
                                            "Booyah Prize"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = EmeraldVerified,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }
                            }

                            Text(
                                text = "Captain: ${reg.captainIgn} (UID: ${reg.captainUid})" +
                                    if (reg.teammateUids.isNotEmpty()) " • Squad: ${reg.teammateUids.joinToString(", ")}" else "",
                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                            )

                            if (unlocked) {
                                CopyFieldRow(
                                    label = "Room ID",
                                    value = linkedTournament?.roomId.orEmpty(),
                                    onCopy = {
                                        val rId = linkedTournament?.roomId.orEmpty()
                                        if (rId.isNotBlank()) {
                                            clipboard.setText(AnnotatedString(rId))
                                            onCopied("Room ID copied!")
                                        }
                                    },
                                )
                                CopyFieldRow(
                                    label = "Room Password",
                                    value = linkedTournament?.roomPassword.orEmpty(),
                                    onCopy = {
                                        val rPass = linkedTournament?.roomPassword.orEmpty()
                                        if (rPass.isNotBlank()) {
                                            clipboard.setText(AnnotatedString(rPass))
                                            onCopied("Room Password copied!")
                                        }
                                    },
                                )
                            } else {
                                Surface(
                                    color = ObsidianInput,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Room Locked",
                                            tint = AmberNotice,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = "Room ID & Password unlock 15 minutes before match start.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = AmberNotice),
                                        )
                                    }
                                }
                            }

                            if (reg.screenshotSubmitted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = EmeraldVerified,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Proof uploaded • Reported Kills: ${reg.submittedKills}",
                                        style = MaterialTheme.typography.labelSmall.copy(color = EmeraldVerified),
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { uploadTarget = reg },
                                    border = BorderStroke(1.dp, ObsidianBorderBright),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("upload_proof_btn_${reg.tournamentId}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.UploadFile,
                                        contentDescription = null,
                                        tint = NeonPurpleSoft,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Upload Result Screenshot",
                                        color = MetallicSilver,
                                    )
                                }
                            }

                            if (linkedTournament != null &&
                                (linkedTournament.status.equals("completed", ignoreCase = true) || linkedTournament.resultsPublished)
                            ) {
                                OutlinedButton(
                                    onClick = { pointsTableTarget = linkedTournament },
                                    border = BorderStroke(1.dp, NeonPurpleStart.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("view_points_table_${reg.tournamentId}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Leaderboard,
                                        contentDescription = "Points Table",
                                        tint = NeonPurpleSoft,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "View Match Points Table",
                                        color = NeonPurpleSoft,
                                        fontWeight = FontWeight.Bold,
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

    uploadTarget?.let { reg ->
        UploadResultModal(
            registration = reg,
            isBusy = isBusy,
            onDismiss = { uploadTarget = null },
            onSubmit = { kills, uri ->
                onSubmitProof(reg.tournamentId, kills, uri) {
                    uploadTarget = null
                }
            },
        )
    }

    pointsTableTarget?.let { target ->
        val matchResults by remember(target.id) {
            observeMatchResults(target.id)
        }.collectAsStateWithLifecycle()

        MatchPointsTableDialog(
            tournament = target,
            results = matchResults,
            profile = profile,
            onDismiss = { pointsTableTarget = null },
        )
    }
}

@Composable
private fun UploadResultModal(
    registration: TournamentRegistration,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Long, String) -> Unit,
) {
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var killsInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
            errorText = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, ObsidianBorderBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianElevated)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Upload Match Proof",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Upload Modal",
                                tint = MetallicSilverDim,
                            )
                        }
                    }
                    HorizontalDivider(color = ObsidianBorder)

                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = registration.tournamentTitle,
                            style = MaterialTheme.typography.labelLarge.copy(color = NeonPurpleSoft),
                        )

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("pick_screenshot_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = "Pick Screenshot",
                                tint = NeonPurpleSoft,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedImageUri == null) {
                                    "Select Free Fire Result Screenshot"
                                } else {
                                    "Screenshot Selected ✓ (Tap to Change)"
                                },
                                color = if (selectedImageUri == null) MetallicSilver else EmeraldVerified,
                            )
                        }

                        TournaXTextField(
                            value = killsInput,
                            onValueChange = {
                                killsInput = it.filter(Char::isDigit).take(2)
                                errorText = null
                            },
                            label = "Total Kills in Match",
                            placeholder = "e.g. 6",
                            leadingIcon = Icons.Rounded.EmojiEvents,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            testTag = "upload_kills_input",
                        )

                        if (errorText != null) {
                            Text(
                                text = errorText!!,
                                style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert),
                            )
                        }

                        TournaXGradientButton(
                            text = "Submit Proof →",
                            isLoading = isBusy,
                            onClick = {
                                val parsedKills = killsInput.toLongOrNull()
                                val uri = selectedImageUri
                                when {
                                    uri.isNullOrBlank() -> errorText = "Please select a match screenshot via Photo Picker."
                                    parsedKills == null -> errorText = "Enter valid kill count (0–99)."
                                    else -> onSubmit(parsedKills, uri)
                                }
                            },
                            testTag = "submit_proof_button",
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubPageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ObsidianElevated)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(backTag),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MetallicSilver,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MetallicSilver,
                    fontSize = 18.sp,
                ),
            )
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
            )
        }
    }
    HorizontalDivider(color = ObsidianBorder)
}

@Composable
private fun MyMatchSummaryTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MetallicSilverDim,
                    fontSize = 9.sp,
                ),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = valueColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                ),
            )
        }
    }
}
