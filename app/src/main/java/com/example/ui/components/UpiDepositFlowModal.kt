package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DepositRequest
import com.example.data.repository.ArenaRepository
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay

enum class DepositDialogStep {
    AMOUNT,
    QR_AND_UTR,
    LIVE_HOLD_POPUP,
}

fun buildUpiPaymentUri(
    amountInr: Long,
    merchantUpiId: String = ArenaRepository.MERCHANT_UPI_ID,
    minDepositInr: Long = 10L,
    maxDepositInr: Long = 500L,
): String {
    val safeMin = minDepositInr.coerceAtLeast(1L)
    val safeMax = maxDepositInr.coerceAtLeast(safeMin)
    val safeAmount = amountInr.coerceIn(safeMin, safeMax)
    val safeUpi = merchantUpiId.trim().ifBlank { ArenaRepository.MERCHANT_UPI_ID }
    val encodedPn = Uri.encode("TournaX")
    val encodedTn = Uri.encode("TournaX Wallet Deposit")
    return "upi://pay?pa=$safeUpi&pn=$encodedPn&am=$safeAmount.00&cu=INR&tn=$encodedTn"
}

fun generateUpiQrBitmap(upiUri: String, sizePx: Int = 560): ImageBitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(upiUri, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height)
    val dark = 0xFF0E0B1A.toInt()
    val white = 0xFFFFFFFF.toInt()
    for (y in 0 until height) {
        val offset = y * width
        for (x in 0 until width) {
            pixels[offset + x] = if (matrix.get(x, y)) dark else white
        }
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}

