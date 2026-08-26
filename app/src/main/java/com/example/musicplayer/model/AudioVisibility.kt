package com.example.musicplayer.model

private val hiddenAudioTitlePrefixes = listOf(
    "AutoBackup_",
    "AutoPlaylist_"
)

fun isVisibleAudioTitle(title: String): Boolean =
    hiddenAudioTitlePrefixes.none { title.startsWith(it) }
