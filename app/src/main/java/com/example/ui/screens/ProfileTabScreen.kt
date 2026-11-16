package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlayerProfile
import com.example.ui.components.LegalTermsModal
import com.example.ui.components.TournaXAvatarImage
import com.example.ui.components.TournaXCard
import com.example.ui.components.TournaXGradientButton
import com.example.ui.components.TournaXTextField
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.CrimsonAlert
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
import com.example.ui.theme.SlateSubtle
import java.util.Locale

@Composable
fun ProfileTabScreen(
    profile: PlayerProfile,
    userEmail: String?,
    isUpdating: Boolean,
    onUpdateAvatar: (Long) -> Unit,
    onUpdateSquad: (squadName: String, squadTag: String, uids: List<String>, onDone: () -> Unit) -> Unit,
    onUpdateProfile: (fullName: String, phone: String, ign: String, onDone: () -> Unit) -> Unit,
    onReportSubmitted: (String) -> Unit,
    onCopied: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current

    var showLegalModal by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditSquadDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showAvatarPicker by remember { mutableStateOf(false) }

    val kdRatio = remember(profile.totalKills, profile.totalTournaments) {
        if (profile.totalTournaments > 0L) {
            String.format(Locale.US, "%.2f", profile.totalKills.toFloat() / profile.totalTournaments.toFloat())
        } else {
            "0.00"
        }
    }

    val squadSummary = remember(profile.squadName, profile.squadTag, profile.squadUids) {
        val count = profile.squadUids.count { it.isNotBlank() }
        if (profile.squadName.isBlank() && count == 0) {
            "Add up to 4 teammate UIDs"
        } else {
            val tag = profile.squadTag.ifBlank { "TX" }
            val name = profile.squadName.ifBlank { "Custom Squad" }
            "[$tag] $name • ${maxOf(count, 1)} players"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Minimal Pro Identity Card
        TournaXCard(borderColor = ObsidianBorderBright) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable { showAvatarPicker = true }
                            .testTag("profile_avatar_button"),
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        TournaXAvatarImage(
                            avatarIndex = profile.avatarIndex,
                            size = 56.dp,
                            contentDescription = "Change Player Avatar",
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(ObsidianElevated)
                                .border(1.dp, ObsidianBorderBright, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = NeonPurpleSoft,
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = profile.freeFireIgn,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Verified IGN",
                                tint = NeonPurpleSoft,
                                modifier = Modifier.size(16.dp),
                            )
                            Surface(
                                color = NeonPurpleGlow,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, NeonPurpleStart.copy(alpha = 0.35f)),
                            ) {
                                Text(
                                    text = "LVL ${profile.level}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = NeonPurpleSoft,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        Text(
                            text = "${profile.fullName} • +91 ${profile.phone}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MetallicSilverDim,
                                fontSize = 13.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("profile_edit_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit Profile",
                            tint = MetallicSilverDim,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Single-line 1-tap UID Copy Bar
                Surface(
                    color = ObsidianInput,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(profile.freeFireUid))
                            onCopied("Free Fire UID copied!")
                        }
                        .minimumInteractiveComponentSize()
                        .testTag("profile_copy_uid"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "FF UID",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SlateSubtle,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                ),
                            )
                            Text(
                                text = profile.freeFireUid,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Copy",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NeonPurpleSoft,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy Free Fire UID",
                                tint = NeonPurpleSoft,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        // Compact Single-Row Career Stats Strip
        TournaXCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MinimalStatCell("MATCHES", "${profile.totalTournaments}", Modifier.weight(1f))
                StatDivider()
                MinimalStatCell("KILLS", "${profile.totalKills}", Modifier.weight(1f))
                StatDivider()
                MinimalStatCell("BOOYAHS", "${profile.booyahs}", Modifier.weight(1f))
                StatDivider()
                MinimalStatCell("K/D", kdRatio, Modifier.weight(1f))
            }
        }

        // Grouped Minimal Settings List
        TournaXCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileActionRow(
                    icon = Icons.Rounded.Group,
                    iconTint = NeonPurpleSoft,
                    title = "My Squad",
                    subtitle = squadSummary,
                    testTag = "edit_squad_button",
                    onClick = { showEditSquadDialog = true },
                )
                HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
                ProfileActionRow(
                    icon = Icons.Rounded.Security,
                    iconTint = NeonPurpleSoft,
                    title = "Support & Report",
                    subtitle = "WhatsApp, Email & cheater desk",
                    testTag = "profile_support_button",
                    onClick = { showSupportDialog = true },
                )
                HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
                ProfileActionRow(
                    icon = Icons.Rounded.Gavel,
                    iconTint = MetallicSilverDim,
                    title = "Legal & Fair Play",
                    subtitle = "Rules, privacy & payout policy",
                    testTag = "profile_legal_terms_button",
                    onClick = { showLegalModal = true },
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Clean Low-Noise Logout Button
        OutlinedButton(
            onClick = onLogout,
            border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("profile_logout_button"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = "Logout",
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }

    if (showAvatarPicker) {
        AvatarPickerDialog(
            selectedIndex = profile.avatarIndex,
            onSelect = { idx ->
                onUpdateAvatar(idx)
                showAvatarPicker = false
            },
            onDismiss = { showAvatarPicker = false },
        )
    }

    if (showLegalModal) {
        LegalTermsModal(
            onDismiss = { showLegalModal = false },
            onAccept = { showLegalModal = false },
            acceptButtonText = "Close Policy",
        )
    }

    if (showEditProfileDialog) {
        EditProfileModal(
            profile = profile,
            userEmail = userEmail,
            isUpdating = isUpdating,
            onSelectAvatar = onUpdateAvatar,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, ign ->
                onUpdateProfile(name, phone, ign) {
                    showEditProfileDialog = false
                }
            },
        )
    }

    if (showEditSquadDialog) {
        EditSquadModal(
            profile = profile,
            isBusy = isUpdating,
            onDismiss = { showEditSquadDialog = false },
            onSave = { name, tag, uids ->
                onUpdateSquad(name, tag, uids) {
                    showEditSquadDialog = false
                }
            },
        )
    }

    if (showSupportDialog) {
        SupportReportDialog(
            onDismiss = { showSupportDialog = false },
            onFeedback = onCopied,
            onSubmit = { msg ->
                showSupportDialog = false
                onReportSubmitted(msg)
            },
        )
    }
}

@Composable
private fun MinimalStatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MetallicSilver,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            ),
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MetallicSilverDim,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun StatDivider() {
    VerticalDivider(
        modifier = Modifier.height(26.dp),
        thickness = 1.dp,
        color = ObsidianBorder,
    )
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MetallicSilver,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MetallicSilverDim,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = SlateSubtle,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AvatarPickerDialog(
    selectedIndex: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = ObsidianCard,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ObsidianBorderBright),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Choose Avatar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    (0L..3L).forEach { idx ->
                        val selected = selectedIndex == idx
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) NeonPurpleStart else ObsidianBorder,
                                    shape = CircleShape,
                                )
                                .clickable { onSelect(idx) }
                                .testTag("avatar_pick_$idx"),
                            contentAlignment = Alignment.Center,
                        ) {
                            TournaXAvatarImage(
                                avatarIndex = idx,
                                size = 52.dp,
                                contentDescription = "Avatar Preset $idx",
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = NeonPurpleSoft, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileModal(
    profile: PlayerProfile,
    userEmail: String?,
    isUpdating: Boolean,
    onSelectAvatar: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(profile.fullName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var ign by remember { mutableStateOf(profile.freeFireIgn) }
    var error by remember { mutableStateOf<String?>(null) }

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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorderBright),
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
            ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (0L..3L).forEach { idx ->
                        val selected = profile.avatarIndex == idx
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) NeonPurpleStart else ObsidianBorder,
                                    shape = CircleShape,
                                )
                                .clickable { onSelectAvatar(idx) }
                                .testTag("edit_avatar_pick_$idx"),
                            contentAlignment = Alignment.Center,
                        ) {
                            TournaXAvatarImage(
                                avatarIndex = idx,
                                size = 44.dp,
                                contentDescription = "Avatar $idx",
                            )
                        }
                    }
                }

                TournaXTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = "Full Name",
                    placeholder = "Min 3 chars",
                    leadingIcon = Icons.Rounded.Person,
                )
                TournaXTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit).take(10); error = null },
                    label = "Phone Number",
                    placeholder = "10 digits",
                    prefixText = "+91",
                    leadingIcon = Icons.Rounded.PhoneAndroid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TournaXTextField(
                    value = ign,
                    onValueChange = { ign = it; error = null },
                    label = "Free Fire IGN",
                    placeholder = "In-Game Name",
                    leadingIcon = Icons.Rounded.SportsEsports,
                )
                if (!userEmail.isNullOrBlank()) {
                    Text(
                        text = "Account: $userEmail",
                        style = MaterialTheme.typography.labelSmall.copy(color = SlateSubtle),
                    )
                }
                if (error != null) {
                    Text(error!!, color = CrimsonAlert, style = MaterialTheme.typography.labelSmall)
                }

                TournaXGradientButton(
                    text = "Save Profile",
                    isLoading = isUpdating,
                    onClick = {
                        when {
                            name.trim().length < 3 -> error = "Name must be at least 3 characters."
                            phone.length != 10 -> error = "Phone must be 10 digits."
                            ign.trim().length < 2 -> error = "IGN must be at least 2 characters."
                            else -> onSave(name.trim(), phone, ign.trim())
                        }
                    },
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cancel", color = MetallicSilverDim)
                }
            }
        }
        }
    }
}

