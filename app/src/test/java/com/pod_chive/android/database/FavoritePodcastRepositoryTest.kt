package com.pod_chive.android.database

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoritePodcastRepositoryTest {

    private val mockContext = org.robolectric.RuntimeEnvironment.getApplication()
    private lateinit var repository: FavoritePodcastRepository

    @Before
    fun setup() {
        repository = FavoritePodcastRepository(mockContext)
    }

    @Test
    fun testInsertAndGetFavorite() = runBlocking {
        val favorite = FavoritePodcast(
            feedLink = "https://example.com/feed.xml",
            imageLocation = "https://example.com/image.jpg",
            description = "Test Podcast",
            title = "Test Podcast"
        )

        repository.insertFavorite(favorite)
        val retrieved = repository.getFavoriteByFeedLink("https://example.com/feed.xml")

        assertEquals("Test Podcast", retrieved?.title)
        assertEquals("https://example.com/feed.xml", retrieved?.feedLink)
    }

    @Test
    fun testDeleteFavorite() = runBlocking {
        val favorite = FavoritePodcast(
            feedLink = "https://example.com/feed.xml",
            imageLocation = "https://example.com/image.jpg",
            description = "Test Podcast",
            title = "Test Podcast"
        )

        repository.insertFavorite(favorite)
        repository.deleteFavorite(favorite)

        val retrieved = repository.getFavoriteByFeedLink("https://example.com/feed.xml")
        assertEquals(null, retrieved)
    }

    @Test
    fun testGetAllFavorites() = runBlocking {
        val fav1 = FavoritePodcast(
            feedLink = "https://example.com/feed1.xml",
            imageLocation = "image1.jpg",
            description = "Podcast 1",
            title = "Podcast 1"
        )
        val fav2 = FavoritePodcast(
            feedLink = "https://example.com/feed2.xml",
            imageLocation = "image2.jpg",
            description = "Podcast 2",
            title = "Podcast 2"
        )

        repository.insertFavorite(fav1)
        repository.insertFavorite(fav2)
        val all = repository.getAllFavorites()

        assertTrue(all.size >= 2)
        assertTrue(all.any { it.feedLink == "https://example.com/feed1.xml" })
        assertTrue(all.any { it.feedLink == "https://example.com/feed2.xml" })
    }

    @Test
    fun testIsFavorite() = runBlocking {
        val favorite = FavoritePodcast(
            feedLink = "https://example.com/feed.xml",
            imageLocation = "image.jpg",
            description = "Test",
            title = "Test"
        )

        repository.insertFavorite(favorite)

        assertTrue(repository.isFavorite("https://example.com/feed.xml"))
        assertFalse(repository.isFavorite("https://example.com/nonexistent.xml"))
    }

    @Test
    fun testFavoriteSorting() = runBlocking {
        val fav1 = FavoritePodcast(
            feedLink = "https://example.com/z.xml",
            imageLocation = "image.jpg",
            description = "Z Podcast",
            title = "Z Podcast"
        )
        val fav2 = FavoritePodcast(
            feedLink = "https://example.com/a.xml",
            imageLocation = "image.jpg",
            description = "A Podcast",
            title = "A Podcast"
        )

        repository.insertFavorite(fav1)
        repository.insertFavorite(fav2)
        val all = repository.getAllFavorites()

        val titles = all.map { it.title }
        assertTrue(titles.indexOf("A Podcast") < titles.indexOf("Z Podcast"))
    }
}


