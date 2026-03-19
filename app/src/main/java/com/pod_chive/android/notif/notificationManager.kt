package com.pod_chive.android.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pod_chive.android.MainActivity
import com.pod_chive.android.R
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.database.FavoritePodcast

class PodchiveNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "favorite_episode_updates"
        private const val CHANNEL_NAME = "Favorite podcast updates"
        private const val CHANNEL_DESCRIPTION = "Notifications for newly released episodes"
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyNewEpisode(favorite: FavoritePodcast, episode: EpisodeDC) {
        if (!canPostNotifications()) return

        ensureChannel()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            favorite.feedLink.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.new_icon)
            .setContentTitle("New episode from ${favorite.title}")
            .setContentText(episode.title)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildString {
                        append(episode.title)
                        if (episode.pubDate.isNotBlank()) {
                            append("\n")
                            append(episode.pubDate)
                        }
                    }
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            (favorite.feedLink + "|" + episode.audioFilePath).hashCode(),
            notification
        )
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
        }

        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}