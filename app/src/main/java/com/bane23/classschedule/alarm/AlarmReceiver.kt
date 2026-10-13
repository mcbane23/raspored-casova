package com.bane23.classschedule.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bane23.classschedule.MainActivity
import com.bane23.classschedule.R
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.data.classesEndOn
import com.bane23.classschedule.ui.label
import java.time.LocalDate

/** Posts the "classes are over" notification for one child and schedules that child's next alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val childId = intent.getStringExtra(AlarmScheduler.EXTRA_CHILD_ID) ?: return
        val child = ChildRepository.get(context).data.value.children.find { it.id == childId } ?: return
        if (!child.alarmEnabled) return

        val end = child.classesEndOn(LocalDate.now())
        if (end != null) {
            val text = if (child.alarmMinutesBefore == 0) {
                "Časovi su završeni u ${end.label()}."
            } else {
                "Časovi se završavaju u ${end.label()}."
            }
            notify(context, child.id.hashCode(), child.name, text)
        }
        AlarmScheduler.schedule(context, child)
    }

    private fun notify(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "classes_over"

        fun ensureChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Kraj nastave", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Obaveštenje kad se završe časovi za taj dan" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}
