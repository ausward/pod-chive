package com.pod_chive.android

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
import androidx.compose.material3.CircularProgressIndicator
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
import com.pod_chive.android.api.Episode
import com.pod_chive.android.api.RetrofitClientFront
import com.pod_chive.android.api.RssDataSource
import com.pod_chive.android.api.RssFeedResult
import com.pod_chive.android.database.FavoritePodcastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

data class EpisodeWithPodcast(
    val episode: Episode,
    val podcastTitle: String,
    val podcastDirectory: String?,
    val audioUrl: String,
    val photoUrl: String,
    val isRss: Boolean
)

@Composable
fun FavoriteEpisodesScreen(navController: NavController) {
    val context = LocalContext.current
    var episodes by remember { mutableStateOf<List<EpisodeWithPodcast>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        try {
            val repository = FavoritePodcastRepository(context)
            val favorites = withContext(Dispatchers.IO) {
                repository.getAllFavorites()
            }

            if (favorites.isEmpty()) {
                isLoading = false
                return@LaunchedEffect
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
                                        result.episodes?.map { episode ->
                                            EpisodeWithPodcast(
                                                episode = episode,
                                                podcastTitle = favorite.title,
                                                podcastDirectory = null,
                                                audioUrl = episode.audioFilePath ?: "",
                                                photoUrl = favorite.imageLocation,
                                                isRss = true
                                            )
                                        } ?: emptyList()
                                    }
                                    is RssFeedResult.Error -> {
                                        android.util.Log.e("FavoriteEpisodes", "RSS Parse Error: ${result.message}")
                                        emptyList()
                                    }
                                }
                            } else {
                                // Local podcast
                                val podcastData = RetrofitClientFront.getInstance(context)
                                    .getPodDetails(favorite.feedLink)
                                podcastData.episodes?.map { episode ->
                                    EpisodeWithPodcast(
                                        episode = episode,
                                        podcastTitle = favorite.title,
                                        podcastDirectory = favorite.feedLink,
                                        audioUrl = "https://pod-chive.com/${episode.audioFilePath}",
                                        photoUrl = "https://pod-chive.com/${favorite.feedLink}/cover.webp",
                                        isRss = false
                                    )
                                } ?: emptyList()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FavoriteEpisodes", "Error loading ${favorite.title}: ${e.message}")
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()
            }

            // Sort by date, newest first
            episodes = allEpisodes.sortedByDescending { episodeWithPodcast ->
                try {
                    // Try pod-chive server format first: "2026/02/27 20:55"
                    val podchiveFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
                    podchiveFormat.parse(episodeWithPodcast.episode.pubDate)?.time
                } catch (e: Exception) {
                    // If parsing fails, try RSS format
                    try {
                        val rssFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                        rssFormat.parse(episodeWithPodcast.episode.pubDate)?.time
                    } catch (e: Exception) {
                        // If that fails, try ISO format
                        try {
                            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                            isoFormat.parse(episodeWithPodcast.episode.pubDate)?.time
                        } catch (e: Exception) {
                            null // Return null if all parsing fails
                        }
                    }
                } ?: 0L // Default to epoch if all parsing fails
            }

        } catch (e: Exception) {
            errorMessage = "Error loading episodes: ${e.message}"
            android.util.Log.e("FavoriteEpisodes", "Error: ${e.message}", e)
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
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
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
                    Text(
                        text = "No episodes found in favorite podcasts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(episodes) { episodeWithPodcast ->
                        EpisodeRow(
                            episode = episodeWithPodcast.episode,
                            directory = episodeWithPodcast.podcastDirectory,
                            podcastTitle = episodeWithPodcast.podcastTitle,
                            navController = navController,
                            playbackState = PlaybackState.STOPPED,
                            AudioUrl = episodeWithPodcast.audioUrl,
                            PhotoUrl = episodeWithPodcast.photoUrl,
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



