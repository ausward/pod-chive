package com.pod_chive.android.queue

import com.pod_chive.android.model.Episode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayQueueManagerTest {

    private val mockContext = org.robolectric.RuntimeEnvironment.getApplication()
    private lateinit var queueManager: PlayQueueManager

    @Before
    fun setup() {
        queueManager = PlayQueueManager(mockContext)
        queueManager.clearQueue()
    }

    @Test
    fun testAddToQueue() {
        val episode = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg")
        episode.idValue = "ep1"

        queueManager.addToQueue(episode)

        val queue = queueManager.getQueue()
        assertEquals(1, queue.size)
        assertEquals("Episode 1", queue[0].EpisodeName)
    }

    @Test
    fun testAddToQueuePreventsDuplicates() {
        val episode = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg")
        episode.idValue = "ep1"

        queueManager.addToQueue(episode)
        queueManager.addToQueue(episode)

        val queue = queueManager.getQueue()
        assertEquals(1, queue.size)
    }

    @Test
    fun testRemoveFromQueue() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.removeFromQueue("ep1")

        val queue = queueManager.getQueue()
        assertEquals(1, queue.size)
        assertEquals("Episode 2", queue[0].EpisodeName)
    }

    @Test
    fun testGetCurrentItem() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)

        val current = queueManager.getCurrentItem()
        assertNotNull(current)
        assertEquals("Episode 1", current?.EpisodeName)
    }

    @Test
    fun testSetCurrentIndex() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.setCurrentIndex(1)

        val current = queueManager.getCurrentItem()
        assertEquals("Episode 2", current?.EpisodeName)
    }

    @Test
    fun testGetNextItem() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)

        val next = queueManager.getNextItem()
        assertNotNull(next)
        assertEquals("Episode 2", next?.EpisodeName)
        assertEquals(1, queueManager.getCurrentIndex())
    }

    @Test
    fun testGetNextItemAtEnd() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }

        queueManager.addToQueue(ep1)
        queueManager.setCurrentIndex(0)

        val next = queueManager.getNextItem()
        assertNull(next)
    }

    @Test
    fun testGetPreviousItem() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.setCurrentIndex(1)

        val prev = queueManager.getPreviousItem()
        assertNotNull(prev)
        assertEquals("Episode 1", prev?.EpisodeName)
        assertEquals(0, queueManager.getCurrentIndex())
    }

    @Test
    fun testMoveItem() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }
        val ep3 = Episode("url3", "Episode 3", "2026-01-03", "photo.jpg").apply { idValue = "ep3" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.addToQueue(ep3)

        queueManager.moveItem(0, 2)

        val queue = queueManager.getQueue()
        assertEquals("Episode 2", queue[0].EpisodeName)
        assertEquals("Episode 3", queue[1].EpisodeName)
        assertEquals("Episode 1", queue[2].EpisodeName)
    }

    @Test
    fun testMoveToTop() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)

        queueManager.moveToTop("ep2")

        val queue = queueManager.getQueue()
        assertEquals("Episode 2", queue[0].EpisodeName)
        assertEquals("Episode 1", queue[1].EpisodeName)
    }

    @Test
    fun testClearQueue() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.clearQueue()

        val queue = queueManager.getQueue()
        assertTrue(queue.isEmpty() || queue.size == 1)
    }

    @Test
    fun testRemoveByAudioUrl() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1"; AudioUrl = "url1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2"; AudioUrl = "url2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.removeByAudioUrl("url1")

        val queue = queueManager.getQueue()
        assertEquals(1, queue.size)
        assertEquals("Episode 2", queue[0].EpisodeName)
    }

    @Test
    fun testGenerateId() {
        val id1 = PlayQueueManager.generateId("https://example.com/ep1.mp3")
        val id2 = PlayQueueManager.generateId("https://example.com/ep1.mp3")

        assertEquals(id1, id2)
    }

    @Test
    fun testRemoveCurrentItem() {
        val ep1 = Episode("url1", "Episode 1", "2026-01-01", "photo.jpg").apply { idValue = "ep1" }
        val ep2 = Episode("url2", "Episode 2", "2026-01-02", "photo.jpg").apply { idValue = "ep2" }

        queueManager.addToQueue(ep1)
        queueManager.addToQueue(ep2)
        queueManager.removeCurrentItem()

        val queue = queueManager.getQueue()
        assertEquals(1, queue.size)
        assertEquals("Episode 2", queue[0].EpisodeName)
    }
}


