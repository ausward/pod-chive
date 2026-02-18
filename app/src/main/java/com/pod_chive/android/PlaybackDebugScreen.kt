package com.pod_chive.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pod_chive.android.playback.PlaybackState
import com.pod_chive.android.playback.PlaybackStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackDebugScreen(navController: NavController) {
    val context = LocalContext.current
    val stateManager = remember { PlaybackStateManager(context) }
    var playbackStates by remember { mutableStateOf<Map<String, PlaybackState>>(emptyMap()) }

    // Load all playback states
    LaunchedEffect(Unit) {
        playbackStates = withContext(Dispatchers.IO) {
            stateManager.getAllPlaybackStates()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playback Debug") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            stateManager.clearAllPlaybackStates()
                            playbackStates = emptyMap()
                        }
                    ) {
                        Text("Clear All")
                    }
                }
            )
        }
    ) { padding ->
        if (playbackStates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No playback states saved yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(playbackStates.toList()) { (audioUrl, state) ->
                    PlaybackStateItem(
                        state = state,
                        onDelete = {
                            stateManager.removePlaybackState(audioUrl)
                            playbackStates = playbackStates.filterKeys { it != audioUrl }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun PlaybackStateItem(
    state: PlaybackState,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    val lastPlayedDate = dateFormat.format(Date(state.lastPlayedAt))

    val progressPercent = if (state.duration > 0) {
        (state.currentPosition.toFloat() / state.duration.toFloat() * 100).toInt()
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Text(
                    text = state.creator,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }

        // Progress Info
        Text(
            text = "Progress: ${formatDuration(state.currentPosition)} / ${formatDuration(state.duration)} ($progressPercent%)",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Last Played
        Text(
            text = "Last played: $lastPlayedDate",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // URL (for debugging)
        Text(
            text = "URL: ${state.audioUrl.take(50)}...",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                .padding(4.dp)
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

