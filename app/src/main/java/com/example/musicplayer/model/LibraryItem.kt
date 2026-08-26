package com.example.musicplayer.model

import com.example.musicplayer.data.local.PlaylistEntity
import com.example.musicplayer.data.local.TagEntity
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

    object Untagged : LibraryItem()

    object RecentlyAdded : LibraryItem()
    
    object CreatePlaylist : LibraryItem()
}
