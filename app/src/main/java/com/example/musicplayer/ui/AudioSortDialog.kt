package com.example.musicplayer.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.ui.adapter.AudioAdapter

/** 曲一覧の並べ替え項目。 */
enum class AudioSortKey(val label: String) {
    TITLE("曲名"),
    ARTIST("アーティスト名"),
    ALBUM("アルバム"),
    ADDED("追加順"),
    PLAY_COUNT("再生回数")
}

/**
 * 曲を [key] で並べ替える。
 * 同じ値の曲は曲名、さらに ID の順に並べるので、並べ替える前の順番に関係なく結果は常に同じになる。
 * （同じ条件を何度選んでも順番が変わらない）
 */
fun sortAudio(list: List<AudioFile>, key: AudioSortKey, descending: Boolean): List<AudioFile> {
    val primary: Comparator<AudioFile> = when (key) {
        AudioSortKey.TITLE -> compareBy { it.title.lowercase() }
        AudioSortKey.ARTIST -> compareBy { it.artist.lowercase() }
        AudioSortKey.ALBUM -> compareBy { it.albumName.lowercase() }
        AudioSortKey.ADDED -> compareBy { it.addedAt }
        AudioSortKey.PLAY_COUNT -> compareBy { it.playCount }
    }
    val ordered = if (descending) primary.reversed() else primary
    return list.sortedWith(
        ordered
            .thenBy { it.title.lowercase() }
            .thenBy { it.id }
    )
}

/** 曲一覧で共通して使う並び替えダイアログ。 */
fun showAudioSortDialog(
    context: Context,
    current: List<AudioFile>,
    adapter: AudioAdapter,
    onSorted: (List<AudioFile>) -> Unit = {}
) {
    val keys = AudioSortKey.entries
    val labels = (keys.map { it.label } + "ランダム").toTypedArray()
    AlertDialog.Builder(context)
        .setTitle("並べ替える項目")
        .setItems(labels) { _, which ->
            if (which == keys.size) {
                val shuffled = current.shuffled()
                adapter.updateList(shuffled)
                onSorted(shuffled)
                return@setItems
            }
            AlertDialog.Builder(context)
                .setTitle("並び順")
                .setItems(arrayOf("昇順", "降順")) { _, order ->
                    val sorted = sortAudio(current, keys[which], descending = order == 1)
                    adapter.updateList(sorted)
                    onSorted(sorted)
                }
                .show()
        }
        .show()
}
