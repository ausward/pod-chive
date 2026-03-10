package com.pod_chive.android.queue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.net.toUri
import com.pod_chive.android.model.Episode
import java.util.UUID

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
    var revealedRowId by remember { mutableStateOf<String?>(null) }

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
            delay(500)

            queueItems = queueManager.getQueue()
            currentIndex = queueManager.getCurrentIndex()

            // Load playback states for all queue items
            val states = mutableMapOf<String, PlaybackState>()
            queueItems.forEach { item ->
                val state = playbackStateManager.getPlaybackState(item.AudioUrl?:"")
                if (state != null) {
                    states[item.AudioUrl?:""] = state
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
                itemsIndexed(
                    items = queueItems,
                    key = { _, item -> item.idValue?: UUID.randomUUID().toString() }
                ) { index, item ->
                    val playbackState = playbackStates[item.AudioUrl]
                    QueueItemRow(
                        item = item,
                        isCurrentlyPlaying = index == currentIndex,
                        playbackState = playbackState,
                        isActionsRevealed = revealedRowId == item.idValue,
                        onRevealActions = { revealedRowId = item.idValue },
                        onHideActions = { if (revealedRowId == item.idValue) revealedRowId = null },
                        onMoveUp = {
                            if (index <= 0) return@QueueItemRow
                            coroutineScope.launch(Dispatchers.IO) {
                                queueManager.moveItem(index, index - 1)
                                withContext(Dispatchers.Main) {
                                    queueItems = queueManager.getQueue()
                                    currentIndex = queueManager.getCurrentIndex()
                                }
                            }
                        },
                        onMoveDown = {
                            if (index >= queueItems.lastIndex) return@QueueItemRow
                            coroutineScope.launch(Dispatchers.IO) {
                                queueManager.moveItem(index, index + 1)
                                withContext(Dispatchers.Main) {
                                    queueItems = queueManager.getQueue()
                                    currentIndex = queueManager.getCurrentIndex()
                                }
                            }
                        },
                        onMoveTop = {
                            if (index <= 0) return@QueueItemRow
                            coroutineScope.launch(Dispatchers.IO) {
                                queueManager.moveItem(index, 0)
                                withContext(Dispatchers.Main) {
                                    queueItems = queueManager.getQueue()
                                    currentIndex = queueManager.getCurrentIndex()
                                }
                            }
                        },
                        onMoveBottom = {
                            if (index >= queueItems.lastIndex) return@QueueItemRow
                            coroutineScope.launch(Dispatchers.IO) {
                                queueManager.moveItem(index, queueItems.lastIndex)
                                withContext(Dispatchers.Main) {
                                    queueItems = queueManager.getQueue()
                                    currentIndex = queueManager.getCurrentIndex()
                                }
                            }
                        },
                        onPlay = {
                            val player = controller?: return@QueueItemRow

                            val mediaItem = MediaItem.Builder()
                                .setMediaId(item.AudioUrl!!)
                                .setUri(item.AudioUrl!!.toUri())
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle(item.EpisodeName)
                                        .setArtist(item.Creator)
                                        .setArtworkUri(item.PhotoUrl!!.toUri())
                                        .build()
                                )
                                .build()

                            Toast.makeText(context, "Playing: ${item.EpisodeName}", Toast.LENGTH_SHORT).show()

                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.play()

                            // PlaybackService will automatically restore position when STATE_READY

                            // Move this item to the top of the queue
                            queueManager.moveToTop(item.idValue?:"")
                            queueItems = queueManager.getQueue()
                            currentIndex = queueManager.getCurrentIndex()

                            // Navigate to PlayPod
                            val encodedAudioUrl = Uri.encode(item.AudioUrl)
                            val encodedTitle = Uri.encode(item.EpisodeName)
                            val encodedPhotoUrl = Uri.encode(item.PhotoUrl)
                            val encodedCreator = Uri.encode(item.Creator)
                            val encodedDescription = Uri.encode(item.Description)
                            val encodededtrans = Uri.encode(item.TranscriptUrl)
                            val encodedDate = Uri.encode(item.PublishDate)
                            navController.navigate(
                                "playpod?audioUrl=$encodedAudioUrl&title=$encodedTitle&photoUrl=$encodedPhotoUrl&creator=$encodedCreator&desc=$encodedDescription&transcripturl=$encodededtrans&publishDate=$encodedDate"
                            )

                        },
                        onRemove = {
                            coroutineScope.launch(Dispatchers.IO) {
                                queueManager.removeFromQueue(item.idValue?:"")
                                withContext(Dispatchers.Main) {
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
    item: Episode,
    isCurrentlyPlaying: Boolean,
    playbackState: PlaybackState?,
    isActionsRevealed: Boolean,
    onRevealActions: () -> Unit,
    onHideActions: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveTop: () -> Unit,
    onMoveBottom: () -> Unit,
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
                .pointerInput(item.idValue) {
                    var totalDragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            totalDragX += dragAmount
                            if (abs(totalDragX) > 6f) change.consume()
                        },
                        onDragEnd = {
                            when {
                                totalDragX > 80f -> onRevealActions()
                                totalDragX < -80f -> onHideActions()
                            }
                        }
                    )
                }
                .clickable { onPlay() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isActionsRevealed) {
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.combinedClickable(
                            onClick = onMoveUp,
                            onLongClick = onMoveTop
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Move up (hold for top)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.combinedClickable(
                            onClick = onMoveDown,
                            onLongClick = onMoveBottom
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Move down (hold for bottom)",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            GlideImage(
                model = item.PhotoUrl,
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
                    text = item.EpisodeName?:"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.Creator?:"",
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

@Composable
fun PlayBackProgressVis(playbackState: PlaybackState?) {
    // Display playback progress if available
    if (playbackState?.currentPosition != playbackState?.duration) {
        if (playbackState != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${formatTime(playbackState.currentPosition)} / ${formatTime(playbackState.duration)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 11.sp
            )
            LinearProgressIndicator(
                progress = {
                    (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(
                        0f,
                        1f
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(top = 4.dp),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    } else if (playbackState?.duration == playbackState?.currentPosition) {
        Text(
            text = "completed",
            fontStyle = MaterialTheme.typography.bodySmall.fontStyle,
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 11.sp
        )
    }


}
