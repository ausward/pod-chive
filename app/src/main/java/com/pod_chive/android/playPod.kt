package com.pod_chive.android

import android.annotation.SuppressLint
import android.content.ComponentName
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.google.common.util.concurrent.MoreExecutors
import com.pod_chive.android.ui.theme.PodchiveTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

@ExperimentalGlideComposeApi
@OptIn(ExperimentalGlideComposeApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlayPod(
    navController: NavController,
    audioUrl: String? = null,
    title: String? = null,
    photoUrl: String? = null,
    creator: String? = null
) {
    // Default values if none provided (for testing or if navigating directly to the tab)
    var finalAudioUrl  by remember { mutableStateOf(audioUrl ?: " ") }
    var finalTitle by remember { mutableStateOf(title ?: " ") }
    var finalPhotoUrl by remember { mutableStateOf(photoUrl ?: " ") }
    var finalCreator  by remember { mutableStateOf(creator ?: " ") }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val context = LocalContext.current

    BackHandler(enabled = navController.previousBackStackEntry != null) {
        navController.navigateUp()
    }

//    BackHandler() {navController.popBackStack() }



    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val pc = controllerFuture.get()
            controller = pc

            // Pull the metadata that was set in EpisodeRow
            val metadataTitle = pc.mediaMetadata.title?.toString()
            val metadataCreator = pc.mediaMetadata.artist?.toString()
            val metadataPhotoUrl = pc.mediaMetadata.artworkUri?.toString()

            if (!metadataTitle.isNullOrBlank()) {
                finalTitle = metadataTitle
            }
            if (!metadataCreator.isNullOrBlank()) {
                finalCreator = metadataCreator
            }
            if (!metadataPhotoUrl.isNullOrBlank()) {
                finalPhotoUrl = metadataPhotoUrl
            }
        }, MoreExecutors.directExecutor())
    }

    Column {
        AudioPlayer(
            audioUrl = finalAudioUrl,
            title = finalTitle,
            photoUrl = finalPhotoUrl,
            creator = finalCreator,
            navController = navController
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
    if (isPlaying) {
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
    audioUrl: String, // This acts as a fallback or "new play" target
    creator: String,
    title: String,
    photoUrl: String,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    // UI State driven by the Controller
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    // Slider State
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    // 1. Setup the connection to the persistent service
    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            mediaController = controller

            // Sync initial state
            isPlaying = controller.isPlaying
            duration = controller.duration.coerceAtLeast(0L)
            playbackSpeed = controller.playbackParameters.speed

            // Try to restore playback position from saved state
            val playbackStateManager = com.pod_chive.android.playback.PlaybackStateManager(context)
            val savedState = playbackStateManager.getPlaybackState(audioUrl)
            if (savedState != null && savedState.currentPosition > 0) {
                // Only restore if the saved position is reasonable (not at the very end)
                if (savedState.currentPosition < savedState.duration * 0.95) {
                    controller.seekTo(savedState.currentPosition)
                    android.util.Log.d("PLAYBACK", "Restored playback position: ${savedState.currentPosition}ms / ${savedState.duration}ms")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)) // Deep charcoal/black
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp

        // Calculate dynamic artwork size based on screen height
        // Leave room for controls (~320dp) and padding
        val availableHeight = screenHeight - 320
        val artworkSize = (availableHeight * 0.5f).coerceIn(150f, 280f).dp

        // Queue button at top right
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        }
        // --- Artwork ---
        Box(
            modifier = Modifier
                .size(artworkSize)
                .shadow(20.dp, RoundedCornerShape(16.dp))
        ) {
            GlideImage(
                model = photoUrl,
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
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            text = creator,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp)
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
            ), valueRange = 0f..1f
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(currentPosition), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Text(formatDuration(duration), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }

//        Spacer(modifier = Modifier.height(24.dp))

        // --- Controls ---
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.outline_pause_24 else R.drawable.play_arrow_24px),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
            modifier = Modifier.padding(bottom = 16.dp)
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
