package com.example.musicplayer

import androidx.appcompat.app.AppCompatActivity
import android.widget.ListView
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.ArrayAdapter
import android.content.Intent



class TagListActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_list)

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
        }
    }
}