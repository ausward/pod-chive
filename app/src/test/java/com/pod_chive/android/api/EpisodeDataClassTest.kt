package com.pod_chive.android.api

import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeDataClassTest {

    @Test
    fun testEpisodeDCConstruction() {
        val episode = EpisodeDC(
            title = "Test Episode",
            description = "Test Description",
            audioFilePath = "https://example.com/audio.mp3",
            pubDate = "2026-01-01",
            transcript = "transcript.txt",
            creator = "Test Creator",
            photo = "https://example.com/photo.jpg"
        )

        assertEquals("Test Episode", episode.title)
        assertEquals("Test Description", episode.description)
        assertEquals("https://example.com/audio.mp3", episode.audioFilePath)
        assertEquals("2026-01-01", episode.pubDate)
        assertEquals("Test Creator", episode.creator)
        assertEquals("https://example.com/photo.jpg", episode.photo)
    }

    @Test
    fun testEpisodeDCWithNullValues() {
        val episode = EpisodeDC(
            title = "Test",
            description = null,
            audioFilePath = "https://example.com/audio.mp3",
            pubDate = "2026-01-01",
            transcript = null,
            creator = null,
            photo = null
        )

        assertEquals("Test", episode.title)
        assertEquals(null, episode.description)
        assertEquals(null, episode.creator)
        assertEquals(null, episode.photo)
    }

    @Test
    fun testPodcastDetailResponseConstruction() {
        val episodes = listOf(
            EpisodeDC(
                "Ep1", "Desc1", "url1", "2026-01-01", null, "Creator", "photo1"
            ),
            EpisodeDC(
                "Ep2", "Desc2", "url2", "2026-01-02", null, "Creator", "photo2"
            )
        )

        val response = PodcastDetailResponse(
            podcastTitle = "Test Podcast",
            podcastDescription = "Podcast Description",
            episodeDCS = episodes
        )

        assertEquals("Test Podcast", response.podcastTitle)
        assertEquals(2, response.episodeDCS.size)
    }

    @Test
    fun testPodcastDetailResponsePlaceCreatorData() {
        val ep1 = EpisodeDC("Ep1", "Desc1", "url1", "2026-01-01", null, null, "photo1")
        val ep2 = EpisodeDC("Ep2", "Desc2", "url2", "2026-01-02", null, "Creator", "photo2")

        val response = PodcastDetailResponse(
            podcastTitle = "Test Podcast",
            podcastDescription = "Podcast Description",
            episodeDCS = mutableListOf(ep1, ep2)
        )

        response.placeCreatorData()

        assertEquals("Test Podcast", response.episodeDCS[0].creator)
        assertEquals("Creator", response.episodeDCS[1].creator)
    }

    @Test
    fun testHomeItemConstruction() {
        val item = homeItem(
            podcast_title = "Test Podcast",
            description = "Test Description",
            rss_url = "https://example.com/feed.xml",
            output_directory = "test_directory",
            cover_image_url = "https://example.com/cover.jpg"
        )

        assertEquals("Test Podcast", item.podcast_title)
        assertEquals("Test Description", item.description)
        assertEquals("https://example.com/feed.xml", item.rss_url)
        assertEquals("test_directory", item.output_directory)
        assertEquals("https://example.com/cover.jpg", item.cover_image_url)
    }

    @Test
    fun testHomeItemWithNullImage() {
        val item = homeItem(
            podcast_title = "Test Podcast",
            description = "Test Description",
            rss_url = "https://example.com/feed.xml",
            output_directory = "test_directory",
            cover_image_url = null
        )

        assertEquals(null, item.cover_image_url)
    }
}

