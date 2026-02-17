package com.pod_chive.android

import android.annotation.SuppressLint
import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

@ExperimentalGlideComposeApi
@OptIn(ExperimentalGlideComposeApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlayPod(
navController: NavController) {
    // Default values if none provided (for testing or if navigating directly to the tab)
    var finalAudioUrl  by remember {mutableStateOf( " ")}// audioUrl ?: "https://pod-chive.com/1A/1A_Presents_Milk_Streets_Holiday_Lollapalooza_The_Best_of_2024.mp3"
    var finalTitle by remember{mutableStateOf( " ")} //title ?: "Milk Street's Holiday Lollapalooza"
    var finalPhotoUrl by remember { mutableStateOf( " ") } //= photoUrl ?: "https://pod-chive.com/1A/cover.webp"
    var finalCreator  by remember { mutableStateOf(" ") }//= creator ?: "1A"
    var controller by remember { mutableStateOf<MediaController?>(null) }
    val context = LocalContext.current

//    BackHandler() {navController.popBackStack() }



    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val pc = controllerFuture.get()
            controller = pc

            // Pull the metadata that was set in EpisodeRow
            finalTitle = pc.mediaMetadata.title?.toString() ?: "Unknown Title"
            finalCreator = pc.mediaMetadata.artist?.toString() ?: "Unknown Creator"
            finalPhotoUrl = pc.mediaMetadata.artworkUri?.toString() ?: ""
        }, MoreExecutors.directExecutor())
    }

    Column {
        AudioPlayer(
            audioUrl = finalAudioUrl,
            title = finalTitle,
            photoUrl = finalPhotoUrl,
            creator = finalCreator
        )
    }
}

@OptIn(UnstableApi::class)
@ExperimentalGlideComposeApi
@Composable
fun AudioPlayerOld(
    audioUrl: String,
    creator:String,
    title: String,
    photoUrl: String
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }




    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

    Box(modifier = Modifier
        .width(250.dp)
        .height(250.dp)) {
        GlideImage(
            model = photoUrl,
            contentDescription = "Album art for $title",
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            loading = placeholder(R.mipmap.ic_launcher),
            contentScale = ContentScale.Crop
        )
        }
//        Spacer(modifier = Modifier.height(29.dp))
        Text(
            textDecoration = TextDecoration.Underline,
            softWrap = true,
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
                text = creator,
        color = Color.LightGray,
        style = MaterialTheme.typography.bodySmall,

        )
//        Spacer(modifier = Modifier.height(24.dp))

        // Progress Slider

        Column(modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
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
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(currentPosition), color = Color.Gray, fontSize = 12.sp)
                Text(formatDuration(duration), color = Color.Gray, fontSize = 12.sp)
            }
        }

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    mediaController?.let {
                        val newPosition = (it.currentPosition - 10000).coerceAtLeast(0)
                        it.seekTo(newPosition)
                    }
                },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.replay_10_24px),
                    contentDescription = "Rewind 10 seconds",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

//            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = {
                    mediaController?.let {
                        if (it.isPlaying) it.pause() else {
                            if (it.mediaItemCount == 0) {
                                it.setMediaItem(MediaItem.fromUri(audioUrl))
                                it.prepare()
                            }
                            it.play()
                        }
                    }
                },
                modifier = Modifier.size(64.dp)
            ) {
                var id = R.drawable.play_arrow_24px
                if (isPlaying) {
                    id = R.drawable.outline_pause_24
                }
                Icon(
                    painter = painterResource(id = id),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

//            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = {
                    mediaController?.let {
                        val newPosition = (it.currentPosition + 30000).coerceAtMost(it.duration)
                        it.seekTo(newPosition)
                    }
                },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_forward_30_24),
                    contentDescription = "Forward 30 seconds",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
//            Spacer(modifier = Modifier.width(16.dp))

        }

//        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.background(Color.hsl(248.7F, 1F, .27f)).border(width = 3.dp, color = Color.White, shape =  RoundedCornerShape(3.dp)).padding(12.dp)
        ){
            IconButton(
                onClick = {
                    if (playbackSpeed > 0.5f){
                        playbackSpeed -= 0.25f
                    mediaController?.setPlaybackSpeed(playbackSpeed)
                }
                },
                modifier = Modifier.size(64.dp)
            ) { Icon(Icons.Filled.ArrowDownward,
                contentDescription = "Slower",
                tint = Color.White)}
            // Speed Control
            Column(modifier = Modifier.clickable(
                enabled = true,

                onClick = {
                    playbackSpeed = 1f; mediaController?.setPlaybackSpeed(
                    playbackSpeed
                )
                }).padding(vertical = 8.dp)) {
                Text(
                    text = String.format(Locale.US, "Speed:" ),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,

                    )
                Text(
                    text = String.format(Locale.US, "%.2fx", playbackSpeed),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,

                    )
            }
            IconButton(
                onClick = {
                    playbackSpeed = updateSpeedOnButtonPress(playbackSpeed)
                    mediaController?.setPlaybackSpeed(playbackSpeed)
                } ,
                modifier = Modifier.size(64.dp),
            ){
                Icon(Icons.Rounded.ArrowUpward,
                    contentDescription = "Faster",
                    tint = Color.White)
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
    photoUrl: String
) {
    val context = LocalContext.current
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
        // --- Artwork ---
        Box(
            modifier = Modifier
                .size(280.dp)
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

//        Spacer(modifier = Modifier.weight(1f))

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
            IconButton(onClick = { mediaController?.seekBack() }) {
                Icon(painterResource(R.drawable.replay_10_24px), null, tint = Color.White, modifier = Modifier.size(36.dp))
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
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.outline_pause_24 else R.drawable.play_arrow_24px),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            IconButton(onClick = { mediaController?.seekForward() }) {
                Icon(painterResource(R.drawable.outline_forward_30_24), null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

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
                IconButton(onClick = { if (playbackSpeed > 0.5f) mediaController?.setPlaybackSpeed(playbackSpeed - 0.25f) }) {
                    Icon(Icons.Filled.ArrowDownward, null, tint = Color.White)
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

                IconButton(onClick = { if (playbackSpeed < 3.0f) mediaController?.setPlaybackSpeed(playbackSpeed + 0.25f) }) {
                    Icon(Icons.Rounded.ArrowUpward, null, tint = Color.White)
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
