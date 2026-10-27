package org.candyos.experience

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView

/**
 * The CandyOS Experience menu - the drawer entry once setup is complete.
 * A plain Holo list, exactly like the 4.4 Settings root screen: one row
 * per destination, opened explicitly. Entries are data-driven so more
 * can be added later.
 */
class MenuActivity : Activity() {

    private class Entry(val label: CharSequence, val target: Class<*>)

    private val entries = listOf(
        Entry("About", AboutActivity::class.java),
        Entry("Testing", TestingActivity::class.java),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_activity)

        val labels = entries.map { it.label }
        val list = requireViewById<ListView>(android.R.id.list)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        list.setOnItemClickListener { _, _, position, _ ->
            startActivity(Intent(this, entries[position].target))
        }
    }
}