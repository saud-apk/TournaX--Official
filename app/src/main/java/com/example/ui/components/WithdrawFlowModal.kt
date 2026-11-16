package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.PlayerProfile
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.ArenaRepository
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.EmeraldSurface
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
import com.example.util.SelfieImageHelper

enum class WithdrawDialogStep {
    FORM,
    PENDING_STATUS,
}

@Composable
fun WithdrawFlowModal(
    profile: PlayerProfile,
    initialStep: WithdrawDialogStep = WithdrawDialogStep.FORM,
    initialRequestId: String = "",
    withdrawalRequests: List<WithdrawalRequest>,
    isBusy: Boolean,
    onSubmitWithdrawal: (
        fullName: String,
        freeFireUid: String,
        upiId: String,
        amountInr: Long,
        selfieBase64: String,
        onSubmitted: (requestId: String) -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    var step by remember(initialStep) { mutableStateOf(initialStep) }
    var fullNameInput by remember(profile.fullName) { mutableStateOf(profile.fullName) }
    var uidInput by remember(profile.freeFireUid) { mutableStateOf(profile.freeFireUid) }
    var upiInput by remember(profile.phone) { mutableStateOf(profile.phone) }
    var amountInput by remember { mutableStateOf("") }
    var selfieBase64 by remember { mutableStateOf("") }
    var showFaceVerificationDialog by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    var activeRequestId by remember(initialRequestId) { mutableStateOf(initialRequestId) }
    var submittedSummary by remember { mutableStateOf<WithdrawalRequest?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            formError = null
            showFaceVerificationDialog = true
        } else {
            formError = "Camera permission is required for in-app live face verification."
        }
    }

    val liveRequest = withdrawalRequests.firstOrNull { it.id == activeRequestId } ?: submittedSummary

    Dialog(
        onDismissRequest = {
            if (!isBusy && !showFaceVerificationDialog) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isBusy && !showFaceVerificationDialog,
            dismissOnClickOutside = !isBusy && !showFaceVerificationDialog,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        step == WithdrawDialogStep.PENDING_STATUS && liveRequest?.status.equals("paid", true) -> EmeraldVerified
                        step == WithdrawDialogStep.PENDING_STATUS && liveRequest?.status.equals("rejected", true) -> CrimsonAlert
                        step == WithdrawDialogStep.PENDING_STATUS -> AmberNotice.copy(alpha = 0.7f)
                        else -> ObsidianBorderBright
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
            ) {
            when (step) {
                WithdrawDialogStep.FORM -> {
                    WithdrawFormContent(
                        availableWinning = profile.walletWinning,
                        fullName = fullNameInput,
                        uid = uidInput,
                        amountInput = amountInput,
                        upiId = upiInput,
                        selfieBase64 = selfieBase64,
                        isProcessingSelfie = false,
                        isBusy = isBusy,
                        errorText = formError,
                        onFullNameChange = {
                            fullNameInput = it.take(80)
                            formError = null
                        },
                        onUidChange = {
                            uidInput = it.filter(Char::isDigit).take(12)
                            formError = null
                        },
                        onAmountChange = {
                            amountInput = it.filter(Char::isDigit).take(6)
                            formError = null
                        },
                        onUpiChange = {
                            upiInput = it.take(80)
                            formError = null
                        },
                        onCaptureSelfieClick = {
                            val hasCameraPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCameraPerm) {
                                formError = null
                                showFaceVerificationDialog = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        onSubmit = {
                            val parsedAmount = amountInput.toLongOrNull() ?: 0L
                            val cleanName = fullNameInput.trim()
                            val cleanUid = uidInput.trim()
                            val cleanUpi = upiInput.trim()
                            when {
                                cleanName.length < 3 -> formError = "Enter your full name (min 3 characters)."
                                !cleanUid.matches(Regex("^[0-9]{8,12}$")) -> formError = "Enter your valid 8–12 digit Free Fire UID."
                                parsedAmount < ArenaRepository.MIN_WITHDRAWAL_POINTS ->
                                    formError = "Minimum withdrawal is ${ArenaRepository.MIN_WITHDRAWAL_POINTS} Points (₹${ArenaRepository.MIN_WITHDRAWAL_POINTS})."
                                parsedAmount > profile.walletWinning ->
                                    formError = "Amount exceeds your Winning Balance (₹${profile.walletWinning})."
                                cleanUpi.length < 5 -> formError = "Enter a valid UPI ID or mobile number for 10 PM payout."
                                selfieBase64.length < 100 -> formError = "Please capture your live selfie before submitting."
                                else -> {
                                    formError = null
                                    onSubmitWithdrawal(
                                        cleanName,
                                        cleanUid,
                                        cleanUpi,
                                        parsedAmount,
                                        selfieBase64,
                                        { reqId ->
                                            activeRequestId = reqId
                                            submittedSummary = WithdrawalRequest(
                                                id = reqId,
                                                userId = profile.userId,
                                                fullName = cleanName,
                                                freeFireUid = cleanUid,
                                                freeFireIgn = profile.freeFireIgn,
                                                userPhone = profile.phone,
                                                upiId = cleanUpi,
                                                amount = parsedAmount,
                                                selfieBase64 = selfieBase64,
                                                status = "pending",
                                            )
                                            step = WithdrawDialogStep.PENDING_STATUS
                                        },
                                        { err ->
                                            formError = err
                                        },
                                    )
                                }
                            }
                        },
                        onDismiss = onDismiss,
                    )
                }

                WithdrawDialogStep.PENDING_STATUS -> {
                    WithdrawPendingStatusContent(
                        request = liveRequest,
                        onDone = onDismiss,
                    )
                }
            }
        }
        }
    }

    if (showFaceVerificationDialog) {
        InAppFaceVerificationDialog(
            onVerified = { verifiedBase64 ->
                selfieBase64 = verifiedBase64
                formError = null
                showFaceVerificationDialog = false
            },
            onDismiss = {
                showFaceVerificationDialog = false
            },
        )
    }
}

