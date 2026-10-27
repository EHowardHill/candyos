package org.candyos.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arm every pending reminder's alarm after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Reminder.all(context.filesDir)
            .filter { !it.done && it.due > System.currentTimeMillis() }
            .forEach { Reminder.schedule(context, it) }
    }
}