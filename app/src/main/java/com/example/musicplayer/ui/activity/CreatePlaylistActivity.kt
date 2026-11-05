package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.MusicRepository
import com.example.musicplayer.domain.playlist.*
import com.example.musicplayer.util.normalizeTag
import com.example.musicplayer.util.containsForSearch
import com.example.musicplayer.ui.view.ExpressionEditText
import android.content.Context
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
        val expressionEdit = findViewById<ExpressionEditText>(R.id.expressionEdit)
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

/**
 * プレイリスト式の入力欄に出す候補。タグ名と、検索式(title{} など)を出す。
 * 式の最後の演算子より後ろの文字で絞り込む。
 *
 * ArrayAdapter は渡されたリストをそのまま表示用に使い、絞り込みのたびに clear() で中身を消す。
 * 全タグのリストを渡すと候補の元データまで消えて2回目以降の候補が出なくなるため、表示用には別のリストを渡す。
 */
internal class TagSuggestionAdapter(
    context: Context,
    allTags: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, ArrayList(allTags)) {
    private val allTags: List<String> = allTags.toList()

    private val tagFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString().orEmpty()
                .split(Regex("[+|&()\\-]"))
                .lastOrNull()
                .orEmpty()
                .trim()
            val tagMatches = if (query.isEmpty()) allTags else allTags.filter {
                containsForSearch(it, query)
            }
            // 検索式は項目名の先頭が一致したら候補にする（例: "ti" → title{}）。
            // 何も入力していないときは、使える検索式をタグの後ろにすべて並べる。
            val searchMatches = SearchField.entries
                .filter { it.keyword.startsWith(query.lowercase()) }
                .map { "${it.keyword}{}" }
            val matches = tagMatches + searchMatches
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
}
