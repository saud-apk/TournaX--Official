package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.data.model.WalletAppConfig
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
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianInput

private enum class WalletTxFilter(val key: String, val label: String) {
    ALL("all", "All"),
    DEPOSITS("deposits", "Deposits"),
    MATCHES("matches", "Matches"),
    PAYOUTS("payouts", "Prizes & Payouts"),
}

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
    merchantUpiId: String = ArenaRepository.MERCHANT_UPI_ID,
    walletConfig: WalletAppConfig = WalletAppConfig(),
    onRetryDepositClaim: (utr: String) -> Unit = {},
    onRetryWithdrawalRefund: (requestId: String) -> Unit = {},
    pendingWithdrawSelfieBase64: String = "",
    onPendingWithdrawSelfieChange: ((String) -> Unit)? = null,
    pendingWithdrawSummary: WithdrawalRequest? = null,
    onPendingWithdrawSummaryChange: ((WithdrawalRequest?) -> Unit)? = null,
    onClearWithdrawDraft: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val effectiveMerchantUpi = walletConfig.merchantUpiId.ifBlank { merchantUpiId }
    val minDepositInr = walletConfig.minDepositInr.coerceAtLeast(1L)
    val maxDepositInr = walletConfig.maxDepositInr.coerceAtLeast(minDepositInr)
    val minWithdrawPts = walletConfig.minWithdrawalPoints.coerceAtLeast(1L)

    var showDepositModal by rememberSaveable { mutableStateOf(false) }
    var depositModalStep by rememberSaveable { mutableStateOf(DepositDialogStep.AMOUNT) }
    var depositModalAmount by rememberSaveable { mutableLongStateOf(100L.coerceIn(minDepositInr, maxDepositInr)) }
    var depositModalUtr by rememberSaveable { mutableStateOf("") }
    var showMinWithdrawNotice by rememberSaveable { mutableStateOf(false) }
    var showWithdrawFlowModal by rememberSaveable { mutableStateOf(false) }
    var withdrawModalStep by rememberSaveable { mutableStateOf(WithdrawDialogStep.FORM) }
    var withdrawActiveRequestId by rememberSaveable { mutableStateOf("") }
    var selectedTxFilterKey by rememberSaveable { mutableStateOf(WalletTxFilter.ALL.key) }
    val selectedTxFilter = WalletTxFilter.entries.firstOrNull { it.key == selectedTxFilterKey } ?: WalletTxFilter.ALL

    val depositPts = profile.safeDepositPoints
    val winningPts = profile.safeWinningPoints
    val withdrawalsById = remember(withdrawalRequests) {
        withdrawalRequests.associateBy { it.id }
    }
    val totalAddedPts = remember(transactions) {
        transactions.filter { tx ->
            tx.isCredit && (tx.id.startsWith("dep_") || tx.title.contains("Deposit", ignoreCase = true))
        }.sumOf { it.amount.coerceAtLeast(0L) }
    }
    val totalWonOrRefundedPts = remember(transactions) {
        transactions.filter { tx ->
            val isDeposit = tx.id.startsWith("dep_") || tx.title.contains("Deposit", ignoreCase = true)
            val isWithdrawalRefund = tx.id.startsWith("wd_refund_") ||
                tx.title.contains("Withdrawal Refunded", ignoreCase = true) ||
                tx.category.contains("Declined Payout Refund", ignoreCase = true)
            tx.isCredit && !isDeposit && !isWithdrawalRefund
        }.sumOf { it.amount.coerceAtLeast(0L) }
    }
    val totalMatchesPaidPts = remember(transactions) {
        transactions.filter { tx ->
            !tx.isCredit && !tx.id.startsWith("tx_wd_")
        }.sumOf { it.amount.coerceAtLeast(0L) }
    }
    val filteredTransactions = remember(transactions, selectedTxFilter) {
        when (selectedTxFilter) {
            WalletTxFilter.ALL -> transactions
            WalletTxFilter.DEPOSITS -> transactions.filter { tx ->
                tx.id.startsWith("dep_") ||
                    tx.title.contains("Deposit", ignoreCase = true) ||
                    tx.category.contains("UTR", ignoreCase = true)
            }
            WalletTxFilter.MATCHES -> transactions.filter { tx ->
                tx.id.startsWith("join_") ||
                    tx.status.equals("Match Entry", ignoreCase = true) ||
                    tx.title.startsWith("Joined:", ignoreCase = true)
            }
            WalletTxFilter.PAYOUTS -> transactions.filter { tx ->
                val isDeposit = tx.id.startsWith("dep_") || tx.title.contains("Deposit", ignoreCase = true)
                (tx.isCredit && !isDeposit) ||
                    tx.id.startsWith("tx_wd_") ||
                    tx.id.startsWith("refund_") ||
                    tx.title.contains("Withdraw", ignoreCase = true) ||
                    tx.title.contains("Prize", ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("wallet_lazy_column"),
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

        // Strict 2-Section Esports Wallet Card: Deposit (Match Only) + Winning (Withdrawable)
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
                    // Section 1: Deposit Points (Match Entry Only) + Add Money
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
                                modifier = Modifier.testTag("wallet_deposit_balance_text"),
                            )
                            Text(
                                text = "Used first for match entry fees",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MetallicSilverDim,
                                    fontSize = 10.sp,
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
                                    depositModalAmount = 100L.coerceIn(minDepositInr, maxDepositInr)
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
                                modifier = Modifier.testTag("wallet_winning_balance_text"),
                            )
                            Text(
                                text = "Min $minWithdrawPts Pts • 10 PM Payout",
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
                                if (profile.walletWinning < minWithdrawPts) {
                                    showMinWithdrawNotice = true
                                } else {
                                    onClearWithdrawDraft()
                                    withdrawModalStep = WithdrawDialogStep.FORM
                                    withdrawActiveRequestId = ""
                                    showWithdrawFlowModal = true
                                }
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (profile.walletWinning >= minWithdrawPts) {
                                    EmeraldVerified.copy(alpha = 0.7f)
                                } else {
                                    ObsidianBorderBright
                                },
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (profile.walletWinning >= minWithdrawPts) {
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
                                tint = if (profile.walletWinning >= minWithdrawPts) {
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

        // Real-Time Wallet Cashflow Summary Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_cashflow_strip"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WalletSummaryMetricTile(
                    label = "ADDED",
                    value = "+$totalAddedPts",
                    accent = NeonPurpleSoft,
                    modifier = Modifier.weight(1f),
                )
                WalletSummaryMetricTile(
                    label = "WON / REFUND",
                    value = "+$totalWonOrRefundedPts",
                    accent = EmeraldVerified,
                    modifier = Modifier.weight(1f),
                )
                WalletSummaryMetricTile(
                    label = "ENTRIES PAID",
                    value = "$totalMatchesPaidPts",
                    accent = AmberNotice,
                    modifier = Modifier.weight(1f),
                )
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
                                        "approved" -> if (req.credited) {
                                            "Approved • +${req.amount} Pts"
                                        } else {
                                            "Approved • Crediting +${req.amount} Pts..."
                                        }
                                        "rejected" -> "${req.adminNote.ifBlank { "Rejected" }} • Tap to retry"
                                        else -> "Pending (2–5 min hold) • Tap"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(color = statusColor),
                                )
                                if (statusLower == "approved" && !req.credited) {
                                    Surface(
                                        color = EmeraldSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = !isBusy) { onRetryDepositClaim(displayUtr) }
                                            .testTag("retry_deposit_credit_$displayUtr"),
                                    ) {
                                        Text(
                                            text = "Retry Credit",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = EmeraldVerified,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }
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
                                        "rejected" -> if (wd.refunded) {
                                            "${wd.adminNote.ifBlank { "Declined" }} • ₹${wd.amount} Refunded ✓"
                                        } else {
                                            wd.adminNote.ifBlank { "Declined • Refunding..." }
                                        }
                                        else -> "Pending • Wait until 10:00 PM"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(color = statusColor),
                                )
                                if (statusLower == "rejected" && !wd.refunded) {
                                    Surface(
                                        color = CrimsonSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = !isBusy) { onRetryWithdrawalRefund(wd.id) }
                                            .testTag("retry_withdrawal_refund_${wd.id}"),
                                    ) {
                                        Text(
                                            text = "Retry Refund",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = CrimsonAlert,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }
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

        // Pro Transactions Header + Category Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "TRANSACTIONS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MetallicSilverDim,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp,
                        ),
                    )
                    Text(
                        text = "${filteredTransactions.size} Entries",
                        style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    WalletTxFilter.entries.forEach { filter ->
                        val active = selectedTxFilter == filter
                        Surface(
                            color = if (active) NeonPurpleGlow else ObsidianCard,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (active) NeonPurpleStart else ObsidianBorder,
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTxFilterKey = filter.key }
                                .testTag("wallet_tx_filter_${filter.key}"),
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (active) NeonPurpleSoft else MetallicSilverDim,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                TournaXCard {
                    Text(
                        text = if (transactions.isEmpty()) {
                            "No transactions yet"
                        } else {
                            "No ${selectedTxFilter.label.lowercase()} transactions yet"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(filteredTransactions, key = { "tx_${it.id}" }) { tx ->
                val linkedWithdrawal = if (tx.id.startsWith("tx_")) {
                    withdrawalsById[tx.id.removePrefix("tx_")]
                } else {
                    null
                }
                val linkedWdStatus = linkedWithdrawal?.status?.lowercase()
                val effectiveTitle = when (linkedWdStatus) {
                    "paid" -> "Withdrawal Paid (-₹${tx.amount})"
                    "rejected" -> "Withdrawal Declined (₹${tx.amount})"
                    else -> tx.title
                }
                val effectiveStatus = when (linkedWdStatus) {
                    "paid" -> "Paid ✓"
                    "rejected" -> if (linkedWithdrawal.refunded) "Declined • Refunded" else "Declined"
                    else -> tx.status
                }
                val statusAccentColor = when (linkedWdStatus) {
                    "paid" -> EmeraldVerified
                    "rejected" -> AmberNotice
                    else -> MetallicSilverDim
                }

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
                                    text = effectiveTitle,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                                Text(
                                    text = "${tx.category} • $effectiveStatus",
                                    style = MaterialTheme.typography.labelSmall.copy(color = statusAccentColor),
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
            merchantUpiId = effectiveMerchantUpi,
            minDepositInr = minDepositInr,
            maxDepositInr = maxDepositInr,
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
            minWithdrawalPoints = minWithdrawPts,
            pendingSelfieBase64 = pendingWithdrawSelfieBase64,
            onPendingSelfieChange = onPendingWithdrawSelfieChange,
            pendingSubmittedSummary = pendingWithdrawSummary,
            onPendingSubmittedSummaryChange = onPendingWithdrawSummaryChange,
            onSubmitWithdrawal = onSubmitWithdrawal,
            onDismiss = {
                showWithdrawFlowModal = false
                onClearWithdrawDraft()
            },
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
                    text = "Minimum $minWithdrawPts Points Required",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Withdrawable Winning Balance: ₹${profile.walletWinning} (${profile.walletWinning} Pts). You need at least $minWithdrawPts Winning Points to unlock the withdrawal form (Deposit Points are for match entries only).",
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

@Composable
private fun WalletSummaryMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianInput,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MetallicSilverDim,
                    fontSize = 9.sp,
                    letterSpacing = 0.6.sp,
                ),
            )
            Text(
                text = "$value Pts",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }
    }
}
