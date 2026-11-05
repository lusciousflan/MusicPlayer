package com.example.musicplayer.ui

import android.content.Intent
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.MusicRepository
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.activity.SettingsActivity
import com.example.musicplayer.ui.adapter.AudioAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * 曲の長押しメニューの操作（キューに追加・タグ編集・メモ・ディレクトリ除外）。
 * どの曲一覧画面からでも同じ動作になるよう、ここにまとめている。
 */
class AudioItemActions(private val activity: AppCompatActivity) {

    private val repository = MusicRepository((activity.application as MyApp).database.audioDao())

    fun addToQueue(audio: AudioFile) {
        activity.startService(
            Intent(activity, MusicService::class.java)
                .setAction("ADD_TO_QUEUE")
                .putExtra("audio", audio)
        )
    }

    /**
     * タグ編集ダイアログを出す。
     * タグが変わったら [onTagsChanged] に変更前と変更後のタグを渡す。
     */
    fun editTags(
        audio: AudioFile,
        onTagsChanged: (before: Set<String>, after: Set<String>) -> Unit = { _, _ -> }
    ) {
        activity.lifecycleScope.launch {
            val tags = repository.getAllTags().map { it.name }
            val before = repository.getTags(audio.id).toSet()
            val checked = tags.map { it in before }.toBooleanArray()

            AlertDialog.Builder(activity)
                .setTitle("${audio.title}のタグを編集")
                .setMultiChoiceItems(tags.toTypedArray(), checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("完了") { _, _ ->
                    activity.lifecycleScope.launch {
                        tags.forEachIndexed { index, tag ->
                            if (checked[index] && tag !in before) {
                                repository.addTag(audio.id, tag)
                            } else if (!checked[index] && tag in before) {
                                repository.removeTag(audio.id, tag)
                            }
                        }
                        notifyIfChanged(audio, before, onTagsChanged)
                    }
                }
                .setNegativeButton("キャンセル", null)
                .setNeutralButton("新規タグ作成") { _, _ ->
                    showCreateTagDialog(audio, before, onTagsChanged)
                }
                .show()
        }
    }

    /**
     * 曲一覧の画面用のタグ編集。
     *
     * 編集の結果、曲がその画面の条件に合わなくなったら（[stillMatches] が false）、すぐに一覧から外し、
     * 「元に戻す」で編集前のタグと一覧上の元の位置に戻せるようにする。
     *
     * @param stillMatches 編集後の曲がその画面に表示される条件を満たすか
     * @param onListChanged 一覧の内容が変わったときに呼ばれる。画面側で保持している曲リストを更新するために使う。
     */
    fun editTagsInList(
        audio: AudioFile,
        adapter: AudioAdapter,
        stillMatches: suspend (audioId: Long) -> Boolean,
        onListChanged: (List<AudioFile>) -> Unit
    ) {
        editTags(audio) { before, _ ->
            activity.lifecycleScope.launch {
                if (stillMatches(audio.id)) return@launch
                val position = adapter.items().indexOfFirst { it.id == audio.id }
                if (position < 0) return@launch

                val removed = adapter.items().filterIndexed { index, _ -> index != position }
                adapter.updateList(removed)
                onListChanged(removed)

                Snackbar.make(
                    activity.findViewById(android.R.id.content),
                    "「${audio.title}」をこの一覧から外しました",
                    Snackbar.LENGTH_LONG
                ).setAction("元に戻す") {
                    activity.lifecycleScope.launch {
                        restoreTags(audio.id, before)
                        val restored = adapter.items().toMutableList().apply {
                            add(position.coerceAtMost(size), audio)
                        }
                        adapter.updateList(restored)
                        onListChanged(restored)
                    }
                }.show()
            }
        }
    }

    fun editNote(audio: AudioFile) {
        activity.lifecycleScope.launch {
            val edit = EditText(activity).apply {
                setText(repository.getNote(audio.id))
                hint = "この曲のメモ"
                minLines = 3
            }
            AlertDialog.Builder(activity)
                .setTitle("楽曲メモ")
                .setView(edit)
                .setPositiveButton("保存") { _, _ ->
                    activity.lifecycleScope.launch {
                        repository.updateNote(audio.id, edit.text.toString())
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    fun excludeDirectory(audio: AudioFile) {
        activity.startActivity(Intent(activity, SettingsActivity::class.java).apply {
            putExtra("addExcludedDirectoryUri", audio.uri)
        })
    }

    private fun showCreateTagDialog(
        audio: AudioFile,
        before: Set<String>,
        onTagsChanged: (before: Set<String>, after: Set<String>) -> Unit
    ) {
        val editText = EditText(activity)

        AlertDialog.Builder(activity)
            .setTitle("新しいタグ")
            .setView(editText)
            .setPositiveButton("追加") { _, _ ->
                val tag = editText.text.toString()
                if (tag.isBlank()) return@setPositiveButton
                activity.lifecycleScope.launch {
                    repository.addTag(audio.id, tag)
                    notifyIfChanged(audio, before, onTagsChanged)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private suspend fun notifyIfChanged(
        audio: AudioFile,
        before: Set<String>,
        onTagsChanged: (before: Set<String>, after: Set<String>) -> Unit
    ) {
        val after = repository.getTags(audio.id).toSet()
        if (after != before) onTagsChanged(before, after)
    }

    /** 曲のタグを [tags] の状態に戻す。タグ編集の取り消しに使う。 */
    private suspend fun restoreTags(audioId: Long, tags: Set<String>) {
        val current = repository.getTags(audioId).toSet()
        (current - tags).forEach { repository.removeTag(audioId, it) }
        (tags - current).forEach { repository.addTag(audioId, it) }
    }
}
