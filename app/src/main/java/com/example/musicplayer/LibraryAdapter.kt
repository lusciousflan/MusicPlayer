package com.example.musicplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent

class LibraryAdapter(
    private val items: List<LibraryItem>,
    private val onPlaylistClick: (PlaylistEntity) -> Unit,
    private val onTagClick: (TagEntity) -> Unit,
    private val onPlaylistLongClick: (PlaylistEntity) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        when (val item = items[position]) {

            is LibraryItem.Header -> {

                holder.text.text = item.title
                holder.text.textSize = 12f
                holder.text.setTextColor(
                    android.graphics.Color.GRAY
                )
                holder.text.setTypeface(null, android.graphics.Typeface.NORMAL)

                holder.itemView.setOnClickListener(null)
            }

            is LibraryItem.Playlist -> {

                holder.text.text = item.playlist.name

                holder.text.textSize = 18f
                holder.text.setTextColor(
                    android.graphics.Color.BLACK
                )
                holder.text.setTypeface(null, android.graphics.Typeface.BOLD)

                holder.itemView.setOnClickListener {
                    onPlaylistClick(item.playlist)
                }

                holder.itemView.setOnLongClickListener {
                    onPlaylistLongClick(item.playlist)
                    true
                }
            }

            is LibraryItem.Tag -> {

                holder.text.text = item.tag.name

                holder.text.textSize = 18f
                holder.text.setTextColor(
                    android.graphics.Color.BLACK
                )
                holder.text.setTypeface(null, android.graphics.Typeface.NORMAL)

                holder.itemView.setOnClickListener {
                    onTagClick(item.tag)
                }
            }
            is LibraryItem.CreatePlaylist -> {

                holder.text.text = "＋ プレイリスト作成"
                holder.text.textSize = 18f

                holder.itemView.setOnClickListener {
                    holder.itemView.context.startActivity(
                        Intent(
                            holder.itemView.context,
                            CreatePlaylistActivity::class.java
                        )
                    )
                }
            }
        }
    }
}