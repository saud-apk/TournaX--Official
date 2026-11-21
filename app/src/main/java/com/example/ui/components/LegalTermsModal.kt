package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput

private data class LegalClause(
    val number: String,
    val title: String,
    val body: String,
)

private val tournaXClauses = listOf(
    LegalClause(
        number = "01",
        title = "Eligibility & 18+ Mandate",
        body = "You strictly confirm that you are 18 years of age or older and legally competent to enter a binding agreement under the Indian Contract Act, 1872. Underage participation is strictly prohibited and results in immediate account termination and prize forfeiture.",
    ),
    LegalClause(
        number = "02",
        title = "Skill-Based Competition (No Fake Claims)",
        body = "All TournaX matches are competitive skill-based esports tournaments. TournaX makes zero guarantees of income, profits, fixed returns, or winnings. Match outcomes depend strictly on player skill, strategy, and fair-play adherence.",
    ),
    LegalClause(
        number = "03",
        title = "One Person, One Account & UID Lock",
        body = "Only one TournaX account per person, phone number, device, and Free Fire UID is permitted. Multi-accounting, smurfing, or account sharing causes permanent ban and prize forfeiture.",
    ),
    LegalClause(
        number = "04",
        title = "Zero-Tolerance Anti-Cheat Policy",
        body = "Hacks, aimbots, ESP, scripts, modified APKs, GFX/third-party tools, teaming, or joining mobile-only lobbies via PC emulators are strictly banned. Moderator and admin anti-cheat decisions are final.",
    ),
    LegalClause(
        number = "05",
        title = "Custom Room & Match Responsibility",
        body = "Room ID & Password are confidential; leaking them results in immediate disqualification. Joining late, entering wrong slots, or participating with unverified UIDs may result in removal from the lobby without refund.",
    ),
    LegalClause(
        number = "06",
        title = "Result Verification & Indian Taxes",
        body = "Prizes require valid match proof and spectator verification. Fake or edited screenshots lead to a permanent ban. Applicable Indian taxes/TDS under the Income-tax Act and identity/KYC rules apply to payouts.",
    ),
    LegalClause(
        number = "07",
        title = "No Publisher Affiliation & Limitation of Liability",
        body = "TournaX is an independent esports platform and is NOT affiliated with, sponsored, or endorsed by Garena or Free Fire. TournaX is not liable for internet lag, device issues, game server downtime, power failure, or publisher account actions. The platform is provided \"as-is\".",
    ),
    LegalClause(
        number = "08",
        title = "Governing Law & Dispute Resolution",
        body = "These terms are governed by the laws of India. Users agree to raise and resolve disputes through TournaX Grievance Redressal prior to arbitration or legal proceedings under Indian jurisdiction.",
    ),
)

@Composable
fun LegalTermsModal(
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    acceptButtonText: String = "I Understand & Accept",
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val modalMaxHeight = (maxHeight * 0.92f).coerceAtLeast(320.dp)
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .heightIn(max = modalMaxHeight),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianElevated)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
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
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TournaX Legal Terms & Fair Play Policy",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                )
                                Text(
                                    text = "India Esports Compliance • Effective 2026-v1",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MetallicSilverDim,
                                    ),
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_legal_modal_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Legal Terms",
                                tint = MetallicSilverDim,
                            )
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // Scrollable Clauses
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        tournaXClauses.forEach { clause ->
                            Surface(
                                color = ObsidianInput,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, ObsidianBorder),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TournaXBadge(text = "CLAUSE ${clause.number}")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = clause.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MetallicSilver,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = clause.body,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MetallicSilverDim,
                                            lineHeight = 19.sp,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // Sticky bottom accept CTA
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianElevated)
                            .padding(16.dp),
                    ) {
                        TournaXGradientButton(
                            text = acceptButtonText,
                            leadingIcon = Icons.Rounded.VerifiedUser,
                            onClick = onAccept,
                            testTag = "accept_legal_terms_button",
                        )
                    }
                }
            }
        }
    }
}
