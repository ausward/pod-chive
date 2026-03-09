package com.pod_chive.android.database

import android.content.Context
import android.content.SharedPreferences
import com.pod_chive.android.model.PodcastShow
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import androidx.core.content.edit

@Serializable
data class FavoritePodcast(
    val id: Int = 0,
    val feedLink: String,
    val imageLocation: String,
    var description: String?,
    val title: String,
    val addedAt: Long = System.currentTimeMillis()
) : PodcastShow(title, feedLink, imageLocation)

class FavoritePodcastRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("podchive_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val FAVORITES_KEY = "favorites"

    suspend fun insertFavorite(favorite: FavoritePodcast) {
        val favorites = getAllFavorites().toMutableList()
        favorites.add(favorite)
        saveFavorites(favorites)
    }

    suspend fun deleteFavorite(favorite: FavoritePodcast) {
        val favorites = getAllFavorites().toMutableList()
        favorites.removeAll { it.feedLink == favorite.feedLink }
        saveFavorites(favorites)
    }

    suspend fun getAllFavorites(): List<FavoritePodcast> {
        return try {
            val jsonStr = prefs.getString(FAVORITES_KEY, "[]") ?: "[]"
            json.decodeFromString<List<FavoritePodcast>>(jsonStr).sortedBy { it.title }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFavoriteByFeedLink(feedLink: String?): FavoritePodcast? {
        return getAllFavorites().firstOrNull { it.feedLink == feedLink }
    }

    suspend fun isFavorite(feedLink: String?): Boolean {
        return getAllFavorites().any { it.feedLink == feedLink }
    }

    private fun saveFavorites(favorites: List<FavoritePodcast>) {
        try {
            val jsonString = json.encodeToString(favorites)
            prefs.edit { putString(FAVORITES_KEY, jsonString) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


