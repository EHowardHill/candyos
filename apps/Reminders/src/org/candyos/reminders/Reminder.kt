package org.candyos.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.io.File
import java.util.UUID

/**
 * One reminder = one file under filesDir/reminders: first line the title,
 * second the due epoch millis, third "done" or "todo".
 */
data class Reminder(
    val id: String,
    var title: String,
    var due: Long,
    var done: Boolean,
) {
    companion object {
        const val EXTRA_ID = "id"

        fun dir(filesDir: File): File =
            File(filesDir, "reminders").apply { mkdirs() }

        fun load(id: String, filesDir: File): Reminder? {
            val f = File(dir(filesDir), "$id.txt")
            if (!f.exists()) return null
            val lines = f.readText().lines()
            if (lines.size < 3) return null
            return Reminder(id, lines[0], lines[1].toLongOrNull() ?: 0L, lines[2] == "done")
        }

        fun all(filesDir: File): List<Reminder> =
            dir(filesDir).listFiles { f -> f.isFile }?.mapNotNull { f ->
                load(f.name.removeSuffix(".txt"), filesDir)
            }?.sortedWith(
                compareBy<Reminder>({ it.done }, { it.due }),
            ) ?: emptyList()

        fun newId(): String = UUID.randomUUID().toString()

        fun save(r: Reminder, filesDir: File) {
            File(dir(filesDir), "${r.id}.txt").writeText(
                "${r.title}\n${r.due}\n${if (r.done) "done" else "todo"}",
            )
        }

        fun delete(id: String, filesDir: File) {
            File(dir(filesDir), "$id.txt").delete()
        }

        /** (Re)arm the AlarmManager fire for a pending reminder. */
        fun schedule(context: Context, r: Reminder) {
            if (r.done || r.due <= System.currentTimeMillis()) return
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val pi = pendingIntent(context, r.id)
            am.cancel(pi)
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.due, pi)
            } catch (e: SecurityException) {
                // No exact-alarm grant: degrade gracefully to inexact.
                am.set(AlarmManager.RTC_WAKEUP, r.due, pi)
            }
        }

        fun cancel(context: Context, id: String) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            am.cancel(pendingIntent(context, id))
        }

        private fun pendingIntent(context: Context, id: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                Intent(context, AlarmReceiver::class.java)
                    .putExtra(EXTRA_ID, id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}