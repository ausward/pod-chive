package com.pod_chive.android.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.pod_chive.android.EpisodeRow
import com.pod_chive.android.PlaybackState
import com.pod_chive.android.R
import com.pod_chive.android.SearchResultType
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.database.FavoritePodcast
import com.pod_chive.android.database.FavoritePodcastRepository
import com.pod_chive.android.model.PodcastShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ShowPodPage(podcastData: PodcastShow?, epData: List<EpisodeDC>?, navController: NavController, isFav: Boolean){
    var isFavorite by remember { mutableStateOf(isFav) }
    val scrollState = rememberScrollState()
    val showStickyTitle = scrollState.value > 320
    val context = LocalContext.current

    // 2. Calculate dynamic size
    // Base size is 250dp, it will shrink as scrollState.value increases
    val maxImageSize = 250f
    val minImageSize = 80f
    val currentImageSize = (maxImageSize - (scrollState.value / 2))
        .coerceAtLeast(minImageSize).dp

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
                model = podcastData?.Cover_Image,
                contentDescription = "Cover",
                requestBuilderTransform = { request ->
                    request
                        .override(500, 500) // Use a fixed resolution (e.g., your max expected size)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                        .centerCrop() // Ensures the bitmap is filled before being scaled by Compose
                },
                modifier = Modifier
                    .size(currentImageSize) // Dynamic size applied here
                    .clip(MaterialTheme.shapes.medium),
                loading = placeholder(R.mipmap.cover),
                failure = placeholder(R.mipmap.shrug)
            )

            Spacer(modifier = Modifier.height(16.dp))

            podcastData?.PodcastName?.let {
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
            episode.description
            EpisodeRow(
                episode,
                podcastData?.outputDirectory,
                navController,
                PlaybackState.STOPPED,

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
                    text = podcastData?.PodcastName ?: "",
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
                        val favorite = repository.getFavoriteByFeedLink(podcastData?.PodcastUrl)
                        if (favorite != null) {
                            repository.deleteFavorite(favorite)
                        }
                    } else {
                        Log.e("FAV", "Inserting favorite: ${podcastData?.PodcastName}, ${podcastData?.PodcastUrl}, ${podcastData?.Cover_Image}, ${podcastData?.showDescription?.take(3)}")
                        repository.insertFavorite(
                            FavoritePodcast(
                                feedLink = podcastData?.PodcastUrl ?: "",
                                imageLocation = podcastData?.Cover_Image ?: "",
                                description = podcastData?.showDescription ?: "",
                                title = podcastData?.PodcastName ?: "",

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
