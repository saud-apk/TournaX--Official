package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.HeroBanner
import com.example.data.model.PlayerProfile
import com.example.ui.components.TournaXAvatarImage
import com.example.ui.components.TournaXBadge
import com.example.ui.components.TournaXCard
import com.example.ui.components.TournaXEmblem
import com.example.ui.components.TournaXWordmark
import com.example.ui.components.claySurface
import com.example.ui.theme.ClaySurfaceBottom
import com.example.ui.theme.ClaySurfaceTop
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleEnd
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput
import kotlinx.coroutines.delay

private val defaultHomeHeroBanners = listOf(
    HeroBanner(
        id = "trusted",
        badge = "TRUSTED & SECURE",
        title = "TournaX — Your Trusted Tournament Partner",
        subtitle = "Fair Play • Transparent System • Real Community",
        ctaLabel = "Join Now",
        linkUrl = "https://tournax.esports.in/arena",
    ),
    HeroBanner(
        id = "championship",
        badge = "CHAMPIONSHIP",
        title = "TournaX Championship — Play • Compete • Win",
        subtitle = "Enter competitive Free Fire India lobbies",
        ctaLabel = "Join Now",
        linkUrl = "https://ff.garena.com/en/esports",
    ),
    HeroBanner(
        id = "daily",
        badge = "DAILY LOBBIES",
        title = "TournaX Daily Tournaments",
        subtitle = "Play Everyday • Win Big",
        ctaLabel = "Join Now",
        linkUrl = "https://discord.com/invite/freefire",
    ),
)

private fun bannerDrawableForPage(page: Int): Int = when (page % 3) {
    0 -> R.drawable.banner_trusted
    1 -> R.drawable.banner_championship
    else -> R.drawable.banner_daily
}

fun avatarIconForIndex(index: Long): ImageVector = when (index.toInt()) {
    1 -> Icons.Rounded.SportsEsports
    2 -> Icons.Rounded.Shield
    3 -> Icons.Rounded.Bolt
    else -> Icons.Rounded.Person
}

