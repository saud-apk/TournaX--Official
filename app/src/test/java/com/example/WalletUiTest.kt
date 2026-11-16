package com.example

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DepositRequest
import com.example.data.model.PlayerProfile
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
        // Verify minimal split balances render cleanly (Deposit = 105 Pts, Total = 355 Pts, Winning = ₹250)
        composeRule.onNodeWithText("355 Pts").assertIsDisplayed()
        composeRule.onNodeWithText("105 Pts").assertIsDisplayed()

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
        composeRule.onNodeWithText("PLEASE DO NOT CLOSE THIS WINDOW", substring = true)
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
}
