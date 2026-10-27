package org.candyos.notes

import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.UUID

/**
 * One note = one file under filesDir/notes. First line is the title,
 * the rest is the body - plain text, greppable, and recoverable by
 * hand if the app ever goes away.
 */
data class Note(
    val id: String,
    var title: String,
    var body: String,
    val modified: Long,
) {
    val snippet: String
        get() = body.replace('\n', ' ').trim()

    val dateLabel: String
        get() = DateFormat.getDateTimeInstance().format(Date(modified))

    companion object {
        fun dir(filesDir: File): File =
            File(filesDir, "notes").apply { mkdirs() }

        fun load(id: String, filesDir: File): Note? {
            val f = File(dir(filesDir), "$id.txt")
            if (!f.exists()) return null
            val text = f.readText()
            val title = text.substringBefore('\n').trim()
            val body = text.substringAfter('\n', "")
            return Note(id, title, body, f.lastModified())
        }

        fun all(filesDir: File): List<Note> =
            dir(filesDir).listFiles { f -> f.isFile }?.sortedWith(
                compareByDescending<File> { it.lastModified() }.thenBy { it.name },
            )?.mapNotNull { f ->
                val id = f.name.removeSuffix(".txt")
                load(id, filesDir)
            } ?: emptyList()

        fun newId(): String = UUID.randomUUID().toString()

        fun save(id: String, title: String, body: String, filesDir: File) {
            if (title.isBlank() && body.isBlank()) return
            File(dir(filesDir), "$id.txt").writeText("$title\n$body")
        }

        fun delete(id: String, filesDir: File) {
            File(dir(filesDir), "$id.txt").delete()
        }
    }
}