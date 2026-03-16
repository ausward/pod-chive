package com.pod_chive.android


import android.content.ComponentName
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.WindowMetrics
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewComfyAlt
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.google.common.util.concurrent.MoreExecutors
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.api.PodcastDetailResponse
import com.pod_chive.android.api.RetrofitClient
import com.pod_chive.android.api.RetrofitClientFront
import com.pod_chive.android.api.homeItem
import com.pod_chive.android.model.Episode
import com.pod_chive.android.model.PodcastShow
import com.pod_chive.android.playback.PlaybackStateManager
import com.pod_chive.android.queue.PlayBackProgressVis
import com.pod_chive.android.ui.components.AnimatedChive
import com.pod_chive.android.ui.components.LoadingIndicator
import com.pod_chive.android.ui.components.SadChive
import com.pod_chive.android.ui.components.ShowPodPage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max


@Composable
fun HomePage(navController: NavController ) {
    val context = LocalContext.current
    var isloading by rememberSaveable { mutableStateOf(false) }
    var podcasts by rememberSaveable() { mutableStateOf<ArrayList<homeItem>>(arrayListOf()) }
    var error = ""
    var grid by rememberSaveable(){ mutableStateOf(true) }




    LaunchedEffect(key1 = true) {
        isloading = true
        try {
            val res = RetrofitClient.getInstance(context = context ).listPodcasts()

            podcasts = ArrayList(res.podcasts)

            Log.d("PODCASTS", podcasts.toString())
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
           LoadingIndicator()
//            Text(
//                text = "Loading...",
//                style = MaterialTheme.typography.titleLarge,
//                color = Color.Red
//            )
        } else if (error != "") {
            Text(
                text = error,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Red
            )
            SadChive(Modifier.fillMaxWidth(), Color.Red, true)

        } else {
            if (grid) {
                var gridsize = max(((LocalWindowInfo.current.containerDpSize.width / 200.dp).toInt()),3)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridsize),
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
                text = podcast.podcast_title ?: "",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
            )
            podcast.description?.let { desc ->
                HorizontalDivider(color = Color.LightGray, thickness = 3.dp)
                val displayDesc = if (desc.length > 60) desc.take(60) + "..." else desc
                HtmlText(html = displayDesc, maxLines = 4)
            }
            
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
                text = podcast.podcast_title ?: "",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class, DelicateCoroutinesApi::class)
@Composable
fun ShowPodDetsFromMainServer(directory: String, navController: NavController) {
    val context = LocalContext.current
    var podcastData by remember { mutableStateOf<PodcastDetailResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var showShowDesc by remember { mutableStateOf(false) }



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
            podcastData!!.placeCreatorData()
            // Check if it's already favorited
            val repository = com.pod_chive.android.database.FavoritePodcastRepository(context)
            isFavorite = repository.isFavorite(directory)
        } catch (e: Exception) {
            Log.e("ShowPodDetsFromMainServer", "Error fetching podcast details", e)
            // Handle error
        } finally {
            isLoading = false
        }
    }


    if (showShowDesc) {                    AlertDialog(
        onDismissRequest = { showShowDesc = false },
        confirmButton = {
            TextButton(onClick = { showShowDesc = false }) {
                Text("Close")
            }
        },
        title = {
            Text(text = podcastData?.podcastTitle ?: "", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                HtmlText(html = podcastData?.podcastDescription ?: "No description available.")
            }
        }
    )}
    if (isLoading) {
        LoadingIndicator()
   } else {
       val podcastShow = PodcastShow(podcastData?.podcastTitle?:"", podcastData?.podcastDescription, "https://pod-chive.com/$directory/out.json", directory, "https://pod-chive.com/$directory/cover.webp" )
        ShowPodPage(podcastShow,podcastData?.episodeDCS, navController = navController, isFavorite)
    }
}

