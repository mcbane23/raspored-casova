package com.bane23.classschedule.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bane23.classschedule.alarm.AlarmReceiver
import com.bane23.classschedule.alarm.AlarmScheduler
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Extracurricular
import com.bane23.classschedule.data.classesEndOn
import com.bane23.classschedule.data.hasActivityOn
import java.time.LocalDate

/** Fixed id so repeated test notifications replace each other instead of piling up. */
private const val TEST_NOTIFICATION_ID = 4242

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** Turns a child's parent alarm on or off, first asking for the notification permission when it is needed. */
@Composable
fun rememberAlarmSwitch(viewModel: AppViewModel): (childId: String, enabled: Boolean) -> Unit =
    rememberNotificationSwitch(viewModel::setAlarm)

/** Turns a child's packing reminder on or off, first asking for the notification permission when it is needed. */
@Composable
fun rememberPackReminderSwitch(viewModel: AppViewModel): (childId: String, enabled: Boolean) -> Unit =
    rememberNotificationSwitch(viewModel::setPackReminder)

/** Turns a child's morning reminder on or off, first asking for the notification permission when it is needed. */
@Composable
fun rememberMorningReminderSwitch(viewModel: AppViewModel): (childId: String, enabled: Boolean) -> Unit =
    rememberNotificationSwitch(viewModel::setMorningReminder)

/**
 * Saves an activity for a child; when its reminder is being turned on (a new activity, or one whose reminder was off),
 * first asks for the notification permission (saving it with the reminder off if refused) and opens the "Alarms &
 * reminders" switch when needed, like the other reminders. Other edits, such as cancelling one session, save directly.
 */
@Composable
fun rememberActivitySaver(viewModel: AppViewModel): (childId: String, activity: Extracurricular) -> Unit {
    val pending = remember { mutableMapOf<String, Extracurricular>() }
    val ask = rememberNotificationSwitch { childId, granted ->
        pending.remove(childId)?.let { viewModel.saveActivity(childId, it.copy(reminderEnabled = it.reminderEnabled && granted)) }
    }
    return { childId, activity ->
        val saved = viewModel.data.value.children.find { it.id == childId }?.activities?.find { it.id == activity.id }
        if (activity.reminderEnabled && saved?.reminderEnabled != true) {
            pending[childId] = activity
            ask(childId, true)
        } else {
            viewModel.saveActivity(childId, activity)
        }
    }
}

/**
 * Turning a reminder on first asks for the notification permission when it is needed, then opens the "Alarms &
 * reminders" switch if exact alarms are not allowed yet, since without it the reminder comes late.
 */
@Composable
private fun rememberNotificationSwitch(set: (childId: String, enabled: Boolean) -> Unit): (String, Boolean) -> Unit {
    val context = LocalContext.current
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.let { set(it, granted) }
        pending = null
        if (granted && !AlarmScheduler.canScheduleExact(context)) openExactAlarmSettings(context)
        // Once refused for good Android no longer asks, so the switch would just stay off; the app's notification
        // settings are where it can be allowed again.
        if (!granted && !shouldExplainNotifications(context)) openFix(context, ReminderProblem.NOTIFICATIONS_OFF)
    }
    return { id, enabled ->
        if (enabled && needsNotificationPermission(context)) {
            pending = id
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            set(id, enabled)
            if (enabled && !AlarmScheduler.canScheduleExact(context)) openExactAlarmSettings(context)
        }
    }
}

/** Whether Android would still show its notification permission dialog (false once the user refused it for good). */
private fun shouldExplainNotifications(context: Context): Boolean {
    var current: Context? = context
    while (current is ContextWrapper && current !is Activity) current = current.baseContext
    val activity = current as? Activity ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
}

/** Whether alarms can fire at the exact minute. Rechecked each time the screen comes back from the phone's settings. */
@Composable
fun rememberExactAlarmAllowed(): Boolean {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        allowed = AlarmScheduler.canScheduleExact(context)
        onPauseOrDispose { }
    }
    return allowed
}

/** Opens this app's "Alarms & reminders" switch (Android 12+). */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

/** Sends a test notification for [child]; [blocked] is true when the phone refused to show it. */
class TestNotification(val send: () -> Unit, val blocked: Boolean)

/**
 * A test of the parent's "classes are over" notification, the child's packing reminder, an activity reminder or the
 * morning reminder.
 */