@Composable
private fun WithdrawFormContent(
    availableWinning: Long,
    fullName: String,
    uid: String,
    amountInput: String,
    upiId: String,
    selfieBase64: String,
    isProcessingSelfie: Boolean,
    isBusy: Boolean,
    errorText: String?,
    onFullNameChange: (String) -> Unit,
    onUidChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onUpiChange: (String) -> Unit,
    onCaptureSelfieClick: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selfieBitmap = remember(selfieBase64) { SelfieImageHelper.decodeSelfieBase64(selfieBase64) }
    val parsedAmount = amountInput.toLongOrNull() ?: 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(EmeraldSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        tint = EmeraldVerified,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WITHDRAW WINNING BALANCE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MetallicSilver,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Min 50 Pts • Host pays daily at 10:00 PM",
                        style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                enabled = !isBusy,
                modifier = Modifier.testTag("withdraw_close_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Withdrawal Form",
                    tint = MetallicSilverDim,
                )
            }
        }

        Surface(
            color = EmeraldSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "AVAILABLE WINNING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = EmeraldVerified,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "₹$availableWinning ($availableWinning Pts)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        TournaXTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = "Full Name (Default Filled)",
            placeholder = "Your Legal Name",
            leadingIcon = Icons.Rounded.Person,
            testTag = "withdraw_fullname_input",
        )

        TournaXTextField(
            value = uid,
            onValueChange = onUidChange,
            label = "Your Free Fire UID",
            placeholder = "8–12 Digit FF UID",
            leadingIcon = Icons.Rounded.Badge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            testTag = "withdraw_uid_input",
        )

        TournaXTextField(
            value = amountInput,
            onValueChange = onAmountChange,
            label = "Withdrawable Amount (Min 50 Pts)",
            placeholder = "Enter 50 – $availableWinning",
            leadingIcon = Icons.Rounded.CurrencyRupee,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            helperText = if (parsedAmount in 50L..availableWinning) {
                "₹$parsedAmount will be paid to your account at 10:00 PM."
            } else {
                "1 Pt = ₹1 • Enter at least 50 Points"
            },
            testTag = "withdraw_amount_input",
        )

        val quickAmounts = remember(availableWinning) {
            listOf(50L, 100L, availableWinning)
                .filter { it in 50L..availableWinning }
                .distinct()
        }
        if (quickAmounts.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickAmounts.forEach { preset ->
                    val isMax = preset == availableWinning
                    val label = if (isMax && preset !in listOf(50L, 100L)) "MAX (₹$preset)" else "₹$preset"
                    val selected = parsedAmount == preset
                    Surface(
                        color = if (selected) NeonPurpleStart else ObsidianInput,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) NeonPurpleStart else ObsidianBorder,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onAmountChange(preset.toString()) }
                            .testTag("withdraw_chip_$preset"),
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (selected) Color.White else MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
        }

        TournaXTextField(
            value = upiId,
            onValueChange = onUpiChange,
            label = "UPI ID or Mobile Number (For 10 PM Payout)",
            placeholder = "name@upi or 10-digit number",
            leadingIcon = Icons.Rounded.AccountBalance,
            testTag = "withdraw_upi_input",
        )

        // Own Live Selfie Capture Card
        Surface(
            color = ObsidianElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (selfieBitmap != null) EmeraldVerified.copy(alpha = 0.65f) else ObsidianBorderBright,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
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
                        if (selfieBitmap != null) {
                            Image(
                                bitmap = selfieBitmap,
                                contentDescription = "Captured Verification Selfie",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.5.dp, EmeraldVerified, RoundedCornerShape(10.dp)),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianInput)
                                    .border(1.dp, ObsidianBorderBright, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = "Selfie Placeholder",
                                    tint = NeonPurpleSoft,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (selfieBitmap != null) "Live Face Verified ✓" else "In-App Face Verification",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = if (selfieBitmap != null) EmeraldVerified else MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = if (selfieBitmap != null) {
                                    "Face liveness & selfie saved securely."
                                } else {
                                    "Quick Left/Right/Up/Down check inside app."
                                },
                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onCaptureSelfieClick,
                    enabled = !isBusy && !isProcessingSelfie,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selfieBitmap != null) EmeraldVerified.copy(alpha = 0.6f) else NeonPurpleStart,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("withdraw_take_selfie_button"),
                ) {
                    if (isProcessingSelfie) {
                        CircularProgressIndicator(
                            color = NeonPurpleSoft,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing Selfie...", color = MetallicSilver)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Start Live Face Verification",
                            tint = if (selfieBitmap != null) EmeraldVerified else NeonPurpleSoft,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selfieBitmap != null) "Re-verify Live Face" else "Verify Live Face",
                            color = if (selfieBitmap != null) EmeraldVerified else NeonPurpleSoft,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // 10 PM Payout Schedule Info
        Surface(
            color = AmberSurface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, AmberNotice.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = AmberNotice,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Note: Host pays all verified withdrawal requests at 10:00 PM IST.",
                    style = MaterialTheme.typography.labelSmall.copy(color = AmberNotice),
                )
            }
        }

        if (!errorText.isNullOrBlank()) {
            Surface(
                color = CrimsonSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert),
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        TournaXGradientButton(
            text = "Done • Submit Withdrawal →",
            onClick = onSubmit,
            isLoading = isBusy,
            enabled = !isProcessingSelfie,
            testTag = "withdraw_done_button",
        )
    }
}

