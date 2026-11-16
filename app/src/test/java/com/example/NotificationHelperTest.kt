package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.notifications.TournaXNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
}
