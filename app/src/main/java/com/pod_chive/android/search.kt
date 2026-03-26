package com.pod_chive.android

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.api.Podcast
import com.pod_chive.android.api.RetrofitClient
import com.pod_chive.android.api.RssDataSource
import com.pod_chive.android.api.RssFeedResult
import com.pod_chive.android.model.PodcastShow
import com.pod_chive.android.ui.components.LoadingIndicator
import com.pod_chive.android.ui.components.ShowPodPage
import java.io.Serializable


// Sealed class for search results
sealed class SearchResultType : Serializable {
    object Empty : SearchResultType() {
        private fun readResolve(): Any = Empty
    }

    data class Podcasts(val podcasts: ArrayList<PodcastShow>) : SearchResultType()
    data class RssEpisodes(val podcastSummary: PodcastShow, val episodeDCS: List<EpisodeDC>) : SearchResultType()
    data class Error(val message: String) : SearchResultType()
}

fun removeDotRSS(podcasts: List<Podcast>): List<Podcast>
{
    val new: MutableList<Podcast> = mutableListOf<Podcast>()
    for (podcast in podcasts){
        podcast.PodcastUrl = podcast.url

        Log.e("REMOVE?", "Podcast URL: $podcast")
        Log.e("REMOVE-check", podcast.url?.endsWith(".rss", ignoreCase = true).toString())
        if (podcast.url?.contains("buzzsprout.com") == true){
            Log.e("REMOVED", "Podcast URL ends with .rss: ${podcast.url}")
        } else{
            new += podcast
        }

    }
    return new

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
            placeholder = { Text(stringResource(R.string.keyword_hint))
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onSearch(text) },
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.SearchRouteName))
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
                        Log.d("PodchiveAPI", "result: ${result.podcast.PodcastName} ${result.episodeDCS?.size}")
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
                response.sort()
                response.results = removeDotRSS(response.results)
                val homeItems = ArrayList(response.results.map { podcast ->
                    PodcastShow(
                        podcast.title,
                         podcast.description ?: "",
                         podcast.PodcastUrl?:"",
                        podcast.PodcastUrl?.substringAfterLast('/'),
                         podcast.image
                    )


//                    homeItem(
//                         podcast.title,
//                        description = podcast.showDescription ?: "",
//                        rss_url = podcast.url,
//                        output_directory = podcast.url.substringAfterLast('/'),
//                        cover_image_url = podcast.imageUrl
//                    )
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
                text = stringResource(R.string.loading),
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            when (val currentResults = searchResults) {
                is SearchResultType.Empty -> {
                    Text(
                        text = stringResource(R.string.keyword_hint),
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is SearchResultType.Error -> {
                    Text(
                        text = "${stringResource(R.string.error)}: ${currentResults.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                is SearchResultType.Podcasts -> {
                    if (currentResults.podcasts.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_results),
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
                            text = "${stringResource(R.string.ep_from)} ${currentResults.podcastSummary.PodcastName}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
//
                                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SearchItemView(podcast: PodcastShow, navController: NavController) {
    // Added navController
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .clickable {
            // Determine the type of podcast and navigate accordingly
            val type = if (podcast.PodcastUrl?.startsWith("http") ?: false) "rss" else "api"
//            val identifier = if (type == "rss") Uri.encode(podcast.PodcastUrl) else podcast.outputDirectory
            navController.navigate(podcast )
        }) {
        GlideImage(
            model = podcast.Cover_Image,
            contentDescription = stringResource(R.string.Podcast_artwork),
            modifier = Modifier
                .width(80.dp)
                .height(80.dp),
            loading = placeholder(R.mipmap.shrug)
        )

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = podcast.PodcastName?:stringResource(R.string.unknown),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2
            )
            Text(
                text = podcast.showDescription?:"",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
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
fun ShowPodDetsFromRSS(homeitems: PodcastShow, navController: NavController) {
    val context = LocalContext.current
    var podcastData by remember { mutableStateOf<PodcastShow?>(homeitems) }
    var epData by remember { mutableStateOf<List<EpisodeDC>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchResults by remember { mutableStateOf<SearchResultType>(SearchResultType.Empty) }
    var isFavorite by remember { mutableStateOf(false) }
    var IsNotificationEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(homeitems) {
        try {

            when (val result = RssDataSource.parseRssFeed(homeitems.PodcastUrl ?: "")) {
                is RssFeedResult.Success -> {
                    epData = result.episodeDCS

                    epData?.size?.let {
                        if (it > 200)
                            epData = epData?.take(500)
                    }
                    podcastData = result.podcast
                    podcastData!!.PodcastUrl = homeitems.PodcastUrl
                    Log.d(
                        "Search",
                        "result: ${result.podcast.PodcastName} ${result.episodeDCS?.size} ${podcastData?.PodcastUrl}"
                    )
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
        ShowPodPage(podcastData, epData, navController)
    }
}
