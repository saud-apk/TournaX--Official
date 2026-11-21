package com.example

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DepositRequest
import com.example.data.model.PlayerProfile
import com.example.data.model.WalletAppConfig
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.ui.screens.WalletTabScreen
import com.example.ui.theme.TournaXTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class WalletUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun walletTabAndAddMoneyAndWithdrawFlow_rendersAndTransitionsWithoutCrashing() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.CAMERA)

        val profile = PlayerProfile(
            userId = "u1",
            fullName = "Aarav Sharma",
            phone = "9876543210",
            freeFireUid = "1234567890",
            freeFireIgn = "TX_Aarav",
            points = 0L,
            walletDeposit = 105L,
            walletWinning = 250L,
        )
        val deposits = listOf(
            DepositRequest(
                id = "412345678901",
                userId = "u1",
                utr = "412345678901",
                amount = 100L,
                status = "pending",
            ),
            // Edge case: manually added doc in console without utr field must never crash LazyColumn keys
            DepositRequest(
                id = "manual_doc_1",
                userId = "u1",
                utr = "",
                amount = 50L,
                status = "pending",
            ),
        )
        val withdrawals = listOf(
            WithdrawalRequest(
                id = "wd_existing_1",
                userId = "u1",
                fullName = "Aarav Sharma",
                freeFireUid = "1234567890",
                upiId = "9876543210",
                amount = 50L,
                status = "pending",
            )
        )
        val txs = listOf(
            WalletTransaction(
                id = "tx_1",
                userId = "u1",
                title = "UPI Deposit (+100 Pts)",
                category = "UTR: 412345678901",
                amount = 100L,
                isCredit = true,
                status = "Approved",
            )
        )

        composeRule.setContent {
            TournaXTheme {
                WalletTabScreen(
                    profile = profile,
                    transactions = txs,
                    depositRequests = deposits,
                    withdrawalRequests = withdrawals,
                    isBusy = false,
                    onSubmitDeposit = { _, utr, onSubmitted, _ ->
                        onSubmitted(utr)
                    },
                    onCopied = {},
                )
            }
        }

        composeRule.waitForIdle()
        // Verify strict 2-section Wallet renders cleanly (Deposit = 105 Pts, Winning = ₹250 (250 Pts), no TOTAL BALANCE)
        composeRule.onNodeWithText("TOTAL BALANCE").assertDoesNotExist()
        composeRule.onNodeWithTag("wallet_deposit_balance_text").assertTextEquals("105 Pts").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_winning_balance_text").assertTextEquals("₹250 (250 Pts)").assertIsDisplayed()

        // Tap pending withdrawal tracker card -> verify 10 PM Pending Confirmation modal displays
        composeRule.onNodeWithTag("withdrawal_request_card_wd_existing_1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WITHDRAWAL PENDING • 10:00 PM PAYOUT").assertIsDisplayed()
        composeRule.onNodeWithTag("withdraw_pending_done_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Open Withdraw Form (>= 50 Pts), open in-app face verification popup, verify 5-step liveness UI, and close
        composeRule.onNodeWithTag("wallet_withdraw_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("WITHDRAW WINNING BALANCE").assertIsDisplayed()
        composeRule.onNodeWithTag("withdraw_fullname_input").assertTextEquals("Aarav Sharma")
        composeRule.onNodeWithTag("withdraw_uid_input").assertTextEquals("1234567890")
        composeRule.onNodeWithTag("withdraw_take_selfie_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("face_verification_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("LIVE FACE VERIFICATION").assertIsDisplayed()
        composeRule.onNodeWithTag("face_verification_progress_pill").assertIsDisplayed()
        composeRule.onNodeWithTag("face_verification_close_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("withdraw_close_button").performClick()
        composeRule.waitForIdle()

        // Full Add Money Flow -> Amount -> QR & UTR -> Live Verification Hold Popup
        composeRule.onNodeWithTag("wallet_add_money_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("add_money_amount_input").performTextReplacement("200")
        composeRule.onNodeWithTag("add_money_pay_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("upi_qr_image").assertIsDisplayed()
        composeRule.onNodeWithTag("add_money_utr_input").performScrollTo().performTextReplacement("498765432109")
        composeRule.onNodeWithTag("add_money_done_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("VERIFYING PAYMENT", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun withdrawBelow50Points_showsMinimum50RequiredDialog() {
        val lowProfile = PlayerProfile(
            userId = "u2",
            fullName = "Kabir Mehta",
            phone = "9812345678",
            freeFireUid = "2345678901",
            freeFireIgn = "TX_Kabir",
            walletDeposit = 100L,
            walletWinning = 40L,
        )

        composeRule.setContent {
            TournaXTheme {
                WalletTabScreen(
                    profile = lowProfile,
                    transactions = emptyList(),
                    depositRequests = emptyList(),
                    withdrawalRequests = emptyList(),
                    isBusy = false,
                    onSubmitDeposit = { _, utr, onSubmitted, _ -> onSubmitted(utr) },
                    onCopied = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wallet_withdraw_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Minimum 50 Points Required").assertIsDisplayed()
        composeRule.onNodeWithText("Got It").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun walletIgnoresLegacyPointsSyncedWithdrawalStatusesFiltersAndValidatesUpiFormat() {
        val profileWithLegacyPoints = PlayerProfile(
            userId = "u3",
            fullName = "Rohan Verma",
            phone = "9811223344",
            freeFireUid = "3456789012",
            freeFireIgn = "TX_Rohan",
            points = 15L,
            walletDeposit = 100L,
            walletWinning = 120L,
        )
        val deposits = listOf(
            DepositRequest(
                id = "411122223333",
                userId = "u3",
                utr = "411122223333",
                amount = 100L,
                status = "rejected",
                adminNote = "UTR amount mismatch",
            )
        )
        val withdrawals = listOf(
            WithdrawalRequest(
                id = "wd_paid_1",
                userId = "u3",
                fullName = "Rohan Verma",
                freeFireUid = "3456789012",
                upiId = "rohan@okaxis",
                amount = 60L,
                status = "paid",
            ),
            WithdrawalRequest(
                id = "wd_rej_1",
                userId = "u3",
                fullName = "Rohan Verma",
                freeFireUid = "3456789012",
                upiId = "rohan@ybl",
                amount = 50L,
                status = "rejected",
                refunded = true,
            ),
        )
        val txs = listOf(
            WalletTransaction(
                id = "dep_411122229999",
                userId = "u3",
                title = "UPI Deposit (+100 Pts)",
                category = "₹100 → 100 Pts • UTR: 411122229999",
                amount = 100L,
                isCredit = true,
                status = "Credited",
            ),
            WalletTransaction(
                id = "join_match_1",
                userId = "u3",
                title = "Joined: Bermuda Cup",
                category = "BR Squad • Entry (-20 Pts)",
                amount = 20L,
                isCredit = false,
                status = "Match Entry",
            ),
            WalletTransaction(
                id = "tx_wd_paid_1",
                userId = "u3",
                title = "Withdrawal Pending (-₹60)",
                category = "10 PM Payout • rohan@okaxis",
                amount = 60L,
                isCredit = false,
                status = "Pending (10 PM)",
            ),
            WalletTransaction(
                id = "tx_wd_rej_1",
                userId = "u3",
                title = "Withdrawal Pending (-₹50)",
                category = "10 PM Payout • rohan@ybl",
                amount = 50L,
                isCredit = false,
                status = "Pending (10 PM)",
            ),
        )

        composeRule.setContent {
            TournaXTheme {
                WalletTabScreen(
                    profile = profileWithLegacyPoints,
                    transactions = txs,
                    depositRequests = deposits,
                    withdrawalRequests = withdrawals,
                    isBusy = false,
                    onSubmitDeposit = { _, utr, onSubmitted, _ -> onSubmitted(utr) },
                    onCopied = {},
                )
            }
        }

        composeRule.waitForIdle()
        // Strict 2-Section Wallet: Deposit = 100 Pts, Winning = ₹120 (120 Pts), no TOTAL BALANCE or legacy Spin inflation
        composeRule.onNodeWithText("TOTAL BALANCE").assertDoesNotExist()
        composeRule.onNodeWithTag("wallet_deposit_balance_text").assertTextEquals("100 Pts").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_winning_balance_text").assertTextEquals("₹120 (120 Pts)").assertIsDisplayed()
        composeRule.onNodeWithText("235 Pts").assertDoesNotExist()
        composeRule.onNodeWithText("Includes 15 Spin Bonus Pts").assertDoesNotExist()
        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasTestTag("wallet_cashflow_strip"))
        composeRule.onNodeWithTag("wallet_cashflow_strip").assertIsDisplayed()

        // Filter chips work cleanly: Matches -> only Joined: Bermuda Cup; Payouts -> withdrawals
        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasTestTag("wallet_tx_filter_matches"))
        composeRule.onNodeWithTag("wallet_tx_filter_matches").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasText("Joined: Bermuda Cup"))
        composeRule.onNodeWithText("Joined: Bermuda Cup").assertIsDisplayed()
        composeRule.onNodeWithText("UPI Deposit (+100 Pts)").assertDoesNotExist()

        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasTestTag("wallet_tx_filter_payouts"))
        composeRule.onNodeWithTag("wallet_tx_filter_payouts").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Synced transaction statuses should override stale Pending (10 PM)
        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasText("Withdrawal Paid (-₹60)"))
        composeRule.onNodeWithText("Withdrawal Paid (-₹60)").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasText("Withdrawal Declined (₹50)"))
        composeRule.onNodeWithText("Withdrawal Declined (₹50)").assertIsDisplayed()

        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasTestTag("wallet_tx_filter_all"))
        composeRule.onNodeWithTag("wallet_tx_filter_all").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Rejected deposit card -> tap -> Retry Same UTR button is available
        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasTestTag("deposit_request_card_411122223333"))
        composeRule.onNodeWithTag("deposit_request_card_411122223333").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("deposit_rejected_retry_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("deposit_rejected_close_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Withdraw modal -> bare phone number without '@' is blocked cleanly
        composeRule.onNodeWithTag("wallet_lazy_column")
            .performScrollToNode(hasTestTag("wallet_withdraw_button"))
        composeRule.onNodeWithTag("wallet_withdraw_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("withdraw_amount_input").performScrollTo().performTextReplacement("60")
        composeRule.onNodeWithTag("withdraw_upi_input").performScrollTo().performTextReplacement("9811223344")
        composeRule.onNodeWithTag("withdraw_done_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter a valid UPI ID (e.g. name@okaxis or 9876543210@ybl).")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun dynamicWalletConfigBounds_applyToWithdrawAndDepositUi() {
        val profile = PlayerProfile(
            userId = "u4",
            fullName = "Vikram Nair",
            phone = "9822334455",
            freeFireUid = "4567890123",
            freeFireIgn = "TX_Vikram",
            walletDeposit = 100L,
            walletWinning = 60L, // Above default 50, below dynamic 75
        )
        val customConfig = WalletAppConfig(
            merchantUpiId = "tournax.pro@ybl",
            minDepositInr = 25L,
            maxDepositInr = 1000L,
            minWithdrawalPoints = 75L,
        )

        composeRule.setContent {
            TournaXTheme {
                WalletTabScreen(
                    profile = profile,
                    transactions = emptyList(),
                    depositRequests = emptyList(),
                    withdrawalRequests = emptyList(),
                    isBusy = false,
                    onSubmitDeposit = { _, utr, onSubmitted, _ -> onSubmitted(utr) },
                    onCopied = {},
                    walletConfig = customConfig,
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Min 75 Pts • 10 PM Payout").assertIsDisplayed()
        composeRule.onNodeWithTag("wallet_withdraw_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Minimum 75 Points Required").assertIsDisplayed()
        composeRule.onNodeWithText("Got It").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("wallet_add_money_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Min ₹25 • Max ₹1000 • Converts to Deposit Pts").assertIsDisplayed()
        composeRule.onNodeWithTag("add_money_amount_input").performTextReplacement("15")
        composeRule.onNodeWithText("Please enter an amount between ₹25 and ₹1000.").assertIsDisplayed()
        composeRule.onNodeWithTag("add_money_amount_input").performTextReplacement("750")
        composeRule.onNodeWithText("Pay ₹750 (Get 750 Pts) →").assertIsDisplayed()
    }
}