enum class PlaybackState { PLAYING, PAUSED, STOPPED }

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun EpisodeRow(
    episodeDC: EpisodeDC,
    directory: String? = null,
    navController: NavController,
    playbackState: PlaybackState,
    showPodcastImage: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val stateManager = remember { PlaybackStateManager(context) }
//    var playbackStates by remember { mutableStateOf<Map<String, PlaybackState>>(emptyMap()) }

    var audioUrl = ""
    var photoUrl = ""
    if (directory != "" && directory != null) {
        audioUrl = "https://pod-chive.com/${episodeDC.audioFilePath}"
        photoUrl = "https://pod-chive.com/$directory/cover.webp"
    } else {
        audioUrl = episodeDC.audioFilePath
        photoUrl = episodeDC.PhotoUrl?:episodeDC.photo?:"https://pod-chive.com/cover.webp"
    }
    Log.e("PHOTOURL", photoUrl)
    Log.e("AUDIOURL", audioUrl)

//    val context = LocalContext.current
//    val stateManager = remember { PlaybackStateManager(context) }
    var playbackStates by remember { mutableStateOf<Map<String, com.pod_chive.android.playback.PlaybackState>>(emptyMap()) }
    var state :  com.pod_chive.android.playback.PlaybackState

    // Load all playback states
    LaunchedEffect(Unit) {
        playbackStates = withContext(Dispatchers.IO) {
             stateManager.getAllPlaybackStates()
        }
    }
    state = stateManager.getPlaybackState(audioUrl) ?: com.pod_chive.android.playback.PlaybackState(
        audioUrl = audioUrl,
        title = episodeDC.title ?: "",
        creator = episodeDC.creator ?: "Unknown",
        photoUrl = photoUrl,
        currentPosition = 0,
        duration = 0
    )

    DisposableEffect(Unit) {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
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
                Text(text = episodeDC.title ?: "", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HtmlText(html = episodeDC.description ?: "No description available.")
                }
            }
        )
    }

    // Helper function to add to queue
    val addToQueue: (String, String) -> Unit = { audioUrl, photoUrl ->
        val queueManager = com.pod_chive.android.queue.PlayQueueManager(context)
       episodeDC.idValue = com.pod_chive.android.queue.PlayQueueManager.generateId(audioUrl)
        episodeDC.AudioUrl = audioUrl
        episodeDC.PhotoUrl = photoUrl

        Log.d("HOME470", episodeDC.toString())


        queueManager.addToQueue(episodeDC)
        Log.d("QUEUE", "Added to queue: ${episodeDC.title}")
        android.widget.Toast.makeText(context, "Added to queue", android.widget.Toast.LENGTH_SHORT)
            .show()
    }

    // Helper function to play
    val playEpisode: (Episode) -> Unit = { episodeDC ->
        if (controller != null) {
            Log.d("LINK", "Clicked play button")
            Log.d("LINK", "Playing audio URL: $audioUrl")

            val player = controller
            if (player != null) {
                Log.e("LINK", audioUrl ?: "Audio URL is null")
                val mediaItem = MediaItem.Builder()
                    .setMediaId(audioUrl)
                    .setUri(Uri.parse(audioUrl))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(episodeDC.EpisodeName ?: "")
                            .setArtist(episodeDC.Creator ?: "Unknown")
                            .setArtworkUri(photoUrl.toUri())
                            .build()
                    )
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                // Add episode to queue at the top
                val queueManager = com.pod_chive.android.queue.PlayQueueManager(context)
                episodeDC.idValue = com.pod_chive.android.queue.PlayQueueManager.generateId(audioUrl)
                episodeDC.AudioUrl = audioUrl
                episodeDC.PhotoUrl = photoUrl

                queueManager.addToQueue(episodeDC)
                queueManager.moveToTop(com.pod_chive.android.queue.PlayQueueManager.generateId(audioUrl))
                Log.d("QUEUE", "Added and moved to top: ${episodeDC.EpisodeName}")



                Log.d("HOME546", episodeDC.MasterToString())
//                navController.navigate(episodeDC.toPlayEpisode())
                navController.navigate("playpod")
            }
        }
    }

    // Create draggable state removed - using simple buttons instead

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showPodcastImage) {
                GlideImage(
                    model = photoUrl,
                    contentDescription = "Cover",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium),
                    loading = placeholder(R.drawable.confused_chive),
                    failure = placeholder(R.drawable.sad_chive),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (showPodcastImage && episodeDC.creator != null) {
                    Text(
                        text = episodeDC.creator ?: "",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = episodeDC.title ?: "",
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val displayDate = episodeDC.pubDate?.let {
                    if (it.length >= 16) it.substring(0, 16) else it
                } ?: ""
                Text(
                    text = displayDate,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))

                episodeDC.description?.let { desc ->
                    val displayDesc = if (desc.length > 60) desc.take(60) + "..." else desc
                    HtmlText(
                        html = displayDesc,
                        maxLines = 2,
                    )
                }

                if (state.duration > 0) {
//                     var progressPercent = 100f * state.currentPosition.toFloat() / state.duration.toFloat()
//                    (state.currentPosition.toFloat() / state.duration.toFloat() * 100f)
                    if (state.currentPosition >= state.duration - 50) {
                        Text(
                            text = "Completed",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        PlayBackProgressVis(state)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Column() {


                // Play button
                IconButton(
                    enabled = controller != null,
                    onClick = {
                        var audioUrl = ""
                        var photoUrl = ""
                        if (directory != null) {
                            audioUrl = "${episodeDC.audioFilePath}"
                            photoUrl = "https://pod-chive.com/$directory/cover.webp"
                        } else {
                            audioUrl = episodeDC.audioFilePath ?: return@IconButton
                            photoUrl = episodeDC.photo ?: return@IconButton
                        }

                        val Temp =
                            Episode(
                                audioUrl,
                                episodeDC.title ?: "",
                                episodeDC.pubDate ?: "",
                                photoUrl
                            )

                        Temp.TranscriptUrl = episodeDC.transcript
                        Temp.Creator = episodeDC.creator
                        Temp.Description = episodeDC.description
                        Log.e("episodeRowPlay", Temp.toString())
                        playEpisode(Temp)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play episode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }
                // Add to Queue button
                IconButton(
                    onClick = {

                        addToQueue(audioUrl, photoUrl)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Add to queue",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(35.dp)
                    )
                }

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
                movementMethod = LinkMovementMethod.getInstance()
                this.maxLines = maxLines
                // Optional: Adjust text size or color to match your theme
                textSize = 16f

                setTextColor(textColor)
            }
        },
        update = { it.text = HtmlCompat.fromHtml(html ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT) }
    )
}
