package com.pod_chive.android


import androidx.media3.session.MediaController
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewComfyAlt
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.pod_chive.android.ui.theme.PodchiveTheme


@Composable
fun HomePage(navController: NavController ) {
    val context = LocalContext.current
    var isloading by rememberSaveable { mutableStateOf(false) }
    var podcasts by rememberSaveable() { mutableStateOf<ArrayList<homeItem>>(arrayListOf()) }
    var error: String = ""
    var grid by rememberSaveable(){ mutableStateOf(false) }


    LaunchedEffect(key1 = true) {
        isloading = true
        try {
            val res = RetrofitClient.getInstance(context = context ).listPodcasts()

            podcasts = ArrayList(res.podcasts)
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


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun showPodDetsFromMainServer(directory: String, navController: NavController) {
    val context = LocalContext.current
    var podcastData by remember { mutableStateOf<PodcastDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }



    val scrollState = rememberScrollState()
    val showStickyTitle = scrollState.value > 320
    // 2. Calculate dynamic size
    // Base size is 250dp, it will shrink as scrollState.value increases
    val maxImageSize = 250f
    val minImageSize = 80f
    val scrollThreshold = 500f // How fast it shrinks

    val currentImageSize = (maxImageSize - (scrollState.value / 2))
        .coerceAtLeast(minImageSize).dp

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
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(600.dp))
        }
    } else {
        PodchiveTheme(dynamicColor = false) {
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
                    model = "https://pod-chive.com/$directory/cover.webp",
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(currentImageSize) // Dynamic size applied here
                        .clip(MaterialTheme.shapes.medium),
                    loading = placeholder(R.mipmap.shrug),
                    failure = placeholder(R.mipmap.shrug)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = podcastData?.podcastTitle ?: " ",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // --- Episode List ---
            // Since we aren't in a LazyColumn, we use a simple forEach
            podcastData?.episodes?.forEach { episode ->
                EpisodeRow(
                    episode,
                    directory,
                    podcastData?.podcastTitle,
                    navController,
                    PlaybackState.STOPPED
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
                            text = podcastData?.podcastTitle ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

enum class PlaybackState { PLAYING, PAUSED, STOPPED }

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun EpisodeRow(
    episode: Episode,
    directory: String? = null,
    podcastTitle: String?,
    navController: NavController,
    playbackState: PlaybackState,
    AudioUrl: String? = null,
    PhotoUrl: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get() as MediaController?
        }, MoreExecutors.directExecutor())

        onDispose {
            controller?.release()
            controller = null
        }
    }

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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HtmlText(html = episode.description ?: "No description available.")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            HtmlText(
                html = episode.description ?: "No description available.",
                maxLines = 3,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        var audioUrl: String = ""
        var photoUrl = ""
        IconButton(
            onClick = {
                if (directory != null) {
                      audioUrl = "https://pod-chive.com/${episode.audioFilePath}"
                      photoUrl = "https://pod-chive.com/$directory/cover.webp"
                } else {
                     audioUrl = AudioUrl!!
                     photoUrl = PhotoUrl!!

                }

                val player = controller!!

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

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                val encodedAudioUrl = Uri.encode(audioUrl)
                val encodedTitle = Uri.encode(episode.title)
                val encodedPhotoUrl = Uri.encode(photoUrl)
                val encodedCreator = Uri.encode(podcastTitle ?: "")
                navController.navigate(
                    "playpod?audioUrl=$encodedAudioUrl&title=$encodedTitle&photoUrl=$encodedPhotoUrl&creator=$encodedCreator"
                )
            },
            modifier = Modifier.size(48.dp)
        ) {
            PodchiveTheme(dynamicColor = false) {
                Icon(
                    imageVector = if (playbackState == PlaybackState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playbackState == PlaybackState.PLAYING) "Pause episode" else "Play episode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                this.maxLines = maxLines
                // Optional: Adjust text size or color to match your theme
                textSize = 16f
                setTextColor(textColor)
            }
        },
        update = { it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) }
    )
}