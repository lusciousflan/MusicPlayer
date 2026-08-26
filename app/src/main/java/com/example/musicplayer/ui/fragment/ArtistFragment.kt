package com.example.musicplayer.ui.fragment

import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.ui.adapter.ArtistAdapter

class ArtistFragment : Fragment(R.layout.fragment_artist) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val artists = mutableSetOf<String>()
        requireContext().contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media.ARTIST), "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
            MediaStore.Audio.Media.ARTIST)!!.use { c ->
            val index = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (c.moveToNext()) artists += c.getString(index)
        }
        view.findViewById<RecyclerView>(R.id.artistRecycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ArtistAdapter(artists.filter { it.isNotBlank() }.sorted())
        }
    }
}
