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
        val player = ExoPlayer.Builder(this)
            .setSeekForwardIncrementMs(30000)
            .build()

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
