package com.pod_chive.android.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

data class PodchiveResponse(
    val results: List<Podcast>
)

data class Podcast(
    val id: Int,
    val url: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val itunesAuthor: String?
)

interface PodchiveApi {
    @Headers("accept: application/json")
    @GET("search_podcasts")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("format") format: String = "json"
    ): PodchiveResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.pod-chive.com/"

    val instance: PodchiveApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PodchiveApi::class.java)
    }
}
