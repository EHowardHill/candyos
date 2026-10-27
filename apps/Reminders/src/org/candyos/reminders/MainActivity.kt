package org.candyos.reminders

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

/**
 * Reminders list: Holo dark rows with a done checkbox, an action-bar
 * "Add" and an overflow "Clear completed". Rows open the editor.
 */
class MainActivity : Activity() {

    private val reminders = mutableListOf<Reminder>()
    private lateinit var adapter: ReminderAdapter

    override fun onCreate(ic: Bundle?) {
        super.onCreate(ic)
        setContentView(R.layout.activity_main)
        val list = findViewById<ListView>(R.id.list)
        adapter = ReminderAdapter()
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            startActivity(
                Intent(this, ReminderEditActivity::class.java)
                    .putExtra(Reminder.EXTRA_ID, reminders[position].id),
            )
        }
        requestNotificationsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        reminders.clear()
        reminders.addAll(Reminder.all(filesDir))
        adapter.notifyDataSetChanged()
        findViewById<View>(R.id.empty).visibility =
            if (reminders.isEmpty()) View.VISIBLE else View.GONE
    }

    fun toggle(r: Reminder, done: Boolean) {
        r.done = done
        Reminder.save(r, filesDir)
        if (done) {
            Reminder.cancel(this, r.id)
        } else if (r.due > System.currentTimeMillis()) {
            Reminder.schedule(this, r)
        }
        reload()
    }

    private fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIF,
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add -> {
                startActivity(Intent(this, ReminderEditActivity::class.java))
                true
            }
            R.id.clear_done -> {
                reminders.filter { it.done }.forEach {
                    Reminder.delete(it.id, filesDir)
                }
                reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class ReminderAdapter : BaseAdapter() {
        override fun getCount() = reminders.size
        override fun getItem(position: Int) = reminders[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.reminder_row, parent, false)
            val r = getItem(position)
            val box = v.findViewById<CheckBox>(R.id.done)
            box.setOnCheckedChangeListener(null)
            box.isChecked = r.done
            box.setOnCheckedChangeListener { _, checked -> toggle(r, checked) }

            val title = v.findViewById<TextView>(R.id.title)
            title.text = r.title
            title.paint.isStrikeThruText = r.done

            v.findViewById<TextView>(R.id.subtitle).text =
                DateFormat.getDateTimeInstance().format(Date(r.due))
            v.findViewById<View>(R.id.row_root).alpha = if (r.done) 0.45f else 1f
            return v
        }
    }

    companion object {
        private const val REQUEST_NOTIF = 1
    }
}