package com.pod_chive.android

import android.content.Intent
import android.os.Bundle
import android.media.AudioManager
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
import androidx.media3.common.Player
import androidx.core.net.toUri

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var playbackStateManager: com.pod_chive.android.playback.PlaybackStateManager
    private var lastRestoredUrl: String? = null
    private lateinit var audioManager: AudioManager
    private var audioFocusListener: AudioManager.OnAudioFocusChangeListener? = null

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

        audioManager = getSystemService(AudioManager::class.java)

        playbackStateManager = com.pod_chive.android.playback.PlaybackStateManager(this)

        val player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(30000)
            .build()

        // Add listener to handle restoration and queue management
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                // Reset flag when media item changes
                lastRestoredUrl = null
                android.util.Log.d("PLAYBACK", "Media item changed: ${mediaItem?.mediaId}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // Duration is now loaded - safe to restore
                        val currentUrl = player.currentMediaItem?.mediaId
                        if (currentUrl != null && currentUrl != lastRestoredUrl) {
                            android.util.Log.d("PLAYBACK", "STATE_READY: Restoring position for $currentUrl")
                            restorePlaybackState(player)
                            lastRestoredUrl = currentUrl
                        }
                    }
                    Player.STATE_ENDED -> {
                        handleEpisodeFinished(player)
                    }
                    Player.STATE_IDLE, Player.STATE_BUFFERING -> {
                        // No action needed for these states
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    savePlaybackState(player)
                    // Request audio focus when starting playback
                    requestAudioFocus()
                } else {
                    // Save state when playback stops
                    savePlaybackState(player)
                    // Abandon audio focus when stopping
                    abandonAudioFocus()
                }
            }

            private fun savePlaybackState(player: ExoPlayer) {
                val mediaItem = player.currentMediaItem ?: return
                try {
                    val duration = player.duration
                    val position = player.currentPosition

                    android.util.Log.d("PLAYBACK", "Save: Checking - position: ${position}ms, duration: ${duration}ms")

                    // Only save if duration has been loaded and is valid
                    if (duration > 0) {
                        val state = com.pod_chive.android.playback.PlaybackState(
                            audioUrl = mediaItem.mediaId,
                            title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                            creator = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                            photoUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: "",
                            currentPosition = position.coerceAtLeast(0),
                            duration = duration.coerceAtLeast(0),
                            playbackSpeed = player.playbackParameters.speed
                        )
                        playbackStateManager.savePlaybackState(state)
                        android.util.Log.d("PLAYBACK", "✓ Saved: ${state.title} at ${state.currentPosition}ms / ${state.duration}ms @ ${state.playbackSpeed}x")
                    } else {
                        android.util.Log.d("PLAYBACK", "Save: Skipped - duration not ready ($duration)")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PLAYBACK", "Save: Error - ${e.message}", e)
                }
            }

            private fun restorePlaybackState(player: ExoPlayer) {
                val mediaItem = player.currentMediaItem ?: return
                val audioUrl = mediaItem.mediaId

                try {
                    android.util.Log.d("PLAYBACK", "Restore: Looking for saved state for $audioUrl")
                    val savedState = playbackStateManager.getPlaybackState(audioUrl)

                    if (savedState != null) {
                        android.util.Log.d("PLAYBACK", "Restore: Found - pos=${savedState.currentPosition}ms, dur=${savedState.duration}ms, speed=${savedState.playbackSpeed}x")

                        if (savedState.currentPosition > 0 && savedState.duration > 0) {
                            val isAtEnd = savedState.currentPosition >= savedState.duration * 0.95

                            if (!isAtEnd) {
                                try {
                                    // Use seekTo with immediate effect
                                    player.seekTo(savedState.currentPosition)
                                    player.setPlaybackSpeed(savedState.playbackSpeed)

                                    android.util.Log.d("PLAYBACK", "✓ Restored: ${savedState.title} @ ${savedState.currentPosition}ms, speed=${savedState.playbackSpeed}x")
                                    android.util.Log.d("PLAYBACK", "  Player position after seek: ${player.currentPosition}ms")
                                } catch (e: Exception) {
                                    android.util.Log.e("PLAYBACK", "Restore: Seek failed - ${e.message}")
                                }
                            } else {
                                android.util.Log.d("PLAYBACK", "✗ Restore: Skipped - at end (${savedState.currentPosition}ms / ${savedState.duration}ms)")
                            }
                        } else {
                            android.util.Log.d("PLAYBACK", "✗ Restore: Invalid data (pos=${savedState.currentPosition}ms, dur=${savedState.duration}ms)")
                        }
                    } else {
                        android.util.Log.d("PLAYBACK", "✗ Restore: No saved state found for $audioUrl")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PLAYBACK", "Restore: Error - ${e.message}", e)
                }
            }

            private fun handleEpisodeFinished(player: ExoPlayer) {
                val audioUrl = player.currentMediaItem?.mediaId ?: return

                try {
                    val queueManager = com.pod_chive.android.queue.PlayQueueManager(this@PlaybackService)
                    val nextItem = queueManager.getNextItem()

                    queueManager.removeByAudioUrl(audioUrl)
                    android.util.Log.d("QUEUE", "Episode finished: $audioUrl")

                    if (nextItem != null) {
                        val nextMediaItem = androidx.media3.common.MediaItem.Builder()
                            .setMediaId(nextItem.AudioUrl?:"")
                            .setUri(nextItem.AudioUrl?.toUri())
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(nextItem.EpisodeName)
                                    .setArtist(nextItem.Creator)
                                    .setArtworkUri(nextItem.PhotoUrl?.toUri())
                                    .build()
                            )
                            .build()

                        player.setMediaItem(nextMediaItem)
                        player.prepare()
                        player.play()
                        android.util.Log.d("QUEUE", "Playing next: ${nextItem.EpisodeName}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("QUEUE", "Error handling finished episode: ${e.message}")
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

        val sessionIntent = packageManager.getLaunchIntentForPackage(packageName)!!
        val sessionPendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionPendingIntent)
            .setCallback(callback)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun requestAudioFocus() {
        try {
            audioFocusListener = object : AudioManager.OnAudioFocusChangeListener {
                override fun onAudioFocusChange(focusChange: Int) {
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            mediaSession?.player?.pause()
                            android.util.Log.d("AUDIO_FOCUS", "Lost focus - paused")
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            mediaSession?.player?.pause()
                            android.util.Log.d("AUDIO_FOCUS", "Transient loss - paused")
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            mediaSession?.player?.setPlaybackSpeed(0.5f)
                            android.util.Log.d("AUDIO_FOCUS", "Transient loss (can duck) - reduced volume")
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            mediaSession?.player?.play()
                            mediaSession?.player?.setPlaybackSpeed(1f)
                            android.util.Log.d("AUDIO_FOCUS", "Gained focus - resumed")
                        }
                    }
                }
            }

            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                audioFocusListener!!,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                android.util.Log.d("AUDIO_FOCUS", "✓ Audio focus granted")
            } else {
                android.util.Log.d("AUDIO_FOCUS", "✗ Audio focus denied")
            }
        } catch (e: Exception) {
            android.util.Log.e("AUDIO_FOCUS", "Error requesting audio focus: ${e.message}")
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (audioFocusListener != null) {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusListener)
                audioFocusListener = null
                android.util.Log.d("AUDIO_FOCUS", "✓ Audio focus abandoned")
            }
        } catch (e: Exception) {
            android.util.Log.e("AUDIO_FOCUS", "Error abandoning audio focus: ${e.message}")
        }
    }

    override fun onDestroy() {
        // Abandon audio focus when the service is destroyed
        abandonAudioFocus()

        mediaSession?.run {
            val player = this.player
            if (player.currentMediaItem != null && ::playbackStateManager.isInitialized) {
                val mediaItem = player.currentMediaItem
                try {
                    val state = com.pod_chive.android.playback.PlaybackState(
                        audioUrl = mediaItem?.mediaId ?: return@run,
                        title = mediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                        creator = mediaItem.mediaMetadata.artist?.toString() ?: "Unknown",
                        photoUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: "",
                        currentPosition = player.currentPosition.coerceAtLeast(0),
                        duration = player.duration.coerceAtLeast(0),
                        playbackSpeed = player.playbackParameters.speed
                    )
                    playbackStateManager.savePlaybackState(state)
                    android.util.Log.d("PLAYBACK", "✓ Saved on destroy: ${state.title}")
                } catch (e: Exception) {
                    android.util.Log.e("PLAYBACK", "Error in onDestroy: ${e.message}")
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
