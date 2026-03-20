package com.pod_chive.android.notif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationManagerConstantsTest {

    @Test
    fun testActionPlayFromNotificationConstant() {
        assertEquals("com.pod_chive.android.action.PLAY_FROM_NOTIFICATION", PodchiveNotificationManager.ACTION_PLAY_FROM_NOTIFICATION)
    }

    @Test
    fun testExtraConstantsExist() {
        assertNotNull(PodchiveNotificationManager.EXTRA_AUDIO_URL)
        assertNotNull(PodchiveNotificationManager.EXTRA_EPISODE_TITLE)
        assertNotNull(PodchiveNotificationManager.EXTRA_PODCAST_TITLE)
        assertNotNull(PodchiveNotificationManager.EXTRA_IMAGE_URL)
        assertNotNull(PodchiveNotificationManager.EXTRA_PUB_DATE)
        assertNotNull(PodchiveNotificationManager.EXTRA_DESCRIPTION)
        assertNotNull(PodchiveNotificationManager.EXTRA_TRANSCRIPT_URL)
    }

    @Test
    fun testIntentExtrasMapping() {
        // Verify all constant strings are defined and non-empty
        assertTrue(PodchiveNotificationManager.EXTRA_AUDIO_URL.isNotEmpty())
        assertTrue(PodchiveNotificationManager.EXTRA_EPISODE_TITLE.isNotEmpty())
        assertTrue(PodchiveNotificationManager.EXTRA_PODCAST_TITLE.isNotEmpty())
        assertTrue(PodchiveNotificationManager.EXTRA_IMAGE_URL.isNotEmpty())
        assertTrue(PodchiveNotificationManager.EXTRA_PUB_DATE.isNotEmpty())
        assertTrue(PodchiveNotificationManager.EXTRA_DESCRIPTION.isNotEmpty())
        assertTrue(PodchiveNotificationManager.EXTRA_TRANSCRIPT_URL.isNotEmpty())

        // Verify constants are unique
        val constants = listOf(
            PodchiveNotificationManager.EXTRA_AUDIO_URL,
            PodchiveNotificationManager.EXTRA_EPISODE_TITLE,
            PodchiveNotificationManager.EXTRA_PODCAST_TITLE,
            PodchiveNotificationManager.EXTRA_IMAGE_URL,
            PodchiveNotificationManager.EXTRA_PUB_DATE,
            PodchiveNotificationManager.EXTRA_DESCRIPTION,
            PodchiveNotificationManager.EXTRA_TRANSCRIPT_URL
        )
        assertEquals(constants.size, constants.distinct().size)
    }
}