@Composable
private fun EditSquadModal(
    profile: PlayerProfile,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, List<String>) -> Unit,
) {
    var squadName by remember { mutableStateOf(profile.squadName) }
    var squadTag by remember { mutableStateOf(profile.squadTag) }
    val uids = remember {
        mutableStateListOf(
            profile.squadUids.getOrNull(0) ?: profile.freeFireUid,
            profile.squadUids.getOrNull(1).orEmpty(),
            profile.squadUids.getOrNull(2).orEmpty(),
            profile.squadUids.getOrNull(3).orEmpty(),
        )
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorderBright),
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
            ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Configure My Squad",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                TournaXTextField(
                    value = squadName,
                    onValueChange = { squadName = it.take(40) },
                    label = "Squad Name",
                    placeholder = "e.g. Obsidian Vipers",
                    leadingIcon = Icons.Rounded.Group,
                )
                TournaXTextField(
                    value = squadTag,
                    onValueChange = { squadTag = it.take(10) },
                    label = "Squad Tag",
                    placeholder = "e.g. TX",
                    leadingIcon = Icons.Rounded.Badge,
                )
                uids.forEachIndexed { idx, value ->
                    TournaXTextField(
                        value = value,
                        onValueChange = { uids[idx] = it.filter(Char::isDigit).take(12) },
                        label = if (idx == 0) "Player 1 UID (IGL)" else "Player ${idx + 1} UID",
                        placeholder = "8 to 12 digit UID",
                        leadingIcon = Icons.Rounded.SportsEsports,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                TournaXGradientButton(
                    text = "Save Squad",
                    isLoading = isBusy,
                    onClick = { onSave(squadName, squadTag, uids.toList()) },
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cancel", color = MetallicSilverDim)
                }
            }
        }
        }
    }
}

