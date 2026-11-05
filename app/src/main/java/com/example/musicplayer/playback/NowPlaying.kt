package com.example.musicplayer.playback

/**
 * 再生中の曲の ID。曲一覧で再生中の曲をハイライトするために使う。
 *
 * MusicService が曲の切り替えのたびに更新し、画面側は同じプロセス内で直接参照・監視する。
 * どちらもメインスレッドからだけ使う。
 */
object NowPlaying {
    const val NONE = -1L

    var audioId: Long = NONE
        private set

    private val listeners = mutableSetOf<(Long) -> Unit>()

    fun update(audioId: Long) {
        if (this.audioId == audioId) return
        this.audioId = audioId
        listeners.toList().forEach { it(audioId) }
    }

    fun addListener(listener: (Long) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (Long) -> Unit) {
        listeners -= listener
    }
}
