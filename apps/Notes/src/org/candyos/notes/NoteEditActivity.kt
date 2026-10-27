package org.candyos.notes

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText

/**
 * Note editor: title on top (bold, single line), body below. Saves on
 * every pause - KitKat-era note apps had no explicit save step either.
 */
class NoteEditActivity : Activity() {

    private var id: String? = null
    private lateinit var titleEdit: EditText
    private lateinit var bodyEdit: EditText
    private var loaded = false

    override fun onCreate(ic: Bundle?) {
        super.onCreate(ic)
        setContentView(R.layout.note_edit)
        titleEdit = findViewById(R.id.title)
        bodyEdit = findViewById(R.id.body)

        if (ic != null) {
            id = ic.getString("id")
            loaded = true
            return
        }
        id = intent.getStringExtra("id")
        if (id != null) {
            Note.load(id!!, filesDir)?.let { n ->
                titleEdit.setText(n.title)
                bodyEdit.setText(n.body)
            } ?: finish()
        }
        loaded = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("id", id)
    }

    override fun onPause() {
        super.onPause()
        // Save on every pause, not just finish: homes/calls mid-edit
        // keep the draft, KitKat-style.
        if (loaded) save()
    }

    private fun save() {
        val title = titleEdit.text.toString().trim()
        val body = bodyEdit.text.toString()
        val noteId = id ?: Note.newId().also { id = it }
        if (title.isBlank() && body.isBlank()) {
            id?.let { Note.delete(it, filesDir) }
            return
        }
        Note.save(noteId, title, body, filesDir)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.discard -> {
                id?.let { Note.delete(it, filesDir) }
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}