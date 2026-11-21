package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LegalTermsModal
import com.example.ui.components.TournaXBadge
import com.example.ui.components.TournaXCard
import com.example.ui.components.TournaXEmblem
import com.example.ui.components.TournaXGradientButton
import com.example.ui.components.TournaXTextField
import com.example.ui.components.TournaXWordmark
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianInput

@Composable
fun OnboardingScreen(
    userEmail: String?,
    defaultFullName: String,
    isSubmitting: Boolean,
    serverError: String?,
    onClearError: () -> Unit,
    onSubmitRegistration: (
        fullName: String,
        phone: String,
        freeFireUid: String,
        freeFireIgn: String,
        termsAccepted: Boolean,
    ) -> Unit,
    onSignOut: () -> Unit,
) {
    var fullName by rememberSaveable { mutableStateOf(defaultFullName) }
    var phone by rememberSaveable { mutableStateOf("") }
    var freeFireUid by rememberSaveable { mutableStateOf("") }
    var freeFireIgn by rememberSaveable { mutableStateOf("") }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var showLegalModal by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var uidError by remember { mutableStateOf<String?>(null) }
    var ignError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        onClearError()
        val trimmedName = fullName.trim()
        val trimmedPhone = phone.trim()
        val trimmedUid = freeFireUid.trim()
        val trimmedIgn = freeFireIgn.trim()

        fullNameError = when {
            trimmedName.length < 3 -> "Full name must be at least 3 characters."
            trimmedName.length > 80 -> "Full name must be under 80 characters."
            else -> null
        }
        phoneError = when {
            !trimmedPhone.matches(Regex("^[0-9]{10}$")) -> "Enter a valid 10-digit Indian mobile number."
            else -> null
        }
        uidError = when {
            !trimmedUid.matches(Regex("^[0-9]{8,12}$")) -> "Free Fire UID must be 8 to 12 numeric digits."
            else -> null
        }
        ignError = when {
            trimmedIgn.length < 2 -> "Free Fire IGN must be at least 2 characters."
            trimmedIgn.length > 30 -> "Free Fire IGN must be under 30 characters."
            else -> null
        }
        termsError = if (!termsAccepted) {
            "You must accept the 18+ Legal Terms & Fair Play Rules."
        } else {
            null
        }

        if (listOf(fullNameError, phoneError, uidError, ignError, termsError).all { it == null }) {
            onSubmitRegistration(trimmedName, trimmedPhone, trimmedUid, trimmedIgn, termsAccepted)
        }
    }

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
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Top Header with Google session chip & switch account button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    TournaXEmblem(size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        TournaXWordmark(fontSize = 20.sp)
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("switch_account_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = "Switch Google Account",
                        tint = MetallicSilverDim,
                    )
                }
            }

        Spacer(modifier = Modifier.height(16.dp))

        TournaXBadge(text = "STEP 2 OF 2 • PLAYER PROFILE & UID LOCK")
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Link Your Free Fire Identity",
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MetallicSilver,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "All details are stored securely in Cloud Firestore. Your Free Fire UID is permanently locked to prevent multi-accounting.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MetallicSilverDim,
            ),
        )

        Spacer(modifier = Modifier.height(18.dp))

        TournaXCard {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                TournaXTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        fullNameError = null
                    },
                    label = "Full Name (As per KYC / ID)",
                    placeholder = "e.g., Aarav Sharma",
                    leadingIcon = Icons.Rounded.Person,
                    errorText = fullNameError,
                    testTag = "signup_full_name_input",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )

                TournaXTextField(
                    value = phone,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(10)
                        phone = digits
                        phoneError = null
                    },
                    label = "Phone Number",
                    placeholder = "10-digit mobile number",
                    prefixText = "+91",
                    leadingIcon = Icons.Rounded.PhoneAndroid,
                    errorText = phoneError,
                    testTag = "signup_phone_input",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                )

                TournaXTextField(
                    value = freeFireUid,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(12)
                        freeFireUid = digits
                        uidError = null
                        onClearError()
                    },
                    label = "Free Fire UID",
                    placeholder = "8 to 12 digit numeric UID",
                    leadingIcon = Icons.Rounded.Fingerprint,
                    helperText = "Locked permanently • 1 UID per TournaX account",
                    errorText = uidError,
                    testTag = "signup_ff_uid_input",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )

                TournaXTextField(
                    value = freeFireIgn,
                    onValueChange = {
                        freeFireIgn = it
                        ignError = null
                    },
                    label = "Free Fire In-Game Name (IGN)",
                    placeholder = "e.g., TX_AaravOP",
                    leadingIcon = Icons.Rounded.SportsEsports,
                    errorText = ignError,
                    testTag = "signup_ff_ign_input",
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                )

                // Mandatory T&C Checkbox
                Column {
                    Surface(
                        color = ObsidianInput,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (termsError != null) CrimsonAlert else ObsidianBorder,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = termsAccepted,
                                onCheckedChange = { checked ->
                                    termsAccepted = checked
                                    if (checked) termsError = null
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = NeonPurpleStart,
                                    uncheckedColor = MetallicSilverDim,
                                    checkmarkColor = Color.White,
                                ),
                                modifier = Modifier.testTag("terms_checkbox"),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = MetallicSilver)) {
                                        append("I confirm I am 18+ and accept the ")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = NeonPurpleSoft,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                    ) {
                                        append("Terms, Conditions & Fair Play Rules")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showLegalModal = true }
                                    .testTag("open_legal_modal_link"),
                            )
                        }
                    }
                    if (termsError != null) {
                        Text(
                            text = termsError.orEmpty(),
                            style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert),
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        )
                    }
                }

                if (serverError != null) {
                    Surface(
                        color = CrimsonSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = serverError,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CrimsonAlert,
                                fontWeight = FontWeight.Medium,
                            ),
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TournaXGradientButton(
                    text = "Save Profile & Enter Arena →",
                    leadingIcon = Icons.Rounded.Badge,
                    isLoading = isSubmitting,
                    onClick = ::validateAndSubmit,
                    testTag = "submit_registration_button",
                )
            }
        }
        }
    }

    if (showLegalModal) {
        LegalTermsModal(
            onDismiss = { showLegalModal = false },
            onAccept = {
                termsAccepted = true
                termsError = null
                showLegalModal = false
            },
        )
    }
}
