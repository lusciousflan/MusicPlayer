package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.*
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.model.LibraryItem
import com.example.musicplayer.ui.fragment.*
import com.example.musicplayer.ui.MiniPlayerController
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.material.tabs.TabLayout
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import android.view.Menu
import android.view.MenuItem


class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var repository: MusicRepository
    private var libraryItems: List<LibraryItem> = emptyList()
    private lateinit var drawerToggle: ActionBarDrawerToggle

    // 画面関連処理
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "MusicPlayer"

        val drawer = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        drawerToggle = ActionBarDrawerToggle(
            this, drawer, R.string.app_name, R.string.app_name
        )
        drawer.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navigationView.setNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.action_settings) {
                startActivity(Intent(this, SettingsActivity::class.java))
                drawer.closeDrawers()
                true
            } else {
                false
            }
        }

        if (!checkPermission()) {
            requestPermission()
        }

        MiniPlayerController(this)

        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
        val pager = findViewById<ViewPager2>(R.id.libraryPager)
        pager.adapter = MainPagerAdapter(this)
        com.google.android.material.tabs.TabLayoutMediator(
            tabLayout,
            pager
        ) { tab, position ->
            tab.text = listOf("楽曲", "ライブラリ", "アルバム", "アーティスト")[position]
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return if (item.itemId == R.id.action_search) {
            startActivity(Intent(this, SearchActivity::class.java))
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            // 起動直後も onCreate の後に必ずここを通るので、同期はここだけで行う
            syncMediaStore(this@MainActivity, (application as MyApp).database)
            withContext(Dispatchers.Main) {
                // ViewPager2 manages the library fragment lifecycle and reloads it on resume.
            }
        }
    }

    private fun checkPermission(): Boolean {
        val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val audioGranted = ContextCompat.checkSelfPermission(this, audioPermission) ==
                PackageManager.PERMISSION_GRANTED

        val notificationGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return audioGranted && notificationGranted
    }

    private fun requestPermission() {
        val permissions = mutableListOf<String>()
        val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissions += audioPermission

        if (Build.VERSION.SDK_INT >= 33) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            val audioGranted = permissions.any { permission ->
                permission == (if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
            } && grantResults.isNotEmpty() &&
                    grantResults.zip(permissions).any { (result, permission) ->
                        permission == (if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE) && result == PackageManager.PERMISSION_GRANTED
                    }

            val notificationGranted = if (Build.VERSION.SDK_INT >= 33) {
                permissions.any { it == Manifest.permission.POST_NOTIFICATIONS } &&
                        grantResults.zip(permissions).any { (result, permission) ->
                            permission == Manifest.permission.POST_NOTIFICATIONS && result == PackageManager.PERMISSION_GRANTED
                        }
            } else {
                true
            }

            if (!audioGranted || !notificationGranted) {
                Toast.makeText(this, "権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showTagDialog(audio: AudioFile) {

        lifecycleScope.launch {
            val tags = repository.getAllTags().map { it.name }
            val currentTags = repository.getTags(audio.id).toSet()
            val checked = tags.map { it in currentTags }.toBooleanArray()

            AlertDialog.Builder(this@MainActivity)
                .setTitle("${audio.title}のタグを編集")
                .setMultiChoiceItems(tags.toTypedArray(), checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("完了") { _, _ ->
                    lifecycleScope.launch {
                        tags.forEachIndexed { index, tag ->
                            if (checked[index] && tag !in currentTags) {
                                repository.addTag(audio.id, tag)
                            } else if (!checked[index] && tag in currentTags) {
                                repository.removeTag(audio.id, tag)
                            }
                        }
                    }
                }
                .setNegativeButton("キャンセル", null)
                .setNeutralButton("新規タグ作成") { _, _ ->
                    showCreateTagDialog(audio)
                }
                .show()
        }
    }

    fun showNoteDialog(audio: AudioFile) {
        lifecycleScope.launch {
            val edit = EditText(this@MainActivity).apply {
                setText(repository.getNote(audio.id))
                hint = "この曲のメモ"
                minLines = 3
            }
            AlertDialog.Builder(this@MainActivity)
                .setTitle("楽曲メモ")
                .setView(edit)
                .setPositiveButton("保存") { _, _ ->
                    lifecycleScope.launch {
                        repository.updateNote(audio.id, edit.text.toString())
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    fun excludeDirectoryFor(audio: AudioFile) {
        startActivity(Intent(this, SettingsActivity::class.java).apply {
            putExtra("addExcludedDirectoryUri", audio.uri)
        })
    }

    fun showBulkTagDialog(audios: List<AudioFile>, onComplete: () -> Unit) {
        lifecycleScope.launch {
            val tags = repository.getAllTags().map { it.name }
            val checked = BooleanArray(tags.size)
            AlertDialog.Builder(this@MainActivity)
                .setTitle("${audios.size}曲にタグを付与")
                .setMultiChoiceItems(tags.toTypedArray(), checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setPositiveButton("完了") { _, _ ->
                    lifecycleScope.launch {
                        tags.forEachIndexed { index, tag ->
                            if (checked[index]) {
                                audios.forEach { repository.addTag(it.id, tag) }
                            }
                        }
                        onComplete()
                    }
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    private fun showAddTagDialog(audio: AudioFile) {

        lifecycleScope.launch {
            val tags = repository.getAllTags()
            val items = tags.map { it.name }.toMutableList()
            items.add("＋ タグを追加する")

            AlertDialog.Builder(this@MainActivity)
                .setTitle("タグを選択")
                .setItems(items.toTypedArray()) { _, which ->
                    val selected = items[which]
                    if (selected == "＋ タグを追加する") {
                        showCreateTagDialog(audio)
                    } else {
                        lifecycleScope.launch {
                            repository.addTag(audio.id, selected)
                        }
                    }
                }
                .show()
        }
    }

    private fun showCreateTagDialog(audio: AudioFile) {

        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("新しいタグ")
            .setView(editText)
            .setPositiveButton("追加") { _, _ ->
                val tag = editText.text.toString()
                if (tag.isBlank()) return@setPositiveButton
                lifecycleScope.launch {
                    repository.addTag(audio.id, tag)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showRemoveTagDialog(audio: AudioFile, tag: String) {
        AlertDialog.Builder(this)
            .setTitle("タグ削除")
            .setMessage("タグ「$tag」を削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                lifecycleScope.launch {
                    repository.removeTag(audio.id, tag)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    suspend fun getTags(audioId: Long, dao: AudioDao): List<String> {
        return dao.getTagsForAudio(audioId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

private class MainPagerAdapter(activity: MainActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SongsFragment()
        1 -> LibraryFragment()
        2 -> AlbumFragment()
        else -> ArtistFragment()
    }
}
