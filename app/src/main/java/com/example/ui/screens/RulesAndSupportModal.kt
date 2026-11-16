package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.TournaXBadge
import com.example.ui.components.TournaXGradientButton
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput

private data class ArenaRuleItem(
    val number: String,
    val title: String,
    val description: String,
)

private val tournaXArenaRules = listOf(
    ArenaRuleItem(
        number = "01",
        title = "18+ & Pure Skill-Based Competition",
        description = "Players must be 18 years or older (Indian Contract Act, 1872). Every Free Fire lobby is 100% skill-based with zero fixed returns or income claims.",
    ),
    ArenaRuleItem(
        number = "02",
        title = "Zero-Tolerance Anti-Cheat Policy",
        description = "Hacks, aimbots, ESP, scripts, modified APKs, GFX tools, teaming, or joining mobile lobbies on PC emulators result in immediate permanent ban and prize forfeiture.",
    ),
    ArenaRuleItem(
        number = "03",
        title = "One Account & Free Fire UID Lock",
        description = "Only 1 TournaX account per player, phone number, device, and 8–12 digit Free Fire UID. Account sharing, smurfing, or unverified UIDs are disqualified.",
    ),
    ArenaRuleItem(
        number = "04",
        title = "Custom Room & Slot Discipline",
        description = "Room ID & Password unlock before match start and must never be shared externally. Join your exact assigned slot on time; late entries are removed without refund.",
    ),
    ArenaRuleItem(
        number = "05",
        title = "Match Proof & Indian Payout Compliance",
        description = "Upload clear, unedited result screenshots showing kills & placement. Edited proofs lead to bans. Indian KYC and applicable TDS laws govern prize payouts.",
    ),
)

object TournaXSupportContacts {
    const val SUPPORT_EMAIL = "yourtournax@gmail.com"
    const val SUPPORT_EMAIL_URI = "mailto:yourtournax@gmail.com?subject=TournaX%20Arena%20Support"
    const val WHATSAPP_DISPLAY = "+91 7378633612"
    const val WHATSAPP_URI = "https://wa.me/917378633612"
    const val INSTAGRAM_HANDLE = "_flx.saud"
    const val INSTAGRAM_URI = "https://www.instagram.com/_flx.saud/"
}

