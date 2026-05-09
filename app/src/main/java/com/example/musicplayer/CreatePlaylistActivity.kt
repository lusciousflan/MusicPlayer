package com.example.musicplayer

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CreatePlaylistActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_playlist)
        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)
        val nameEdit = findViewById<EditText>(R.id.playlistName)
        val container = findViewById<LinearLayout>(R.id.tagContainer)
        val saveButton = findViewById<Button>(R.id.savePlaylist)

        lifecycleScope.launch {

            val tags = repository.getAllTags()
            val checkBoxes = mutableListOf<CheckBox>()

            tags.forEach { tag ->

                val checkBox = CheckBox(this@CreatePlaylistActivity)
                checkBox.text = tag.name

                container.addView(checkBox)
                checkBoxes.add(checkBox)
            }

            saveButton.setOnClickListener {

                val expressionEdit = findViewById<EditText>(R.id.expressionEdit)

                lifecycleScope.launch {

                    repository.createPlaylist(
                        name = nameEdit.text.toString(),
                        expression = expressionEdit.text.toString()  
                    )

                    finish()
                }
            }
        }
    }
}