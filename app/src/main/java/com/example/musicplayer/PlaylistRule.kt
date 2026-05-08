package com.example.musicplayer

data class PlaylistRule(
    val includeTags: Set<String> = emptySet(),
    val excludeTags: Set<String> = emptySet()
)