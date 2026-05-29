package com.example.notely

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notely.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: NoteRepository
    private lateinit var adapter: NotesAdapter

    private val editorLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = NoteRepository(this)

        adapter = NotesAdapter(
            emptyList(),
            onClick = { open(it.id) },
            onLongClick = { confirmDelete(it) }
        )
        binding.notesList.layoutManager = LinearLayoutManager(this)
        binding.notesList.adapter = adapter

        binding.fab.setOnClickListener { open(null) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val items = repo.list()
        adapter.submit(items)
        binding.emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun open(id: String?) {
        val intent = Intent(this, EditorActivity::class.java)
        if (id != null) intent.putExtra(EditorActivity.EXTRA_ID, id)
        editorLauncher.launch(intent)
    }

    private fun confirmDelete(note: NoteMeta) {
        AlertDialog.Builder(this)
            .setMessage(R.string.delete_note_q)
            .setPositiveButton(R.string.delete) { _, _ ->
                repo.delete(note.id)
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
