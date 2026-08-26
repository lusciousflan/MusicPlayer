package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.MusicRepository
import androidx.appcompat.app.AppCompatActivity
import android.widget.ListView
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.ArrayAdapter
import android.content.Intent
import androidx.appcompat.app.AlertDialog

class TagListActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_list)
        title = "タグ一覧"

        listView = findViewById(R.id.tagListView)
        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        lifecycleScope.launch {

            val tags = repository.getAllTags()
            val names = tags.map { it.name }
            val adapter = ArrayAdapter(
                this@TagListActivity,
                android.R.layout.simple_list_item_1,
                names
            )

            listView.adapter = adapter
            listView.setOnItemClickListener { _, _, position, _ ->
                val intent = Intent(
                    this@TagListActivity,
                    TagSongsActivity::class.java
                )
                intent.putExtra("tag", names[position])
                startActivity(intent)
            }
            listView.setOnItemLongClickListener { _, _, position, _ ->
                val tag = names[position]
                AlertDialog.Builder(this@TagListActivity)
                    .setTitle("タグ削除")
                    .setMessage("「$tag」をすべての曲から外して削除しますか？")
                    .setPositiveButton("削除") { _, _ ->
                        lifecycleScope.launch {
                            repository.deleteTag(tag)
                            recreate()
                        }
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
                true
            }
        }
    }
}
