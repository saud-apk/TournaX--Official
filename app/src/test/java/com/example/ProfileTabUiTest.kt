package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.PlayerProfile
import com.example.ui.screens.ProfileTabScreen
import com.example.ui.theme.TournaXTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ProfileTabUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun minimalProfileTab_rendersIdentityStatsAndActionsCleanly() {
        var selectedAvatar = 0L
        var logoutClicked = false

        val profile = PlayerProfile(
            userId = "u1",
            fullName = "Aarav Sharma",
            phone = "9876543210",
            freeFireUid = "1234567890",
            freeFireIgn = "TX_Aarav",
            avatarIndex = 0L,
            level = 4L,
            totalTournaments = 12L,
            totalKills = 36L,
            booyahs = 3L,
            squadName = "Vipers",
            squadTag = "TX",
            squadUids = listOf("1234567890", "2233445566"),
        )

        composeRule.setContent {
            TournaXTheme {
                ProfileTabScreen(
                    profile = profile,
                    userEmail = "aarav@example.com",
                    isUpdating = false,
                    onUpdateAvatar = { selectedAvatar = it },
                    onUpdateSquad = { _, _, _, onDone -> onDone() },
                    onUpdateProfile = { _, _, _, onDone -> onDone() },
                    onReportSubmitted = {},
                    onCopied = {},
                    onLogout = { logoutClicked = true },
                )
            }
        }

        composeRule.waitForIdle()

        // Identity + compact 1-line UID bar + 4-stat telemetry strip
        composeRule.onNodeWithText("TX_Aarav").assertIsDisplayed()
        composeRule.onNodeWithText("LVL 4").assertIsDisplayed()
        composeRule.onNodeWithText("1234567890").assertIsDisplayed()
        composeRule.onNodeWithText("MATCHES").assertIsDisplayed()
        composeRule.onNodeWithText("3.00").assertIsDisplayed()

        // Grouped action rows
        composeRule.onNodeWithTag("edit_squad_button").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_support_button").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_legal_terms_button").assertIsDisplayed()

        // Avatar picker flow
        composeRule.onNodeWithTag("profile_avatar_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("avatar_pick_2").performClick()
        composeRule.waitForIdle()
        assertEquals(2L, selectedAvatar)

        // Edit profile dialog opens
        composeRule.onNodeWithTag("profile_edit_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save Profile").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()

        // Logout works
        composeRule.onNodeWithTag("profile_logout_button").performClick()
        assertTrue(logoutClicked)
    }
}
