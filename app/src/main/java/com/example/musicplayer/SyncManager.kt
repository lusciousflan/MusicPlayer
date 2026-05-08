package com.example.musicplayer

import android.content.Context

suspend fun syncMediaStore(context: Context, dao: AudioDao) {

    val mediaList = loadAudioFromMediaStore(context)
    val mediaIds = mediaList.map { it.id }

    val dbIds = dao.getAllIds()

    dao.deleteAll()           // ← 全削除
    dao.insertAll(mediaList)  // ← 全追加

    // // 新規 or 更新
    // dao.insertAll(mediaList)

    // // 削除（端末から消えた曲）
    // dao.deleteMissing(mediaIds)
}