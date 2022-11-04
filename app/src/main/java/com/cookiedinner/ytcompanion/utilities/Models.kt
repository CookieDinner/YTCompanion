package com.cookiedinner.ytcompanion.utilities

data class YoutubeVideoMetadata(
    val title: String,
    val author_name: String,
    val author_url: String,
    val type: String,
    val height: Int,
    val width: Int,
    val version: Float,
    val provider_name: String,
    val provider_url: String,
    val thumbnail_height: Int,
    val thumbnail_width: Int,
    val thumbnail_url: String,
    val html: String,
    var video_link: String?
)