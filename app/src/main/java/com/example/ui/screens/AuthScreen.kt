package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.LegalTermsModal
import com.example.ui.components.TournaXBadge
import com.example.ui.components.TournaXCard
import com.example.ui.components.TournaXEmblem
import com.example.ui.components.TournaXGradientButton
import com.example.ui.components.TournaXWordmark
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleEnd
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianInput

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = New Player
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showLegalModal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Spacer(modifier = Modifier.height(10.dp))
        TournaXEmblem(size = 82.dp)
        Spacer(modifier = Modifier.height(14.dp))
        TournaXWordmark(fontSize = 28.sp)
        Spacer(modifier = Modifier.height(6.dp))
        TournaXBadge(text = "SKILL-BASED INDIAN ESPORTS • 18+")

        Spacer(modifier = Modifier.height(22.dp))

        // Segmented Toggle: Returning Player | New Player
        Surface(
            color = ObsidianInput,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SegmentedAuthTab(
                    label = "Login",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    testTag = "auth_tab_login",
                    modifier = Modifier.weight(1f),
                )
                SegmentedAuthTab(
                    label = "New Player",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    testTag = "auth_tab_signup",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        TournaXCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (selectedTab == 0) {
                        "Enter the TournaX Arena"
                    } else {
                        "Create Your Verified Player ID"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MetallicSilver,
                        fontSize = 19.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (selectedTab == 0) {
                        "Sign in securely with your Google account to sync your locked Free Fire UID and arena profile from Cloud Firestore."
                    } else {
                        "First verify your Google identity, then link your +91 mobile number, Free Fire UID, IGN, and accept our 18+ Fair Play Policy."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MetallicSilverDim,
                    ),
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Honest 3-point security architecture overview
                AuthFeatureRow(
                    icon = Icons.Rounded.Security,
                    title = "Firebase Auth & Cloud Firestore",
                    subtitle = "Zero-trust user isolation powered by Google Sign-In",
                )
                Spacer(modifier = Modifier.height(10.dp))
                AuthFeatureRow(
                    icon = Icons.Rounded.Lock,
                    title = "One Person, One Free Fire UID",
                    subtitle = if (selectedTab == 0) {
                        "Your 8–12 digit UID stays permanently locked to your account"
                    } else {
                        "Duplicate Free Fire UIDs are automatically blocked in Firestore"
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
                AuthFeatureRow(
                    icon = Icons.Rounded.Badge,
                    title = "Strict Anti-Cheat & 18+ Compliance",
                    subtitle = "No hacks, emulators in mobile lobbies, or fake earning claims",
                )

                if (authError != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = CrimsonSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = authError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CrimsonAlert,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TournaXGradientButton(
                    text = if (selectedTab == 0) {
                        "Continue with Google →"
                    } else {
                        "Verify Google & Register →"
                    },
                    leadingIcon = Icons.Rounded.AccountCircle,
                    isLoading = isLoading,
                    testTag = "google_sign_in_button",
                    onClick = {
                        authError = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { message ->
                                isLoading = false
                                authError = message
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            },
                        )
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = EmeraldVerified,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Protected by Jetpack Credential Manager",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MetallicSilverDim,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legal Policy shortcut
        Surface(
            color = ObsidianInput,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { showLegalModal = true }
                .testTag("open_legal_from_auth_button"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Gavel,
                        contentDescription = "Legal Terms",
                        tint = NeonPurpleSoft,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TournaX Legal Terms & Fair Play Policy",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MetallicSilver,
                                fontSize = 13.sp,
                            ),
                        )
                        Text(
                            text = "18+ Indian Contract Act, Anti-Cheat & TDS disclosures",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MetallicSilverDim,
                            ),
                        )
                    }
                }
                Text(
                    text = "Read →",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = NeonPurpleSoft,
                        fontSize = 13.sp,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Independent skill-based esports platform. Not affiliated with, sponsored, or endorsed by Garena or Free Fire.",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MetallicSilverDim,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        }
    }

    if (showLegalModal) {
        LegalTermsModal(
            onDismiss = { showLegalModal = false },
            onAccept = { showLegalModal = false },
            acceptButtonText = "Close Policy",
        )
    }
}

@Composable
private fun SegmentedAuthTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(NeonPurpleStart, NeonPurpleEnd))
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                },
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (selected) Color.White else MetallicSilverDim,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            color = ObsidianInput,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = NeonPurpleSoft,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MetallicSilver,
                    fontSize = 13.sp,
                ),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MetallicSilverDim,
                ),
            )
        }
    }
}
