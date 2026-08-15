package com.example.musicplayer.domain.playlist

data class PlaylistRule(
    val includeTags: Set<String> = emptySet(),
    val excludeTags: Set<String> = emptySet()
)
