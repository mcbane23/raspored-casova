package com.bane23.classschedule.ui

import com.bane23.classschedule.data.ActivityKind
import com.bane23.classschedule.data.ActivityReminder
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Extracurricular
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

// Serbian labels for extracurricular activities, kept out of the composables so they can be unit tested.

fun ActivityKind.emoji(): String = when (this) {
    ActivityKind.SPORT -> "⚽"
    ActivityKind.SWIMMING -> "🏊"
    ActivityKind.DANCE -> "💃"
    ActivityKind.MUSIC -> "🎵"
    ActivityKind.ART -> "🎨"
    ActivityKind.LANGUAGE -> "🗣️"
    ActivityKind.TRIP -> "🚌"
    ActivityKind.CAMP -> "🏕️"
    ActivityKind.OTHER -> "⭐"
}

fun ActivityKind.label(): String = when (this) {
    ActivityKind.SPORT -> "Sport"
    ActivityKind.SWIMMING -> "Plivanje"
    ActivityKind.DANCE -> "Folklor i ples"
    ActivityKind.MUSIC -> "Hor i muzika"
    ActivityKind.ART -> "Likovna radionica"
    ActivityKind.LANGUAGE -> "Strani jezik"
    ActivityKind.TRIP -> "Izlet"
    ActivityKind.CAMP -> "Rekreativna nastava"
    ActivityKind.OTHER -> "Ostalo"
}

/** Trips and school camps usually happen once; the rest repeat every week. */
fun ActivityKind.usuallyOnce(): Boolean = this == ActivityKind.TRIP || this == ActivityKind.CAMP

/** "17:30–19:00" for a one-day activity; for one over several days "od 08:00", "ceo dan" or "do 15:00" on [date]. */
fun Extracurricular.timeLabelOn(date: LocalDate): String = when {
    weekly || firstDay == lastDay -> "${start.label()}–${end.label()}"
    date == firstDay -> "od ${start.label()}"
    date == lastDay -> "do ${end.label()}"
    else -> "ceo dan"
}

/** "Pon, Sre · 17:30–19:00" for a weekly activity, "Subota, 7. novembar · 08:00–18:00" or a range for a one-off. */
fun Extracurricular.scheduleLabel(): String =
    if (weekly) {
        DayOfWeek.entries.filter { repeatsOn(it) }.joinToString(", ") { it.shortLabel() } + " · ${start.label()}–${end.label()}"
    } else if (firstDay == lastDay) {
        "${firstDay.label()} · ${start.label()}–${end.label()}"
    } else {
        "${dateRangeLabel(firstDay, lastDay)} · od ${start.label()} do ${end.label()}"
    }

/** "30 min pre", "1 h pre" or "Na početku" for the reminder setting. */
fun reminderOffsetLabel(minutes: Int): String = if (minutes == 0) "Na početku" else "${durationLabel(minutes.toLong())} pre"

/** Title and text of an activity reminder notification. */
data class ActivityReminderContent(val title: String, val text: String)

/** "⚽ Fudbal u 17:30" / "Ana · za 30 min · Sportska hala" for a reminder posted at [now]. */
fun activityReminderContent(child: Child, reminder: ActivityReminder, now: LocalDateTime): ActivityReminderContent {
    val activity = reminder.activity
    val day = when (reminder.start.toLocalDate()) {
        now.toLocalDate() -> ""
        now.toLocalDate().plusDays(1) -> "sutra "
        else -> "${reminder.start.toLocalDate().label().lowercase()} "
    }
    val title = "${activity.kind.emoji()} ${activity.title} ${day}u ${reminder.start.toLocalTime().label()}"
    val minutes = minutesUntil(now, reminder.start)
    val text = listOfNotNull(
        child.name.ifBlank { null },
        if (minutes <= 0) "počinje sada" else "za ${durationLabel(minutes)}",
        activity.place.ifBlank { null },
        activity.note.ifBlank { null },
    ).joinToString(" · ")
    return ActivityReminderContent(title, text)
}

/** Whole minutes from [from] to [to], rounded to the nearest minute so "za 30 min" is not shown as "za 29 min". */
private fun minutesUntil(from: LocalDateTime, to: LocalDateTime): Long =
    (java.time.Duration.between(from, to).seconds + 30) / 60
