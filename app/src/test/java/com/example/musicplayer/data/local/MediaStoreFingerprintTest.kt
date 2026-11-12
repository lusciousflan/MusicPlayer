package com.example.musicplayer.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaStoreFingerprintTest {

    private fun volume(name: String = "external_primary", version: String = "v1", generation: Long = 10) =
        MediaStoreVolumeState(name, version, generation)

    private fun fingerprint(
        volumes: List<MediaStoreVolumeState> = listOf(volume()),
        excluded: Set<String> = emptySet(),
        formatVersion: Int = 1
    ) = mediaStoreFingerprint(formatVersion, volumes, excluded)

    @Test
    fun `何も変わっていなければ同じになる`() {
        assertEquals(fingerprint(), fingerprint())
    }

    @Test
    fun `ボリュームや除外ディレクトリの並び順には左右されない`() {
        val sd = volume(name = "1234-5678", generation = 3)
        assertEquals(
            fingerprint(volumes = listOf(volume(), sd), excluded = linkedSetOf("A/", "B/")),
            fingerprint(volumes = listOf(sd, volume()), excluded = linkedSetOf("B/", "A/"))
        )
    }

    @Test
    fun `ファイルが変わると変わる`() {
        assertNotEquals(fingerprint(), fingerprint(volumes = listOf(volume(generation = 11))))
    }

    @Test
    fun `MediaStore のデータベースが作り直されると変わる`() {
        assertNotEquals(fingerprint(), fingerprint(volumes = listOf(volume(version = "v2"))))
    }

    @Test
    fun `SD カードを抜き差しすると変わる`() {
        assertNotEquals(
            fingerprint(),
            fingerprint(volumes = listOf(volume(), volume(name = "1234-5678")))
        )
    }

    @Test
    fun `除外ディレクトリを変えると変わる`() {
        assertNotEquals(fingerprint(), fingerprint(excluded = setOf("Notifications/")))
    }

    @Test
    fun `同期の内容を変えたときは変わる`() {
        assertNotEquals(fingerprint(), fingerprint(formatVersion = 2))
    }
}
