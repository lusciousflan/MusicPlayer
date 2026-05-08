package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import androidx.appcompat.widget.PopupMenu

class AudioAdapter(
    private val list: List<AudioFile>,
    private val getAlbumArtUri: (Long) -> Uri,
    private val onClick: (AudioFile, Int) -> Unit,
    private val onAddToQueue: (AudioFile) -> Unit,
    private val onEditTag: (AudioFile) -> Unit
) : RecyclerView.Adapter<AudioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.titleText)
        val artist: TextView = view.findViewById(R.id.artistText)
        val albumArt: ImageView = itemView.findViewById(R.id.albumArt)
    }

    private var currentPlayingIndex: Int = -1

    fun setCurrentPlaying(index: Int) {
        val oldIndex = currentPlayingIndex
        currentPlayingIndex = index

        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (index != -1) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audio, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val audio = list[position]
        holder.title.text = audio.title
        holder.artist.text = audio.artist

        // val albumUri = getAlbumArtUri(audio.albumId)
        Glide.with(holder.itemView)
            .load(getAlbumArtUri(audio.albumId))
            .placeholder(R.drawable.default_art)
            .error(R.drawable.default_art)
            .into(holder.albumArt)

            val isPlaying = position == currentPlayingIndex

        // 再生中の曲をハイライトする
        holder.itemView.setBackgroundColor(
            if (isPlaying) 0x33FF9800  // 薄いオレンジ
            else 0x00000000
        )

        holder.title.setTextColor(
            if (isPlaying) 0xFFFF9800.toInt()
            else 0xFF000000.toInt()
        )

        holder.itemView.setOnClickListener {
            onClick(audio, position)
        }
        // holder.itemView.setOnLongClickListener {
        //     onAddToQueue(audio)
        //     true
        // }
        // holder.itemView.setOnLongClickListener {
        //     showTagDialog(it.context, audio.id, dao)
        //     true
        // }
        holder.itemView.setOnLongClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("キューに追加")
            popup.menu.add("タグ編集")

            popup.setOnMenuItemClickListener { item ->
                when (item.title) {

                    "キューに追加" -> {
                        onAddToQueue(audio)
                        true
                    }

                    "タグ編集" -> {
                        // showTagDialog(view.context, audio.id, dao)
                        onEditTag(audio)
                        true
                    }

                    else -> false
                }
            }

            popup.show()
            true
        }
        
    }
    
}