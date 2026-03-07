package com.pod_chive.android

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.api.RetrofitClientFront
import com.pod_chive.android.api.RssDataSource
import com.pod_chive.android.api.RssFeedResult
import com.pod_chive.android.database.FavoritePodcastRepository
import com.pod_chive.android.ui.components.LoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.content.edit
import com.pod_chive.android.database.FavoritePodcast
import kotlinx.serialization.Contextual

@Serializable
/**
 * Represents an episode with associated podcast information.
 *
 */
data class EpisodeWithShowData(
    @Contextual
    val episodeDC: EpisodeDC,
    val podcastDirectory: String?,
//    val photoUrl: String,
    val isRss: Boolean
)

@Serializable
private data class CachedEpisodeWithPodcast(
    val title: String,
    val description: String? = null,
    val audioFilePath: String,
    val pubDate: String,
    val episodeData: EpisodeWithShowData,
    val transcript: String? = null
)

@Serializable
private data class FavoriteEpisodesCachePayload(
    val favoritesSignature: String,
    val cachedAtMs: Long,
    val episodes: List<CachedEpisodeWithPodcast>
)

private class FavoriteEpisodesPageCache(context: Context) {
    private val prefs = context.getSharedPreferences("favorite_episodes_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(favoritesSignature: String, ttlMs: Long): List<EpisodeWithShowData>? {
        val raw = prefs.getString("payload", null) ?: return null
        return try {
            val payload = json.decodeFromString<FavoriteEpisodesCachePayload>(raw)
            val notExpired = System.currentTimeMillis() - payload.cachedAtMs <= ttlMs
            if (!notExpired || payload.favoritesSignature != favoritesSignature) return null
            payload.episodes.map { it.toEpisodeWithPodcast() }
        } catch (_: Exception) {
            null
        }
    }

    fun save(favoritesSignature: String, episodes: List<EpisodeWithShowData>) {
        val payload = FavoriteEpisodesCachePayload(
            favoritesSignature = favoritesSignature,
            cachedAtMs = System.currentTimeMillis(),
            episodes = episodes.map { it.toCachedEpisode() }
        )
        prefs.edit { putString("payload", json.encodeToString(payload)) }
    }

    private fun CachedEpisodeWithPodcast.toEpisodeWithPodcast(): EpisodeWithShowData {


        return EpisodeWithShowData(
            episodeDC = EpisodeDC(
                title = title,
                description = description,
                audioFilePath = audioFilePath,
                pubDate = pubDate,
                transcript = transcript,
                creator = episodeData.episodeDC.creator,
                photo = episodeData.episodeDC.photo
            ),
            podcastDirectory = episodeData.podcastDirectory,
            isRss = episodeData.isRss,
        )
    }

    private fun EpisodeWithShowData.toCachedEpisode(): CachedEpisodeWithPodcast {
        return CachedEpisodeWithPodcast(
            title = episodeDC.title,
            description = episodeDC.description,
            audioFilePath = episodeDC.audioFilePath,
            pubDate = episodeDC.pubDate,
            episodeData = EpisodeWithShowData(
                podcastDirectory = podcastDirectory,
                isRss = isRss,
                episodeDC = episodeDC
            )
        )
    }
}

private fun buildFavoritesSignature(favorites: List<FavoritePodcast>): String {
    return favorites
        .map { "${it.feedLink}|${it.title}|${it.imageLocation}" }
        .sorted()
        .joinToString(";")
}

