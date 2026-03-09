package com.pod_chive.android.model

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import androidx.savedstate.SavedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

val EpisodeNavType = object : NavType<Episode?>(isNullableAllowed = true) {


    override fun get(bundle: Bundle, key: String): Episode? {
        return bundle.getString(key)?.let { Json.decodeFromString(it) }
    }

    override fun parseValue(value: String): Episode {
        // We decode the Uri to handle special characters in URLs
        return Json.decodeFromString(Uri.decode(value))
    }

    override fun put(bundle: Bundle, key: String, value: Episode?) {
        bundle.putString(key, Json.encodeToString(value))
    }

    override fun serializeAsValue(value: Episode?): String {
        // We encode the string to make it safe for a URL path
        return Uri.encode(Json.encodeToString(value))
    }
}

@Serializable
open class Episode {
    var AudioUrl: String? = null
    var EpisodeName: String? = null
    var PublishDate: String? = null
    var PhotoUrl: String? = null

    @get:JvmName("getEpisodeCreator")
    @set:JvmName("setEpisodeCreator")
    var Creator: String? = null
    @get:JvmName("getEpisodeDescription")
    @set:JvmName("setEpisodeDescription")
    var Description: String? = null
    
    var TranscriptUrl: String? = null

    var TranscriptData: String? = null
    var duration: Long? = null


    constructor(audioUrl: String, episodeName: String, publishDate: String, photoUrl: String) {
        this.EpisodeName = episodeName
        this.PublishDate = publishDate
        this.PhotoUrl = photoUrl
        this.AudioUrl = audioUrl
    }

    constructor(title:String, description: String?, audioFilePath:String, pubDate: String,
                transcript:String?, creator:String?, PhotoUrl:String?)
    {
        this.EpisodeName = title
        this.Description = description
        this.AudioUrl = audioFilePath
        this.PublishDate = pubDate
        this.TranscriptUrl = transcript
        this.Creator = creator
        this.PhotoUrl = PhotoUrl
    }

    /**
     * Information constructor
     */
    constructor(desc:String?, transcript:String?, pubdate:String?, creator:String, title:String){
        this.Description = desc
        this.TranscriptUrl = transcript
        this.PublishDate = pubdate?:""
        this.Creator = creator
        this.EpisodeName = title
    }




}
@Serializable
open class PodcastShow{
    var PodcastName: String? = null
    var isRSS: Boolean = false
    var PodcastUrl: String? = null
    var Cover_Image: String? = null
    @Transient
    var EpisodeList:Array<Episode> = emptyArray()
    var html_summary_location: String? = null
    var audio_location: String? = null

    open var outputDirectory: String? = null


    var showDescription: String? = null
    var creator: String? = null


    constructor()

    constructor(PodcastName: String, PodcastUrl: String, image: String){
        this.PodcastName = PodcastName
        this.PodcastUrl = PodcastUrl
        this.Cover_Image = image
    }

    constructor(PodcastName: String){
        this.PodcastName = PodcastName
    }

    constructor(PodcastName: String, audio_location: String, Cover_Image: String, output_directory: String?, isRSS: Boolean, description: String?, creator: String?){
        this.PodcastName = PodcastName
        this.PodcastUrl = PodcastUrl
        this.Cover_Image = Cover_Image
        this.outputDirectory = output_directory!!
        this.isRSS = isRSS
        this.audio_location = audio_location
        this.showDescription = description
        this.creator = creator
    }


    constructor(PodcastName: String, description: String?, url: String, output_directory: String?, imageUrl: String?) {
        this.PodcastName = PodcastName
        this.showDescription = description
        this.PodcastUrl = url
        this.outputDirectory = output_directory!!
        this.Cover_Image = imageUrl
    }



}
