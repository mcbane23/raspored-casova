package com.bane23.classschedule.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.widget.WidgetUpdater

/**
 * Alarms are cleared on reboot and go stale when the clock or time zone changes, so schedule them again (and redraw
 * the home-screen widget). Also runs when the user grants exact alarms, so pending inexact alarms are replaced by
 * exact ones right away.
 */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.scheduleAll(context, ChildRepository.get(context).data.value)
        WidgetUpdater.refresh(context, goAsync())
    }
}
