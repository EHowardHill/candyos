package org.candyos.notes

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView

/**
 * Notes list: Holo dark ListView with an action-bar "Add", long-press
 * to delete. Layout mirrors the 4.4 Settings two-line rows.
 */
class MainActivity : Activity() {

    private lateinit var adapter: NoteAdapter
    private val notes = mutableListOf<Note>()

    override fun onCreate(ic: Bundle?) {
        super.onCreate(ic)
        setContentView(R.layout.activity_main)
        val list = findViewById<ListView>(R.id.list)
        adapter = NoteAdapter()
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            startActivity(
                Intent(this, NoteEditActivity::class.java)
                    .putExtra("id", notes[position].id),
            )
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            confirmDelete(notes[position])
            true
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        notes.clear()
        notes.addAll(Note.all(filesDir))
        adapter.notifyDataSetChanged()
        findViewById<View>(R.id.empty).visibility =
            if (notes.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_title)
            .setMessage(
                getString(R.string.delete_message, note.title.ifBlank { note.snippet }),
            )
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                Note.delete(note.id, filesDir)
                reload()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add -> {
                startActivity(Intent(this, NoteEditActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class NoteAdapter : BaseAdapter() {
        override fun getCount() = notes.size
        override fun getItem(position: Int) = notes[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.note_row, parent, false)
            val n = getItem(position)
            v.findViewById<TextView>(R.id.title).text =
                n.title.ifBlank { getString(R.string.untitled) }
            v.findViewById<TextView>(R.id.subtitle).text =
                "${n.dateLabel}  ${n.snippet}".trim()
            return v
        }
    }
}