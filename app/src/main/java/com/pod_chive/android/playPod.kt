package com.pod_chive.android

import android.annotation.SuppressLint
import android.content.ComponentName
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.Help
import androidx.compose.material.icons.automirrored.sharp.PlaylistPlay
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.common.util.concurrent.MoreExecutors
import com.pod_chive.android.playback.PlaybackState
import com.pod_chive.android.playback.PlaybackStateManager
import com.pod_chive.android.queue.PlayQueueManager
import com.pod_chive.android.ui.components.Information
import com.pod_chive.android.ui.theme.PodchiveTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri
import com.pod_chive.android.model.Episode
import com.pod_chive.android.ui.components.AnimatedChive


@ExperimentalGlideComposeApi
@OptIn(ExperimentalGlideComposeApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlayPod(
    NEW_POD: Boolean = false,


    navController: NavController,
    episodeOBJ: Episode,
) {

    Log.e("PLAYPOD", episodeOBJ.toString())
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val context = LocalContext.current
    val transcript = if (episodeOBJ.TranscriptUrl != null && episodeOBJ.TranscriptUrl != "") {
        if (episodeOBJ.TranscriptUrl!!.contains("http*")) {
            Log.e("PLAYPOD", "transcripturl: ${episodeOBJ.TranscriptUrl}")
            episodeOBJ.TranscriptData = URL(episodeOBJ.TranscriptUrl).readText()
        }
        else {
            episodeOBJ.TranscriptUrl
        }
    }
    else {
        ""
    }

    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.navigateUp()
    }



    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val pc = controllerFuture.get()
            controller = pc
            if (controller?.isPlaying != true) {
            // If no audio URL was provided, load and play the top item from the queue
//            if (audioUrl.isNullOrBlank() || audioUrl == " ") {
                val queueManager = PlayQueueManager(context)
                val queueItems = queueManager.getQueue()
                if (queueItems.isNotEmpty()) {
                    val topItem = queueItems[0]

                    val mediaItem = MediaItem.Builder()
                        .setMediaId(topItem.AudioUrl?:"")
                        .setUri(topItem.AudioUrl!!.toUri())
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(topItem.EpisodeName)
                                .setArtist(topItem.Creator)
                                .setArtworkUri(topItem.PhotoUrl!!.toUri())
                                .build()
                        )
                        .build()

                    pc.setMediaItem(mediaItem)
                    pc.prepare()
                    pc.play()

                    Log.d("PLAYPLAY", "Playing top queue item: ${topItem.EpisodeName}")
                } else {
                    Log.d("PLAYPLAY", "Queue is empty, no item to play")
                }
            }
        }, MoreExecutors.directExecutor())
    }

    Column {
        AudioPlayer(
            NEW_POD,
            episodeOBJ,
            navController = navController,

        )
        // MiniPlayerControls()
    }
}


@Composable
fun MiniPlayerControls() {
    val context = LocalContext.current
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    val stateManager = PlaybackStateManager(context)


    LaunchedEffect(Unit) {
        val sessionToken =
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller

            isPlaying = controller.isPlaying
            duration = controller.duration.coerceAtLeast(0L)

            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    duration = player.duration.coerceAtLeast(0L)

                }
            })
        }, MoreExecutors.directExecutor())
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = mediaController?.currentPosition ?: 0L
            delay(1000)
        }
    }

    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    if (progress > 0f && duration != currentPosition) {
        PodchiveTheme() {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = { mediaController?.seekBack() }) {
                    Icon(
                        painter = painterResource(R.drawable.replay_10_24px),
                        contentDescription = "Rewind 10 seconds",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    formatDuration(currentPosition),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )

                Slider(
                    value = if (isDragging) sliderPosition else progress.coerceIn(0f, 1f),
                    onValueChange = {
                        isDragging = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        mediaController?.seekTo((sliderPosition * duration).toLong())
                        isDragging = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .widthIn(max = 140.dp)
                )
                Text(
                    formatDuration(duration),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall
                )
                IconButton(
                    onClick = {
                        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
                    }
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.outline_pause_24 else R.drawable.play_arrow_24px),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { mediaController?.seekForward() }) {
                    Icon(
                        painter = painterResource(R.drawable.outline_forward_30_24),
                        contentDescription = "Forward 30 seconds",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }


        }
    }
}