@Composable
fun RulesAndSupportModal(
    onDismiss: () -> Unit,
    onOpenFullLegalTerms: () -> Unit,
    onFeedback: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = All Rules, 1 = Support

    val openContactUri: (String, String) -> Unit = { uri, fallbackLabel ->
        try {
            uriHandler.openUri(uri)
        } catch (_: Exception) {
            onFeedback("Could not open $fallbackLabel app. Copied contact instead.")
        }
    }

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
                border = BorderStroke(1.dp, ObsidianBorderBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .heightIn(max = modalMaxHeight)
                    .testTag("rules_support_modal"),
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
                                imageVector = if (selectedSection == 0) Icons.Rounded.Gavel else Icons.Rounded.SupportAgent,
                                contentDescription = "Rules and Support",
                                tint = NeonPurpleSoft,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rules & Official Support",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                    ),
                                )
                                Text(
                                    text = "Fair Play Guidelines & Direct Admin Helpdesk",
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
                            modifier = Modifier.testTag("close_rules_support_modal"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Rules & Support",
                                tint = MetallicSilverDim,
                            )
                        }
                    }

                    // Segmented Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianElevated)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RulesSupportTabChip(
                            label = "All Rules",
                            icon = Icons.Rounded.Gavel,
                            selected = selectedSection == 0,
                            onClick = { selectedSection = 0 },
                            testTag = "tab_all_rules",
                            modifier = Modifier.weight(1f),
                        )
                        RulesSupportTabChip(
                            label = "Support",
                            icon = Icons.Rounded.SupportAgent,
                            selected = selectedSection == 1,
                            onClick = { selectedSection = 1 },
                            testTag = "tab_support",
                            modifier = Modifier.weight(1f),
                        )
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selectedSection == 0) {
                            tournaXArenaRules.forEach { rule ->
                                Surface(
                                    color = ObsidianInput,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TournaXBadge(text = "RULE ${rule.number}")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = rule.title,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    color = MetallicSilver,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                            )
                                        }
                                        Text(
                                            text = rule.description,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MetallicSilverDim,
                                                lineHeight = 19.sp,
                                            ),
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = ObsidianElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, NeonPurpleStart.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "OFFICIAL TOURNAX HELPDESK",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = NeonPurpleSoft,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                        ),
                                    )
                                    Text(
                                        text = "Reach out directly for Room ID issues, match disputes, anti-cheat reports, or prize support.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MetallicSilverDim,
                                        ),
                                    )
                                }
                            }

                            SupportChannelCard(
                                title = "Email Support",
                                value = TournaXSupportContacts.SUPPORT_EMAIL,
                                subtitle = "Official Grievance & Tournament Desk",
                                icon = Icons.Rounded.Email,
                                accent = NeonPurpleSoft,
                                testTag = "support_email_card",
                                onOpen = {
                                    openContactUri(TournaXSupportContacts.SUPPORT_EMAIL_URI, "Email")
                                },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(TournaXSupportContacts.SUPPORT_EMAIL))
                                    onFeedback("Support email copied!")
                                },
                            )

                            SupportChannelCard(
                                title = "WhatsApp Support",
                                value = TournaXSupportContacts.WHATSAPP_DISPLAY,
                                subtitle = "Fast Room & Match Query Help",
                                icon = Icons.AutoMirrored.Rounded.Chat,
                                accent = EmeraldVerified,
                                testTag = "support_whatsapp_card",
                                onOpen = {
                                    openContactUri(TournaXSupportContacts.WHATSAPP_URI, "WhatsApp")
                                },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(TournaXSupportContacts.WHATSAPP_DISPLAY))
                                    onFeedback("WhatsApp number copied!")
                                },
                            )

                            SupportChannelCard(
                                title = "Instagram",
                                value = "@${TournaXSupportContacts.INSTAGRAM_HANDLE}",
                                subtitle = "Updates, Highlights & DM Support (${TournaXSupportContacts.INSTAGRAM_HANDLE})",
                                icon = Icons.Rounded.CameraAlt,
                                accent = AmberNotice,
                                testTag = "support_instagram_card",
                                onOpen = {
                                    openContactUri(TournaXSupportContacts.INSTAGRAM_URI, "Instagram")
                                },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(TournaXSupportContacts.INSTAGRAM_HANDLE))
                                    onFeedback("Instagram handle copied!")
                                },
                            )
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // Sticky Footer Action
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ObsidianElevated)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (selectedSection == 0) {
                            TournaXGradientButton(
                                text = "View Full Legal Terms (8 Clauses)",
                                leadingIcon = Icons.Rounded.VerifiedUser,
                                onClick = {
                                    onDismiss()
                                    onOpenFullLegalTerms()
                                },
                                testTag = "rules_modal_view_legal_button",
                            )
                        } else {
                            TournaXGradientButton(
                                text = "Done",
                                onClick = onDismiss,
                                testTag = "rules_modal_done_button",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesSupportTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) NeonPurpleGlow else ObsidianInput,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) NeonPurpleStart else ObsidianBorder),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) NeonPurpleSoft else MetallicSilverDim,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (selected) MetallicSilver else MetallicSilverDim,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
fun SupportChannelCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
) {
    Surface(
        color = ObsidianInput,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MetallicSilverDim,
                    ),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    ),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accent,
                        fontSize = 11.sp,
                    ),
                )
            }
            IconButton(
                onClick = onCopy,
                modifier = Modifier.testTag("${testTag}_copy"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy $title",
                    tint = MetallicSilverDim,
                    modifier = Modifier.size(18.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = "Open $title",
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