@Composable
fun rememberTestNotification(child: Child, reminder: AlarmReceiver.Reminder = AlarmReceiver.Reminder.CLASSES_OVER): TestNotification {
    val context = LocalContext.current
    var blocked by remember { mutableStateOf(false) }

    fun show() {
        blocked = if (reminder == AlarmReceiver.Reminder.ACTIVITY) {
            val today = LocalDate.now()
            val activity = child.activities.firstOrNull { child.hasActivityOn(it, today) } ?: child.activities.firstOrNull()
            !AlarmReceiver.showNotification(
                context,
                AlarmReceiver.Reminder.ACTIVITY,
                child.id,
                TEST_NOTIFICATION_ID,
                activity?.let { "${it.kind.emoji()} ${it.title} u ${it.start.label()}" } ?: "🏅 Aktivnost uskoro",
                "Probni podsetnik radi.",
                alarmSound = child.activityReminderSound,
                silent = child.activityReminderSilent,
            )
        } else if (reminder == AlarmReceiver.Reminder.MORNING) {
            val content = morningReminderContent(child, LocalDate.now())
            !AlarmReceiver.showNotification(
                context,
                AlarmReceiver.Reminder.MORNING,
                child.id,
                TEST_NOTIFICATION_ID,
                content?.title ?: "☀️ Proveri plan za danas",
                content?.text ?: "Probni podsetnik radi.",
                alarmSound = child.morningReminderSound,
                silent = child.morningReminderSilent,
                bigText = content?.bigText,
            )
        } else if (reminder == AlarmReceiver.Reminder.PACK_BAG) {
            val content = packReminderContent(child, LocalDate.now())
            !AlarmReceiver.showNotification(
                context,
                AlarmReceiver.Reminder.PACK_BAG,
                child.id,
                TEST_NOTIFICATION_ID,
                content?.title ?: "🎒 Spakuj torbu",
                content?.text ?: "Probni podsetnik radi.",
                alarmSound = child.packReminderSound,
                silent = child.packReminderSilent,
                bigText = content?.bigText,
            )
        } else {
            val end = child.classesEndOn(LocalDate.now())
            val text = if (end != null) "Probno obaveštenje. Danas se časovi završavaju u ${end.label()}." else "Probno obaveštenje radi."
            !AlarmReceiver.showNotification(
                context,
                AlarmReceiver.Reminder.CLASSES_OVER,
                child.id,
                TEST_NOTIFICATION_ID,
                child.name.ifBlank { "Raspored časova" },
                text,
                alarmSound = child.alarmSound,
                silent = child.alarmSilent,
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) show() else blocked = true
    }
    return TestNotification(
        send = { if (needsNotificationPermission(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else show() },
        blocked = blocked,
    )
}

/**
 * Whether battery optimization may hold back the reminders. Rechecked each time the screen comes back, so the
 * warning disappears after the user changes the setting.
 */
@Composable
fun rememberBatteryRestricted(): Boolean {
    val context = LocalContext.current
    var restricted by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        val power = context.getSystemService(PowerManager::class.java)
        restricted = power != null && !power.isIgnoringBatteryOptimizations(context.packageName)
        onPauseOrDispose { }
    }
    return restricted
}

/** Opens the phone's battery optimization list, where the user can set the app to "not optimized". */
fun openBatterySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

/** Something that keeps the alarms and reminders from arriving on time. */
enum class ReminderProblem(val title: String, val detail: String) {
    NOTIFICATIONS_OFF("Obaveštenja su isključena", "Alarmi i podsetnici neće stići dok ih ne uključiš."),
    NOT_EXACT("Alarm može da kasni", "Dozvoli „Alarmi i podsetnici” da obaveštenje stigne tačno na vreme."),
}

/**
 * The first [ReminderProblem] on this phone, or null when the reminders will arrive. Rechecked each time the screen
 * comes back, so the warning disappears after the user fixes the setting.
 */
@Composable
fun rememberReminderProblem(): ReminderProblem? {
    val context = LocalContext.current
    var problem by remember { mutableStateOf<ReminderProblem?>(null) }
    LifecycleResumeEffect(Unit) {
        problem = when {
            !NotificationManagerCompat.from(context).areNotificationsEnabled() -> ReminderProblem.NOTIFICATIONS_OFF
            !AlarmScheduler.canScheduleExact(context) -> ReminderProblem.NOT_EXACT
            else -> null
        }
        onPauseOrDispose { }
    }
    return problem
}

/** Opens the phone setting that fixes [problem]. */
fun openFix(context: Context, problem: ReminderProblem) {
    when (problem) {
        ReminderProblem.NOTIFICATIONS_OFF -> try {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        } catch (_: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        ReminderProblem.NOT_EXACT -> openExactAlarmSettings(context)
    }
}

/** The [ReminderProblem] to warn about on the main tabs, only when one of [children] has a reminder turned on. */
@Composable
fun rememberReminderWarning(children: List<Child>): ReminderProblem? {
    val problem = rememberReminderProblem()
    return problem.takeIf { children.any { it.hasReminderOn() } }
}

/** Whether any of the child's alarms or reminders is turned on. */
fun Child.hasReminderOn(): Boolean =
    alarmEnabled || packReminderEnabled || morningReminderEnabled || activities.any { it.reminderEnabled }

/**
 * Warning on the main tabs that the phone will hold back the reminders, so a parent who switched the alarm on from
 * the Family card still finds out, with a button to the setting that fixes it.
 */
@Composable
fun ReminderProblemBanner(problem: ReminderProblem) {
    val context = LocalContext.current
    NoteBanner("⚠️", "${problem.title}. ${problem.detail}") {
        TextButton(colors = accentTextButton(), onClick = { openFix(context, problem) }) { Text("Popravi") }
    }
}