private fun parseEpisodeTimeMillis(pubDate: String): Long {
    return try {
        val podchiveFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
        podchiveFormat.parse(pubDate)?.time ?: 0L
    } catch (_: Exception) {
        try {
            val rssFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            rssFormat.parse(pubDate)?.time ?: 0L
        } catch (_: Exception) {
            try {
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                isoFormat.parse(pubDate)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }
}

@Composable
fun FavoriteEpisodesScreen(navController: NavController) {
    val context = LocalContext.current
    var episodes by remember { mutableStateOf<List<EpisodeWithShowData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        errorMessage = null

        try {
            val repository = FavoritePodcastRepository(context)
            val pageCache = FavoriteEpisodesPageCache(context)
            val favorites = withContext(Dispatchers.IO) { repository.getAllFavorites() }

            if (favorites.isEmpty()) {
                episodes = emptyList()
                isLoading = false
                return@LaunchedEffect
            }

            val favoritesSignature = buildFavoritesSignature(favorites)
            val cachedEpisodes = withContext(Dispatchers.IO) {
                pageCache.load(favoritesSignature = favoritesSignature, ttlMs = 20 * 60 * 1000)
            }

            if (!cachedEpisodes.isNullOrEmpty()) {
                episodes = cachedEpisodes
                isLoading = false
            } else {
                isLoading = true
            }


            // Fetch episodes from all favorite podcasts concurrently
            val allEpisodes = withContext(Dispatchers.IO) {
                favorites.map { favorite ->
                    async {
                        try {
                            if (favorite.feedLink.startsWith("http")) {
                                // RSS feed
                                when (val result = RssDataSource.parseRssFeed(favorite.feedLink)) {
                                    is RssFeedResult.Success -> {
                                        result.episodeDCS?.map { episode ->
                                            EpisodeWithShowData(
                                                episodeDC = episode,
                                                podcastDirectory = null,
                                                isRss = true
                                            )
                                        } ?: emptyList()
                                    }
                                    is RssFeedResult.Error -> {
                                        Log.e("FavoriteEpisodes", "RSS Parse Error: ${result.message}")
                                        emptyList()
                                    }
                                }
                            } else {
                                // Local podcast
                                val podcastData = RetrofitClientFront.getInstance(context)
                                    .getPodDetails(favorite.feedLink)
                                podcastData.episodeDCS.map { episode ->
                                    episode.photo = "https://pod-chive.com/${favorite.feedLink}/cover.webp"
                                    episode.creator = favorite.title
                                    episode.audioFilePath = "https://pod-chive.com/${episode.audioFilePath}"
                                    EpisodeWithShowData(
                                        episodeDC = episode,
                                        podcastDirectory = favorite.feedLink,
                                        isRss = false
                                    )
                                } ?: emptyList()
                            }
                        } catch (e: Exception) {
                            Log.e("FavoriteEpisodes", "Error loading ${favorite.title}: ${e.message}")
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()
            }

            val sortedEpisodes = allEpisodes.sortedByDescending { parseEpisodeTimeMillis(it.episodeDC.pubDate) }
            episodes = sortedEpisodes

            withContext(Dispatchers.IO) {
                pageCache.save(favoritesSignature, sortedEpisodes)
            }
        } catch (e: Exception) {
            if (episodes.isEmpty()) {
                errorMessage = "Error loading episodes: ${e.message}"
            }
            Log.e("FavoriteEpisodes", "Error: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navController.previousBackStackEntry != null) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = "New Episodes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                LoadingIndicator()
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            episodes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column (modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No episodes found in favorite podcasts",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box (
                            contentAlignment = Alignment.Center
                        ){

                            Button(
                                onClick = { navController.navigate("search") },
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text(text = "Explore Some Podcasts")

                            }
                        }
                    }
                }
            }
            else -> {

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(episodes) { episodeWithPodcast ->


                        EpisodeRow(
                            episodeDC = episodeWithPodcast.episodeDC,
                            directory = episodeWithPodcast.podcastDirectory,
                            podcastSHOWTitle = episodeWithPodcast.episodeDC.creator,
                            navController = navController,
                            playbackState = PlaybackState.STOPPED,
                            AudioUrl = episodeWithPodcast.episodeDC.audioFilePath,
                            PhotoUrl = episodeWithPodcast.episodeDC.photo,
                            showPodcastImage = true
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}
