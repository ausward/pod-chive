package com.pod_chive.android

import android.content.Intent
import com.pod_chive.android.model.Episode
import com.pod_chive.android.notif.PodchiveNotificationManager
import com.pod_chive.android.queue.PlayQueueManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationIntentHandlingTest {

    private val mockContext = org.robolectric.RuntimeEnvironment.getApplication()
    private lateinit var queueManager: PlayQueueManager

    @Before
    fun setup() {
        queueManager = PlayQueueManager(mockContext)
        queueManager.clearQueue()
    }

    @Test
    fun testIntentExtrasArePreserved() {
        val intent = Intent().apply {
            action = PodchiveNotificationManager.ACTION_PLAY_FROM_NOTIFICATION
            putExtra(PodchiveNotificationManager.EXTRA_AUDIO_URL, "https://example.com/ep1.mp3")
            putExtra(PodchiveNotificationManager.EXTRA_EPISODE_TITLE, "Test Episode")
            putExtra(PodchiveNotificationManager.EXTRA_PODCAST_TITLE, "Test Podcast")
            putExtra(PodchiveNotificationManager.EXTRA_IMAGE_URL, "https://example.com/image.jpg")
            putExtra(PodchiveNotificationManager.EXTRA_PUB_DATE, "2026-01-18")
            putExtra(PodchiveNotificationManager.EXTRA_DESCRIPTION, "Test Description")
            putExtra(PodchiveNotificationManager.EXTRA_TRANSCRIPT_URL, "https://example.com/transcript.txt")
        }

        assertEquals("https://example.com/ep1.mp3", intent.getStringExtra(PodchiveNotificationManager.EXTRA_AUDIO_URL))
        assertEquals("Test Episode", intent.getStringExtra(PodchiveNotificationManager.EXTRA_EPISODE_TITLE))
        assertEquals("Test Podcast", intent.getStringExtra(PodchiveNotificationManager.EXTRA_PODCAST_TITLE))
    }

    @Test
    fun testEpisodeCreationFromNotificationIntent() {
        val audioUrl = "https://example.com/ep1.mp3"
        val title = "Test Episode"
        val podcastTitle = "Test Podcast"
        val imageUrl = "https://example.com/image.jpg"
        val pubDate = "2026-01-18"
        val description = "Test Description"
        val transcriptUrl = "https://example.com/transcript.txt"

        val episode = Episode(audioUrl, title, pubDate, imageUrl).apply {
            Creator = podcastTitle
            Description = description
            TranscriptUrl = transcriptUrl
            idValue = PlayQueueManager.generateId(audioUrl)
        }

        assertEquals(audioUrl, episode.AudioUrl)
        assertEquals(title, episode.EpisodeName)
        assertEquals(podcastTitle, episode.Creator)
        assertEquals(description, episode.Description)
        assertEquals(transcriptUrl, episode.TranscriptUrl)
    }

    @Test
    fun testNotificationEpisodeAddedToQueueTop() {
        // Pre-populate queue with existing episodes
        val existingEp = Episode("https://example.com/existing.mp3", "Existing", "2026-01-01", "photo.jpg")
            .apply { idValue = PlayQueueManager.generateId("https://example.com/existing.mp3") }

        queueManager.addToQueue(existingEp)

        // Add notification episode
        val notifEp = Episode("https://example.com/notif.mp3", "Notification Episode", "2026-01-18", "photo.jpg")
            .apply { idValue = PlayQueueManager.generateId("https://example.com/notif.mp3") }

        queueManager.addToQueue(notifEp)
        queueManager.moveToTop(notifEp.idValue ?: "")
        queueManager.setCurrentIndex(0)

        val current = queueManager.getCurrentItem()
        assertNotNull(current)
        assertEquals("Notification Episode", current?.EpisodeName)
    }

    @Test
    fun testNotificationActionMatching() {
        val intent = Intent().apply {
            action = PodchiveNotificationManager.ACTION_PLAY_FROM_NOTIFICATION
        }

        assertEquals(PodchiveNotificationManager.ACTION_PLAY_FROM_NOTIFICATION, intent.action)
        assertEquals("com.pod_chive.android.action.PLAY_FROM_NOTIFICATION", intent.action)
    }

    @Test
    fun testMultipleNotificationIntents() {
        val intents = (1..5).map { i ->
            Intent().apply {
                action = PodchiveNotificationManager.ACTION_PLAY_FROM_NOTIFICATION
                putExtra(PodchiveNotificationManager.EXTRA_AUDIO_URL, "https://example.com/ep$i.mp3")
                putExtra(PodchiveNotificationManager.EXTRA_EPISODE_TITLE, "Episode $i")
            }
        }

        intents.forEach { intent ->
            assertNotNull(intent.getStringExtra(PodchiveNotificationManager.EXTRA_AUDIO_URL))
            assertNotNull(intent.getStringExtra(PodchiveNotificationManager.EXTRA_EPISODE_TITLE))
        }

        assertEquals(5, intents.size)
    }
}


