package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DepositRequest
import com.example.data.model.PlayerProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.ArenaRepository
import com.example.ui.components.DepositDialogStep
import com.example.ui.components.TournaXCard
import com.example.ui.components.UpiDepositFlowModal
import com.example.ui.components.WithdrawDialogStep
import com.example.ui.components.WithdrawFlowModal
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleEnd
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated

@Composable
fun WalletTabScreen(
    profile: PlayerProfile,
    transactions: List<WalletTransaction>,
    depositRequests: List<DepositRequest>,
    withdrawalRequests: List<WithdrawalRequest> = emptyList(),
    isBusy: Boolean,
    onSubmitDeposit: (
        amountInr: Long,
        utr: String,
        onSubmitted: (normalizedUtr: String) -> Unit,
        onError: (String) -> Unit,
    ) -> Unit,
    onSubmitWithdrawal: (
        fullName: String,
        freeFireUid: String,
        upiId: String,
        amountInr: Long,
        selfieBase64: String,
        onSubmitted: (requestId: String) -> Unit,
        onError: (String) -> Unit,
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDepositModal by remember { mutableStateOf(false) }
    var depositModalStep by remember { mutableStateOf(DepositDialogStep.AMOUNT) }
    var depositModalAmount by remember { mutableLongStateOf(100L) }
    var depositModalUtr by remember { mutableStateOf("") }
    var showMinWithdrawNotice by remember { mutableStateOf(false) }
    var showWithdrawFlowModal by remember { mutableStateOf(false) }
    var withdrawModalStep by remember { mutableStateOf(WithdrawDialogStep.FORM) }
    var withdrawActiveRequestId by remember { mutableStateOf("") }

    val depositPts = profile.walletDeposit + profile.points
    val winningPts = profile.walletWinning
    val totalPlayablePts = depositPts + winningPts

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "WALLET",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = MetallicSilver,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = "1 ₹ = 1 Pt",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = EmeraldVerified,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }

        // Minimal Split Balance Card: Total + Deposit (Add Money) + Winning (Withdraw)
        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ObsidianBorderBright),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column {
                        Text(
                            text = "TOTAL BALANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MetallicSilverDim,
                                letterSpacing = 1.sp,
                            ),
                        )
                        Text(
                            text = "$totalPlayablePts Pts",
                            style = MaterialTheme.typography.displaySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                            ),
                        )
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // Row 1: Deposit Points (Match Entry Only) + Add Money
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "DEPOSIT (MATCH ONLY)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                    fontSize = 10.sp,
                                ),
                            )
                            Text(
                                text = "$depositPts Pts",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(NeonPurpleStart, NeonPurpleEnd)),
                                )
                                .clickable {
                                    depositModalStep = DepositDialogStep.AMOUNT
                                    depositModalAmount = 100L
                                    depositModalUtr = ""
                                    showDepositModal = true
                                }
                                .testTag("wallet_add_money_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddCircleOutline,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add Money",
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    ),
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = ObsidianBorder)

                    // Row 2: Winning Balance (Withdrawable) + Withdraw
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "WINNING (WITHDRAWABLE)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldVerified,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = "₹$winningPts ($winningPts Pts)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = EmeraldVerified,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Text(
                                text = "Min ${ArenaRepository.MIN_WITHDRAWAL_POINTS} Pts • 10 PM Payout",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                    fontSize = 10.sp,
                                ),
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (profile.walletWinning < ArenaRepository.MIN_WITHDRAWAL_POINTS) {
                                    showMinWithdrawNotice = true
                                } else {
                                    withdrawModalStep = WithdrawDialogStep.FORM
                                    withdrawActiveRequestId = ""
                                    showWithdrawFlowModal = true
                                }
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (profile.walletWinning >= ArenaRepository.MIN_WITHDRAWAL_POINTS) {
                                    EmeraldVerified.copy(alpha = 0.7f)
                                } else {
                                    ObsidianBorderBright
                                },
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (profile.walletWinning >= ArenaRepository.MIN_WITHDRAWAL_POINTS) {
                                    EmeraldVerified
                                } else {
                                    MetallicSilverDim
                                },
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("wallet_withdraw_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalance,
                                contentDescription = null,
                                tint = if (profile.walletWinning >= ArenaRepository.MIN_WITHDRAWAL_POINTS) {
                                    EmeraldVerified
                                } else {
                                    MetallicSilverDim
                                },
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Withdraw",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }

        // Deposit Requests Live Tracker (only when present)
        if (depositRequests.isNotEmpty()) {
            item {
                Text(
                    text = "DEPOSIT REQUESTS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MetallicSilverDim,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp,
                    ),
                )
            }

            items(depositRequests, key = { "dep_${it.id}_${it.utr}" }) { req ->
                val displayUtr = req.utr.ifBlank { req.id }
                val statusLower = req.status.lowercase()
                val statusColor = when (statusLower) {
                    "approved" -> EmeraldVerified
                    "rejected" -> CrimsonAlert
                    else -> AmberNotice
                }
                val statusBg = when (statusLower) {
                    "approved" -> EmeraldSurface
                    "rejected" -> CrimsonSurface
                    else -> AmberSurface
                }
                val statusIcon = when (statusLower) {
                    "approved" -> Icons.Rounded.CheckCircle
                    "rejected" -> Icons.Rounded.Cancel
                    else -> Icons.Rounded.Schedule
                }

                TournaXCard(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            depositModalUtr = displayUtr
                            depositModalAmount = req.amount
                            depositModalStep = DepositDialogStep.LIVE_HOLD_POPUP
                            showDepositModal = true
                        }
                        .testTag("deposit_request_card_$displayUtr"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(statusBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = req.status,
                                    tint = statusColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "UTR: $displayUtr",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                                Text(
                                    text = when (statusLower) {
                                        "approved" -> "Approved • +${req.amount} Pts"
                                        "rejected" -> req.adminNote.ifBlank { "Rejected • Tap for info" }
                                        else -> "Pending (2–5 min hold) • Tap"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(color = statusColor),
                                )
                            }
                        }
                        Text(
                            text = "${req.amount} Pts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }

        // Withdrawal Requests Live Tracker (10 PM Host Payout)
        if (withdrawalRequests.isNotEmpty()) {
            item {
                Text(
                    text = "WITHDRAWAL REQUESTS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MetallicSilverDim,
                        letterSpacing = 1.sp,
                        fontSize = 11.sp,
                    ),
                )
            }

            items(withdrawalRequests, key = { "wd_${it.id}" }) { wd ->
                val statusLower = wd.status.lowercase()
                val statusColor = when (statusLower) {
                    "paid" -> EmeraldVerified
                    "rejected" -> CrimsonAlert
                    else -> AmberNotice
                }
                val statusBg = when (statusLower) {
                    "paid" -> EmeraldSurface
                    "rejected" -> CrimsonSurface
                    else -> AmberSurface
                }
                val statusIcon = when (statusLower) {
                    "paid" -> Icons.Rounded.CheckCircle
                    "rejected" -> Icons.Rounded.Cancel
                    else -> Icons.Rounded.Schedule
                }

                TournaXCard(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            withdrawActiveRequestId = wd.id
                            withdrawModalStep = WithdrawDialogStep.PENDING_STATUS
                            showWithdrawFlowModal = true
                        }
                        .testTag("withdrawal_request_card_${wd.id}"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(statusBg),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = wd.status,
                                    tint = statusColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "UID: ${wd.freeFireUid} • ${wd.upiId}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                                Text(
                                    text = when (statusLower) {
                                        "paid" -> "Paid ✓ • ₹${wd.amount} sent"
                                        "rejected" -> wd.adminNote.ifBlank { "Declined • Tap for details" }
                                        else -> "Pending • Wait until 10:00 PM"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(color = statusColor),
                                )
                            }
                        }
                        Text(
                            text = "₹${wd.amount}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }

        // Minimal Transactions Header & List
        item {
            Text(
                text = "TRANSACTIONS",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = MetallicSilverDim,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp,
                ),
            )
        }

        if (transactions.isEmpty()) {
            item {
                TournaXCard {
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(transactions, key = { "tx_${it.id}" }) { tx ->
                TournaXCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (tx.isCredit) EmeraldSurface else CrimsonSurface),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (tx.isCredit) {
                                        Icons.Rounded.ArrowDownward
                                    } else {
                                        Icons.Rounded.ArrowUpward
                                    },
                                    contentDescription = tx.category,
                                    tint = if (tx.isCredit) EmeraldVerified else CrimsonAlert,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = tx.title,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                                Text(
                                    text = "${tx.category} • ${tx.status}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                                )
                            }
                        }
                        Text(
                            text = "${if (tx.isCredit) "+" else "-"}${tx.amount} Pts",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = if (tx.isCredit) EmeraldVerified else CrimsonAlert,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showDepositModal) {
        UpiDepositFlowModal(
            initialStep = depositModalStep,
            initialAmount = depositModalAmount,
            initialActiveUtr = depositModalUtr,
            depositRequests = depositRequests,
            isBusy = isBusy,
            onSubmitDeposit = onSubmitDeposit,
            onCopied = onCopied,
            onDismiss = { showDepositModal = false },
        )
    }

    if (showWithdrawFlowModal) {
        WithdrawFlowModal(
            profile = profile,
            initialStep = withdrawModalStep,
            initialRequestId = withdrawActiveRequestId,
            withdrawalRequests = withdrawalRequests,
            isBusy = isBusy,
            onSubmitWithdrawal = onSubmitWithdrawal,
            onDismiss = { showWithdrawFlowModal = false },
        )
    }

    if (showMinWithdrawNotice) {
        AlertDialog(
            onDismissRequest = { showMinWithdrawNotice = false },
            containerColor = ObsidianCard,
            titleContentColor = MetallicSilver,
            textContentColor = MetallicSilverDim,
            title = {
                Text(
                    text = "Minimum 50 Points Required",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Withdrawable Winning Balance: ₹${profile.walletWinning} (${profile.walletWinning} Pts). You need at least ${ArenaRepository.MIN_WITHDRAWAL_POINTS} Winning Points to unlock the withdrawal form (Deposit Points are for match entries only).",
                )
            },
            confirmButton = {
                TextButton(onClick = { showMinWithdrawNotice = false }) {
                    Text("Got It", color = NeonPurpleSoft, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}
