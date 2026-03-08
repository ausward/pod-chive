package com.pod_chive.android.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.pod_chive.android.model.PodcastShow
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.http.Path
import java.io.File
import java.io.Serializable

data class PodchiveResponse(
    var results: List<Podcast>
) : Serializable {
    fun sort() {
        results = results.sortedBy { it.lastUpdate }
        }
}

data class Podcast(
    val id: Int,
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val itunesAuthor: String?,
    val lastUpdate: Long?
) : PodcastShow(title, url, imageUrl!!, null, false, description, itunesAuthor ),  Serializable


data class homeList(
    val podcasts: List<homeItem>
) : Serializable

@kotlinx.serialization.Serializable
/**
 * DO NOT CHANGE THE SIGNATURE. THE PARAMS MUST MACH TEH RESULTS FROM https://api.pod-chive.com/list_podcasts
 * @param podcast_title podcast SHOW title
 * @param description podcast show description
 * @param rss_url podcast OG rss feed
 * /@param html_summary_location  NOT USED, REMOVED
 * @param output_directory  location of podcast data on server
 * @param cover_image_url  location of podcast show cover image
 */
data class homeItem(
    val podcast_title: String,
    val description: String,
    val rss_url: String,
//    val html_summary_location: String,
    val output_directory: String,
    val cover_image_url: String? = null // New field for direct image URL
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
//        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(podcast_title)
        parcel.writeString(description)
        parcel.writeString(rss_url)
//        parcel.writeString(html_summary_location)
        parcel.writeString(output_directory)
        parcel.writeString(cover_image_url)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<homeItem> {
        override fun createFromParcel(parcel: Parcel): homeItem {
            return homeItem(parcel)
        }

        override fun newArray(size: Int): Array<homeItem?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        return "homeItem(podcast_Episode_title='$podcast_title', description='${description.slice(0..2)}', rss_url='$rss_url', html_summary_location='NOT USED', output_directory='$output_directory', cover_image_url='$cover_image_url')"

    }

}


// python based api.pod-chive.com client


object RetrofitClient {
    private const val BASE_URL = "https://api.pod-chive.com/"
    private var apiInstance: PodchiveApi? = null

    fun getInstance(context: Context): PodchiveApi {
        return apiInstance ?: synchronized(this) {
            val cacheSize = (10 * 1024 * 1024).toLong() // 10 MB
            val cacheDir = File(context.cacheDir, "http_cache")
            val myCache = Cache(cacheDir, cacheSize)

            val okHttpClient = OkHttpClient.Builder()
                .cache(myCache)
                // Optional: Add an interceptor here if the API doesn't
                // provide Cache-Control headers (see below)
                .build()

            val instance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PodchiveApi::class.java)

            apiInstance = instance
            instance
        }
    }
}


interface PodchiveApi {
    @Headers("accept: application/json")
    @GET("search_podcasts")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("format") format: String = "json"
    ): PodchiveResponse

    @GET("list_podcasts")
    suspend fun listPodcasts(): homeList

}



// pod-chive.com nginx details

object RetrofitClientFront {
    private const val BASE_URL = "https://pod-chive.com/"
    private var apiInstance: PodchiveFront? = null

    fun getInstance(context: Context): PodchiveFront {
        return apiInstance ?: synchronized(this) {
            val cacheSize = (10 * 1024 * 1024).toLong() // 10 MB
            val cacheDir = File(context.cacheDir, "http_cache2")
            val myCache = Cache(cacheDir, cacheSize)

            val okHttpClient = OkHttpClient.Builder()
                .cache(myCache)
                .build()

            val instance = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PodchiveFront::class.java)

            apiInstance = instance
            instance
        }
    }
}

interface PodchiveFront {

    @GET("{path}/out.json")
    suspend fun getPodDetails(@Path("path") path: String): PodcastDetailResponse

}




data class PodcastDetailResponse(
    @SerializedName("PodcastTitle")
    val podcastTitle: String,

    @SerializedName("Episodes")
    val episodeDCS: List<EpisodeDC>
) : Serializable

@kotlinx.serialization.Serializable
data class EpisodeDC(
    @SerializedName("Title")
    val title: String,

    @SerializedName("description") // This one was lowercase in your sample
    var description: String?,

    @SerializedName("AudioFilePath")
    var audioFilePath: String,

    @SerializedName("pubDate")
    val pubDate: String,

    @SerializedName("transcript")
    val transcript: String?,

    @SerializedName("creator")
    var creator: String?,

    @SerializedName("photo")
    var photo: String?

) : com.pod_chive.android.model.Episode(title, description, audioFilePath, pubDate, transcript,
    creator, photo),
    Serializable
