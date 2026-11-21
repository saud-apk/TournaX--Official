package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import com.example.data.model.PlayerProfile
import com.example.data.model.Tournament
import com.example.data.model.TournamentParticipant
import com.example.data.model.TournamentRegistration
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
class PlayTabUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playTabAndTournamentDetailLobby_rendersProCardsAndPreMatchLobby() {
        val profile = PlayerProfile(
            userId = "u1",
            fullName = "Aarav Sharma",
            phone = "9876543210",
            freeFireUid = "1234567890",
            freeFireIgn = "TX_Aarav",
            walletDeposit = 100L,
            walletWinning = 50L,
        )
        val match1 = Tournament(
            id = "real_match_1",
            title = "TournaX Bermuda Cup",
            mode = "BR Solo",
            mapName = "Bermuda",
            dateTimeLabel = "Tonight • 9:00 PM IST",
            status = "upcoming",
            entryFee = 20L,
            prizePool = 500L,
            perKill = 5L,
            prizeFirst = 300L,
            prizeSecond = 120L,
            prizeThird = 80L,
            maxSlots = 48L,
            filledSlots = 12L,
            slotUnitLabel = "Players",
            rulesSummary = "Mobile only. No hacks or emulators.",
            roomId = "99887766",
            roomPassword = "PASS1",
            roomUnlocked = false,
        )
        val joinedMatch = Tournament(
            id = "real_match_2",
            title = "CS 4v4 Evening Clash",
            mode = "CS 4v4",
            mapName = "Kalahari",
            dateTimeLabel = "Tonight • 10:00 PM IST",
            status = "upcoming",
            entryFee = 10L,
            prizePool = 200L,
            perKill = 0L,
            prizeFirst = 140L,
            prizeSecond = 60L,
            prizeThird = 0L,
            maxSlots = 16L,
            filledSlots = 8L,
            slotUnitLabel = "Teams",
            roomId = "77665544",
            roomPassword = "CS99",
            roomUnlocked = false,
        )
        val registrations = listOf(
            TournamentRegistration(
                id = "real_match_2",
                userId = "u1",
                tournamentId = "real_match_2",
                tournamentTitle = "CS 4v4 Evening Clash",
                mode = "CS 4v4",
                mapName = "Kalahari",
                dateTimeLabel = "Tonight • 10:00 PM IST",
                slotNumber = 4L,
                captainUid = "1234567890",
                captainIgn = "TX_Aarav",
            )
        )
        val realMatch2Participants = listOf(
            TournamentParticipant(
                id = "u2",
                userId = "u2",
                tournamentId = "real_match_2",
                slotNumber = 1L,
                captainIgn = "Rival_Viper",
                captainUid = "9988776655",
                squadTag = "VPR",
            ),
            TournamentParticipant(
                id = "u1",
                userId = "u1",
                tournamentId = "real_match_2",
                slotNumber = 4L,
                captainIgn = "TX_Aarav",
                captainUid = "1234567890",
                squadTag = "TX",
            ),
        )

        composeRule.setContent {
            TournaXTheme {
                PlayTabScreen(
                    profile = profile,
                    tournaments = listOf(match1, joinedMatch),
                    registrations = registrations,
                    isBusy = false,
                    onJoinTournament = { _, _, _ -> },
                    onGoToWallet = {},
                    onCopied = {},
                    onOpenMyTournaments = {},
                    observeMatchParticipants = { id ->
                        if (id == "real_match_2") {
                            MutableStateFlow(realMatch2Participants)
                        } else {
                            MutableStateFlow(emptyList())
                        }
                    },
                )
            }
        }

        composeRule.waitForIdle()

