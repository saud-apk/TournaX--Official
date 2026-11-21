package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.TournaXNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationHelperTest {

    @Test
    fun createNotificationChannels_registersArenaAndWalletHighPriorityChannels() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        TournaXNotificationHelper.createNotificationChannels(context)

        val manager = context.getSystemService(NotificationManager::class.java)
        assertNotNull(manager)
        val arenaChannel = manager.getNotificationChannel(TournaXNotificationHelper.CHANNEL_ARENA_ALERTS)
        val walletChannel = manager.getNotificationChannel(TournaXNotificationHelper.CHANNEL_WALLET_PAYOUTS)

        assertNotNull(arenaChannel)
        assertNotNull(walletChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, arenaChannel.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, walletChannel.importance)
    }

    @Test
    fun showPushNotification_attachesDeepLinkCategoryAndDistinctPendingIntents() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val manager = app.getSystemService(NotificationManager::class.java)
        val shadowManager = Shadows.shadowOf(manager)

        TournaXNotificationHelper.showPushNotification(
            context = app,
            title = "Deposit Approved",
            body = "₹100 credited",
            category = "wallet",
        )
        TournaXNotificationHelper.showPushNotification(
            context = app,
            title = "Room Dropped",
            body = "Join Room #1234",
            category = "room",
        )

        val posted = shadowManager.allNotifications
        assertEquals(2, posted.size)
        val firstIntent = Shadows.shadowOf(posted[0].contentIntent).savedIntent
        val secondIntent = Shadows.shadowOf(posted[1].contentIntent).savedIntent
        assertEquals("wallet", firstIntent.getStringExtra(TournaXNotificationHelper.EXTRA_NOTIFICATION_CATEGORY))
        assertEquals("room", secondIntent.getStringExtra(TournaXNotificationHelper.EXTRA_NOTIFICATION_CATEGORY))
        assertNotEquals(posted[0].contentIntent, posted[1].contentIntent)
    }
}
