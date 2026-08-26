package com.example.musicplayer.ui.fragment

import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.ui.adapter.AlbumAdapter
import com.example.musicplayer.ui.adapter.AlbumItem

class AlbumFragment : Fragment(R.layout.fragment_album) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val albums = mutableListOf<AlbumItem>()
        requireContext().contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM),
            null, null, "${MediaStore.Audio.Albums.ALBUM} COLLATE NOCASE"
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val name = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            while (cursor.moveToNext()) albums += AlbumItem(cursor.getLong(id), cursor.getString(name))
        }
        view.findViewById<RecyclerView>(R.id.albumRecycler).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = AlbumAdapter(albums.distinctBy { it.id })
        }
    }
}