        // Hub renders Wallet Points pill, Joined counter, Spotlight Banner & Mode cards
        composeRule.onNodeWithTag("play_wallet_points_chip").assertIsDisplayed()
        composeRule.onNodeWithText("100 • 50 Pts").assertIsDisplayed()
        composeRule.onNodeWithTag("play_my_slots_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("featured_tournament_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_card_br").performScrollTo().assertIsDisplayed().performClick()

        composeRule.waitForIdle()

        // Inside BR matches list -> open TournaX Bermuda Cup pre-match detail lobby
        composeRule.onNodeWithTag("tournament_cta_real_match_1").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Option 1: Description tab verifies Overview, Prize Breakdown, Rules, Join CTA
        composeRule.onNodeWithTag("match_tab_description").assertIsDisplayed()
        composeRule.onNodeWithTag("match_tab_members").assertIsDisplayed()
        composeRule.onNodeWithText("LOBBY FILL STATUS").assertIsDisplayed()
        composeRule.onNodeWithText("TOURNAMENT OVERVIEW").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("PRIZE BREAKDOWN").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("300 Pts").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("MATCH RULES & FAIR PLAY").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_join_tournament_button").performScrollTo().assertIsDisplayed()

        // Option 2: Switch to Joined Members on empty lobby (no fake members)
        composeRule.onNodeWithTag("match_tab_members").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("joined_members_empty_state").assertIsDisplayed()
        composeRule.onNodeWithText("No Members Joined Yet").assertIsDisplayed()

        // Switch back to Description cleanly
        composeRule.onNodeWithTag("match_tab_description").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("LOBBY FILL STATUS").assertIsDisplayed()

        // Close dialog and check ALL LOBBIES + Joined Room Vault + Real Joined Roster
        composeRule.onNodeWithTag("close_tournament_dialog").performClick()
        composeRule.onNodeWithTag("mode_back_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("mode_card_all").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("play_matches_list").performScrollToIndex(1)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tournament_cta_real_match_2").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Slot #4 Confirmed").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Room ID: ••••••••", substring = true).performScrollTo().assertIsDisplayed()

        // Switch to Joined Members tab -> verify real participants appear
        composeRule.onNodeWithTag("match_tab_members").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Joined (2)").assertIsDisplayed()
        composeRule.onNodeWithTag("joined_member_row_u2").assertIsDisplayed()
        composeRule.onNodeWithText("Rival_Viper").assertIsDisplayed()
        composeRule.onNodeWithTag("joined_member_row_u1").assertIsDisplayed()
        composeRule.onNodeWithText("YOU").assertIsDisplayed()
    }

