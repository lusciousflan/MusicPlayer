package com.example.musicplayer.playback

import android.content.SharedPreferences

/** 設定画面で変えられる再生まわりの設定。設定は "music_settings" の SharedPreferences に保存する。 */
object PlaybackSettings {
    const val KEY_PAUSE_FOR_OTHER_APPS = "pause_for_other_apps"

    /**
     * 他のアプリが音を鳴らしたとき(動画・通話など)に一時停止するか。
     * true なら音声フォーカスを扱い、他のアプリに合わせて一時停止・音量を下げる。
     */
    fun pauseForOtherApps(preferences: SharedPreferences): Boolean =
        preferences.getBoolean(KEY_PAUSE_FOR_OTHER_APPS, true)
}
