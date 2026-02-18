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
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.http.Path
import java.io.File
import java.io.Serializable

data class PodchiveResponse(
    val results: List<Podcast>
) : Serializable

data class Podcast(
    val id: Int,
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val itunesAuthor: String?
) : Serializable


data class homeList(
    val podcasts: List<homeItem>
) : Serializable

@kotlinx.serialization.Serializable
data class homeItem(
    val podcast_title: String,
    val description: String,
    val rss_url: String,
    val html_summary_location: String,
    val output_directory: String,
    val cover_image_url: String? = null // New field for direct image URL
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(podcast_title)
        parcel.writeString(description)
        parcel.writeString(rss_url)
        parcel.writeString(html_summary_location)
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
    val episodes: List<Episode>
) : Serializable

data class Episode(
    @SerializedName("Title")
    val title: String,

    @SerializedName("description") // This one was lowercase in your sample
    val description: String?,

    @SerializedName("AudioFilePath")
    val audioFilePath: String
) : Serializable