@OptIn(UnstableApi::class)
@ExperimentalGlideComposeApi
@Composable
fun AudioPlayer(
    New_POD: Boolean = false,

    episodeOBJ: Episode,
//    audioUrl: String, // This acts as a fallback or "new play" target
//    creator: String,
//    title: String,
//    photoUrl: String,
    navController: NavController,
//    desc: String? = null,
//    transcript: String? = null,
//    pubdate: String? = null
) {
//    Log.e("DATE" , pubdate.toString())

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    // UI State driven by the Controller
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isloading by remember { mutableStateOf(true) }

    // Slider State
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    // Save state when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            // Save current playback state when navigating away
            mediaController?.let { controller ->
                val playbackStateManager = PlaybackStateManager(context)
                val currentMediaItem = controller.currentMediaItem
                currentMediaItem?.let { mediaItem ->
                    val state = PlaybackState(
                        audioUrl = mediaItem.mediaId,
                        title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                        creator = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                        photoUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: "",
                        currentPosition = controller.currentPosition.coerceAtLeast(0),
                        duration = controller.duration.coerceAtLeast(0),
                        playbackSpeed = playbackSpeed
                    )
                    playbackStateManager.savePlaybackState(state)
                    Log.d("PLAYBACK", "PlayPod dispose: Saved state on navigate away with speed: ${playbackSpeed}x")
                }
            }
        }
    }

    // 1. Setup the connection to the persistent service
    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller
            isloading = controller.isLoading
            // Sync initial state
            isPlaying = controller.isPlaying
            duration = controller.duration.coerceAtLeast(0L)
            playbackSpeed = controller.playbackParameters.speed


        if (New_POD) {
            // Try to restore playback position from saved state
            val playbackStateManager = PlaybackStateManager(context)
            Log.d(
                "PLAYBACK",
                "AudioPlayer: Looking for saved state for URL: ${episodeOBJ.AudioUrl}"
            )

            var savedState = playbackStateManager.getPlaybackState(episodeOBJ.AudioUrl!!)

            // If not found and URL contains encoded characters, try to find a match
            if (savedState == null && episodeOBJ.AudioUrl!!.contains("%")) {
                Log.d("PLAYBACK", "URL is encoded, searching for decoded match...")
                val decodedUrl = try {
                    Uri.decode(episodeOBJ.AudioUrl)
                } catch (e: Exception) {
                    Log.d("PLAYBACK", "Error decoding URL: ${e.message}")
                    episodeOBJ.AudioUrl
                }
                savedState = playbackStateManager.getPlaybackState(decodedUrl!!)
                if (savedState != null) {
                    Log.d("PLAYBACK", "Found state using decoded URL")
                }
            }

            // If still not found, search all states for matching audio URL
            if (savedState == null) {
                Log.d("PLAYBACK", "Searching all saved states for any match...")
                val allStates = playbackStateManager.getAllPlaybackStates()
                savedState = allStates.values.firstOrNull {
                    it.audioUrl == episodeOBJ.AudioUrl ||
                            Uri.decode(it.audioUrl) == episodeOBJ.AudioUrl ||
                            Uri.decode(it.audioUrl) == Uri.decode(episodeOBJ.AudioUrl)
                }
                if (savedState != null) {
                    Log.d("PLAYBACK", "Found matching state in all states")
                }
            }

            if (savedState != null) {
                Log.d(
                    "PLAYBACK",
                    "Found saved state: ${savedState.title} at ${savedState.currentPosition}ms/${savedState.duration}ms"
                )
                // Only restore if the saved position is reasonable (not at the very end)
                if (savedState.currentPosition > 0 && savedState.currentPosition < savedState.duration * 0.95) {
                    Log.e("STATECONTROLLER", controller.currentMediaItem?.mediaId ?: "")
                    Log.e("STATESAVED", savedState.audioUrl ?: "")

//                    if (savedState.currentPosition > controller.currentPosition) {
                        controller.seekTo(savedState.currentPosition)
                        Log.d(
                            "PLAYBACK",
                            "✓ Restored playback position: ${savedState.currentPosition}ms / ${savedState.duration}ms"
                        )
//                    }
                } else {
                    Log.d(
                        "PLAYBACK",
                        "✗ Skipped restore - position unreasonable: ${savedState.currentPosition}ms / ${savedState.duration}ms"
                    )
                }
            } else {
                Log.d("PLAYBACK", "✗ No saved state found for: ${episodeOBJ.AudioUrl}")
            }
        }


            // Setup a listener to update the UI in real-time
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackParametersChanged(params: PlaybackParameters) {
                    playbackSpeed = params.speed
                }
                override fun onEvents(player: Player, events: Player.Events) {
                    duration = player.duration.coerceAtLeast(0L)
                }
            })
        }, MoreExecutors.directExecutor())
    }

    // 2. The Progress Ticker (Updates every second)
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = mediaController?.currentPosition ?: 0L
            delay(1000)
        }
    }

    // Save speed change when user adjusts playback speed
    LaunchedEffect(playbackSpeed) {
        mediaController?.let { controller ->
            val playbackStateManager = PlaybackStateManager(context)
            val currentMediaItem = controller.currentMediaItem
            currentMediaItem?.let { mediaItem ->
                try {
                    val state = PlaybackState(
                        audioUrl = mediaItem.mediaId,
                        title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                        creator = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                        photoUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: "",
                        currentPosition = controller.currentPosition.coerceAtLeast(0),
                        duration = controller.duration.coerceAtLeast(0),
                        playbackSpeed = playbackSpeed
                    )
                    playbackStateManager.savePlaybackState(state)
                    Log.d("PLAYBACK", "Saved speed change: ${playbackSpeed}x")
                } catch (e: Exception) {
                    Log.e("PLAYBACK", "Error saving speed: ${e.message}")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface), // Deep charcoal/black
//            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        val configuration = LocalConfiguration.current
        val screenHeight = LocalWindowInfo.current.containerSize.width

        // Calculate dynamic artwork size based on screen height
        // Leave room for controls (~320dp) and padding
        val availableHeight = screenHeight - 200
        val artworkSize = (availableHeight * 0.25f).coerceIn(150f, 280f).dp

        // Queue button at top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.End
        ) {

            IconButton(onClick = {
                navController.navigate("queue")
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Sharp.PlaylistPlay,
                    contentDescription = "View Queue",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick =  {
                    val temp = episodeOBJ.toInformation()
//                    val temp = Information(episodeOBJ.desc, transcript, pubdate, creator, title)
                    navController.navigate(temp)
                }
            ) { Icon(
                imageVector = Icons.AutoMirrored.Sharp.Help,
                contentDescription = "View Details")}
        }
        // --- Artwork ---
        Box(
            modifier = Modifier
                .size(artworkSize)
                .shadow(20.dp, RoundedCornerShape(16.dp))
        ) {
            GlideImage(
                model = episodeOBJ.PhotoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }

//        Spacer(modifier = Modifier.height(32.dp))

        // --- Info ---
        Text(
            text = episodeOBJ.EpisodeName?:"Hydration Error",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text = episodeOBJ.Creator?:"Unknown Show Name",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- Progress Slider ---
        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

        Slider(
            value = if (isDragging) sliderPosition else progress.coerceIn(0f, 1f),
            onValueChange = {
                isDragging = true
                sliderPosition = it
            },
            onValueChangeFinished = {
                mediaController?.seekTo((sliderPosition * duration).toLong())
                isDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ), valueRange = 0f..1f,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(currentPosition), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Text(formatDuration(duration), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }

//        Spacer(modifier = Modifier.height(24.dp))

        // --- Controls ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { mediaController?.seekBack() }, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.replay_10_24px), null, tint = Color.White, modifier = Modifier.size(28.dp))
            }

            // Big Play/Pause Button
            Surface(
                onClick = {
                    mediaController?.let {
                        if (it.isPlaying) it.pause() else it.play()
                    }
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (mediaController?.isLoading?:false) {
                        AnimatedChive(isLoading = mediaController?.isLoading?:true)
                    } else {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.outline_pause_24 else R.drawable.play_arrow_24px),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            IconButton(onClick = { mediaController?.seekForward() }, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(R.drawable.outline_forward_30_24), null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Speed Controls ---
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(bottom = 16.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var isDecreasePressed by remember { mutableStateOf(false) }
                var isIncreasePressed by remember { mutableStateOf(false) }
                var isDecreaseLongPress by remember { mutableStateOf(false) }
                var isIncreaseLongPress by remember { mutableStateOf(false) }

                // Decrease Speed Button - Normal tap: -0.25f, Long press: -1.0f
                IconButton(
                    onClick = {
                        // Only handle normal tap if it wasn't a long press
                        if (!isDecreaseLongPress && playbackSpeed > 0.5f) {
                            mediaController?.setPlaybackSpeed(
                                (playbackSpeed - 0.25f).coerceAtLeast(0.5f)
                            )
                        }
                        isDecreaseLongPress = false
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            width = 2.dp,
                            color = if (isDecreasePressed) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Icon(
                            Icons.Filled.ArrowDownward,
                            null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Press -> {
                                                    isDecreasePressed = true
                                                    isDecreaseLongPress = false

                                                    // After 1000ms of holding, decrease speed by 1.0f
                                                    scope.launch {
                                                        delay(1000)
                                                        if (isDecreasePressed && playbackSpeed > 0.5f) {
                                                            isDecreaseLongPress = true
                                                            mediaController?.setPlaybackSpeed(
                                                                (playbackSpeed - 1.0f).coerceAtLeast(0.5f)
                                                            )
                                                        }
                                                    }
                                                }

                                                PointerEventType.Release -> {
                                                    isDecreasePressed = false
                                                }

                                                else -> {}
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { mediaController?.setPlaybackSpeed(1.0f) }
                ) {
                    Text("SPEED", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    Text(String.format(Locale.US, "%.2fx", playbackSpeed), color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Increase Speed Button - Normal tap: +0.25f, Long press: +1.0f
                IconButton(
                    onClick = {
                        // Only handle normal tap if it wasn't a long press
                        if (!isIncreaseLongPress && playbackSpeed < 4.0f) {
                            mediaController?.setPlaybackSpeed(
                                (playbackSpeed + 0.25f).coerceAtMost(4.0f)
                            )
                        }
                        isIncreaseLongPress = false
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            width = 2.dp,
                            color = if (isIncreasePressed) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            when (event.type) {
                                                PointerEventType.Press -> {
                                                    isIncreasePressed = true
                                                    isIncreaseLongPress = false

                                                    // After 1000ms of holding, increase speed by 1.0f
                                                    scope.launch {
                                                        delay(1000)
                                                        if (isIncreasePressed && playbackSpeed < 4.0f) {
                                                            isIncreaseLongPress = true
                                                            mediaController?.setPlaybackSpeed(
                                                                (playbackSpeed + 1.0f).coerceAtMost(4.0f)
                                                            )
                                                        }
                                                    }
                                                }

                                                PointerEventType.Release -> {
                                                    isIncreasePressed = false
                                                }

                                                else -> {}
                                            }
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }

        // --- Description & Transcript Section ---
//        var episodeDescription by remember { mutableStateOf<String?>(null) }
//        var episodeTranscript by remember { mutableStateOf<String?>(null) }
//        var isLoadingDetails by remember { mutableStateOf(false) }

//        // Try to fetch episode details from the currently playing media item
//        LaunchedEffect(mediaController?.currentMediaItem?.mediaId) {
//            val currentMediaItem = mediaController?.currentMediaItem
//            if (currentMediaItem != null) {
//                // Check if we can extract directory from audioUrl to fetch episode data
//                val mediaId = currentMediaItem.mediaId
//
//                // If it's from our server (contains pod-chive.com), try to fetch details
//                if (mediaId.contains("pod-chive.com")) {
//                    isLoadingDetails = true
//                    try {
//                        // Extract directory from URL like https://pod-chive.com/PodcastName/episode.mp3
//                        val urlParts = mediaId.split("/")
//                        if (urlParts.size >= 4) {
//                            val directory = urlParts[3]
//
//                            // Fetch podcast details
//                            val podcastData = com.pod_chive.android.api.RetrofitClientFront.getInstance(context)
//                                .getPodDetails(directory)
//
//                            // Find matching episode by audio file path
//                            val matchingEpisode = podcastData.episodes?.find { episode ->
//                                mediaId.endsWith(episode.audioFilePath)
//                            }
//
//                            episodeDescription = matchingEpisode?.description
//                            // Note: transcript would need to be added to Episode data class
//                            // For now, we'll show a placeholder
//                        }
//                    } catch (e: Exception) {
//                        android.util.Log.e("PLAYPOD", "Error fetching episode details: ${e.message}")
//                    } finally {
//                        isLoadingDetails = false
//                    }
//                }
//            }
//        }

//        // Show description if available
//        if (!desc.isNullOrBlank()) {
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Surface(
//                color = Color(0xFF1E1E1E),
//                shape = RoundedCornerShape(12.dp),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 24.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp)
//                ) {
//                    Text(
//                        text = "Description",
//                        color = MaterialTheme.colorScheme.primary,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    // Use HtmlText composable from home.kt
//                    HtmlText(
//                        html = desc,
//                        modifier = Modifier.fillMaxWidth()
//                    )
//                }
//            }
//        }
//
//        // Transcript section (placeholder for future implementation)
//        if (!transcript.isNullOrBlank()) {
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Surface(
//                color = Color(0xFF1E1E1E),
//                shape = RoundedCornerShape(12.dp),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 24.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp)
//                ) {
//                    Text(
//                        text = "Transcript",
//                        color = MaterialTheme.colorScheme.primary,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        text = transcript,
//                        color = MaterialTheme.colorScheme.onSurface,
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                }
//            }
//        }
//
//        // Bottom padding
//        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun updateSpeedOnButtonPress(playbackSpeed: Float): Float  {
//    if (playbackSpeed < 3.5f) {
        if (playbackSpeed >= 1.5f)
        { return playbackSpeed + 0.25f}
        return playbackSpeed + 0.10f
//    } else { return 0.25f}
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
