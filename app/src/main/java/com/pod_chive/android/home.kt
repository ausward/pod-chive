package com.pod_chive.android


import androidx.media3.session.MediaController
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

import android.content.ComponentName
import android.net.Uri
import android.util.Log
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.filled.ViewComfyAlt
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.google.common.util.concurrent.MoreExecutors
import com.pod_chive.android.api.Episode
import com.pod_chive.android.api.PodcastDetailResponse
import com.pod_chive.android.api.RetrofitClient
import com.pod_chive.android.api.RetrofitClientFront
import com.pod_chive.android.api.homeItem
import java.lang.System.console




@Composable
fun HomePage(navController: NavController ) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isloading by rememberSaveable { mutableStateOf(false) }
    var podcasts by rememberSaveable { mutableStateOf<List<homeItem>>(emptyList()) }
    var error: String = ""
    var grid by rememberSaveable { mutableStateOf(true) }


    LaunchedEffect(key1 = true) {
        isloading = true
        try {
            val res = RetrofitClient.getInstance(context = context ).listPodcasts()

            podcasts = res.podcasts
        } catch (e: Exception) {
            error = e.message.toString()
        } finally {
            isloading = false
        }
    }
    Column() {
        Row(modifier = Modifier.fillMaxWidth(),
            // This centers everything inside the row vertically
            verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Welcome to Podchive!",
//                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { grid = !grid }) {
            Icon(
                imageVector = if (grid) Icons.Filled.ViewDay else Icons.Filled.ViewComfyAlt,
                contentDescription = "Toggle View Layout",
                tint = Color.DarkGray
            )
        }}
        if (isloading) {
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Red
            )
        } else if (error != "") {
            Text(
                text = error,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Red
            )

        } else {
            if (grid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), // 2 columns for grid
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(podcasts) { podcast ->
                        MainPodGridItem(podcast){navController.navigate("details/${podcast.output_directory}")}
                    }
                }
            } else {
                LazyColumn {
                    items(podcasts) { podcast ->
                        MainPodListExpanderHor(podcast){navController.navigate("details/${podcast.output_directory}")}
                        HorizontalDivider(thickness = 2.dp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainPodListExpanderHor(podcast: homeItem, onItemClick: () -> Unit ){
    val photoURL = "https://pod-chive.com/" + podcast.output_directory + "/cover.webp"
    Row(modifier = Modifier.padding(vertical = 2.dp).clip(MaterialTheme.shapes.medium).clickable{onItemClick()}) {
        GlideImage(
            model = photoURL,
            contentDescription = "Podcast Album Cover",
            modifier = Modifier.width(80.dp).clip(MaterialTheme.shapes.medium)
                .height(80.dp).align(Alignment.CenterVertically),
            loading = placeholder(R.mipmap.shrug),
            failure = placeholder(R.mipmap.shrug),

            )
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = podcast.podcast_title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
            )
            HorizontalDivider(color = Color.LightGray, thickness = 3.dp)
            HtmlText(html = podcast.description, maxLines = 4)
            
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainPodGridItem(podcast: homeItem, onItemClick: () -> Unit) {
    val photoURL = "https://pod-chive.com/" + podcast.output_directory + "/cover.webp"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp).clickable{onItemClick()} ){
            GlideImage(
                model = photoURL,
                contentDescription = "Cover",
                modifier = Modifier.aspectRatio(1f).clip(MaterialTheme.shapes.medium),
                loading = placeholder(R.mipmap.shrug)
            )
            Text(
                text = podcast.podcast_title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


@Composable
fun showPodDetsFromMainServer(directory: String, navController: NavController) {
    val context = LocalContext.current
    var podcastData by remember { mutableStateOf<PodcastDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(directory) {
        try {
            podcastData = RetrofitClientFront.getInstance(context).getPodDetails(directory)
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        CircularProgressIndicator()
    } else {
        podcastData?.let { data ->
            LazyColumn {
                item {
                    Text(text = data.podcastTitle, style = MaterialTheme.typography.headlineLarge)
                }
                items(data.episodes) { episode ->
                    EpisodeRow(episode, directory, data.podcastTitle, navController)
                    HorizontalDivider( color = Color.Black, thickness = 3.dp)

                }
            }
        }
    }
}

//@OptIn(ExperimentalGlideComposeApi::class)
//@Composable
//fun EpisodeRow(episode: Episode, directory: String) {
//
//    Row(modifier = Modifier.padding(top =16.dp)) {
//            GlideImage(
//                model = "https://pod-chive.com/" + directory + "/cover.webp",
//                contentDescription = "Cover",
//                Modifier.size(width = 80.dp, height = 80.dp).clip(MaterialTheme.shapes.medium).align(Alignment.CenterVertically),
//
//                )
//            Column(Modifier.padding(horizontal = 4.dp)) {
//            Text(text = episode.title, fontWeight = FontWeight.Bold)
//                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray, thickness = 1.dp)
//
//                Text(text = episode.description ?: "", maxLines = 4, fontSize = 12.sp)
//        }
//
//    }
//}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun EpisodeRow(episode: Episode, directory: String, podcastTitle: String, navController: NavController) {
    // State to track if the dialog is open
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // This is your live connection to the music player
    var controller by remember { mutableStateOf<MediaController?>(null) }

    // This stays the same - it connects to your background PlaybackService
    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get() as MediaController?
        }, MoreExecutors.directExecutor())
    }

    // 1. The Popup (AlertDialog)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            },
            title = {
                Text(text = episode.title, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                // Allows the HTML content to scroll if it's longer than the screen
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HtmlText(html = episode.description ?: "No description available.")
                }
            }
        )
    }

    // 2. The Row UI
    Row(
        modifier = Modifier
            .padding(top = 16.dp, start = 8.dp, end = 8.dp)
            .clickable { showDialog = true } // Open popup on click
    ) {
        GlideImage(
            model = "https://pod-chive.com/$directory/cover.webp",
            contentDescription = "Cover",
            modifier = Modifier
                .size(80.dp)
                .clip(MaterialTheme.shapes.medium)
                .align(Alignment.CenterVertically),
            loading = placeholder(R.mipmap.shrug),
            failure = placeholder(R.mipmap.shrug)
        )

        Column(modifier = Modifier.padding(horizontal = 8.dp).weight(1f)) {
            Text(
                text = episode.title,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )

            // Fixed Divider: Removed .weight(1f)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Color.LightGray,
                thickness = 1.dp
            )

            HtmlText(
                html = episode.description ?: "No description available.",
                maxLines = 3
            )
            
        }
        Button(
            onClick = {
                val audioUrl = "https://pod-chive.com/${episode.audioFilePath}"
                val photoUrl = "https://pod-chive.com/$directory/cover.webp"
                val encodedAudioUrl = Uri.encode(audioUrl)
                val encodedTitle = Uri.encode(episode.title)
                val encodedPhotoUrl = Uri.encode(photoUrl)
                val encodedCreator = Uri.encode(podcastTitle)

                Log.d("EpisodeRow", "Audio URL: $audioUrl")
                Log.d("EpisodeRow", "Photo URL: $photoUrl")
                val player = controller!!

                // 2. Create the MediaItem with Metadata (for lock screen/notifications)
                val mediaItem = MediaItem.Builder()
                    .setMediaId(audioUrl)
                    .setUri(Uri.parse(audioUrl))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(episode.title)
                            .setArtist(podcastTitle)
                            .setArtworkUri(Uri.parse(photoUrl))
                            .build()
                    )
                    .build()

                // 3. Command the service to play
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
//                navController.navigate("playpod?audioUrl=$encodedAudioUrl&title=$encodedTitle&photoUrl=$encodedPhotoUrl&creator=$encodedCreator")
            }
        ) {
            Text(text = "Play")
        }
    }
}





@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                this.maxLines = maxLines
                // Optional: Adjust text size or color to match your theme
                textSize = 16f
            }
        },
        update = { it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) }
    )
}