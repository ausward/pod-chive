package com.pod_chive.android.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EpisodeTest {

    private lateinit var episode: Episode

    @Before
    fun setup() {
        episode = Episode(
            "https://example.com/audio.mp3",
            "Test Episode",
            "2026-01-01",
            "https://example.com/photo.jpg"
        )
    }

    @Test
    fun testEpisodeConstruction() {
        assertEquals("https://example.com/audio.mp3", episode.AudioUrl)
        assertEquals("Test Episode", episode.EpisodeName)
        assertEquals("2026-01-01", episode.PublishDate)
        assertEquals("https://example.com/photo.jpg", episode.PhotoUrl)
    }

    @Test
    fun testEpisodeConstructionWithFullFields() {
        val episode = Episode(
            title = "Test",
            description = "Description",
            audioFilePath = "https://example.com/audio.mp3",
            pubDate = "2026-01-01",
            transcript = "transcript url",
            creator = "Creator",
            PhotoUrl = "photo.jpg"
        )

        assertEquals("Test", episode.EpisodeName)
        assertEquals("Description", episode.Description)
        assertEquals("https://example.com/audio.mp3", episode.AudioUrl)
        assertEquals("2026-01-01", episode.PublishDate)
        assertEquals("transcript url", episode.TranscriptUrl)
        assertEquals("Creator", episode.Creator)
        assertEquals("photo.jpg", episode.PhotoUrl)
    }

    @Test
    fun testToPlayEpisode() {
        val playEpisode = episode.toPlayEpisode()
        assertNotNull(playEpisode)
        assertEquals(episode, playEpisode.EpisodeObj)
    }

    @Test
    fun testToString() {
        val str = episode.toString()
        assertNotNull(str)
        assertTrue(str.contains("Test Episode"))
    }

    @Test
    fun testEpisodeIdValue() {
        episode.idValue = "ep123"
        assertEquals("ep123", episode.idValue)
    }

    @Test
    fun testEpisodeDuration() {
        episode.duration = 3600000L
        assertEquals(3600000L, episode.duration)
    }

    @Test
    fun testEpisodeTranscriptData() {
        episode.TranscriptData = "Transcript content here"
        assertEquals("Transcript content here", episode.TranscriptData)
    }

    @Test
    fun testPodcastShowConstruction() {
        val show = PodcastShow("Test Podcast", "https://example.com/feed", "https://example.com/image.jpg")
        assertEquals("Test Podcast", show.PodcastName)
        assertEquals("https://example.com/feed", show.PodcastUrl)
        assertEquals("https://example.com/image.jpg", show.Cover_Image)
    }

    @Test
    fun testPodcastShowFullConstruction() {
        val show = PodcastShow(
            "Test Podcast",
            "https://example.com/feed",
            "https://example.com/image.jpg",
            "test_directory",
            true,
            "Test Description",
            "Test Creator"
        )

        assertEquals("Test Podcast", show.PodcastName)
        assertEquals("https://example.com/feed", show.PodcastUrl)
        assertEquals("test_directory", show.outputDirectory)
        assertEquals(true, show.isRSS)
        assertEquals("Test Description", show.showDescription)
        assertEquals("Test Creator", show.creator)
    }
}

