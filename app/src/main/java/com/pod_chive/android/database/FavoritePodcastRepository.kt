package com.pod_chive.android.database

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
    val addedAt: Long = System.currentTimeMillis(),
    var notification: Boolean = false
) : PodcastShow(title, feedLink, imageLocation){
    override fun toString(): String {
        return "FavoritePodcast(id=$id, feedLink='$feedLink', imageLocation='$imageLocation', description=$description, title='$title', addedAt=$addedAt, notification=$notification)"
    }
}



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

     fun getAllFavorites(): List<FavoritePodcast> {
        return try {
            val jsonStr = prefs.getString(FAVORITES_KEY, "[]") ?: "[]"
            json.decodeFromString<List<FavoritePodcast>>(jsonStr).sortedBy { it.title }
        } catch (e: Exception) {
            Log.e("FavoritePodcastRepository", "Error getting favorites", e)
            emptyList()
        }
    }
    suspend fun addNotification(favorite: FavoritePodcast) {
        val favoriteNew = getFavoriteByFeedLink(favorite.feedLink)
        favoriteNew?.notification = true
        deleteFavorite(favorite)
       val arrayofFav = getAllFavorites().toMutableList()
        arrayofFav.add(favoriteNew!!)
        saveFavorites(arrayofFav)
    }

    suspend fun removeNotification(favorite: FavoritePodcast) {
        val favoriteNew = getFavoriteByFeedLink(favorite.feedLink)
        favoriteNew?.notification = false
        deleteFavorite(favorite)
        val arrayofFav = getAllFavorites().toMutableList()
        arrayofFav.add(favoriteNew!!)
        saveFavorites(arrayofFav)
        Log.e("FAVNEW", "Favorite: $favoriteNew")
    }


    suspend fun getFavoriteByFeedLink(feedLink: String?): FavoritePodcast? {
        return getAllFavorites().firstOrNull { it.feedLink == feedLink }
    }

    suspend fun isFavorite(feedLink: String?): Boolean {
        return getAllFavorites().any { it.feedLink == feedLink }
    }

    suspend fun isNotificationEnabled(feedLink: String?): Boolean {
        return getAllFavorites().any { it.feedLink == feedLink && it.notification }
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


