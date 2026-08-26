package com.example.musicplayer.ui.adapter

import com.example.musicplayer.R
import com.example.musicplayer.data.local.PlaylistEntity
import com.example.musicplayer.data.local.TagEntity
import com.example.musicplayer.model.LibraryItem
import com.example.musicplayer.ui.activity.CreatePlaylistActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.example.musicplayer.ui.view.setOnClickListenerWithPressAnimation

class LibraryAdapter(
    private val items: List<LibraryItem>,
    private val onPlaylistClick: (PlaylistEntity) -> Unit,
    private val onTagClick: (TagEntity) -> Unit,
    private val onUntaggedClick: () -> Unit,
    private val onPlaylistLongClick: (PlaylistEntity) -> Unit
) : RecyclerView.Adapter<LibraryAdapter.ViewHolder>() {

    private fun primaryTextColor(view: TextView): Int {
        val value = TypedValue()
        view.context.theme.resolveAttribute(android.R.attr.textColorPrimary, value, true)
        return ContextCompat.getColorStateList(view.context, value.resourceId)?.defaultColor
            ?: view.currentTextColor
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(android.R.id.text1)
        val playlistName: TextView? = view.findViewById(R.id.playlistName)
        val playlistExpression: TextView? = view.findViewById(R.id.playlistExpression)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(
            if (viewType == 1) R.layout.item_library_playlist
            else android.R.layout.simple_list_item_1,
            parent,
            false
        )

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is LibraryItem.Playlist) 1 else 0

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
                holder.text.visibility = View.GONE
                holder.playlistName?.text = item.playlist.name
                holder.playlistExpression?.text = item.playlist.expression

                holder.itemView.setOnClickListenerWithPressAnimation {
                    onPlaylistClick(item.playlist)
                }

                holder.itemView.setOnLongClickListener {
                    onPlaylistLongClick(item.playlist)
                    true
                }
            }

            is LibraryItem.Tag -> {
                holder.text.visibility = View.VISIBLE
                holder.text.text = item.tag.name
                holder.text.textSize = 18f
                holder.text.setTextColor(
                    primaryTextColor(holder.text)
                )
                holder.text.setTypeface(null, android.graphics.Typeface.NORMAL)

                holder.itemView.setOnClickListenerWithPressAnimation {
                    onTagClick(item.tag)
                }
            }
            is LibraryItem.CreatePlaylist -> {
                holder.text.visibility = View.VISIBLE
                holder.text.text = "＋ プレイリスト作成"
                holder.text.textSize = 18f
                holder.itemView.setOnClickListenerWithPressAnimation {
                    holder.itemView.context.startActivity(
                        Intent(
                            holder.itemView.context,
                            CreatePlaylistActivity::class.java
                        )
                    )
                }
            }

            LibraryItem.Untagged -> {
                holder.text.visibility = View.VISIBLE
                holder.text.text = "タグなし"
                holder.text.textSize = 18f
                holder.text.setTextColor(primaryTextColor(holder.text))
                holder.itemView.setOnClickListenerWithPressAnimation { onUntaggedClick() }
            }

            LibraryItem.RecentlyAdded -> {
                holder.text.visibility = View.VISIBLE
                holder.text.text = "最近追加した曲"
                holder.text.textSize = 18f
                holder.text.setTextColor(primaryTextColor(holder.text))
                holder.itemView.setOnClickListenerWithPressAnimation {
                    val intent = Intent(holder.itemView.context, com.example.musicplayer.ui.activity.TagSongsActivity::class.java)
                    intent.putExtra("recentlyAdded", true)
                    holder.itemView.context.startActivity(intent)
                }
            }
        }
    }
}
