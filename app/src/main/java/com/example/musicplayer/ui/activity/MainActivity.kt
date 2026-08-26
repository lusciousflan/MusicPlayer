package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.*
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.model.LibraryItem
import com.example.musicplayer.model.isVisibleAudioTitle
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.adapter.AudioAdapter
import com.example.musicplayer.ui.fragment.*
import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.Toast
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private var audioList: List<AudioFile> = emptyList()
    private lateinit var playPauseButton: Button
    private lateinit var seekBar: SeekBar
    private lateinit var timeText: TextView
    private lateinit var adapter: AudioAdapter
    lateinit var miniTitle: TextView
    lateinit var miniArtist: TextView
    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView
    private var libraryItems: List<LibraryItem> = emptyList()
    private lateinit var drawerToggle: ActionBarDrawerToggle

    // シークバーの状態を更新
    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val current = intent?.getIntExtra("current", 0) ?: 0
            val duration = intent?.getIntExtra("duration", 0) ?: 0
            seekBar.max = duration
            seekBar.progress = current
            timeText.text = "${formatTime(current)} / ${formatTime(duration)}"
            Log.d(
                "MainActivity",
                "progress=$current duration=$duration"
            )
        }
    }
    
    // 再生/一時停止ボタンの見た目を切り替える
    private val playPauseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isPlaying = intent?.getBooleanExtra("isPlaying", false) ?: false
            updatePlayPauseButton(isPlaying)
        }
    }
    private fun updatePlayPauseButton(isPlaying: Boolean) {
        playPauseButton.text = if (isPlaying) "⏸" else "▶"
    }

    // 再生中の曲のタイトルを受け取る
    private val nowPlayingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val title = intent?.getStringExtra("title") ?: return
            miniTitle.text = title
            miniArtist.text = intent.getStringExtra("artist").orEmpty()
        }
    }

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

        if (checkPermission()) {
            // setupRecycler()
        } else {
            requestPermission()
        }
    
        playPauseButton = findViewById(R.id.playPauseButton)
        seekBar = findViewById(R.id.seekBar)
        timeText = findViewById(R.id.timeText)
        miniTitle = findViewById(R.id.miniTitle)
        miniArtist = findViewById(R.id.miniArtist)

        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        lifecycleScope.launch(Dispatchers.IO) {
            syncMediaStore(this@MainActivity, dao)
        }
        findViewById<LinearLayout>(R.id.miniPlayer).setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        playPauseButton.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            intent.action = "TOGGLE_PLAY"
            startService(intent)
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intent = Intent(this@MainActivity, MusicService::class.java)
                    intent.action = "SEEK"
                    intent.putExtra("position", progress)
                    startService(intent)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // findViewById<Button>(R.id.playlistButton).setOnClickListener {
        //     startActivity(Intent(this, PlaylistListActivity::class.java))
        // }

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
            syncMediaStore(this@MainActivity, (application as MyApp).database.audioDao())
            withContext(Dispatchers.Main) {
                // ViewPager2 manages the library fragment lifecycle and reloads it on resume.
            }
        }
        registerReceiver(playPauseReceiver, IntentFilter("PLAYING_STATE_CHANGED"))
        registerReceiver(progressReceiver, IntentFilter("MUSIC_PROGRESS"))
        registerReceiver(nowPlayingReceiver, IntentFilter("NOW_PLAYING"))

        val requestStateIntent = Intent(this, MusicService::class.java)
        requestStateIntent.action = "REQUEST_STATE"
        startService(requestStateIntent)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(playPauseReceiver)
        unregisterReceiver(progressReceiver)
        unregisterReceiver(nowPlayingReceiver)
    }

    private fun setupRecycler() {
        audioList = getAudioFiles()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AudioAdapter(
            audioList,
            getAlbumArtUri = { albumId ->
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
            },
            onClick = { audio, position ->
                val intent = Intent(this, MusicService::class.java)
                intent.action = "PLAY"
                intent.putExtra("audio", audio)
                startService(intent)
                adapter.setCurrentPlaying(position)
            },
            onAddToQueue = { audio ->
                val intent = Intent(this, MusicService::class.java)
                intent.action = "ADD_TO_QUEUE"
                intent.putExtra("audio", audio)
                startService(intent)
            },
            onEditTag = { audio ->
                showTagDialog(audio)
            }
            )
        recyclerView.adapter = adapter
    }

    fun getAudioFiles(): List<AudioFile> {
        val list = mutableListOf<AudioFile>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID
        ).apply {
            add(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA)
        }
        val cursor = contentResolver.query(
            collection,
            projection.toTypedArray(),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'",
            null,
            null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val pathCol = it.getColumnIndexOrThrow(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol)
                val artist = it.getString(artistCol)
                if (!isVisibleAudioTitle(title)) continue
                if (isExcludedPath(this, it.getString(pathCol).orEmpty())) continue
                val uri = ContentUris.withAppendedId(collection, id).toString()
                val albumId = it.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))

                list.add(AudioFile(id, title, artist, uri, albumId))
            }
        }
        return list
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
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

            if (audioGranted && notificationGranted) {
                // setupRecycler()
            } else {
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
                .setTitle("タグ編集")
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
