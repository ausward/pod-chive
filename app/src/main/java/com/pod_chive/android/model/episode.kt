package com.pod_chive.android.model

import kotlinx.serialization.Serializable

@Serializable
open class Episode {
    val AudioUrl: String
    val EpisodeName: String
    var PublishDate: String
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




}
@Serializable
open class PodcastShow{
    var PodcastName: String? = null
    var isRSS: Boolean = false
    var PodcastUrl: String? = null
    var Cover_Image: String? = null
    var EpisodeList:Array<Episode> = emptyArray()
    var html_summary_location: String? = null
    var audio_location: String? = null

    var output_directory: String? = null

    constructor()

    constructor(PodcastName: String){
        this.PodcastName = PodcastName
    }

    constructor(PodcastName: String, audio_location: String, Cover_Image: String, output_directory: String?, isRSS: Boolean){
        this.PodcastName = PodcastName
        this.PodcastUrl = PodcastUrl
        this.Cover_Image = Cover_Image
        this.output_directory = output_directory!!
        this.isRSS = isRSS
        this.audio_location = audio_location
    }

}