@Composable
fun HomeTabScreen(
    profile: PlayerProfile,
    banners: List<HeroBanner> = emptyList(),
    unreadNotificationsCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenRulesAndSupport: () -> Unit,
    onOpenRewardsOrWallet: () -> Unit,
    onOpenMyTournaments: () -> Unit,
    onOpenWalletTab: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onGoToPlayTab: () -> Unit,
    onLinkError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val activeBanners = androidx.compose.runtime.remember(banners) {
        banners.ifEmpty { defaultHomeHeroBanners }
    }
    val pagerState = rememberPagerState(pageCount = { activeBanners.size.coerceAtLeast(1) })

    LaunchedEffect(activeBanners.size) {
        if (activeBanners.size <= 1) return@LaunchedEffect
        while (true) {
            delay(3500L)
            if (!pagerState.isScrollInProgress && activeBanners.isNotEmpty()) {
                val next = (pagerState.currentPage + 1) % activeBanners.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    val openExternalLink: (String) -> Unit = { rawUrl ->
        val trimmed = rawUrl.trim()
        if (trimmed.isNotBlank()) {
            val normalized = if (
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
            try {
                uriHandler.openUri(normalized)
            } catch (_: Exception) {
                onLinkError("Unable to open link on this device.")
            }
        }
    }

    val winRatePercent = if (profile.totalTournaments > 0L) {
        ((profile.booyahs * 100L) / profile.totalTournaments).coerceIn(0L, 100L)
    } else {
        0L
    }
    val xpProgress = ((profile.xp % 500L).toFloat() / 500f).coerceIn(0.05f, 1f)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 360.dp
        val isWideWidth = maxWidth >= 600.dp
        val bannerHeight = when {
            isCompactWidth -> 156.dp
            isWideWidth -> 220.dp
            else -> 188.dp
        }
        val sidePadding = if (isCompactWidth) 12.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = sidePadding, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompactWidth) 12.dp else 16.dp),
        ) {
        // 1. Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TournaXEmblem(size = if (isCompactWidth) 30.dp else 34.dp)
                Spacer(modifier = Modifier.width(if (isCompactWidth) 8.dp else 10.dp))
                TournaXWordmark(fontSize = if (isCompactWidth) 18.sp else 21.sp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(
                    onClick = onOpenRulesAndSupport,
                    modifier = Modifier.testTag("home_rules_support_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Gavel,
                        contentDescription = "All Rules & Support",
                        tint = NeonPurpleSoft,
                    )
                }

                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = "Open Notifications",
                            tint = MetallicSilver,
                        )
                    }
                    if (unreadNotificationsCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(CrimsonAlert)
                                .border(1.5.dp, ObsidianCard, CircleShape),
                        )
                    }
                }
            }
        }

        // 2. User Header Row
        TournaXCard(borderColor = ObsidianBorder) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TournaXAvatarImage(
                    avatarIndex = profile.avatarIndex,
                    size = 48.dp,
                    contentDescription = "Player Avatar",
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.freeFireIgn.ifBlank { profile.fullName },
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified Player",
                            tint = NeonPurpleStart,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Lv. ${profile.level} • Verified Player",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MetallicSilverDim,
                        ),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        color = NeonPurpleStart,
                        trackColor = ObsidianInput,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp)),
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    color = ObsidianInput,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ObsidianBorderBright),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenRewardsOrWallet)
                        .testTag("home_points_pill"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = "TournaX Points",
                            tint = NeonPurpleStart,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val totalPlayablePts = profile.walletDeposit + profile.points + profile.walletWinning
                        Text(
                            text = "$totalPlayablePts Pts",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompactWidth) 12.sp else 13.sp,
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add Money & Open Wallet",
                            tint = MetallicSilverDim,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        // 3. Auto-Sliding Hero Banner Carousel (Admin-Managed Image + Clickable Link)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(0.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_hero_carousel"),
            ) { page ->
                val banner = activeBanners[page]
                val hasLink = banner.linkUrl.isNotBlank()
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ObsidianBorderBright),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bannerHeight)
                        .claySurface(
                            cornerRadius = 18.dp,
                            topColor = ClaySurfaceTop,
                            bottomColor = ClaySurfaceBottom,
                            elevation = 8.dp,
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = hasLink) { openExternalLink(banner.linkUrl) }
                        .testTag("hero_banner_$page"),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (banner.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title.ifBlank { "TournaX Banner" },
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = bannerDrawableForPage(page)),
                                error = painterResource(id = bannerDrawableForPage(page)),
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Image(
                                painter = painterResource(id = bannerDrawableForPage(page)),
                                contentDescription = banner.title.ifBlank { "TournaX Banner" },
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (banner.badge.isNotBlank() && banner.imageUrl.isNotBlank()) {
                            Surface(
                                color = ObsidianCard.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ObsidianBorderBright),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = banner.badge,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonPurpleSoft,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                        if (hasLink) {
                            Surface(
                                color = ObsidianCard.copy(alpha = 0.88f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ObsidianBorderBright),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(28.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = "Open Banner Link",
                                        tint = MetallicSilver,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pagination Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(activeBanners.size) { idx ->
                    val isSelected = pagerState.currentPage == idx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(if (isSelected) 18.dp else 6.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isSelected) NeonPurpleStart else ObsidianBorderBright),
                    )
                }
            }
        }

        // 4. Lifetime Stats Strip (3-Column Card)
        TournaXCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LifetimeStatColumn(
                    icon = Icons.Rounded.EmojiEvents,
                    value = "${profile.totalTournaments}",
                    label = "Tournaments",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(ObsidianBorder),
                )
                LifetimeStatColumn(
                    icon = Icons.Rounded.WorkspacePremium,
                    value = "${profile.totalKills}",
                    label = "Total Kills",
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(ObsidianBorder),
                )
                LifetimeStatColumn(
                    icon = Icons.Rounded.Star,
                    value = "$winRatePercent%",
                    label = "Win Rate",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 5. Quick Action Grid (Unified Matte Pro Tiles)
        Text(
            text = "QUICK ACCESS",
            style = MaterialTheme.typography.labelLarge.copy(
                color = MetallicSilverDim,
                letterSpacing = 1.sp,
                fontSize = 11.sp,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionTile(
                icon = Icons.Rounded.EmojiEvents,
                label = "My\nMatches",
                accent = MetallicSilver,
                onClick = onOpenMyTournaments,
                testTag = "quick_my_tournaments",
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                icon = Icons.Rounded.AccountBalanceWallet,
                label = "My\nWallet",
                accent = MetallicSilver,
                onClick = onOpenWalletTab,
                testTag = "quick_my_wallet",
                modifier = Modifier.weight(1f),
            )
            QuickActionTile(
                icon = Icons.Rounded.BarChart,
                label = "Leader\nboard",
                accent = MetallicSilver,
                onClick = onOpenLeaderboard,
                testTag = "quick_leaderboard",
                modifier = Modifier.weight(1f),
            )
        }

        // 6. Clean Pro Tournament Entry CTA
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ObsidianBorderBright),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onGoToPlayTab)
                .testTag("home_enter_play_cta"),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonPurpleGlow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = null,
                            tint = NeonPurpleStart,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Tournament Lobbies",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "Browse BR, CS & 1v1 Matches",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MetallicSilverDim,
                            ),
                        )
                    }
                }
                Surface(
                    color = NeonPurpleStart,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Go to Play Tab",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LifetimeStatColumn(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MetallicSilverDim,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MetallicSilver,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MetallicSilverDim,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label.replace('\n', ' '),
                    tint = accent,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MetallicSilver,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp,
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}
