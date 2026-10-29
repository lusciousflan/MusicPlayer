package com.example.musicplayer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicplayer.R
import com.example.musicplayer.data.local.albumArtUri
import com.example.musicplayer.ui.activity.AlbumSongsActivity
import com.example.musicplayer.ui.view.setOnClickListenerWithPressAnimation

data class AlbumItem(val id: Long, val name: String)

class AlbumAdapter(initialItems: List<AlbumItem>) : RecyclerView.Adapter<AlbumAdapter.Holder>() {
    private var items = initialItems
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val art: ImageView = view.findViewById(R.id.albumArt)
        val name: TextView = view.findViewById(R.id.albumName)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
    )
    override fun getItemCount() = items.size
    fun updateList(newItems: List<AlbumItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        Glide.with(holder.art).load(albumArtUri(item.id))
            .placeholder(R.drawable.default_art).error(R.drawable.default_art).into(holder.art)
        holder.itemView.setOnClickListenerWithPressAnimation {
            holder.itemView.context.startActivity(AlbumSongsActivity.intent(holder.itemView.context, item.id, item.name))
        }
    }
}