@Composable
private fun WithdrawPendingStatusContent(
    request: WithdrawalRequest?,
    onDone: () -> Unit,
) {
    val status = request?.status?.lowercase() ?: "pending"
    val amount = request?.amount ?: 0L
    val selfieBitmap = remember(request?.selfieBase64) {
        SelfieImageHelper.decodeSelfieBase64(request?.selfieBase64.orEmpty())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (status) {
            "paid" -> {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(EmeraldSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Withdrawal Paid",
                        tint = EmeraldVerified,
                        modifier = Modifier.size(42.dp),
                    )
                }

                Text(
                    text = "WITHDRAWAL PAID ✓",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = EmeraldVerified,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "₹$amount has been sent to ${request?.upiId.orEmpty()} by the TournaX host.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilver),
                    textAlign = TextAlign.Center,
                )
            }

            "rejected" -> {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CrimsonSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cancel,
                        contentDescription = "Withdrawal Declined",
                        tint = CrimsonAlert,
                        modifier = Modifier.size(42.dp),
                    )
                }

                Text(
                    text = "WITHDRAWAL DECLINED",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = request?.adminNote?.ifBlank {
                        "Your withdrawal request could not be verified. Contact WhatsApp +91 7378633612 for assistance."
                    } ?: "Request declined.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilver),
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                Surface(
                    color = AmberSurface,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, AmberNotice.copy(alpha = 0.7f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = AmberNotice,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WITHDRAWAL PENDING • 10:00 PM PAYOUT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AmberNotice,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                            ),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(EmeraldSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = "Withdrawal Pending Confirmation",
                        tint = EmeraldVerified,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Text(
                    text = "Withdrawal Successful • Pending ✓",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Please wait until 10:00 PM — our host will verify your UID & selfie and pay ₹$amount at 10:00 PM.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MetallicSilver,
                        lineHeight = 20.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Full Firebase Record Summary with Selfie Preview
        if (request != null) {
            Surface(
                color = ObsidianElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ObsidianBorderBright),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selfieBitmap != null) {
                            Image(
                                bitmap = selfieBitmap,
                                contentDescription = "Submitted Selfie",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, EmeraldVerified, RoundedCornerShape(10.dp)),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = request.fullName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MetallicSilver,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = "FF UID: ${request.freeFireUid} • Payout: ${request.upiId}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                            )
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "WITHDRAWAL AMOUNT",
                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                        )
                        Text(
                            text = "₹${request.amount} (${request.amount} Pts)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = EmeraldVerified,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "PAYOUT WINDOW",
                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                        )
                        Text(
                            text = when (status) {
                                "paid" -> "Paid ✓"
                                "rejected" -> "Declined"
                                else -> "Pending • Wait until 10 PM"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = when (status) {
                                    "paid" -> EmeraldVerified
                                    "rejected" -> CrimsonAlert
                                    else -> AmberNotice
                                },
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPurpleStart,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("withdraw_pending_done_button"),
        ) {
            Text("Done • Back to Wallet", fontWeight = FontWeight.Bold)
        }
    }
}
