package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.MusicRepository
import com.example.musicplayer.domain.playlist.*
import com.example.musicplayer.util.normalizeTag
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.graphics.Color
import android.widget.ArrayAdapter
import android.widget.Filterable
import android.widget.Filter
import android.widget.Filter.FilterResults

class CreatePlaylistActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_playlist)
        title = if (intent.hasExtra("playlistId")) "プレイリスト編集" else "プレイリスト作成"
        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)
        val nameEdit = findViewById<EditText>(R.id.playlistName)
        val expressionStatus = findViewById<TextView>(R.id.expressionStatus)
        val expressionEdit = findViewById<AutoCompleteTextView>(R.id.expressionEdit)
        val editingId = intent.getLongExtra("playlistId", -1L)

        if (editingId != -1L) {
            lifecycleScope.launch {
                val playlist = repository.getPlaylistById(editingId)
                nameEdit.setText(playlist.name)
                expressionEdit.setText(playlist.expression)
                title = "プレイリスト編集"
            }
        }

        lifecycleScope.launch {
            val tagNames = repository.getAllTags().map { it.name }
            expressionEdit.setAdapter(
                TagSuggestionAdapter(
                    this@CreatePlaylistActivity,
                    tagNames
                )
            )
            expressionEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // 演算子の後ろだけを候補検索対象にするため、式の途中でも候補を出せる。
                    val fragment = s?.toString().orEmpty()
                        .split(Regex("[+|&()\\-]"))
                        .lastOrNull()
                        .orEmpty()
                        .trim()
                    if (fragment.isNotEmpty()) {
                        (expressionEdit.adapter as? Filterable)?.filter?.filter(fragment)
                        expressionEdit.post { expressionEdit.showDropDown() }
                    }
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }

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

                    lifecycleScope.launch {

                        try {
                            val tokens = tokenize(text)
                            val evaluator = PlaylistEvaluator(
                                    tokens,
                                    emptyList()
                                )

                            evaluator.evaluate()

                            val existingTags = dao.getAllTagNames()
                                .map {
                                    normalizeTag(it)
                                }
                                .toSet()

                            val unknownTags = tokens
                                .filterIsInstance<Token.Tag>()
                                .map { it.name }
                                .distinct()
                                .filter {
                                    it !in existingTags
                                }

                            if (unknownTags.isNotEmpty()) {
                                expressionStatus.setTextColor(Color.YELLOW)
                                expressionStatus.text = "Unknown tag: ${unknownTags.joinToString()}"
                            } else {
                                expressionStatus.setTextColor(Color.GREEN)
                                expressionStatus.text = "✓ Valid expression"
                            }

                        } catch (
                            e: PlaylistSyntaxException
                        ) {
                            expressionStatus.setTextColor(Color.RED)
                            expressionStatus.text = e.message
                        }
                    }
                }
                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )

        findViewById<Button>(R.id.savePlaylist).setOnClickListener {
            lifecycleScope.launch {
                val expression = expressionEdit.text.toString()
                val name = nameEdit.text.toString().ifBlank { expression }

                if (editingId == -1L) {
                    repository.createPlaylist(name, expression)
                } else {
                    repository.updatePlaylist(editingId, name, expression)
                }

                finish()
            }
        }
    }
}

private class TagSuggestionAdapter(
    context: CreatePlaylistActivity,
    private val allTags: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, allTags) {
    private val tagFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = toHiragana(
                constraint?.toString().orEmpty()
                    .split(Regex("[+|&()\\-]"))
                    .lastOrNull()
                    .orEmpty()
                    .trim()
            )
            val matches = if (query.isEmpty()) allTags else allTags.filter {
                toHiragana(it).contains(query)
            }
            return FilterResults().apply {
                values = matches
                count = matches.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            @Suppress("UNCHECKED_CAST")
            addAll(results?.values as? List<String> ?: emptyList())
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter = tagFilter

    private fun toHiragana(value: String): String = buildString(value.length) {
        value.forEach { character ->
            append(
                if (character in '\u30A1'..'\u30F6') {
                    (character.code - 0x60).toChar()
                } else {
                    character
                }
            )
        }
    }
}
