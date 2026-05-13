package com.example.musicplayer

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView

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
        val expressionStatus = findViewById<TextView>(R.id.expressionStatus)

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

                expressionEdit.addTextChangedListener(

                    object : TextWatcher {

                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {}

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {

                            val text = s.toString()

                            if (text.isBlank()) {
                                expressionStatus.text = ""
                                return
                            }

                            try {

                                val tokens = tokenize(text)
                                val evaluator = PlaylistEvaluator(
                                        tokens,
                                        emptyList()
                                    )

                                evaluator.evaluate()
                                expressionStatus.text = "✓ Valid expression"

                            } catch (
                                e: PlaylistSyntaxException
                            ) {
                                expressionStatus.text = e.message
                            }
                        }

                        override fun afterTextChanged(
                            s: Editable?
                        ) {}
                    }
                )

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