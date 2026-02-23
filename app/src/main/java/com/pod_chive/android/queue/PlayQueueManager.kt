package com.pod_chive.android.queue

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class QueueItem(
    val id: String, // Unique ID (could be timestamp + title hash)
    val title: String,
    val audioUrl: String,
    val photoUrl: String,
    val creator: String,
    val description: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)

class PlayQueueManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("podchive_queue", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val QUEUE_KEY = "play_queue"
    private val CURRENT_INDEX_KEY = "current_index"

    fun addToQueue(item: QueueItem) {
        val queue = getQueue().toMutableList()
        // Check if item already exists by ID or audioUrl (double check to prevent duplicates)
        if (queue.none { it.id == item.id || it.audioUrl == item.audioUrl }) {
            queue.add(item)
            saveQueue(queue)
        }
    }

    fun removeFromQueue(itemId: String) {
        val queue = getQueue().toMutableList()
        queue.removeAll { it.id == itemId }
        saveQueue(queue)
    }

    fun getQueue(): List<QueueItem> {
        return try {
            val jsonStr = prefs.getString(QUEUE_KEY, "[]") ?: "[]"
            json.decodeFromString<List<QueueItem>>(jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearQueue() {
        saveQueue(emptyList())
        setCurrentIndex(0)
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val queue = getQueue().toMutableList()
        if (fromIndex in queue.indices && toIndex in queue.indices) {
            val item = queue.removeAt(fromIndex)
            queue.add(toIndex, item)
            saveQueue(queue)
        }
    }

    fun moveToTop(itemId: String) {
        val queue = getQueue().toMutableList()
        val itemIndex = queue.indexOfFirst { it.id == itemId }
        if (itemIndex >= 0 && itemIndex > 0) {
            val item = queue.removeAt(itemIndex)
            queue.add(0, item)
            saveQueue(queue)
            setCurrentIndex(0)
        }
    }

    fun getCurrentIndex(): Int {
        return prefs.getInt(CURRENT_INDEX_KEY, 0)
    }

    fun setCurrentIndex(index: Int) {
        prefs.edit().putInt(CURRENT_INDEX_KEY, index).apply()
    }

    fun getNextItem(): QueueItem? {
        val queue = getQueue()
        val currentIndex = getCurrentIndex()
        val nextIndex = currentIndex + 1
        return if (nextIndex < queue.size) {
            setCurrentIndex(nextIndex)
            queue[nextIndex]
        } else null
    }

    fun getPreviousItem(): QueueItem? {
        val queue = getQueue()
        val currentIndex = getCurrentIndex()
        val prevIndex = currentIndex - 1
        return if (prevIndex >= 0) {
            setCurrentIndex(prevIndex)
            queue[prevIndex]
        } else null
    }

    fun getCurrentItem(): QueueItem? {
        val queue = getQueue()
        val currentIndex = getCurrentIndex()
        return if (currentIndex in queue.indices) queue[currentIndex] else null
    }

    fun removeCurrentItem() {
        val queue = getQueue().toMutableList()
        val currentIndex = getCurrentIndex()
        if (currentIndex in queue.indices) {
            queue.removeAt(currentIndex)
            saveQueue(queue)
            // Keep the same index, which now points to the next item
            // (or past the end if we removed the last item)
        }
    }

    fun removeByAudioUrl(audioUrl: String) {
        val queue = getQueue().toMutableList()
        queue.removeAll { it.audioUrl == audioUrl }
        saveQueue(queue)
    }

    private fun saveQueue(queue: List<QueueItem>) {
        try {
            val jsonString = json.encodeToString(queue)
            prefs.edit().putString(QUEUE_KEY, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun generateId(title: String, audioUrl: String): String {
            // Use only audioUrl hash for ID so the same episode always has the same ID
            // This prevents duplicates even if added at different times
            return "episode_${audioUrl.hashCode()}"
        }
    }
}