    @Test
    fun joiningMatch_displaysCongratulationsPopupWithSlotAndActions() {
        val profile = PlayerProfile(
            userId = "u1",
            fullName = "Aarav Sharma",
            phone = "9876543210",
            freeFireUid = "1234567890",
            freeFireIgn = "TX_Aarav",
            walletDeposit = 100L,
            walletWinning = 50L,
        )
        val match1 = Tournament(
            id = "real_match_1",
            title = "TournaX Bermuda Cup",
            mode = "BR Solo",
            mapName = "Bermuda",
            dateTimeLabel = "Tonight • 9:00 PM IST",
            status = "upcoming",
            entryFee = 20L,
            prizePool = 500L,
            perKill = 5L,
            prizeFirst = 300L,
            prizeSecond = 120L,
            prizeThird = 80L,
            maxSlots = 48L,
            filledSlots = 12L,
            slotUnitLabel = "Players",
            rulesSummary = "Mobile only.",
            roomUnlocked = false,
        )
        var myTournamentsOpened = false

        composeRule.setContent {
            TournaXTheme {
                PlayTabScreen(
                    profile = profile,
                    tournaments = listOf(match1),
                    registrations = emptyList(),
                    isBusy = false,
                    onJoinTournament = { _, _, onJoined ->
                        onJoined(7L)
                    },
                    onGoToWallet = {},
                    onCopied = {},
                    onOpenMyTournaments = { myTournamentsOpened = true },
                )
            }
        }

        composeRule.waitForIdle()

        // Open featured tournament detail and click Join
        composeRule.onNodeWithTag("featured_tournament_banner").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_join_tournament_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Congratulations popup must appear with confirmed slot badge and quick actions
        composeRule.onNodeWithTag("match_joined_congrats_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("ENTRY CONFIRMED").assertIsDisplayed()
        composeRule.onNodeWithText("CONGRATULATIONS!").assertIsDisplayed()
        composeRule.onNodeWithTag("congrats_slot_badge").assertIsDisplayed()
        composeRule.onNodeWithText("SLOT #7").assertIsDisplayed()
        composeRule.onNodeWithText("TX_Aarav").performScrollTo().assertIsDisplayed()
        composeRule.onNode(
            hasText("500 Pts") and hasAnyAncestor(hasTestTag("match_joined_congrats_dialog")),
        ).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("congrats_view_room_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("congrats_my_matches_button").performScrollTo().assertIsDisplayed()

        // Tapping "View Room & Slot" closes congrats dialog and keeps tournament detail visible
        composeRule.onNodeWithTag("congrats_view_room_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("match_joined_congrats_dialog").assertDoesNotExist()
        composeRule.onNodeWithTag("match_tab_description").assertIsDisplayed()

        // Trigger join again and tap "Open My Tournaments"
        composeRule.onNodeWithTag("confirm_join_tournament_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("congrats_my_matches_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assert(myTournamentsOpened)
        composeRule.onNodeWithTag("match_joined_congrats_dialog").assertDoesNotExist()
    }

    @Test
    fun freeMatchesSection_displaysSquareCardAfterLoneWolfAndFiltersZeroEntryMatches() {
        val profile = PlayerProfile(
            userId = "u1",
            fullName = "Aarav Sharma",
            freeFireUid = "1234567890",
            freeFireIgn = "TX_Aarav",
        )
        val paidMatch = Tournament(
            id = "paid_match_1",
            title = "Paid Pro Scrim",
            mode = "BR Squad",
            mapName = "Bermuda",
            dateTimeLabel = "8:00 PM IST",
            status = "upcoming",
            entryFee = 25L,
            prizePool = 500L,
            maxSlots = 12L,
            filledSlots = 4L,
        )
        val freeMatch = Tournament(
            id = "free_match_1",
            title = "Free Sunday Showdown",
            mode = "CS 4v4",
            mapName = "Kalahari",
            dateTimeLabel = "9:30 PM IST",
            status = "upcoming",
            entryFee = 0L,
            prizePool = 250L,
            maxSlots = 16L,
            filledSlots = 6L,
        )

        composeRule.setContent {
            TournaXTheme {
                PlayTabScreen(
                    profile = profile,
                    tournaments = listOf(paidMatch, freeMatch),
                    registrations = emptyList(),
                    isBusy = false,
                    onJoinTournament = { _, _, _ -> },
                    onGoToWallet = {},
                    onCopied = {},
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("mode_card_lone_wolf").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("mode_card_free_matches").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("FREE MATCHES").assertIsDisplayed()
        composeRule.onNodeWithText("Zero Entry Fee").assertIsDisplayed()
        composeRule.onNodeWithTag("tournament_cta_free_match_1").assertIsDisplayed()
        composeRule.onNodeWithTag("tournament_cta_paid_match_1").assertDoesNotExist()
    }

    @Test
    fun clashSquadFiltersAndCancelledExclusion_behaveAccurately() {
        val profile = PlayerProfile(
            userId = "u1",
            fullName = "Aarav Sharma",
            freeFireUid = "1234567890",
            freeFireIgn = "TX_Aarav",
            squadUids = listOf("2222333344", "3333444455", "4444555566"),
            walletDeposit = 200L,
        )
        val cs1v1 = Tournament(
            id = "cs_1v1",
            title = "CS 1v1 Headshot Room",
            mode = "Clash Squad 1v1",
            mapName = "Bermuda",
            dateTimeLabel = "7:00 PM IST",
            status = "upcoming",
            entryFee = 10L,
            prizePool = 50L,
            maxSlots = 16L,
            filledSlots = 2L,
        )
        val cs4v4 = Tournament(
            id = "cs_4v4",
            title = "CS 4v4 Squad War",
            mode = "Clash Squad 4v4",
            mapName = "Kalahari",
            dateTimeLabel = "8:00 PM IST",
            status = "upcoming",
            entryFee = 20L,
            prizePool = 200L,
            maxSlots = 8L,
            filledSlots = 1L,
        )
        val cancelledMatch = Tournament(
            id = "cs_cancelled",
            title = "Cancelled Scrim",
            mode = "Clash Squad 4v4",
            mapName = "Bermuda",
            dateTimeLabel = "9:00 PM IST",
            status = "cancelled",
            entryFee = 10L,
            prizePool = 100L,
            maxSlots = 8L,
            filledSlots = 0L,
        )

        composeRule.setContent {
            TournaXTheme {
                PlayTabScreen(
                    profile = profile,
                    tournaments = listOf(cs1v1, cs4v4, cancelledMatch),
                    registrations = emptyList(),
                    isBusy = false,
                    onJoinTournament = { _, _, _ -> },
                    onGoToWallet = {},
                    onCopied = {},
                )
            }
        }

        composeRule.waitForIdle()
        // Cancelled match excluded from hub count (2 instead of 3)
        composeRule.onNodeWithText("2 Lobbies").assertIsDisplayed()

        // Open CS mode
        composeRule.onNodeWithTag("mode_card_cs").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Filter by "Squad" -> should show Clash Squad 4v4 and hide Clash Squad 1v1
        composeRule.onNodeWithText("Squad").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tournament_cta_cs_4v4").assertIsDisplayed()
        composeRule.onNodeWithTag("tournament_cta_cs_1v1").assertDoesNotExist()

        // Switch back to "All" and open Clash Squad 1v1 -> should NOT show teammate UID boxes
        composeRule.onNodeWithText("All").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tournament_cta_cs_1v1").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("teammate_uid_input_0").assertDoesNotExist()
        composeRule.onNodeWithTag("close_tournament_dialog").performClick()
        composeRule.waitForIdle()

        // Open Clash Squad 4v4 -> pre-fills teammate 1 even when captain UID was omitted from squadUids[0]
        composeRule.onNodeWithTag("tournament_cta_cs_4v4").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("teammate_uid_input_0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2222333344").assertIsDisplayed()
    }
}