@Composable
fun UpiDepositFlowModal(
    initialStep: DepositDialogStep,
    initialAmount: Long,
    initialActiveUtr: String,
    depositRequests: List<DepositRequest>,
    isBusy: Boolean,
    merchantUpiId: String = ArenaRepository.MERCHANT_UPI_ID,
    minDepositInr: Long = 10L,
    maxDepositInr: Long = 500L,
    onSubmitDeposit: (
        amountInr: Long,
        utr: String,
        onSubmitted: (normalizedUtr: String) -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
    onCopied: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val safeMin = minDepositInr.coerceAtLeast(1L)
    val safeMax = maxDepositInr.coerceAtLeast(safeMin)

    var step by rememberSaveable(initialStep) { mutableStateOf(initialStep) }
    var amountInput by rememberSaveable(initialAmount) {
        val defaultAmt = if (initialAmount in safeMin..safeMax) initialAmount else 100L.coerceIn(safeMin, safeMax)
        mutableStateOf(defaultAmt.toString())
    }
    var utrInput by rememberSaveable { mutableStateOf("") }
    var submitError by remember { mutableStateOf<String?>(null) }
    var activeUtr by rememberSaveable(initialActiveUtr) { mutableStateOf(initialActiveUtr) }
    var submittedAmount by rememberSaveable(initialAmount) {
        mutableStateOf(initialAmount.coerceIn(safeMin, safeMax))
    }

    val parsedAmount = amountInput.toLongOrNull() ?: 0L
    val isAmountValid = parsedAmount in safeMin..safeMax
    val liveRequest = depositRequests.firstOrNull { it.utr.equals(activeUtr, ignoreCase = true) }

    val isTerminalOutcome = liveRequest?.status.equals("approved", ignoreCase = true) ||
        liveRequest?.status.equals("rejected", ignoreCase = true)
    val allowSystemDismiss = step != DepositDialogStep.LIVE_HOLD_POPUP || isTerminalOutcome || liveRequest == null

    Dialog(
        onDismissRequest = {
            if (allowSystemDismiss && !isBusy) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = allowSystemDismiss && !isBusy,
            dismissOnClickOutside = allowSystemDismiss && !isBusy,
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
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        step == DepositDialogStep.LIVE_HOLD_POPUP && liveRequest?.status.equals("approved", true) -> EmeraldVerified
                        step == DepositDialogStep.LIVE_HOLD_POPUP && liveRequest?.status.equals("rejected", true) -> CrimsonAlert
                        step == DepositDialogStep.LIVE_HOLD_POPUP -> AmberNotice.copy(alpha = 0.7f)
                        else -> ObsidianBorderBright
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
            ) {
            when (step) {
                DepositDialogStep.AMOUNT -> AmountStepContent(
                    amountInput = amountInput,
                    parsedAmount = parsedAmount,
                    isAmountValid = isAmountValid,
                    minDepositInr = safeMin,
                    maxDepositInr = safeMax,
                    merchantUpiId = merchantUpiId,
                    onAmountChanged = { raw ->
                        val maxDigits = maxOf(4, safeMax.toString().length + 1)
                        amountInput = raw.filter { it.isDigit() }.take(maxDigits)
                    },
                    onDismiss = onDismiss,
                    onContinueToPay = {
                        submittedAmount = parsedAmount
                        submitError = null
                        step = DepositDialogStep.QR_AND_UTR
                    },
                )

                DepositDialogStep.QR_AND_UTR -> QrAndUtrStepContent(
                    amountInr = parsedAmount,
                    minDepositInr = safeMin,
                    maxDepositInr = safeMax,
                    merchantUpiId = merchantUpiId,
                    utrInput = utrInput,
                    submitError = submitError,
                    isBusy = isBusy,
                    onUtrChanged = { raw ->
                        utrInput = raw.uppercase().filter { it.isLetterOrDigit() }.take(22)
                        submitError = null
                    },
                    onBackToAmount = { step = DepositDialogStep.AMOUNT },
                    onCopied = onCopied,
                    onSubmitDone = {
                        val cleaned = utrInput.trim().uppercase()
                        if (!cleaned.matches(Regex("^[A-Z0-9]{10,22}$"))) {
                            submitError = "Enter a valid 10–22 character UPI UTR / Ref number."
                        } else {
                            submitError = null
                            onSubmitDeposit(
                                parsedAmount,
                                cleaned,
                                { normalizedUtr ->
                                    activeUtr = normalizedUtr
                                    submittedAmount = parsedAmount
                                    step = DepositDialogStep.LIVE_HOLD_POPUP
                                },
                                { err ->
                                    submitError = err
                                },
                            )
                        }
                    },
                    onDismiss = onDismiss,
                )

                DepositDialogStep.LIVE_HOLD_POPUP -> LiveVerificationPopupContent(
                    utr = activeUtr,
                    amountInr = liveRequest?.amount ?: submittedAmount,
                    merchantUpiId = merchantUpiId,
                    liveRequest = liveRequest,
                    onRetryRejected = {
                        utrInput = activeUtr
                        amountInput = (liveRequest?.amount ?: submittedAmount).coerceIn(safeMin, safeMax).toString()
                        submitError = null
                        step = DepositDialogStep.AMOUNT
                    },
                    onCloseWindow = onDismiss,
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountStepContent(
    amountInput: String,
    parsedAmount: Long,
    isAmountValid: Boolean,
    minDepositInr: Long,
    maxDepositInr: Long,
    merchantUpiId: String,
    onAmountChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onContinueToPay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonPurpleGlow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = NeonPurpleSoft,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ADD MONEY • 1 ₹ = 1 PT",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MetallicSilver,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "Min ₹$minDepositInr • Max ₹$maxDepositInr • Converts to Deposit Pts",
                        style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("add_money_close_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Add Money",
                    tint = MetallicSilverDim,
                )
            }
        }

        OutlinedTextField(
            value = amountInput,
            onValueChange = onAmountChanged,
            label = { Text("Enter Amount (₹$minDepositInr – ₹$maxDepositInr)") },
            prefix = {
                Text(
                    text = "₹ ",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = NeonPurpleSoft,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            },
            singleLine = true,
            isError = amountInput.isNotEmpty() && !isAmountValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPurpleStart,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = MetallicSilver,
                unfocusedTextColor = MetallicSilver,
                focusedLabelColor = NeonPurpleSoft,
                unfocusedLabelColor = MetallicSilverDim,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_money_amount_input"),
        )

        if (amountInput.isNotEmpty() && !isAmountValid) {
            Text(
                text = "Please enter an amount between ₹$minDepositInr and ₹$maxDepositInr.",
                style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert),
            )
        }

        Text(
            text = "QUICK SELECT",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MetallicSilverDim,
                letterSpacing = 1.sp,
            ),
        )

        val presets = remember(minDepositInr, maxDepositInr) {
            (listOf(minDepositInr, 50L, 100L, 200L, 500L, maxDepositInr))
                .filter { it in minDepositInr..maxDepositInr }
                .distinct()
                .sorted()
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { preset ->
                val selected = parsedAmount == preset
                Surface(
                    color = if (selected) NeonPurpleStart else ObsidianInput,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) NeonPurpleStart else ObsidianBorder,
                    ),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(min = 58.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onAmountChanged(preset.toString()) }
                        .testTag("add_money_chip_$preset"),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "₹$preset",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (selected) Color.White else MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        Surface(
            color = ObsidianElevated,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = EmeraldVerified,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAmountValid) {
                        "Pay ₹$parsedAmount → Get $parsedAmount Deposit Points (For Tournament Entries • Non-Withdrawable)"
                    } else {
                        "Pay directly via FamPay/UPI QR ($merchantUpiId). 1 ₹ = 1 Point • Single-use UTR lock."
                    },
                    style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                )
            }
        }

        Button(
            onClick = onContinueToPay,
            enabled = isAmountValid,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPurpleStart,
                contentColor = Color.White,
                disabledContainerColor = ObsidianInput,
                disabledContentColor = MetallicSilverDim,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("add_money_pay_button"),
        ) {
            Text(
                text = if (isAmountValid) "Pay ₹$parsedAmount (Get $parsedAmount Pts) →" else "Enter ₹$minDepositInr – ₹$maxDepositInr to Pay",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun QrAndUtrStepContent(
    amountInr: Long,
    minDepositInr: Long,
    maxDepositInr: Long,
    merchantUpiId: String,
    utrInput: String,
    submitError: String?,
    isBusy: Boolean,
    onUtrChanged: (String) -> Unit,
    onBackToAmount: () -> Unit,
    onCopied: (String) -> Unit,
    onSubmitDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val upiUri = remember(amountInr, merchantUpiId, minDepositInr, maxDepositInr) {
        buildUpiPaymentUri(amountInr, merchantUpiId, minDepositInr, maxDepositInr)
    }
    val qrBitmap = remember(upiUri) { generateUpiQrBitmap(upiUri) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToAmount,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("add_money_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Change Amount",
                        tint = MetallicSilver,
                    )
                }
                Column {
                    Text(
                        text = "SCAN & PAY ₹$amountInr (= $amountInr PTS)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MetallicSilver,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "Official UPI • $merchantUpiId",
                        style = MaterialTheme.typography.labelSmall.copy(color = EmeraldVerified),
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                enabled = !isBusy,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MetallicSilverDim,
                )
            }
        }

        // Crisp QR Card
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val qrSize = minOf(196.dp, maxWidth * 0.58f).coerceAtLeast(140.dp)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, NeonPurpleStart),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Scan UPI QR Code for $merchantUpiId amount ₹$amountInr",
                        modifier = Modifier
                            .size(qrSize)
                            .testTag("upi_qr_image"),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.QrCode2,
                            contentDescription = null,
                            tint = Color(0xFF0E0B1A),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pay Exactly ₹$amountInr • TournaX",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF0E0B1A),
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // Copy UPI ID & Open UPI App row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(merchantUpiId))
                    onCopied("UPI ID copied: $merchantUpiId")
                },
                border = BorderStroke(1.dp, ObsidianBorderBright),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicSilver),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("copy_upi_id_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy UPI ID",
                    tint = NeonPurpleSoft,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Copy UPI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                    val chooser = Intent.createChooser(intent, "Pay ₹$amountInr via UPI")
                    try {
                        context.startActivity(chooser)
                    } catch (_: Exception) {
                        clipboard.setText(AnnotatedString(merchantUpiId))
                        onCopied("UPI app not found on device. UPI ID copied: $merchantUpiId")
                    }
                },
                border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldVerified),
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("open_upi_app_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Open UPI App",
                    tint = EmeraldVerified,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open UPI App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        OutlinedTextField(
            value = utrInput,
            onValueChange = onUtrChanged,
            label = { Text("Enter 12-Digit UPI UTR / Ref No.") },
            placeholder = { Text("e.g. 412345678901") },
            singleLine = true,
            enabled = !isBusy,
            isError = submitError != null,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
            ),
            supportingText = {
                Text(
                    text = "Pay ₹$amountInr first, then enter UTR here. One UTR can never be used twice.",
                    color = MetallicSilverDim,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPurpleStart,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = MetallicSilver,
                unfocusedTextColor = MetallicSilver,
                focusedLabelColor = NeonPurpleSoft,
                unfocusedLabelColor = MetallicSilverDim,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_money_utr_input"),
        )

        if (!submitError.isNullOrBlank()) {
            Surface(
                color = CrimsonSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = submitError,
                    style = MaterialTheme.typography.labelSmall.copy(color = CrimsonAlert),
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        Button(
            onClick = onSubmitDone,
            enabled = !isBusy && utrInput.trim().length >= 10,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPurpleStart,
                contentColor = Color.White,
                disabledContainerColor = ObsidianInput,
                disabledContentColor = MetallicSilverDim,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("add_money_done_button"),
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Locking UTR...", fontWeight = FontWeight.Bold)
            } else {
                Text(
                    text = "Done • Verify Payment",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun LiveVerificationPopupContent(
    utr: String,
    amountInr: Long,
    merchantUpiId: String,
    liveRequest: DepositRequest?,
    onRetryRejected: () -> Unit,
    onCloseWindow: () -> Unit,
) {
    val status = liveRequest?.status?.lowercase() ?: "pending"
    var elapsedSeconds by remember(utr) { mutableIntStateOf(0) }

    LaunchedEffect(utr, status) {
        if (status == "pending") {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "hold_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (status) {
            "approved" -> {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(EmeraldSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Payment Approved",
                        tint = EmeraldVerified,
                        modifier = Modifier.size(44.dp),
                    )
                }

                Text(
                    text = "+$amountInr DEPOSIT PTS ADDED! ✓",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = EmeraldVerified,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Your ₹$amountInr UPI payment is verified and converted (1₹ = 1Pt) into $amountInr Deposit Points in your TournaX Wallet!",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilver),
                    textAlign = TextAlign.Center,
                )

                Surface(
                    color = EmeraldSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "UTR: $utr",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "Status: Approved & Credited to Wallet",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldVerified),
                        )
                    }
                }

                Button(
                    onClick = onCloseWindow,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldVerified,
                        contentColor = Color(0xFF061A11),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("deposit_approved_done_button"),
                ) {
                    Text("Awesome! Go to Wallet ✓", fontWeight = FontWeight.Bold)
                }
            }

            "rejected" -> {
                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(CrimsonSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Cancel,
                        contentDescription = "Payment Rejected",
                        tint = CrimsonAlert,
                        modifier = Modifier.size(44.dp),
                    )
                }

                Text(
                    text = "VERIFICATION DECLINED",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = CrimsonAlert,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                val reason = liveRequest?.adminNote?.ifBlank {
                    "This UTR could not be matched with a settled payment to $merchantUpiId."
                } ?: "This UTR could not be verified."

                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilver),
                    textAlign = TextAlign.Center,
                )

                Surface(
                    color = ObsidianElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Need help? Contact TournaX Support:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MetallicSilver,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        Text(
                            text = "WhatsApp: +91 7378633612 • Email: yourtournax@gmail.com",
                            style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                        )
                    }
                }

                Button(
                    onClick = onRetryRejected,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurpleStart,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("deposit_rejected_retry_button"),
                ) {
                    Text("Fix Amount & Retry Same UTR", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCloseWindow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CrimsonAlert,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("deposit_rejected_close_button"),
                ) {
                    Text("Close Window", fontWeight = FontWeight.Bold)
                }
            }

            else -> {
                // Pending Live 2-5 Minute Verification Hold Window
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = AmberSurface,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, AmberNotice.copy(alpha = 0.7f)),
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = AmberNotice,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VERIFYING PAYMENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberNotice,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                ),
                            )
                        }
                    }
                    IconButton(
                        onClick = onCloseWindow,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("deposit_pending_close_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Minimize Verification Window",
                            tint = MetallicSilverDim,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = NeonPurpleStart,
                        trackColor = ObsidianBorderBright,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(84.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(NeonPurpleGlow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Trusted Verification",
                            tint = EmeraldVerified,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }

                Text(
                    text = "Verifying ₹$amountInr (→ $amountInr Pts)",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MetallicSilver,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "Your ₹$amountInr payment (=$amountInr Deposit Points) may remain on hold for 2–5 minutes while our admin verifies your UTR against $merchantUpiId. Bilkul bhi tension na lein — TournaX is 100% trusted & your money is completely safe!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MetallicSilver,
                        lineHeight = 20.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

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
                        val mins = elapsedSeconds / 60
                        val secs = elapsedSeconds % 60
                        val timerText = "%02d:%02d".format(mins, secs)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "UTR NUMBER",
                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                            )
                            Text(
                                text = utr,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = NeonPurpleSoft,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "DEPOSIT AMOUNT",
                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                            )
                            Text(
                                text = "₹$amountInr",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = EmeraldVerified,
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
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = AmberNotice,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE CHECK TIMER",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                                )
                            }
                            Text(
                                text = "$timerText • Est. 2–5 min",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AmberNotice,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }

                Surface(
                    color = EmeraldSurface.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "✓ 100% Trusted & Verified Escrow System",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldVerified),
                        )
                        Text(
                            text = "✓ Wallet auto-credits the exact second Admin approves",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldVerified),
                        )
                        Text(
                            text = "✓ Duplicate UTR protection active (Single-use UTR)",
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldVerified),
                        )
                    }
                }

                TextButton(
                    onClick = onCloseWindow,
                    modifier = Modifier.testTag("deposit_minimize_pending_button"),
                ) {
                    Text(
                        text = "Keep Pending & Return to Wallet",
                        style = MaterialTheme.typography.labelMedium.copy(color = MetallicSilverDim),
                    )
                }
            }
        }
    }
}
