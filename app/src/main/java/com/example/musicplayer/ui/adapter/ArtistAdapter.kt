package com.example.musicplayer.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.ui.activity.ArtistSongsActivity
import com.example.musicplayer.ui.view.setOnClickListenerWithPressAnimation

class ArtistAdapter(artists: List<String>) : RecyclerView.Adapter<ArtistAdapter.Holder>() {
    private var artists: List<String> = artists
    class Holder(view: View) : RecyclerView.ViewHolder(view) { val text: TextView = view.findViewById(android.R.id.text1) }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false))
    override fun getItemCount() = artists.size
    fun updateList(newArtists: List<String>) { artists = newArtists; notifyDataSetChanged() }
    override fun onBindViewHolder(holder: Holder, position: Int) { holder.text.text = artists[position]; holder.itemView.setOnClickListenerWithPressAnimation { holder.itemView.context.startActivity(Intent(holder.itemView.context, ArtistSongsActivity::class.java).putExtra("artist", artists[position])) } }
}
