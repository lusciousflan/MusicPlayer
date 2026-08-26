package com.example.musicplayer.ui.adapter

import com.example.musicplayer.R
import com.example.musicplayer.model.AudioFile
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
import android.widget.ImageView
import android.widget.CheckBox
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import androidx.appcompat.widget.PopupMenu
import com.example.musicplayer.ui.view.setOnClickListenerWithPressAnimation

class AudioAdapter(
    list: List<AudioFile>,
    private val getAlbumArtUri: (Long) -> Uri,
    private val onClick: (AudioFile, Int) -> Unit,
    private val onAddToQueue: (AudioFile) -> Unit,
    private val onEditTag: (AudioFile) -> Unit,
    private val onEditMemo: (AudioFile) -> Unit = {},
    private val onExcludeDirectory: (AudioFile) -> Unit = {},
    private val onSelectionChanged: (Int) -> Unit = {}
) : RecyclerView.Adapter<AudioAdapter.ViewHolder>() {

    private var list: List<AudioFile> = list

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.titleText)
        val artist: TextView = view.findViewById(R.id.artistText)
        val albumArt: ImageView = itemView.findViewById(R.id.albumArt)
        val selectionCheckBox: CheckBox = itemView.findViewById(R.id.selectionCheckBox)
    }

    private var currentPlayingIndex: Int = -1
    private var selectionMode = false
    private val selectedIds = mutableSetOf<Long>()

    fun startSelection() {
        selectionMode = true
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun stopSelection() {
        selectionMode = false
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getSelectedAudios(): List<AudioFile> = list.filter { it.id in selectedIds }

    fun isSelectionMode(): Boolean = selectionMode

    private fun toggleSelection(audio: AudioFile, position: Int) {
        if (!selectedIds.add(audio.id)) selectedIds.remove(audio.id)
        notifyItemChanged(position)
        onSelectionChanged(selectedIds.size)
    }

    fun setCurrentPlaying(index: Int) {
        val oldIndex = currentPlayingIndex
        currentPlayingIndex = index

        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (index != -1) notifyItemChanged(index)
    }

    fun updateList(newList: List<AudioFile>) {
        list = newList
        currentPlayingIndex = -1
        selectedIds.clear()
        notifyDataSetChanged()
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
        holder.selectionCheckBox.setOnCheckedChangeListener(null)
        holder.selectionCheckBox.visibility = if (selectionMode) View.VISIBLE else View.GONE
        holder.selectionCheckBox.isChecked = audio.id in selectedIds
        holder.selectionCheckBox.setOnCheckedChangeListener { _, checked ->
            if (checked != (audio.id in selectedIds)) toggleSelection(audio, position)
        }

        // RecyclerViewで再利用されたアイテムに前回のアニメーション状態を残さない
        holder.itemView.animate().cancel()
        holder.itemView.scaleX = 1f
        holder.itemView.scaleY = 1f

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

        if (audio.id in selectedIds) {
            holder.title.setTextColor(0xFF2196F3.toInt())
        } else if (isPlaying) {
            holder.title.setTextColor(0xFFFF9800.toInt())
        } else {
            holder.title.setTextColor(android.util.TypedValue().let { value ->
                holder.title.context.theme.resolveAttribute(android.R.attr.textColorPrimary, value, true)
                androidx.core.content.ContextCompat.getColorStateList(holder.title.context, value.resourceId)?.defaultColor
                    ?: holder.title.currentTextColor
            })
        }

        holder.itemView.setOnClickListenerWithPressAnimation {
            if (selectionMode) {
                toggleSelection(audio, position)
                return@setOnClickListenerWithPressAnimation
            }
            onClick(audio, position)
        }
        holder.itemView.setOnLongClickListener { view ->
            if (selectionMode) {
                toggleSelection(audio, position)
                return@setOnLongClickListener true
            }
            val popup = PopupMenu(view.context, view)
            popup.menu.add("キューに追加")
            popup.menu.add("タグ編集")
            popup.menu.add("メモ")
            popup.menu.add("このディレクトリを除外")

            popup.setOnMenuItemClickListener { item ->
                when (item.title) {

                    "キューに追加" -> {
                        onAddToQueue(audio)
                        true
                    }

                    "タグ編集" -> {
                        onEditTag(audio)
                        true
                    }

                    "メモ" -> {
                        onEditMemo(audio)
                        true
                    }

                    "このディレクトリを除外" -> {
                        onExcludeDirectory(audio)
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
