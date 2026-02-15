package com.pod_chive.android

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

@ExperimentalGlideComposeApi
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalGlideComposeApi::class,
    ExperimentalGlideComposeApi::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlayPod() {
    Column {
        AudioPlayer(
            audioUrl = "https://pod-chive.com/1A/1A_Presents_Milk_Streets_Holiday_Lollapalooza_The_Best_of_2024.mp3",
            title = "Milk Street's Holiday Lollapalooza",
            photoUrl = "https://pod-chive.com/1A/cover.webp",
            creator = "1A"
        )
    }
}

@OptIn(UnstableApi::class)
@ExperimentalGlideComposeApi
@Composable
fun AudioPlayer(
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



    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
            }, MoreExecutors.directExecutor()
        )

        onDispose {
            mediaController?.release()
        }
    }

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
        }
        mediaController?.addListener(listener)

        onDispose {
            mediaController?.removeListener(listener)
        }
    }

    LaunchedEffect(mediaController, isPlaying) {
        while (true) {
            currentPosition = mediaController?.currentPosition ?: 0L
            duration = mediaController?.duration?.takeIf { it > 0 } ?: 0L
            delay(500)
        }
    }

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
//            Slider(
//                value = playbackSpeed,
//                onValueChange = {
//                    playbackSpeed = it
//                    mediaController?.setPlaybackSpeed(it)
//                },
//                valueRange = 0.5f..3.0f,
//                steps = 10,
//                modifier = Modifier.fillMaxWidth()
//            )
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
