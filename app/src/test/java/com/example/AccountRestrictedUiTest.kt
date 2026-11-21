package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.PlayerProfile
import com.example.ui.screens.AccountRestrictedScreen
import com.example.ui.theme.TournaXTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class AccountRestrictedUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permanentBan_displaysReasonUidAppealAndSignOut() {
        var signedOut = false
        val bannedProfile = PlayerProfile(
            userId = "u_ban_1",
            fullName = "Aarav Sharma",
            freeFireUid = "1234567890",
            freeFireIgn = "TX_Aarav",
            accountStatus = "banned",
            banReason = "Auto-Aim Hack Detected in Match #44",
            bannedBy = "admin_panel",
        )

        composeRule.setContent {
            TournaXTheme {
                AccountRestrictedScreen(
                    profile = bannedProfile,
                    nowMillis = 1_700_000_000_000L,
                    onSignOut = { signedOut = true },
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("ACCOUNT BANNED").assertIsDisplayed()
        composeRule.onNodeWithText("Arena Access Permanently Locked").assertIsDisplayed()
        composeRule.onNodeWithText("Auto-Aim Hack Detected in Match #44").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1234567890").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("appeal_whatsapp_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("appeal_email_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("restricted_sign_out_button").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(signedOut)
    }

    @Test
    fun timedSuspension_displaysCountdownTimer_andAutoExpiresInModel() {
        val baseNow = 1_700_000_000_000L
        val ninetyMinutesMs = 5_400_000L
        val suspendedProfile = PlayerProfile(
            userId = "u_susp_1",
            fullName = "Rohan Verma",
            freeFireUid = "9876543210",
            freeFireIgn = "TX_Rohan",
            accountStatus = "suspended",
            banReason = "Teaming Investigation Hold",
            suspendedUntil = Timestamp(Date(baseNow + ninetyMinutesMs)),
            bannedBy = "admin_panel",
        )

        assertTrue(suspendedProfile.isRestricted(baseNow))
        assertFalse(suspendedProfile.isRestricted(baseNow + ninetyMinutesMs + 1000L))

        composeRule.setContent {
            TournaXTheme {
                AccountRestrictedScreen(
                    profile = suspendedProfile,
                    nowMillis = baseNow,
                    onSignOut = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("ACCOUNT SUSPENDED").assertIsDisplayed()
        composeRule.onNodeWithText("UNLOCKS IN").assertIsDisplayed()
        composeRule.onNodeWithText("1h 30m 0s").assertIsDisplayed()
        composeRule.onNodeWithText("Teaming Investigation Hold").performScrollTo().assertIsDisplayed()
    }
}
