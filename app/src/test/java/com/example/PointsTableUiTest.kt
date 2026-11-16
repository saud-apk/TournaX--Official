package com.example

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.PlayerProfile
import com.example.data.model.Tournament
import com.example.data.model.TournamentResultEntry
import com.example.ui.screens.PlayTabScreen
import com.example.ui.theme.TournaXTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class PointsTableUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun completedTournament_opensPointsTableWithPodiumMvpAndStandings() {
        val profile = PlayerProfile(
            userId = "u1",
            fullName = "Aarav Sharma",
            phone = "9876543210",
            freeFireUid = "1234567890",
            freeFireIgn = "TX_Aarav",
        )
        val completedMatch = Tournament(
            id = "real_match_done_1",
            title = "Bermuda Scrims Finals",
            mode = "BR Squad",
            mapName = "Bermuda",
            dateTimeLabel = "Ended • 9:00 PM IST",
            status = "completed",
            entryFee = 20L,
            prizePool = 500L,
            perKill = 5L,
            prizeFirst = 300L,
            prizeSecond = 120L,
            prizeThird = 80L,
            maxSlots = 12L,
            filledSlots = 12L,
            slotUnitLabel = "Teams",
            resultsPublished = true,
            mvpIgn = "TX_Aarav",
        )
        val resultsFlow = MutableStateFlow(
            listOf(
                TournamentResultEntry(
                    id = "team_1",
                    tournamentId = "real_match_done_1",
                    rank = 1L,
                    playerOrTeamName = "Vipers",
                    squadTag = "TX",
                    freeFireUid = "1234567890",
                    userId = "u1",
                    booyahs = 1L,
                    placementPoints = 12L,
                    kills = 9L,
                    killPoints = 9L,
                    totalPoints = 21L,
                    prizeWon = 300L,
                    isMvp = true,
                ),
                TournamentResultEntry(
                    id = "team_2",
                    tournamentId = "real_match_done_1",
                    rank = 2L,
                    playerOrTeamName = "KobraSquad",
                    squadTag = "IND",
                    booyahs = 0L,
                    placementPoints = 9L,
                    kills = 7L,
                    killPoints = 7L,
                    totalPoints = 16L,
                    prizeWon = 120L,
                    isMvp = false,
                ),
            )
        )

        composeRule.setContent {
            TournaXTheme {
                PlayTabScreen(
                    profile = profile,
                    tournaments = listOf(completedMatch),
                    registrations = emptyList(),
                    isBusy = false,
                    onJoinTournament = { _, _, _ -> },
                    onGoToWallet = {},
                    onCopied = {},
                    observeMatchResults = { resultsFlow },
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("mode_card_all").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Completed tab
        composeRule.onNodeWithTag("status_pill_completed").performClick()
        composeRule.waitForIdle()

        // Completed card shows "Points Table" CTA
        composeRule.onNodeWithText("Points Table").assertIsDisplayed()
        composeRule.onNodeWithTag("tournament_cta_real_match_done_1").performClick()
        composeRule.waitForIdle()

        // Points Table section displays Admin Verified & 4-column Standings (NO., PLAYER NAME, KILLS, WINNING)
        composeRule.onNodeWithText("OFFICIAL STANDINGS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("ADMIN VERIFIED").assertIsDisplayed()
        composeRule.onNodeWithText("NO.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PLAYER NAME").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("KILLS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("WINNING").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("YOU").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("points_table_row_team_1")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText("300 Pts")))
        composeRule.onNodeWithTag("points_table_row_team_2")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(hasAnyDescendant(hasText("120 Pts")))
    }
}
