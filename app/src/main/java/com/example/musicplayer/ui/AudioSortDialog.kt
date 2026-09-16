package com.example.musicplayer.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.ui.adapter.AudioAdapter
import java.util.IdentityHashMap

private val sortBaseByAdapter = IdentityHashMap<AudioAdapter, List<AudioFile>>()

/** 曲一覧で共通して使う並び替えダイアログ。 */
fun showAudioSortDialog(
    context: Context,
    current: List<AudioFile>,
    adapter: AudioAdapter,
    onSorted: (List<AudioFile>) -> Unit = {}
) {
    val base = sortBaseByAdapter.getOrPut(adapter) { current.toList() }
    val labels = arrayOf("曲名", "アーティスト名", "アルバム", "追加順", "再生回数", "ランダム")
    AlertDialog.Builder(context)
        .setTitle("並べ替える項目")
        .setItems(labels) { _, which ->
            if (which == 5) {
                val shuffled = base.shuffled()
                adapter.updateList(shuffled)
                onSorted(shuffled)
                return@setItems
            }
            AlertDialog.Builder(context)
                .setTitle("並び順")
                .setItems(arrayOf("昇順", "降順")) { _, descending ->
                    val sorted = when (which) {
                        0 -> base.sortedBy { it.title.lowercase() }
                        1 -> base.sortedBy { it.artist.lowercase() }
                        2 -> base.sortedBy { it.albumId }
                        3 -> base.sortedBy { it.addedAt }
                        4 -> base.sortedBy { it.playCount }
                        else -> base
                    }.let { if (descending == 1) it.asReversed() else it }
                    adapter.updateList(sorted)
                    onSorted(sorted)
                }
                .show()
        }
        .show()
}
