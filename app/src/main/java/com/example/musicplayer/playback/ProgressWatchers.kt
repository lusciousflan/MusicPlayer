package com.example.musicplayer.playback

/**
 * 再生位置を表示している画面の数。
 *
 * 画面が1つも表示されていない(画面オフやバックグラウンド)ときは、MusicService が再生位置を定期送信しないようにするために使う。
 * 画面は表示されたら [watch]、隠れたら [unwatch] を呼ぶ。どちらもメインスレッドからだけ使う。
 */
object ProgressWatchers {
    private var count = 0
    private val listeners = mutableSetOf<(Boolean) -> Unit>()

    val isWatched: Boolean
        get() = count > 0

    fun watch() {
        count++
        if (count == 1) notifyListeners()
    }

    fun unwatch() {
        if (count == 0) return
        count--
        if (count == 0) notifyListeners()
    }

    /** 見ている画面が「なし ⇔ あり」に変わったときに呼ばれる。 */
    fun addListener(listener: (Boolean) -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners -= listener
    }

    private fun notifyListeners() {
        val watched = isWatched
        listeners.toList().forEach { it(watched) }
    }

    /**
     * 次に再生位置を送るまでの時間(ms)。
     * 画面は秒単位で表示するので、再生位置が次の秒に変わった直後に送る。
     */
    fun nextUpdateDelayMs(positionMs: Long): Long =
        1_000L - Math.floorMod(positionMs, 1_000L) + UPDATE_MARGIN_MS

    // 秒が切り替わる直前に送ってしまわないよう、少し遅らせる
    private const val UPDATE_MARGIN_MS = 20L
}
