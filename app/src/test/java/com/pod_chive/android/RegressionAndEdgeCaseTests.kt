package com.pod_chive.android

import com.pod_chive.android.model.Episode
import com.pod_chive.android.queue.PlayQueueManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RegressionAndEdgeCaseTests {

    private val mockContext = org.robolectric.RuntimeEnvironment.getApplication()
    private lateinit var queueManager: PlayQueueManager

    @Before
    fun setup() {
        queueManager = PlayQueueManager(mockContext)
        queueManager.clearQueue()
    }

    @Test
    fun testEmptyQueueHandling() {
        queueManager.clearQueue()
        val current = queueManager.getCurrentItem()

        // Queue is either empty or contains only current item
        val queue = queueManager.getQueue()
        assertTrue(queue.isEmpty() || queue.size == 1)
    }

    @Test
    fun testGetNextItemOnEmptyQueueReturnsNull() {
        queueManager.clearQueue()
        val next = queueManager.getNextItem()
        assertNull(next)
    }

    @Test
    fun testGetPreviousItemAtIndexZeroReturnsNull() {
        val ep = Episode("url", "Episode", "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1" }

        queueManager.addToQueue(ep)
        queueManager.setCurrentIndex(0)

        val prev = queueManager.getPreviousItem()
        assertNull(prev)
    }

    @Test
    fun testNullAudioUrlHandling() {
        val ep = Episode("url", "Episode", "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1"; AudioUrl = null }

        queueManager.addToQueue(ep)


        // Should not crash and queue should remain
        val queue = queueManager.getQueue()
        assertTrue(queue.isNotEmpty())
    }

    @Test
    fun testRemoveNonexistentItemFromQueue() {
        val ep = Episode("url", "Episode", "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1" }

        queueManager.addToQueue(ep)
        queueManager.removeFromQueue("nonexistent_id")

        // Queue should remain unchanged
        val queue = queueManager.getQueue()
        assertEquals(1, queue.size)
    }

    @Test
    fun testMoveItemOutOfBounds() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg")
            .apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)

        // Try to move to invalid index
        queueManager.moveItem(0, 10)

        // Should not move and queue should remain unchanged
        val queue = queueManager.getQueue()
        assertEquals(2, queue.size)
        assertEquals("Episode 1", queue[0].EpisodeName)
    }

    @Test
    fun testMoveItemToSamePosition() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg")
            .apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)

        queueManager.moveItem(0, 0)

        val queue = queueManager.getQueue()
        assertEquals(2, queue.size)
        assertEquals("Episode 1", queue[0].EpisodeName)
    }

    @Test
    fun testDuplicateIdGeneration() {
        val id1 = PlayQueueManager.generateId("https://example.com/episode.mp3")
        val id2 = PlayQueueManager.generateId("https://example.com/episode.mp3")

        // Same URL should always generate same ID
        assertEquals(id1, id2)
    }

    @Test
    fun testUniqueIdGeneration() {
        val id1 = PlayQueueManager.generateId("https://example.com/episode1.mp3")
        val id2 = PlayQueueManager.generateId("https://example.com/episode2.mp3")

        // Different URLs should generate different IDs
        assertTrue(id1 != id2)
    }

    @Test
    fun testLargeQueueHandling() {
        // Add 100 episodes to queue
        for (i in 1..100) {
            val ep = Episode("url$i", "Episode $i", "2026-01-$i", "photo.jpg")
                .apply { idValue = "ep$i" }
            queueManager.addToQueue(ep)
        }

        val queue = queueManager.getQueue()
        assertEquals(100, queue.size)

        // Verify first and last
        assertEquals("Episode 1", queue[0].EpisodeName)
        assertEquals("Episode 100", queue[99].EpisodeName)
    }

    @Test
    fun testCurrentIndexBoundaryConditions() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg")
            .apply { idValue = "ep2" }
        val ep3 = Episode("url3", "Episode 3", "2026-01-03", "photo.jpg")
            .apply { idValue = "ep3" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.addToQueue(ep3)

        // Set index beyond bounds
        queueManager.setCurrentIndex(100)

        val current = queueManager.getCurrentItem()
        // Should either be null or last item
        assertTrue(current == null || current?.EpisodeName == "Episode 3")
    }

    @Test
    fun testRemoveCurrentItemUpdatesFocus() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg")
            .apply { idValue = "ep2" }
        val ep3 = Episode("url3", "Episode 3", "2026-01-03", "photo.jpg")
            .apply { idValue = "ep3" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.addToQueue(ep3)

        queueManager.setCurrentIndex(1)
        assertEquals("Episode 2", queueManager.getCurrentItem()?.EpisodeName)

        queueManager.removeCurrentItem()

        val queue = queueManager.getQueue()
        assertEquals(2, queue.size)
    }

    @Test
    fun testEpisodeWithSpecialCharacters() {
        val episodeTitle = "Episode: \"Special\" & <Characters>"
        val ep = Episode("url", episodeTitle, "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1" }

        queueManager.addToQueue(ep)

        val queue = queueManager.getQueue()
        assertEquals(episodeTitle, queue[0].EpisodeName)
    }

    @Test
    fun testQueueSerializationAndDeserialization() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg")
            .apply { idValue = "ep1"; AudioUrl = "url1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg")
            .apply { idValue = "ep2"; AudioUrl = "url2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.setCurrentIndex(1)

        // Get queue (internally serialized and deserialized)
        val retrieved = queueManager.getQueue()
        val index = queueManager.getCurrentIndex()

        assertEquals(2, retrieved.size)
        assertEquals(1, index)
        assertEquals("Episode 2", retrieved[index].EpisodeName)
    }
}


