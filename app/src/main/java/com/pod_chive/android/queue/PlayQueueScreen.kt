package com.pod_chive.android.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.pod_chive.android.R
import com.pod_chive.android.playback.PlaybackState
import com.pod_chive.android.playback.PlaybackStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.ComponentName
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.pod_chive.android.PlaybackService

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlayQueueScreen(navController: NavController) {
    val context = LocalContext.current
    val queueManager = remember { PlayQueueManager(context) }
    val playbackStateManager = remember { PlaybackStateManager(context) }
    var queueItems by remember { mutableStateOf(queueManager.getQueue()) }
    var currentIndex by remember { mutableStateOf(queueManager.getCurrentIndex()) }
    var playbackStates by remember { mutableStateOf<Map<String, PlaybackState>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    var controller by remember { mutableStateOf<MediaController?>(null) }

    // Connect to MediaController
    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    // Refresh queue and playback states periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            queueItems = queueManager.getQueue()
            currentIndex = queueManager.getCurrentIndex()

            // Load playback states for all queue items
            val states = mutableMapOf<String, PlaybackState>()
            queueItems.forEach { item ->
                val state = playbackStateManager.getPlaybackState(item.audioUrl)
                if (state != null) {
                    states[item.audioUrl] = state
                }
            }
            playbackStates = states
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Play Queue (${queueItems.size})")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (queueItems.isNotEmpty()) {
                        IconButton(onClick = {
                            queueManager.clearQueue()
                            queueItems = emptyList()
                            currentIndex = 0
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear Queue"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (queueItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Queue is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add episodes to start playing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                itemsIndexed(queueItems) { index, item ->
                    val playbackState = playbackStates[item.audioUrl]
                    QueueItemRow(
                        item = item,
                        isCurrentlyPlaying = index == currentIndex,
                        playbackState = playbackState,
                        onPlay = {
                            val player = controller?: return@QueueItemRow

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(item.audioUrl)
                                .setUri(Uri.parse(item.audioUrl))
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(item.title)
                                        .setArtist(item.creator)
                                        .setArtworkUri(Uri.parse(item.photoUrl))
                                        .build()
                                )
                                .build()

                            Toast.makeText(context, "Playing: ${item.title}", Toast.LENGTH_SHORT).show()

                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.play()

                            // PlaybackService will automatically restore position when STATE_READY

                            // Move this item to the top of the queue
                            queueManager.moveToTop(item.id)
                            queueItems = queueManager.getQueue()
                            currentIndex = queueManager.getCurrentIndex()

                            // Navigate to PlayPod
                            val encodedAudioUrl = Uri.encode(item.audioUrl)
                            val encodedTitle = Uri.encode(item.title)
                            val encodedPhotoUrl = Uri.encode(item.photoUrl)
                            val encodedCreator = Uri.encode(item.creator)
                            navController.navigate(
                                "playpod?audioUrl=$encodedAudioUrl&title=$encodedTitle&photoUrl=$encodedPhotoUrl&creator=$encodedCreator"
                            )

                        },
                        onRemove = {
                            coroutineScope.launch(Dispatchers.IO) {
                                queueManager.removeFromQueue(item.id)
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    queueItems = queueManager.getQueue()
                                    currentIndex = queueManager.getCurrentIndex()
                                }
                            }
                        }
                    )
                    if (index < queueItems.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun QueueItemRow(
    item: QueueItem,
    isCurrentlyPlaying: Boolean,
    playbackState: PlaybackState?,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    val backgroundColor = if (isCurrentlyPlaying) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlay() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = item.photoUrl,
                contentDescription = "Episode artwork",
                modifier = Modifier
                    .size(60.dp)
                    .clip(MaterialTheme.shapes.small),
                loading = placeholder(R.mipmap.shrug),
                failure = placeholder(R.mipmap.shrug),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.creator,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Display playback progress if available
                if (playbackState != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${formatTime(playbackState.currentPosition)} / ${formatTime(playbackState.duration)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    LinearProgressIndicator(
                        progress = { (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isCurrentlyPlaying) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Currently playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove from queue",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

