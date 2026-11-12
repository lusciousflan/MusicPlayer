package com.example.musicplayer.data.local

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.room.withTransaction

/** MediaStore の楽曲一覧を DB に反映する。 */
suspend fun syncMediaStore(context: Context, db: AppDatabase) {
    syncAudio(db, loadAudioFromMediaStore(context), System.currentTimeMillis())
}

/**
 * 前回の同期から MediaStore か除外ディレクトリの設定が変わっていれば、MediaStore を DB に反映する。
 * 画面に戻るたびに全曲を読み直さないようにするため(電池の消費を抑えるため)。
 * 変更を確かめられない端末(Android 10 以前)では毎回同期する。
 */
suspend fun syncMediaStoreIfChanged(context: Context, db: AppDatabase) {
    val preferences = context.getSharedPreferences(SYNC_PREFERENCES, Context.MODE_PRIVATE)
    // 同期より前に記録しておく。同期中に MediaStore が変わっても、次回の同期で取りこぼさない。
    val fingerprint = currentMediaStoreFingerprint(context)
    if (fingerprint != null && fingerprint == preferences.getString(KEY_FINGERPRINT, null)) return
    syncMediaStore(context, db)
    if (fingerprint != null) preferences.edit().putString(KEY_FINGERPRINT, fingerprint).apply()
}

private const val SYNC_PREFERENCES = "media_store_sync"
private const val KEY_FINGERPRINT = "fingerprint"

// 同期で DB に書く内容を変えたとき(DB に列を追加して同期で埋めるときなど)は、この値を上げて全端末で同期し直させる
private const val SYNC_FORMAT_VERSION = 1

private fun currentMediaStoreFingerprint(context: Context): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    return try {
        val volumes = MediaStore.getExternalVolumeNames(context).map { volume ->
            MediaStoreVolumeState(
                name = volume,
                version = MediaStore.getVersion(context, volume),
                generation = MediaStore.getGeneration(context, volume)
            )
        }
        mediaStoreFingerprint(SYNC_FORMAT_VERSION, volumes, excludedDirectories(context))
    } catch (e: Exception) {
        // 変更を確かめられなければ、毎回同期する
        null
    }
}

/** MediaStore の1つのボリューム(内部ストレージ・SD カードなど)の状態。 */
data class MediaStoreVolumeState(val name: String, val version: String, val generation: Long)

/**
 * 同期結果に影響するものをまとめた文字列。前回の同期時と同じなら、同期しても結果は変わらない。
 *
 * - generation: そのボリュームのファイルが追加・変更・削除されるたびに増える
 * - version: MediaStore のデータベースが作り直されると変わる(このとき generation は 0 からやり直しになる)
 */
fun mediaStoreFingerprint(
    formatVersion: Int,
    volumes: List<MediaStoreVolumeState>,
    excludedDirectories: Set<String>
): String = buildString {
    append(formatVersion)
    volumes.sortedBy { it.name }.forEach { append('\n').append(it.name).append(':').append(it.version).append(':').append(it.generation) }
    append("\nexcluded:")
    excludedDirectories.sorted().forEach { append('\n').append(it) }
}

/**
 * [mediaList] を DB に反映する。書き込みは1回のトランザクションにまとめ、変化のない曲には書き込まない。
 * 読み取りもトランザクション内で行うので、同期中に増えた再生回数などを古い値で上書きしない。
 */
suspend fun syncAudio(db: AppDatabase, mediaList: List<AudioEntity>, now: Long) {
    db.withTransaction {
        val dao = db.audioDao()
        val plan = planAudioSync(dao.getAllAudio(), mediaList, now)
        if (plan.inserts.isNotEmpty()) dao.insertAll(plan.inserts)
        if (plan.updates.isNotEmpty()) dao.updateAudio(plan.updates)
        plan.deletedIds.forEach { id -> dao.deleteAudio(id) }
    }
}

/** 同期で DB に対して行う変更。 */
data class AudioSyncPlan(
    val inserts: List<AudioEntity>,
    val updates: List<AudioEntity>,
    val deletedIds: List<Long>
)

/**
 * DB の曲([stored])と MediaStore の曲([mediaList])を比べて、必要な変更を求める。
 *
 * - MediaStore にだけある曲は追加する。追加日時は MediaStore の追加日時、なければ [now]。
 * - 両方にある曲は、曲名などのメタデータだけを MediaStore に合わせる。
 *   メモ・再生回数・追加日時はアプリ独自の情報なので保持する。
 *   ただし追加日時が未記録(0)の曲は、MediaStore の追加日時で補う。
 * - DB にだけある曲は削除する。
 */
fun planAudioSync(stored: List<AudioEntity>, mediaList: List<AudioEntity>, now: Long): AudioSyncPlan {
    val storedById = stored.associateBy { it.id }
    val inserts = mutableListOf<AudioEntity>()
    val updates = mutableListOf<AudioEntity>()

    mediaList.forEach { media ->
        val current = storedById[media.id]
        if (current == null) {
            inserts += media.copy(addedAt = media.addedAt.takeIf { it > 0 } ?: now)
        } else {
            val updated = current.copy(
                title = media.title,
                artist = media.artist,
                uri = media.uri,
                albumId = media.albumId,
                albumName = media.albumName,
                addedAt = if (current.addedAt > 0) current.addedAt else media.addedAt
            )
            if (updated != current) updates += updated
        }
    }

    val mediaIds = mediaList.mapTo(HashSet()) { it.id }
    val deletedIds = stored.map { it.id }.filter { it !in mediaIds }

    return AudioSyncPlan(inserts, updates, deletedIds)
}
