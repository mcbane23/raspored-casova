package com.bane23.classschedule

import android.app.Application
import android.content.Context
import com.bane23.classschedule.alarm.AlarmReceiver
import com.bane23.classschedule.widget.WidgetUpdater

/** Loads the chosen language before any screen, receiver or widget builds a text. */
class ScheduleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LanguageStore.load(this)
    }

    companion object {
        /** Renames the notification channels and redraws the widget in the language just picked. */
        fun refreshLanguageDependents(context: Context) {
            AlarmReceiver.ensureChannels(context)
            WidgetUpdater.refresh(context)
        }
    }
}
