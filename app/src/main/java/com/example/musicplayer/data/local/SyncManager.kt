package com.example.musicplayer.data.local

import android.content.Context
import androidx.room.withTransaction

/** MediaStore の楽曲一覧を DB に反映する。 */
suspend fun syncMediaStore(context: Context, db: AppDatabase) {
    syncAudio(db, loadAudioFromMediaStore(context), System.currentTimeMillis())
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
