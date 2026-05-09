package com.example.musicplayer

sealed class LibraryItem {

    data class Header(
        val title: String
    ) : LibraryItem()

    data class Playlist(
        val playlist: PlaylistEntity
    ) : LibraryItem()

    data class Tag(
        val tag: TagEntity
    ) : LibraryItem()
    
    object CreatePlaylist : LibraryItem()
}