@Composable
private fun SupportReportDialog(
    onDismiss: () -> Unit,
    onFeedback: (String) -> Unit,
    onSubmit: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    var suspectUid by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ObsidianBorderBright),
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = AmberNotice)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Report Cheater / Support",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    Text(
                        text = "Direct Desk: ${TournaXSupportContacts.SUPPORT_EMAIL} • WhatsApp ${TournaXSupportContacts.WHATSAPP_DISPLAY} • Instagram @${TournaXSupportContacts.INSTAGRAM_HANDLE}",
                        style = MaterialTheme.typography.labelSmall.copy(color = NeonPurpleSoft),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(TournaXSupportContacts.SUPPORT_EMAIL))
                                onFeedback("Copied ${TournaXSupportContacts.SUPPORT_EMAIL}")
                                try {
                                    uriHandler.openUri(TournaXSupportContacts.SUPPORT_EMAIL_URI)
                                } catch (_: Exception) {
                                }
                            },
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "Email",
                                color = MetallicSilver,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(TournaXSupportContacts.WHATSAPP_DISPLAY))
                                onFeedback("Copied ${TournaXSupportContacts.WHATSAPP_DISPLAY}")
                                try {
                                    uriHandler.openUri(TournaXSupportContacts.WHATSAPP_URI)
                                } catch (_: Exception) {
                                }
                            },
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "WhatsApp",
                                color = EmeraldVerified,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(TournaXSupportContacts.INSTAGRAM_HANDLE))
                                onFeedback("Copied @${TournaXSupportContacts.INSTAGRAM_HANDLE}")
                                try {
                                    uriHandler.openUri(TournaXSupportContacts.INSTAGRAM_URI)
                                } catch (_: Exception) {
                                }
                            },
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "Insta",
                                color = AmberNotice,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                TournaXTextField(
                    value = suspectUid,
                    onValueChange = { suspectUid = it.filter(Char::isDigit).take(12) },
                    label = "Suspect FF UID / Match ID",
                    placeholder = "Optional 8-12 digit UID",
                    leadingIcon = Icons.Rounded.Security,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TournaXTextField(
                    value = notes,
                    onValueChange = { notes = it.take(240) },
                    label = "Details",
                    placeholder = "Describe emulator abuse, teaming, or match issue",
                    leadingIcon = Icons.Rounded.ContentCopy,
                    singleLine = false,
                )
                TournaXGradientButton(
                    text = "Submit Ticket",
                    onClick = {
                        onSubmit("Grievance ticket logged. TournaX moderators review reports within 24 hours.")
                    },
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cancel", color = MetallicSilverDim)
                }
            }
        }
        }
    }
}
