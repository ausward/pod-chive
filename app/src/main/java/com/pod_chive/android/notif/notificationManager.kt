package com.pod_chive.android.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.pod_chive.android.MainActivity
import com.pod_chive.android.R
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.database.FavoritePodcast


class PodchiveNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "favorite_episode_updates"
        private const val SUMMARY_NOTIFICATION_ID = 41_001
        private const val TAG = "PodNotif"

        const val ACTION_PLAY_FROM_NOTIFICATION = "com.pod_chive.android.action.PLAY_FROM_NOTIFICATION"
        const val EXTRA_AUDIO_URL = "extra_audio_url"
        const val EXTRA_EPISODE_TITLE = "extra_episode_title"
        const val EXTRA_PODCAST_TITLE = "extra_podcast_title"
        const val EXTRA_IMAGE_URL = "extra_image_url"
        const val EXTRA_PUB_DATE = "extra_pub_date"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_TRANSCRIPT_URL = "extra_transcript_url"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifyNewEpisode(favorite: FavoritePodcast, episode: EpisodeDC) {
        if (!canPostNotifications()) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted. Skipping single-episode notification.")
            return
        }

        ensureChannel()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_PLAY_FROM_NOTIFICATION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_AUDIO_URL, episode.audioFilePath)
            putExtra(EXTRA_EPISODE_TITLE, episode.title)
            putExtra(EXTRA_PODCAST_TITLE, episode.creator ?: favorite.title)
            putExtra(EXTRA_IMAGE_URL, episode.photo ?: favorite.imageLocation)
            putExtra(EXTRA_PUB_DATE, episode.pubDate)
            putExtra(EXTRA_DESCRIPTION, episode.description)
            putExtra(EXTRA_TRANSCRIPT_URL, episode.transcript)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            (episode.audioFilePath + episode.title).hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )




        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_chive)
            .setContentTitle(context.getString(R.string.new_episode_from, favorite.title))
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
            .setColor(Color.GREEN)
            .setContentIntent(openAppPendingIntent)
            .build()

        try {
            val id = (favorite.feedLink + "|" + episode.audioFilePath).hashCode()
            NotificationManagerCompat.from(context).notify(id, notification)
            Log.d(TAG, "Posted episode notification id=$id title=${episode.title}")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException while posting episode notification", se)
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error posting episode notification", t)
        }
    }



    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        systemManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel ensured: $CHANNEL_ID")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public fun canPostNotifications(): Boolean {
        val granted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Log.w(TAG, "Notification permission missing")
        }
        return granted
    }
}




@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionHandler() {
    // Notification permission is only required for Android 13 (Tiramisu) and above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        )

        Log.e("NotificationPermissionHandler", "Permission state: ${permissionState.status}")
        if (!permissionState.status.isGranted) {
                // Request the permission directly
                SideEffect {
                    permissionState.launchPermissionRequest()
                }
        }
        if (permissionState.status.shouldShowRationale){
            SideEffect {
                permissionState.launchPermissionRequest()
            }
        }

    }
}
