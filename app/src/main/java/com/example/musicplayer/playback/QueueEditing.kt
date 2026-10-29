package com.example.musicplayer.playback

import com.example.musicplayer.model.AudioFile
import kotlin.random.Random

/**
 * 追加しようとしている曲から、すでにキューにある曲と、追加リスト内で重複する曲を取り除く。
 * @param queuedIds キューにある曲の ID（MediaItem の mediaId）
 */
fun audiosNotInQueue(queuedIds: Set<String>, audios: List<AudioFile>): List<AudioFile> {
    val seen = queuedIds.toHashSet()
    return audios.filter { seen.add(it.id.toString()) }
}

/**
 * [upcoming] の並び順を保ったまま、[newItems] をランダムな位置に混ぜ込んだリストを返す。
 * シャッフル中にキューへ曲を追加するとき、再生予定の曲の間に新しい曲を散らばらせるために使う。
 */
fun <T> mixRandomly(upcoming: List<T>, newItems: List<T>, random: Random = Random.Default): List<T> {
    val result = ArrayList<T>(upcoming.size + newItems.size)
    result.addAll(upcoming)
    newItems.shuffled(random).forEach { item ->
        result.add(random.nextInt(result.size + 1), item)
    }
    return result
}
