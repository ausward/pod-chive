package com.pod_chive.android.api

import android.util.Log
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import java.io.Serializable

// This class will handle parsing RSS feeds and converting them to our app's data models
object RssDataSource {

    private val parser = RssParser()

    suspend fun parseRssFeed(rssFeedUrl: String): RssFeedResult {
        return try {
            val channel = parser.getRssChannel(rssFeedUrl)
//            Log.d("RssDataSource", "Channel: $channel")
            val homeItem = channel.toHomeItem(rssFeedUrl)
            val episodes = channel.items?.mapNotNull { it.toEpisode() }
//        Log.d("RssDataSource", "Episodes: $episodes")
            RssFeedResult.Success(homeItem, episodes)
        } catch (e: Exception) {
            RssFeedResult.Error(e.localizedMessage ?: "Unknown error parsing RSS feed")
        }
    }

    // Helper function to convert RssChannel to homeItem
    private fun RssChannel.toHomeItem(originalRssFeedUrl: String): homeItem {
        val imageUrl = this.image?.url

        val temp = homeItem(
            podcast_title = this.title ?: "Untitled Podcast",
            description = this.description ?: "No description available.",
            rss_url = originalRssFeedUrl,
            html_summary_location = this.link ?: "",
            output_directory = "",
            cover_image_url = imageUrl
        )
//        Log.d("RssDataSource", "HomeItem: $temp")
        return temp
    }

    // Helper function to convert RssItem to Episode
    private fun RssItem.toEpisode(): Episode? {
        // Ensure we have an audio URL from the enclosure
        val audioUrl = this.rawEnclosure?.url ?: return null

        val temp = Episode(
            title = this.title ?: "Untitled Episode",
            description = this.content ?: this.description, // Prioritize content:encoded
            audioFilePath = audioUrl,
            pubDate = this.pubDate ?: ""
        )
//        Log.d("RssDataSource", "Episode: $temp")
        return temp
    }
}

sealed class RssFeedResult : Serializable {
    data class Success(val podcast: homeItem, val episodes: List<Episode>?) : RssFeedResult() // Modified
    data class Error(val message: String) : RssFeedResult()
}
