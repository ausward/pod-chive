package com.pod_chive.android

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

@kotlin.OptIn(ExperimentalGlideComposeApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PlayPod() {
    Column {
        AudioPlayer(
            audioUrl = "https://pod-chive.com/1A/1A_Presents_Milk_Streets_Holiday_Lollapalooza_The_Best_of_2024.mp3",
            title = "1A Presents: Milk Street's Holiday Lollapalooza",
            photoUrl = "https://pod-chive.com/1A/cover.webp"
        )
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@ExperimentalGlideComposeApi
@Composable
fun AudioPlayer(
    audioUrl: String,
    title: String,
    photoUrl: String
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var mediaController by remember { mutableStateOf<MediaController?>(null) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }


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
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        GlideImage(
            model = photoUrl,
            contentDescription = "Album art for $title",
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(12.dp)),
            loading = placeholder(R.mipmap.ic_launcher),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Slider
        Slider(
            value = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
            onValueChange = {
                mediaController?.seekTo((it * duration).toLong())
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatDuration(currentPosition), color = Color.White)
            Text(text = formatDuration(duration), color = Color.White)
        }


        Spacer(modifier = Modifier.height(24.dp))

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
                    imageVector = Drawable(R.drawable.replay_10_24px),
                    contentDescription = "Rewind 10 seconds",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

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
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

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
                    imageVector = Icons.Default.Forward30,
                    contentDescription = "Forward 30 seconds",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = {
                    mediaController?.stop()
                },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Speed Control
        Text(
            text = String.format(Locale.US, "Speed: %.2fx", playbackSpeed),
            color = Color.Gray
        )
        Slider(
            value = playbackSpeed,
            onValueChange = {
                playbackSpeed = it
                mediaController?.setPlaybackSpeed(it)
            },
            valueRange = 0.5f..2.0f,
            steps = 5,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
