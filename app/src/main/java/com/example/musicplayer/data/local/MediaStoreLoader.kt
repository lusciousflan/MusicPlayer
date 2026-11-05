package com.example.musicplayer.data.local

import android.content.Context
import android.provider.MediaStore
import android.content.ContentUris
import android.net.Uri
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.model.isVisibleAudioTitle
import android.os.Build

private val ALBUM_ART_BASE_URI: Uri = Uri.parse("content://media/external/audio/albumart")

/** アルバムIDから、そのアルバムのアルバムアート画像を指すURIを作る。 */
fun albumArtUri(albumId: Long): Uri = ContentUris.withAppendedId(ALBUM_ART_BASE_URI, albumId)

fun excludedDirectories(context: Context): Set<String> = context.getSharedPreferences(
    "music_settings", Context.MODE_PRIVATE
).getStringSet("excluded_directories", emptySet()).orEmpty()

fun isExcludedPath(context: Context, path: String): Boolean =
    isExcludedPath(excludedDirectories(context), path)

private fun isExcludedPath(excluded: Set<String>, path: String): Boolean =
    excluded.any { path.startsWith(it, ignoreCase = true) }

// 除外ディレクトリの判定に使うパスの列。Android 10 以降は絶対パス(DATA)が使えないため相対パスを使う。
private val pathColumn: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.RELATIVE_PATH
    } else {
        MediaStore.Audio.Media.DATA
    }

/**
 * MediaStore から楽曲を読み込む。アプリ内で MediaStore の楽曲を読む処理はすべてこの関数を経由する。
 *
 * 音楽ファイル以外、非表示にするタイトルの曲、設定で除外したディレクトリの曲は結果に含めない。
 *
 * @param selection 追加の絞り込み条件（例: "${MediaStore.Audio.Media.ARTIST} = ?"）。
 * @param selectionArgs [selection] 内の `?` に入れる値。
 * @param sortOrder 並び順。null の場合は MediaStore が返す順番のまま。
 */
fun queryAudioFiles(
    context: Context,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): List<AudioFile> {

    val list = mutableListOf<AudioFile>()

    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DATE_ADDED,
        pathColumn
    )

    val musicOnly = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    val fullSelection = if (selection == null) musicOnly else "$musicOnly AND ($selection)"

    val excluded = excludedDirectories(context)

    val cursor = context.contentResolver.query(
        uri,
        projection,
        fullSelection,
        selectionArgs,
        sortOrder
    )

    cursor?.use {
        val idIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumIdIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val albumNameIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val dateAddedIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val pathIndex = it.getColumnIndexOrThrow(pathColumn)

        while (it.moveToNext()) {
            val title = it.getString(titleIndex).orEmpty()
            if (!isVisibleAudioTitle(title)) continue
            val path = it.getString(pathIndex).orEmpty()
            if (isExcludedPath(excluded, path)) continue
            val id = it.getLong(idIndex)

            list.add(
                AudioFile(
                    id = id,
                    title = title,
                    artist = it.getString(artistIndex).orEmpty(),
                    uri = ContentUris.withAppendedId(uri, id).toString(),
                    albumId = it.getLong(albumIdIndex),
                    albumName = it.getString(albumNameIndex).orEmpty(),
                    // MediaStore の追加日時は秒単位なので、アプリ内で使うミリ秒に揃える
                    addedAt = it.getLong(dateAddedIndex) * 1000
                )
            )
        }
    }

    return list
}

/** DB 同期用に、MediaStore の楽曲を [AudioEntity] として読み込む。 */
fun loadAudioFromMediaStore(context: Context): List<AudioEntity> =
    queryAudioFiles(context).map { audio ->
        AudioEntity(
            audio.id, audio.title, audio.artist, audio.uri, audio.albumId,
            addedAt = audio.addedAt,
            albumName = audio.albumName
        )
    }
