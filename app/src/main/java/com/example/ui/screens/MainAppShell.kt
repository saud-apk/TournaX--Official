package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlayerProfile
import com.example.ui.components.LegalTermsModal
import com.example.ui.theme.ClayDropShadow
import com.example.ui.theme.ClaySurfaceTop
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.viewmodel.ArenaViewModel

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME("Home", Icons.Rounded.Home, Icons.Outlined.Home),
    PLAY("Play", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    WALLET("Wallet", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    PROFILE("Profile", Icons.Rounded.Person, Icons.Outlined.Person),
}

private enum class SubPage {
    MY_TOURNAMENTS,
    LEADERBOARD,
}

@Composable
fun MainAppShell(
    profile: PlayerProfile,
    userEmail: String?,
    arenaViewModel: ArenaViewModel,
    isUpdatingProfile: Boolean,
    profileActionError: String?,
    onClearProfileError: () -> Unit,
    onUpdateProfile: (fullName: String, phone: String, freeFireIgn: String, onDone: () -> Unit) -> Unit,
    onLogout: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }
    var activeSubPage by remember { mutableStateOf<SubPage?>(null) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showRulesAndSupport by remember { mutableStateOf(false) }
    var showLegalFromRules by remember { mutableStateOf(false) }

    val tournaments by arenaViewModel.tournaments.collectAsStateWithLifecycle()
    val registrations by arenaViewModel.registrations.collectAsStateWithLifecycle()
    val notifications by arenaViewModel.notifications.collectAsStateWithLifecycle()
    val banners by arenaViewModel.banners.collectAsStateWithLifecycle()
    val transactions by arenaViewModel.transactions.collectAsStateWithLifecycle()
    val depositRequests by arenaViewModel.depositRequests.collectAsStateWithLifecycle()
    val withdrawalRequests by arenaViewModel.withdrawalRequests.collectAsStateWithLifecycle()
    val leaderboard by arenaViewModel.leaderboard.collectAsStateWithLifecycle()
    val isArenaBusy by arenaViewModel.isBusy.collectAsStateWithLifecycle()
    val actionMessage by arenaViewModel.actionMessage.collectAsStateWithLifecycle()
    val actionError by arenaViewModel.actionError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(
        profile.userId,
        profile.freeFireIgn,
        profile.totalKills,
        profile.booyahs,
        profile.walletWinning,
        profile.avatarIndex,
    ) {
        arenaViewModel.syncRealLeaderboard(profile)
    }

    LaunchedEffect(actionMessage, actionError, profileActionError) {
        val message = actionError ?: profileActionError ?: actionMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            arenaViewModel.clearFeedback()
            onClearProfileError()
        }
    }

    val unreadCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBackground,
            contentWindowInsets = WindowInsets(0.dp),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (activeSubPage == null && !useNavigationRail) {
                    val navShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    Column(
                        modifier = Modifier
                            .shadow(
                                elevation = 14.dp,
                                shape = navShape,
                                ambientColor = ClayDropShadow,
                                spotColor = ClayDropShadow,
                            )
                            .clip(navShape),
                    ) {
                        HorizontalDivider(
                            thickness = 1.5.dp,
                            color = ObsidianBorderBright,
                        )
                        NavigationBar(
                            containerColor = ClaySurfaceTop,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            MainTab.entries.forEach { tab ->
                                val isSelected = selectedTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            ),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = com.example.ui.theme.NeonPurpleStart,
                                        selectedTextColor = com.example.ui.theme.MetallicSilver,
                                        indicatorColor = NeonPurpleGlow,
                                        unselectedIconColor = MetallicSilverDim,
                                        unselectedTextColor = MetallicSilverDim,
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}"),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (activeSubPage == null && useNavigationRail) {
                    NavigationRail(
                        containerColor = ClaySurfaceTop,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        MainTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = com.example.ui.theme.NeonPurpleStart,
                                    selectedTextColor = com.example.ui.theme.MetallicSilver,
                                    indicatorColor = NeonPurpleGlow,
                                    unselectedIconColor = MetallicSilverDim,
                                    unselectedTextColor = MetallicSilverDim,
                                ),
                                modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}"),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    VerticalDivider(
                        thickness = 1.dp,
                        color = ObsidianBorderBright,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 860.dp),
                    ) {
                        when (activeSubPage) {
                SubPage.MY_TOURNAMENTS -> MyTournamentsScreen(
                    tournaments = tournaments,
                    registrations = registrations,
                    isBusy = isArenaBusy,
                    onSubmitProof = { tournamentId, kills, uri, onDone ->
                        arenaViewModel.submitScreenshot(tournamentId, kills, uri, onDone)
                    },
                    onGoToPlayTab = {
                        activeSubPage = null
                        selectedTab = MainTab.PLAY
                    },
                    onCopied = arenaViewModel::showInfo,
                    onBack = { activeSubPage = null },
                    profile = profile,
                    observeMatchResults = arenaViewModel::observeMatchResults,
                )

                SubPage.LEADERBOARD -> LeaderboardScreen(
                    entries = leaderboard,
                    profile = profile,
                    onBack = { activeSubPage = null },
                )

                null -> when (selectedTab) {
                    MainTab.HOME -> HomeTabScreen(
                        profile = profile,
                        banners = banners,
                        unreadNotificationsCount = unreadCount,
                        onOpenNotifications = { showNotificationsSheet = true },
                        onOpenRulesAndSupport = { showRulesAndSupport = true },
                        onOpenRewardsOrWallet = { selectedTab = MainTab.WALLET },
                        onOpenMyTournaments = { activeSubPage = SubPage.MY_TOURNAMENTS },
                        onOpenWalletTab = { selectedTab = MainTab.WALLET },
                        onOpenLeaderboard = { activeSubPage = SubPage.LEADERBOARD },
                        onGoToPlayTab = { selectedTab = MainTab.PLAY },
                        onLinkError = arenaViewModel::showInfo,
                    )

                    MainTab.PLAY -> PlayTabScreen(
                        profile = profile,
                        tournaments = tournaments,
                        registrations = registrations,
                        isBusy = isArenaBusy,
                        onJoinTournament = { tournament, teammateUids, onJoined ->
                            arenaViewModel.joinTournament(
                                tournament = tournament,
                                captainUid = profile.freeFireUid,
                                captainIgn = profile.freeFireIgn,
                                teammateUids = teammateUids,
                                onJoined = { slot -> onJoined(slot) },
                            )
                        },
                        onGoToWallet = { selectedTab = MainTab.WALLET },
                        onCopied = arenaViewModel::showInfo,
                        onOpenMyTournaments = { activeSubPage = SubPage.MY_TOURNAMENTS },
                        observeMatchResults = arenaViewModel::observeMatchResults,
                        observeMatchParticipants = arenaViewModel::observeTournamentParticipants,
                        onEnsureOwnParticipantSynced = { tournamentId, registration ->
                            arenaViewModel.ensureOwnParticipantSynced(tournamentId, registration, profile)
                        },
                    )

                    MainTab.WALLET -> WalletTabScreen(
                        profile = profile,
                        transactions = transactions,
                        depositRequests = depositRequests,
                        withdrawalRequests = withdrawalRequests,
                        isBusy = isArenaBusy,
                        onSubmitDeposit = { amountInr, utr, onSubmitted, onError ->
                            arenaViewModel.submitDepositRequest(
                                amountInr = amountInr,
                                utr = utr,
                                profile = profile,
                                userEmail = userEmail,
                                onSubmitted = onSubmitted,
                                onError = onError,
                            )
                        },
                        onSubmitWithdrawal = { fullName, freeFireUid, upiId, amountInr, selfieBase64, onSubmitted, onError ->
                            arenaViewModel.submitWithdrawalRequest(
                                fullName = fullName,
                                freeFireUid = freeFireUid,
                                upiId = upiId,
                                amountInr = amountInr,
                                selfieBase64 = selfieBase64,
                                profile = profile,
                                userEmail = userEmail,
                                onSubmitted = onSubmitted,
                                onError = onError,
                            )
                        },
                        onCopied = arenaViewModel::showInfo,
                    )

                    MainTab.PROFILE -> ProfileTabScreen(
                        profile = profile,
                        userEmail = userEmail,
                        isUpdating = isUpdatingProfile || isArenaBusy,
                        onUpdateAvatar = arenaViewModel::updateAvatar,
                        onUpdateSquad = { squadName, squadTag, uids, onDone ->
                            arenaViewModel.updateSquad(squadName, squadTag, uids, onDone)
                        },
                        onUpdateProfile = onUpdateProfile,
                        onReportSubmitted = arenaViewModel::showInfo,
                        onCopied = arenaViewModel::showInfo,
                        onLogout = onLogout,
                    )
                }
            }
        }
    }
    }
    }
    }

    if (showNotificationsSheet) {
        NotificationsSheet(
            notifications = notifications,
            onMarkRead = arenaViewModel::markNotificationRead,
            onMarkAllRead = arenaViewModel::markAllNotificationsRead,
            onLinkError = arenaViewModel::showInfo,
            onDismiss = { showNotificationsSheet = false },
        )
    }

    if (showRulesAndSupport) {
        RulesAndSupportModal(
            onDismiss = { showRulesAndSupport = false },
            onOpenFullLegalTerms = { showLegalFromRules = true },
            onFeedback = arenaViewModel::showInfo,
        )
    }

    if (showLegalFromRules) {
        LegalTermsModal(
            onDismiss = { showLegalFromRules = false },
            onAccept = { showLegalFromRules = false },
            acceptButtonText = "Close Policy",
        )
    }
}
