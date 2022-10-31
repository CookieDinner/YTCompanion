package com.cookiedinner.ytcompanion.models

import android.media.Image

data class BookmarkedVideo(
    val name: String,
    val link: String,
)

data class DownloadedVideo(
    val name: String,
    val fileLocation: String,
)

data class FabData(
    val isExpanded: Boolean? = null,
    val isVisible: Boolean? = null,
    val newOnClickCallback: (() -> Unit)? = null,
    val newIcon: Int? = null
)