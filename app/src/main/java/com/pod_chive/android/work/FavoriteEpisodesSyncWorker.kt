package com.pod_chive.android.work

import android.Manifest
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.api.RetrofitClientFront
import com.pod_chive.android.api.RssDataSource
import com.pod_chive.android.api.RssFeedResult
import com.pod_chive.android.database.FavoritePodcast
import com.pod_chive.android.database.FavoritePodcastRepository
import com.pod_chive.android.notif.PodchiveNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.net.toUri

class FavoriteEpisodesSyncJobService : JobService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null) return false

        serviceScope.launch {
            try {
                runSync(applicationContext)
                jobFinished(params, false)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync favorite episodes", e)
                jobFinished(params, true)
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        serviceScope.coroutineContext.cancelChildren()
        return true
    }

    private suspend fun runSync(context: Context) {
        val repository = FavoritePodcastRepository(context)
        val notifier = PodchiveNotificationManager(context)
        val stateStore = FavoriteEpisodeNotificationStateStore(context)

        val favorites = repository.getAllFavorites()
        favorites.forEach { favorite ->
            processFavorite(context, favorite, notifier, stateStore)
        }
    }

    private suspend fun processFavorite(
        context: Context,
        favorite: FavoritePodcast,
        notifier: PodchiveNotificationManager,
        stateStore: FavoriteEpisodeNotificationStateStore
    ) {
        val episodes = fetchEpisodesForFavorite(context, favorite)
            .sortedByDescending { parseEpisodeTimeMillis(it.pubDate) }

        if (episodes.isEmpty()) return

        val newestEpisodeId = episodeIdentity(episodes.first())
        val lastNotifiedEpisodeId = stateStore.getLastNotifiedEpisodeId(favorite.feedLink)

        if (lastNotifiedEpisodeId == null) {
            // First run baseline: track newest without spamming old episodes.
            stateStore.setLastNotifiedEpisodeId(favorite.feedLink, newestEpisodeId)
            return
        }

        if (lastNotifiedEpisodeId == newestEpisodeId) return

        val unseenEpisodes = episodes.takeWhile { episodeIdentity(it) != lastNotifiedEpisodeId }
        if (unseenEpisodes.isEmpty()) {
            stateStore.setLastNotifiedEpisodeId(favorite.feedLink, newestEpisodeId)
            return
        }

        unseenEpisodes
            .asReversed()
            .forEach { episode ->
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                notifier.notifyNewEpisode(favorite, episode)
            }

        stateStore.setLastNotifiedEpisodeId(favorite.feedLink, newestEpisodeId)
    }

    private suspend fun fetchEpisodesForFavorite(context: Context, favorite: FavoritePodcast): List<EpisodeDC> {
        return try {
            if (favorite.feedLink.contains("pod-chive.com")) {
                val directory = extractDirectoryFromFeedUrl(favorite.feedLink) ?: return emptyList()
                val response = RetrofitClientFront
                    .getInstance(context)
                    .getPodDetails(directory)
                response.episodeDCS.onEach { episode ->
                    if (episode.creator.isNullOrBlank()) {
                        episode.creator = favorite.title
                    }
                    if (episode.photo.isNullOrBlank()) {
                        episode.photo = favorite.imageLocation
                    }
                }
            } else {
                when (val parsed = RssDataSource.parseRssFeed(favorite.feedLink)) {
                    is RssFeedResult.Success -> parsed.episodeDCS ?: emptyList()
                    is RssFeedResult.Error -> {
                        Log.w(TAG, "RSS parse error for ${favorite.feedLink}: ${parsed.message}")
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load episodes for ${favorite.feedLink}", e)
            emptyList()
        }
    }

    private fun extractDirectoryFromFeedUrl(feedLink: String): String? {
        val segments = feedLink.toUri().pathSegments
        return segments.firstOrNull { it.isNotBlank() && !it.equals("feed.xml", true) && !it.equals("rss", true) }
    }

    private fun episodeIdentity(episode: EpisodeDC): String {
        return buildString {
            append(episode.audioFilePath)
            append('|')
            append(episode.title)
            append('|')
            append(episode.pubDate)
        }
    }

    private fun parseEpisodeTimeMillis(pubDate: String): Long {
        return try {
            val podchiveFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
            podchiveFormat.parse(pubDate)?.time ?: 0L
        } catch (_: Exception) {
            try {
                val rssFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                rssFormat.parse(pubDate)?.time ?: 0L
            } catch (_: Exception) {
                try {
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                    isoFormat.parse(pubDate)?.time ?: 0L
                } catch (_: Exception) {
                    0L
                }
            }
        }
    }

    companion object {
        private const val TAG = "FavoriteSyncJobService"
    }
}

private class FavoriteEpisodeNotificationStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("favorite_episode_notification_state", Context.MODE_PRIVATE)

    fun getLastNotifiedEpisodeId(feedLink: String): String? {
        return prefs.getString(feedLinkKey(feedLink), null)
    }

    fun setLastNotifiedEpisodeId(feedLink: String, episodeId: String) {
        prefs.edit { putString(feedLinkKey(feedLink), episodeId) }
    }

    private fun feedLinkKey(feedLink: String): String = "last_notified:${Uri.encode(feedLink)}"
}
