package com.pod_chive.android.queue

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.pod_chive.android.model.Episode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
//
//@Serializable
//data class QueueItem(
//    val id: String, // Unique ID (could be timestamp + title hash)
//    val title: String,
//    val audioUrl: String,
//    val photoUrl: String,
//    val creator: String,
//    val description: String? = null,
//    val addedAt: Long = System.currentTimeMillis(),
//    val transcript: String? = null,
//    val publishDate: String? = null
//): Episode(title, description, audioUrl, publishDate?:"", transcript, creator, photoUrl, id)


class PlayQueueManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("podchive_queue", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val QUEUE_KEY = "play_queue"
    private val CURRENT_INDEX_KEY = "current_index"

    fun addToQueue(item: Episode) {
        val queue = getQueue().toMutableList()
        // Check if item already exists by ID or audioUrl (double check to prevent duplicates)
        if (queue.none { it.idValue == item.idValue || it.AudioUrl == item.AudioUrl }) {
            queue.add(item)
            saveQueue(queue)
        }
    }

    fun removeFromQueue(itemId: String) {
        val queue = getQueue().toMutableList()
        val currentIndex = getCurrentIndex()
        val removedIndex = queue.indexOfFirst { it.idValue == itemId }
        if (removedIndex == -1) return

        queue.removeAt(removedIndex)
        saveQueue(queue)

        val newIndex = when {
            queue.isEmpty() -> 0
            removedIndex < currentIndex -> currentIndex - 1
            currentIndex >= queue.size -> queue.lastIndex
            else -> currentIndex
        }
        setCurrentIndex(newIndex.coerceAtLeast(0))
    }

    fun getQueue(): List<Episode> {
        return try {
            val jsonStr = prefs.getString(QUEUE_KEY, "[]") ?: "[]"
            json.decodeFromString<List<Episode>>(jsonStr)
        } catch (e: Exception) {
            Log.e("PlayQueueManager", "Error getting queue", e)
            emptyList()
        }
    }

    fun clearQueue() {
        saveQueue(emptyList())
        setCurrentIndex(0)
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val queue = getQueue().toMutableList()
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return

        val item = queue.removeAt(fromIndex)
        queue.add(toIndex, item)
        saveQueue(queue)

        val currentIndex = getCurrentIndex()
        val newCurrentIndex = when {
            currentIndex == fromIndex -> toIndex
            fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex - 1
            fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex + 1
            else -> currentIndex
        }
        setCurrentIndex(newCurrentIndex.coerceIn(0, queue.lastIndex))
    }

    fun moveToTop(itemId: String) {
        val queue = getQueue().toMutableList()
        val itemIndex = queue.indexOfFirst { it.idValue == itemId }
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
        prefs.edit { putInt(CURRENT_INDEX_KEY, index) }
    }

    fun getNextItem(): Episode? {
        val queue = getQueue()
        val currentIndex = getCurrentIndex()
        val nextIndex = currentIndex + 1
        return if (nextIndex < queue.size) {
            setCurrentIndex(nextIndex)
            queue[nextIndex]
        } else null
    }

    fun getPreviousItem(): Episode? {
        val queue = getQueue()
        val currentIndex = getCurrentIndex()
        val prevIndex = currentIndex - 1
        return if (prevIndex >= 0) {
            setCurrentIndex(prevIndex)
            queue[prevIndex]
        } else null
    }

    fun getCurrentItem(): Episode? {
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

            val newIndex = when {
                queue.isEmpty() -> 0
                currentIndex >= queue.size -> queue.lastIndex
                else -> currentIndex
            }
            setCurrentIndex(newIndex)
        }
    }

    fun removeByAudioUrl(audioUrl: String) {
        val queue = getQueue().toMutableList()
        val currentIndex = getCurrentIndex()
        val removedBeforeOrAtCurrent = queue.withIndex()
            .filter { it.value.AudioUrl == audioUrl && it.index <= currentIndex }
            .size

        queue.removeAll { it.AudioUrl == audioUrl }
        saveQueue(queue)

        val newIndex = when {
            queue.isEmpty() -> 0
            removedBeforeOrAtCurrent == 0 -> currentIndex.coerceAtMost(queue.lastIndex)
            else -> (currentIndex - removedBeforeOrAtCurrent).coerceIn(0, queue.lastIndex)
        }
        setCurrentIndex(newIndex)
    }

    private fun saveQueue(queue: List<Episode>) {
        try {
            val jsonString = json.encodeToString(queue)
            prefs.edit { putString(QUEUE_KEY, jsonString) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun generateId( audioUrl: String): String {
            // Use only audioUrl hash for ID so the same episode always has the same ID
            // This prevents duplicates even if added at different times
            return "episode_${audioUrl.hashCode()}"
        }
    }
}
