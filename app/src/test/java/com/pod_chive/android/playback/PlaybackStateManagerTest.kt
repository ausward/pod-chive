package com.pod_chive.android.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackStateManagerTest {

    private val mockContext = org.robolectric.RuntimeEnvironment.getApplication()
    private lateinit var manager: PlaybackStateManager

    @Before
    fun setup() {
        manager = PlaybackStateManager(mockContext)
    }

    @Test
    fun testSavePlaybackState() {
        val state = PlaybackState(
            audioUrl = "https://example.com/episode.mp3",
            title = "Test Episode",
            creator = "Test Creator",
            photoUrl = "https://example.com/photo.jpg",
            currentPosition = 30000,
            duration = 3600000,
            playbackSpeed = 1.5f
        )

        manager.savePlaybackState(state)

        val retrieved = manager.getPlaybackState("https://example.com/episode.mp3")
        assertNotNull(retrieved)
        assertEquals("Test Episode", retrieved?.title)
        assertEquals(30000L, retrieved?.currentPosition)
        assertEquals(1.5f, retrieved?.playbackSpeed)
    }

    @Test
    fun testGetNonexistentPlaybackState() {
        val retrieved = manager.getPlaybackState("https://example.com/nonexistent.mp3")
        assertEquals(null, retrieved)
    }

    @Test
    fun testOverwritePlaybackState() {
        val state1 = PlaybackState(
            audioUrl = "https://example.com/episode.mp3",
            title = "Episode 1",
            creator = "Creator",
            photoUrl = "photo.jpg",
            currentPosition = 10000,
            duration = 3600000,
            playbackSpeed = 1.0f
        )
        val state2 = PlaybackState(
            audioUrl = "https://example.com/episode.mp3",
            title = "Episode 1",
            creator = "Creator",
            photoUrl = "photo.jpg",
            currentPosition = 50000,
            duration = 3600000,
            playbackSpeed = 2.0f
        )

        manager.savePlaybackState(state1)
        manager.savePlaybackState(state2)

        val retrieved = manager.getPlaybackState("https://example.com/episode.mp3")
        assertEquals(50000L, retrieved?.currentPosition)
        assertEquals(2.0f, retrieved?.playbackSpeed)
    }

    @Test
    fun testGetPlayingEpisode() {
        val state = PlaybackState(
            audioUrl = "https://example.com/episode.mp3",
            title = "Test Episode",
            creator = "Test Creator",
            photoUrl = "https://example.com/photo.jpg",
            currentPosition = 30000,
            duration = 3600000,
            playbackSpeed = 1.5f
        )

        manager.savePlaybackState(state)
        val episode = manager.getPlayingEpisode()

        assertNotNull(episode)
        assertEquals("Test Episode", episode?.title)
        assertEquals("Test Creator", episode?.creator)
    }

    @Test
    fun testPlaybackStateWithZeroDuration() {
        val state = PlaybackState(
            audioUrl = "https://example.com/episode.mp3",
            title = "Test Episode",
            creator = "Test Creator",
            photoUrl = "photo.jpg",
            currentPosition = 0,
            duration = 0,
            playbackSpeed = 1.0f
        )

        manager.savePlaybackState(state)
        val retrieved = manager.getPlaybackState("https://example.com/episode.mp3")

        assertNotNull(retrieved)
        assertEquals(0L, retrieved?.duration)
    }

    @Test
    fun testPlaybackSpeedPersistence() {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

        speeds.forEach { speed ->
            val state = PlaybackState(
                audioUrl = "https://example.com/ep_$speed.mp3",
                title = "Episode at $speed speed",
                creator = "Creator",
                photoUrl = "photo.jpg",
                currentPosition = 5000,
                duration = 3600000,
                playbackSpeed = speed
            )
            manager.savePlaybackState(state)

            val retrieved = manager.getPlaybackState("https://example.com/ep_$speed.mp3")
            assertEquals(speed, retrieved?.playbackSpeed)
        }
    }
}


