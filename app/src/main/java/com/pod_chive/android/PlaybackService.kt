package com.pod_chive.android

import android.content.Intent
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import android.app.PendingIntent

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var playbackStateManager: com.pod_chive.android.playback.PlaybackStateManager

    private val forward30Button by lazy {
        CommandButton.Builder()
            .setDisplayName("Forward 30s")
            .setSessionCommand(SessionCommand("COMMAND_FORWARD_30", Bundle.EMPTY))
            .setIconResId(R.drawable.outline_forward_30_24)
            .build()
    }

    private val speedButton by lazy {
        CommandButton.Builder()
            .setDisplayName("Speed")
            .setSessionCommand(SessionCommand("COMMAND_TOGGLE_SPEED", Bundle.EMPTY))
            .setIconResId(R.drawable.speed_24px)
            .build()
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()

        // Initialize playback state manager once
        playbackStateManager = com.pod_chive.android.playback.PlaybackStateManager(this)

        val player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(30000)
            .build()


        // Add listener to handle queue management when playback ends
        player.addListener(object : androidx.media3.common.Player.Listener {
            private var lastRestoredUrl: String? = null

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    androidx.media3.common.Player.STATE_READY -> {
                        // When ready to play, restore position if available (only once per media item)
                        val currentUrl = player.currentMediaItem?.mediaId
                        if (currentUrl != null && currentUrl != lastRestoredUrl) {
                            restorePlaybackPosition(player, playbackStateManager)
                            lastRestoredUrl = currentUrl
                        }
                    }
                    androidx.media3.common.Player.STATE_ENDED -> {
                        lastRestoredUrl = null
                        val currentMediaItem = player.currentMediaItem
                        currentMediaItem?.mediaId?.let { audioUrl ->
                            val queueManager = com.pod_chive.android.queue.PlayQueueManager(this@PlaybackService)

                            // Get next episode BEFORE removing current
                            val nextItem = queueManager.getNextItem()

                            // Now remove current episode from queue
                            queueManager.removeByAudioUrl(audioUrl)
                            android.util.Log.d("QUEUE", "PlaybackService: Episode finished, removed from queue: $audioUrl")

                            if (nextItem != null) {
                                android.util.Log.d("QUEUE", "PlaybackService: Playing next in queue: ${nextItem.title}")

                                // Create and play next media item
                                val nextMediaItem = androidx.media3.common.MediaItem.Builder()
                                    .setMediaId(nextItem.audioUrl)
                                    .setUri(android.net.Uri.parse(nextItem.audioUrl))
                                    .setMediaMetadata(
                                        androidx.media3.common.MediaMetadata.Builder()
                                            .setTitle(nextItem.title)
                                            .setArtist(nextItem.creator)
                                            .setArtworkUri(android.net.Uri.parse(nextItem.photoUrl))
                                            .build()
                                    )
                                    .build()

                                player.setMediaItem(nextMediaItem)
                                player.prepare()
                                player.play()
                            } else {
                                android.util.Log.d("QUEUE", "PlaybackService: No more items in queue")
                            }
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // Save state when playback is paused or stopped
                if (!isPlaying) {
                    savePlaybackState(player, playbackStateManager)
                    android.util.Log.d("PLAYBACK", "Paused - saved playback state")
                }
            }

            private fun savePlaybackState(
                player: androidx.media3.exoplayer.ExoPlayer,
                manager: com.pod_chive.android.playback.PlaybackStateManager
            ) {
                val currentMediaItem = player.currentMediaItem
                currentMediaItem?.let { mediaItem ->
                    try {
                        val state = com.pod_chive.android.playback.PlaybackState(
                            audioUrl = mediaItem.mediaId,
                            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                            creator = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                            photoUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: "",
                            currentPosition = player.currentPosition.coerceAtLeast(0),
                            duration = player.duration.coerceAtLeast(0),
                            playbackSpeed = player.playbackParameters.speed
                        )
                        manager.savePlaybackState(state)
                        android.util.Log.d("PLAYBACK", "Saved: ${state.title} at ${state.currentPosition}ms / ${state.duration}ms, speed: ${state.playbackSpeed}x")
                    } catch (e: Exception) {
                        android.util.Log.e("PLAYBACK", "Error saving state: ${e.message}")
                    }
                }
            }

            private fun restorePlaybackPosition(
                player: androidx.media3.exoplayer.ExoPlayer,
                manager: com.pod_chive.android.playback.PlaybackStateManager
            ) {
                val currentMediaItem = player.currentMediaItem ?: return
                val audioUrl = currentMediaItem.mediaId

                android.util.Log.d("PLAYBACK", "PlaybackService: Attempting to restore position for: $audioUrl")

                var savedState = manager.getPlaybackState(audioUrl)

                // Try different URL matching strategies
                if (savedState == null && audioUrl.contains("%")) {
                    val decodedUrl = try {
                        android.net.Uri.decode(audioUrl)
                    } catch (e: Exception) {
                        audioUrl
                    }
                    savedState = manager.getPlaybackState(decodedUrl)
                    if (savedState != null) {
                        android.util.Log.d("PLAYBACK", "Found state using decoded URL")
                    }
                }

                // Search all states for a match
                if (savedState == null) {
                    val allStates = manager.getAllPlaybackStates()
                    savedState = allStates.values.firstOrNull {
                        it.audioUrl == audioUrl ||
                        try {
                            android.net.Uri.decode(it.audioUrl) == audioUrl ||
                            android.net.Uri.decode(it.audioUrl) == android.net.Uri.decode(audioUrl)
                        } catch (e: Exception) {
                            false
                        }
                    }
                }

                if (savedState != null) {
                    android.util.Log.d("PLAYBACK", "Found saved state: ${savedState.title}")
                    // Check if position is reasonable (not at the very end and greater than 0)
                    if (savedState.currentPosition > 0 &&
                        savedState.duration > 0 &&
                        savedState.currentPosition < savedState.duration * 0.95) {

                        try {
                            player.seekTo(savedState.currentPosition)
                            // Restore playback speed
                            player.setPlaybackSpeed(savedState.playbackSpeed)
                            android.util.Log.d("PLAYBACK", "✓ Restored position: ${savedState.currentPosition}ms / ${savedState.duration}ms, speed: ${savedState.playbackSpeed}x")
                        } catch (e: Exception) {
                            android.util.Log.e("PLAYBACK", "Error seeking or setting speed: ${e.message}")
                        }
                    } else {
                        android.util.Log.d("PLAYBACK", "✗ Skipped restore - position unreasonable: ${savedState.currentPosition}ms / ${savedState.duration}ms")
                    }
                } else {
                    android.util.Log.d("PLAYBACK", "✗ No saved state found for: $audioUrl")
                }
            }
        })

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand("COMMAND_FORWARD_30", Bundle.EMPTY))
                    .add(SessionCommand("COMMAND_TOGGLE_SPEED", Bundle.EMPTY))
                    .build()

                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setCustomLayout(listOf(forward30Button, speedButton))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    "COMMAND_FORWARD_30" -> {
                        session.player.seekForward()
                    }
                    "COMMAND_TOGGLE_SPEED" -> {
                        val currentSpeed = session.player.playbackParameters.speed
                        val newSpeed = if (currentSpeed < 2.0f) 2.5f else 1.0f
                        session.player.setPlaybackSpeed(newSpeed)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        val sessionIntent = packageManager.getLaunchIntentForPackage(packageName)!! // Force non-nullable
        val sessionPendingIntent: PendingIntent = PendingIntent.getActivity( // Now this can be non-nullable
            this,
            0,
            sessionIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionPendingIntent) // Pass the non-nullable PendingIntent
            .setCallback(callback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            // Save current playback state before destroying
            val player = this.player
            if (player.currentMediaItem != null && ::playbackStateManager.isInitialized) {
                val currentMediaItem = player.currentMediaItem
                currentMediaItem?.let { mediaItem ->
                    val state = com.pod_chive.android.playback.PlaybackState(
                        audioUrl = mediaItem.mediaId,
                        title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                        creator = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                        photoUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: "",
                        currentPosition = player.currentPosition.coerceAtLeast(0),
                        duration = player.duration.coerceAtLeast(0),
                        playbackSpeed = player.playbackParameters.speed
                    )
                    playbackStateManager.savePlaybackState(state)
                    android.util.Log.d("PLAYBACK", "onDestroy: Saved final state: ${state.title} at ${state.currentPosition}ms, speed: ${state.playbackSpeed}x")
                }
            }

            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}
