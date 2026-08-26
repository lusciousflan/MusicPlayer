package com.example.musicplayer.ui.activity

import android.os.Bundle
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.musicplayer.R
import java.io.File

class SettingsActivity : AppCompatActivity() {
    private val preferences by lazy { getSharedPreferences("music_settings", MODE_PRIVATE) }
    private val excluded = linkedSetOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(
            preferences.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )
        setContentView(R.layout.activity_settings)
        title = "設定"
        excluded += preferences.getStringSet("excluded_directories", emptySet()).orEmpty()
        intent.getStringExtra("addExcludedDirectoryUri")?.let { uri ->
            resolveDirectory(uri)?.let { directory -> excluded.add(directory) }
            saveExcludedDirectories()
        }

        val list = findViewById<ListView>(R.id.excludedDirectoryList)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, excluded.toMutableList())
        list.adapter = adapter
        findViewById<Button>(R.id.addExcludedDirectory).setOnClickListener { showAddDialog() }
        findViewById<Button>(R.id.themeButton).setOnClickListener { showThemeDialog() }
        list.setOnItemLongClickListener { _, _, position, _ ->
            val path = adapter.getItem(position) ?: return@setOnItemLongClickListener true
            AlertDialog.Builder(this)
                .setTitle("除外ディレクトリを削除")
                .setMessage(path)
                .setPositiveButton("削除") { _, _ ->
                    excluded.remove(path)
                    saveExcludedDirectories()
                    adapter.remove(path)
                }
                .setNegativeButton("キャンセル", null)
                .show()
            true
        }
    }

    private fun showThemeDialog() {
        val labels = arrayOf("ライトモード", "ダークモード", "システムに合わせる")
        val modes = intArrayOf(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppCompatDelegate.MODE_NIGHT_YES,
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        val selected = modes.indexOf(
            preferences.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        )
        AlertDialog.Builder(this)
            .setTitle("テーマ")
            .setSingleChoiceItems(labels, selected) { dialog, which ->
                preferences.edit().putInt("night_mode", modes[which]).apply()
                AppCompatDelegate.setDefaultNightMode(modes[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "例: Notifications/ または Music/通知音/"
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("除外ディレクトリを追加")
            .setMessage("MediaStoreの相対パスを入力してください")
            .setView(input)
            .setPositiveButton("追加") { _, _ ->
                val value = input.text.toString().trim().trim('/')
                if (value.isNotEmpty()) {
                    val normalized = "$value/"
                    if (excluded.add(normalized)) {
                        saveExcludedDirectories()
                        adapter.add(normalized)
                    }
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun saveExcludedDirectories() {
        preferences.edit().putStringSet("excluded_directories", excluded.toSet()).apply()
    }

    private fun resolveDirectory(audioUri: String): String? {
        val column = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            MediaStore.Audio.Media.DATA
        }
        contentResolver.query(Uri.parse(audioUri), arrayOf(column), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val path = cursor.getString(0).orEmpty()
            if (path.isBlank()) return null
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (path.endsWith('/')) path else "$path/"
            } else {
                File(path).parentFile?.path?.let { if (it.endsWith('/')) it else "$it/" }
            }
        }
        return null
    }
}
