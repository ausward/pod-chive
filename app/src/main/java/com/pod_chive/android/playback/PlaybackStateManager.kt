package com.pod_chive.android.playback

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class PlaybackState(
    val audioUrl: String,
    val title: String,
    val creator: String,
    val photoUrl: String,
    val currentPosition: Long,
    val duration: Long,
    val lastPlayedAt: Long = System.currentTimeMillis()
)

class PlaybackStateManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("podchive_playback", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val PLAYBACK_STATES_KEY = "playback_states"

    fun savePlaybackState(state: PlaybackState) {
        try {
            val states = getAllPlaybackStates().toMutableMap()
            states[state.audioUrl] = state
            val jsonString = json.encodeToString(states)
            prefs.edit().putString(PLAYBACK_STATES_KEY, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPlaybackState(audioUrl: String): PlaybackState? {
        return try {
            val jsonStr = prefs.getString(PLAYBACK_STATES_KEY, "{}") ?: "{}"
            val states = json.decodeFromString<Map<String, PlaybackState>>(jsonStr)
            states[audioUrl]
        } catch (e: Exception) {
            null
        }
    }

    fun getAllPlaybackStates(): Map<String, PlaybackState> {
        return try {
            val jsonStr = prefs.getString(PLAYBACK_STATES_KEY, "{}") ?: "{}"
            json.decodeFromString<Map<String, PlaybackState>>(jsonStr)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun removePlaybackState(audioUrl: String) {
        try {
            val states = getAllPlaybackStates().toMutableMap()
            states.remove(audioUrl)
            val jsonString = json.encodeToString(states)
            prefs.edit().putString(PLAYBACK_STATES_KEY, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearAllPlaybackStates() {
        prefs.edit().remove(PLAYBACK_STATES_KEY).apply()
    }

    fun getRecentlyPlayed(limit: Int = 10): List<PlaybackState> {
        return getAllPlaybackStates()
            .values
            .sortedByDescending { it.lastPlayedAt }
            .take(limit)
    }
}

