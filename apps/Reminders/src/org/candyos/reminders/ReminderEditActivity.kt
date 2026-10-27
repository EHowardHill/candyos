package org.candyos.reminders

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import java.util.Calendar
import java.util.Date

/**
 * Reminder editor: title + Holo date/time pickers (framework dialogs,
 * which the KitKat UI donor styles dress as 4.4). Defaults to an hour
 * from now, 4.4 alarm-clock style.
 */
class ReminderEditActivity : Activity() {

    private var id: String? = null
    private lateinit var titleEdit: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private val cal = Calendar.getInstance()

    override fun onCreate(ic: Bundle?) {
        super.onCreate(ic)
        setContentView(R.layout.reminder_edit)

        titleEdit = findViewById(R.id.title)
        dateButton = findViewById(R.id.date)
        timeButton = findViewById(R.id.time)

        if (ic != null) {
            id = ic.getString("id")
            cal.timeInMillis = ic.getLong("due", System.currentTimeMillis())
        } else {
            val loaded = intent.getStringExtra(Reminder.EXTRA_ID)?.let {
                Reminder.load(it, filesDir)
            }
            if (loaded != null) {
                id = loaded.id
                titleEdit.setText(loaded.title)
                cal.timeInMillis = loaded.due
            } else {
                cal.timeInMillis = System.currentTimeMillis() + 60 * 60 * 1000L
            }
        }
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        refreshLabels()

        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    cal.set(Calendar.YEAR, y)
                    cal.set(Calendar.MONTH, m)
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    refreshLabels()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
            ).show()
        }
        timeButton.setOnClickListener {
            TimePickerDialog(
                this,
                { _, h, min ->
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, min)
                    refreshLabels()
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this),
            ).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("id", id)
        outState.putLong("due", cal.timeInMillis)
    }

    private fun refreshLabels() {
        dateButton.text = java.text.DateFormat.getDateInstance().format(Date(cal.timeInMillis))
        timeButton.text = android.text.format.DateFormat.getTimeFormat(this)
            .format(Date(cal.timeInMillis))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                save()
                true
            }
            R.id.discard -> {
                id?.let { Reminder.delete(it, filesDir) }
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun save() {
        val title = titleEdit.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.need_title, Toast.LENGTH_SHORT).show()
            return
        }
        val r = Reminder(id ?: Reminder.newId().also { id = it }, title, cal.timeInMillis, false)
        Reminder.save(r, filesDir)
        if (r.due > System.currentTimeMillis()) {
            Reminder.schedule(this, r)
        } else {
            Toast.makeText(this, R.string.past_due, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}