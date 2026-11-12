package com.example.musicplayer.playback

import android.content.Context
import android.content.SharedPreferences
import com.example.musicplayer.model.AudioFile

/**
 * アプリを終了しても再生キューが消えないように保存しておく内容。
 * 曲そのものではなく曲 ID だけを保存し、復元時にデータベースから曲情報を引き直す。
 */
data class SavedQueue(
    val audioIds: List<Long>,
    val currentIndex: Int,
    val positionMs: Long,
    val isShuffle: Boolean
)

/** 保存したキューを、今ある曲だけで組み立て直した結果。 */
data class RestoredQueue(
    val audios: List<AudioFile>,
    val currentIndex: Int,
    val positionMs: Long
)

/**
 * 保存したキューを復元する。
 * 保存後に削除された曲は飛ばす。再生中だった曲が消えていたら、その次に残っている曲の先頭から再開する
 * （後ろに残っていなければ、手前に残っている最後の曲）。
 *
 * @param audiosById 今データベースにある曲
 * @return 復元できる曲が1曲もなければ null
 */
fun restoreQueue(saved: SavedQueue, audiosById: Map<Long, AudioFile>): RestoredQueue? {
    val audios = mutableListOf<AudioFile>()
    var currentIndex = -1
    var positionMs = 0L
    saved.audioIds.forEachIndexed { savedIndex, id ->
        val audio = audiosById[id] ?: return@forEachIndexed
        if (currentIndex < 0 && savedIndex >= saved.currentIndex) {
            currentIndex = audios.size
            if (savedIndex == saved.currentIndex) positionMs = saved.positionMs
        }
        audios += audio
    }
    if (audios.isEmpty()) return null
    if (currentIndex < 0) currentIndex = audios.lastIndex
    return RestoredQueue(audios, currentIndex, positionMs.coerceAtLeast(0L))
}

fun encodeAudioIds(ids: List<Long>): String = ids.joinToString(",")

fun decodeAudioIds(text: String): List<Long> =
    text.split(',').mapNotNull { it.trim().toLongOrNull() }

/**
 * 再生キューを SharedPreferences に保存・読み込みする。
 *
 * SharedPreferences は1項目だけ変えてもファイル全体を書き直すため、
 * 曲が切り替わるたびに変わる再生位置と、1000曲を超えることもある曲 ID の一覧は別のファイルに分ける。
 * こうすると曲の切り替えでは小さいファイルだけを書き直せばよい。
 */
class QueueStore(context: Context) {
    private val itemsPreferences =
        context.getSharedPreferences("playback_queue_items", Context.MODE_PRIVATE)
    private val statePreferences =
        context.getSharedPreferences("playback_queue_state", Context.MODE_PRIVATE)

    /**
     * @param includeItems false なら曲 ID の一覧は書き直さず、再生位置などだけを保存する
     * @param sync true なら書き込み完了まで待つ（終了直前など、プロセスがすぐ止まりうるとき用）
     */
    fun save(queue: SavedQueue, includeItems: Boolean, sync: Boolean = false) {
        if (includeItems) {
            itemsPreferences.edit()
                .putString(KEY_IDS, encodeAudioIds(queue.audioIds))
                .finish(sync)
        }
        statePreferences.edit()
            .putInt(KEY_INDEX, queue.currentIndex)
            .putLong(KEY_POSITION, queue.positionMs)
            .putBoolean(KEY_SHUFFLE, queue.isShuffle)
            .finish(sync)
    }

    private fun SharedPreferences.Editor.finish(sync: Boolean) {
        if (sync) commit() else apply()
    }

    fun load(): SavedQueue? {
        val ids = decodeAudioIds(itemsPreferences.getString(KEY_IDS, "").orEmpty())
        if (ids.isEmpty()) return null
        return SavedQueue(
            audioIds = ids,
            currentIndex = statePreferences.getInt(KEY_INDEX, 0),
            positionMs = statePreferences.getLong(KEY_POSITION, 0L),
            isShuffle = statePreferences.getBoolean(KEY_SHUFFLE, false)
        )
    }

    private companion object {
        const val KEY_IDS = "audio_ids"
        const val KEY_INDEX = "current_index"
        const val KEY_POSITION = "position_ms"
        const val KEY_SHUFFLE = "is_shuffle"
    }
}
