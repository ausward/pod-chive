package com.pod_chive.android

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.api.RetrofitClient
import com.pod_chive.android.api.RssDataSource
import com.pod_chive.android.api.RssFeedResult
import com.pod_chive.android.api.homeItem
import com.pod_chive.android.database.FavoritePodcast
import com.pod_chive.android.database.FavoritePodcastRepository
import com.pod_chive.android.ui.components.LoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable


// Sealed class for search results
sealed class SearchResultType : Serializable {
    object Empty : SearchResultType() {
        private fun readResolve(): Any = Empty
    }

    data class Podcasts(val podcasts: ArrayList<homeItem>) : SearchResultType()
    data class RssEpisodes(val podcastSummary: homeItem, val episodeDCS: List<EpisodeDC>) : SearchResultType()
    data class Error(val message: String) : SearchResultType()
}

//enum class PlaybackState { PLAYING, PAUSED, STOPPED }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodSearchBar(onSearch: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(50),
            maxLines = 1,
            placeholder = { Text("Keyword or RSS URL")
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onSearch(text) },
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
    }
}

@Composable
fun FindPod(SearchString: String, navController: NavController) { // Added NavController
    val context = LocalContext.current
    var searchResults by remember { mutableStateOf<SearchResultType>(SearchResultType.Empty) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(SearchString) {
        if (SearchString.isBlank()) {
            searchResults = SearchResultType.Empty
            return@LaunchedEffect
        }
        isLoading = true
        searchResults = SearchResultType.Empty // Clear previous results

        try {
            if (SearchString.startsWith("http://") || SearchString.startsWith("https://")) {
                // Assume it's an RSS URL
                when (val result = RssDataSource.parseRssFeed(SearchString)) {
                    is RssFeedResult.Success -> {
                        Log.d("PodchiveAPI", "result: ${result.podcast.podcast_title} ${result.episodeDCS?.size}")
                        if (!result.episodeDCS.isNullOrEmpty()) {
//                            Log.d("PodchiveAPI", "Episodes: ${result.episodes.size}")
                            searchResults = SearchResultType.RssEpisodes(result.podcast, result.episodeDCS)
                            navController.navigate(result.podcast)
                        } else {
                            Log.d("PodchiveAPI", "No episodes found: ${result}")
                            // If no episodes, just show the podcast summary
                            searchResults = SearchResultType.Podcasts(arrayListOf(result.podcast))
                        }
                    }
                    is RssFeedResult.Error -> {
                        searchResults = SearchResultType.Error(result.message)
                    }
                }
            } else {
                // Assume it's a keyword search
                val response = RetrofitClient.getInstance(context).searchPodcasts(term = SearchString)
                Log.d("PodchiveAPI", "response: $response")
                val homeItems = ArrayList(response.results.map { podcast ->
                    homeItem(
                         podcast.title,
                        description = podcast.description ?: "",
                        rss_url = podcast.url,
                        output_directory = podcast.url.substringAfterLast('/'),
                        cover_image_url = podcast.imageUrl
                    )
                })
                searchResults = SearchResultType.Podcasts(homeItems)
            }
        } catch (e: Exception) {
            searchResults = SearchResultType.Error(e.localizedMessage ?: "Unknown error")
            Log.e("PodchiveAPI", "Error fetching podcasts", e)
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (isLoading) {
            Text(
                text = "Loading...",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            when (val currentResults = searchResults) {
                is SearchResultType.Empty -> {
                    Text(
                        text = "Start searching or enter an RSS URL.",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is SearchResultType.Error -> {
                    Text(
                        text = "Error: ${currentResults.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                is SearchResultType.Podcasts -> {
                    if (currentResults.podcasts.isEmpty()) {
                        Text(
                            text = "No podcasts found. Try a different search term or RSS URL.",
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn {
                            items(currentResults.podcasts) { podcast ->
                                SearchItemView(podcast, navController)
                            }
                        }
                    }
                }
                is SearchResultType.RssEpisodes -> {
                    Column {
                        Text(
                            text = "Episodes from ${currentResults.podcastSummary.podcast_title}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
//                        LazyColumn {
//                            items(currentResults.episodes) { episode ->
//                                EpisodeRow(
//                                    episode = episode,
//                                    podcastTitle = currentResults.podcastSummary.podcast_title,
//                                    navController = navController,
//                                    playbackState = PlaybackState.STOPPED, // Placeholder
////                                    podcastSummary = currentResults.podcastSummary // Pass summary for image
//                                )
                                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchItemView(podcast: homeItem, navController: NavController) {
    // Added navController
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .clickable {
            // Determine the type of podcast and navigate accordingly
            val type = if (podcast.rss_url.startsWith("http")) "rss" else "api"
            val identifier = if (type == "rss") Uri.encode(podcast.rss_url) else podcast.output_directory
            navController.navigate(podcast )
        }) {
        GlideImage(
            model = podcast.cover_image_url,
            contentDescription = "Podcast artwork",
            modifier = Modifier
                .width(80.dp)
                .height(80.dp),
            loading = placeholder(R.mipmap.shrug)
        )

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = podcast.podcast_title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2
            )
            Text(
                text = podcast.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.tertiary
    )
}



@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ShowPodDetsFromRSS(homeitems: homeItem, navController: NavController) {
    val context = LocalContext.current
    var podcastData by remember { mutableStateOf<homeItem?>(null) }
    var epData by remember { mutableStateOf<List<EpisodeDC>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val showStickyTitle = scrollState.value > 320
    var isFavorite by remember { mutableStateOf(false) }
    // 2. Calculate dynamic size
    // Base size is 250dp, it will shrink as scrollState.value increases
    val maxImageSize = 250f
    var searchResults by remember { mutableStateOf<SearchResultType>(SearchResultType.Empty) }

    val minImageSize = 80f
//    val scrollThreshold = 500f // How fast it shrinks

    val currentImageSize = (maxImageSize - (scrollState.value / 2))
        .coerceAtLeast(minImageSize).dp

    LaunchedEffect(homeitems) {
        try {

            when (val result = RssDataSource.parseRssFeed(homeitems.rss_url)) {
                is RssFeedResult.Success -> {
                    epData = result.episodeDCS
                    podcastData = result.podcast
                    Log.d(
                        "PodchiveAPI",
                        "result: ${result.podcast.podcast_title} ${result.episodeDCS?.size}"
                    )
                    val repository = FavoritePodcastRepository(context)
                    isFavorite = repository.isFavorite(podcastData?.rss_url)
                    if (!result.episodeDCS.isNullOrEmpty()) {
                        Log.d("PodchiveAPI", "Episodes: ${result.episodeDCS.size}")
                        searchResults =
                            SearchResultType.RssEpisodes(result.podcast, result.episodeDCS)
                    } else {
                        Log.d("PodchiveAPI", "No episodes found: ${result}")
                        // If no episodes, just show the podcast summary
                        searchResults = SearchResultType.Podcasts(arrayListOf(result.podcast))
                    }
                }

                else -> {}
            }
            } catch (e: Exception) {
            Log.e("PodchiveAPI", "Error fetching podcast details", e)
        } finally {
            isLoading = false
        }

    }

    if (isLoading) {
        LoadingIndicator()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState) // Standard Column scrolling
        ) {
            // --- Shinking Header ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlideImage(
                    model = podcastData?.cover_image_url,
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(currentImageSize) // Dynamic size applied here
                        .clip(MaterialTheme.shapes.medium),
                    loading = placeholder(R.mipmap.shrug),
                    failure = placeholder(R.mipmap.shrug)
                )

                Spacer(modifier = Modifier.height(16.dp))

                podcastData?.podcast_title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Episode List ---
            // Since we aren't in a LazyColumn, we use a simple forEach
            epData?.forEach { episode ->
                EpisodeRow(
                    episode,
                    null,

                    navController,
                    PlaybackState.STOPPED,
//                    episode.audioFilePath,
//                    podcastData?.cover_image_url
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.tertiary,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (showStickyTitle) MaterialTheme.colorScheme.surface else Color.Transparent,
            tonalElevation = if (showStickyTitle) 4.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding() // Handles the notch/status bar area
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (showStickyTitle) {
                    Text(
                        text = podcastData?.podcast_title ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(onClick = {
                    val repository = FavoritePodcastRepository(context)
                    GlobalScope.launch(Dispatchers.IO) {
                        if (isFavorite) {
                            val favorite = repository.getFavoriteByFeedLink(podcastData?.rss_url)
                            if (favorite != null) {
                                repository.deleteFavorite(favorite)
                            }
                        } else {

                                repository.insertFavorite(
                                    FavoritePodcast(
                                        feedLink = podcastData?.rss_url ?: "",
                                        imageLocation = podcastData?.cover_image_url ?: "",
                                        description = podcastData?.description ?: "",
                                        title = podcastData?.podcast_title ?: ""
                                    )
                                )

                        }
                        isFavorite = !isFavorite
                    }
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
