package com.bane23.classschedule.ui

import com.bane23.classschedule.data.ActivityKind
import com.bane23.classschedule.data.ActivityReminder
import com.bane23.classschedule.data.ActivitySession
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Extracurricular
import com.bane23.classschedule.data.SessionChange
import com.bane23.classschedule.data.SessionState
import com.bane23.classschedule.data.tr
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

// Labels (Serbian or English, see Lang) for extracurricular activities, kept out of the composables so they can be unit tested.

fun ActivityKind.emoji(): String = when (this) {
    ActivityKind.SPORT -> "⚽"
    ActivityKind.SWIMMING -> "🏊"
    ActivityKind.DANCE -> "💃"
    ActivityKind.MUSIC -> "🎵"
    ActivityKind.DRAMA -> "🎭"
    ActivityKind.ART -> "🎨"
    ActivityKind.LANGUAGE -> "🗣️"
    ActivityKind.TRIP -> "🚌"
    ActivityKind.CAMP -> "🏕️"
    ActivityKind.OTHER -> "⭐"
}

fun ActivityKind.label(): String = when (this) {
    ActivityKind.SPORT -> "Sport"
    ActivityKind.SWIMMING -> tr("Plivanje", "Swimming")
    ActivityKind.DANCE -> tr("Folklor i ples", "Folk dance and dance")
    ActivityKind.MUSIC -> tr("Hor i muzika", "Choir and music")
    ActivityKind.DRAMA -> tr("Gluma", "Drama")
    ActivityKind.ART -> tr("Likovna radionica", "Art workshop")
    ActivityKind.LANGUAGE -> tr("Strani jezik", "Foreign language")
    ActivityKind.TRIP -> tr("Izlet", "School trip")
    ActivityKind.CAMP -> tr("Rekreativna nastava", "School camp")
    ActivityKind.OTHER -> tr("Ostalo", "Other")
}

/** Trips and school camps usually happen once; the rest repeat every week. */
fun ActivityKind.usuallyOnce(): Boolean = this == ActivityKind.TRIP || this == ActivityKind.CAMP

/** "17:30–19:00" for a one-day activity; for one over several days "od 08:00", "ceo dan" or "do 15:00" on [date]. */
fun Extracurricular.timeLabelOn(date: LocalDate): String = when {
    weekly || firstDay == lastDay -> "${start.label()}–${end.label()}"
    date == firstDay -> tr("od ${start.label()}", "from ${start.label()}")
    date == lastDay -> tr("do ${end.label()}", "until ${end.label()}")
    else -> tr("ceo dan", "all day")
}

/** "Pon, Sre · 17:30–19:00" for a weekly activity, "Subota, 7. novembar · 08:00–18:00" or a range for a one-off. */
fun Extracurricular.scheduleLabel(): String =
    if (weekly) {
        DayOfWeek.entries.filter { repeatsOn(it) }.joinToString(", ") { it.shortLabel() } + " · ${start.label()}–${end.label()}"
    } else if (firstDay == lastDay) {
        "${firstDay.label()} · ${start.label()}–${end.label()}"
    } else {
        "${dateRangeLabel(firstDay, lastDay)} · " + tr("od ${start.label()} do ${end.label()}", "from ${start.label()} to ${end.label()}")
    }

/** "pomereno na sredu, 22. oktobar u 18:00" or "otkazano" for a changed session; null for a regular one. */
fun SessionChange.label(): String {
    val to = movedTo ?: return tr("otkazano", "cancelled")
    val time = minuteLabel(startMinute)
    return if (to == regularDay) tr("pomereno na $time", "moved to $time")
    else tr("pomereno na ${to.accusativeLabel()} u $time", "moved to ${to.accusativeLabel()} at $time")
}

/** Short tag for a session that is not as planned: "Otkazano" or "Pomereno"; null for a regular one. */
fun ActivitySession.tag(): String? = when (state) {
    SessionState.REGULAR -> null
    SessionState.CANCELLED -> tr("Otkazano", "Cancelled")
    SessionState.MOVED_AWAY, SessionState.MOVED_HERE -> tr("Pomereno", "Moved")
}

/** The extra line for a changed session: where it moved to, or where it came from; null when there is nothing to add. */
fun ActivitySession.detail(): String? = when (state) {
    SessionState.REGULAR, SessionState.CANCELLED -> null
    SessionState.MOVED_AWAY -> activity.changeOn(date)?.label()
    SessionState.MOVED_HERE -> if (regularDay == date) tr("drugo vreme samo ovog puta", "different time this once only")
    else tr("redovno: ${regularDay.label().lowercase()}", "usually: ${regularDay.label()}")
}

/** "17:30" for minutes since midnight. */
fun minuteLabel(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

/** "30 min pre", "1 h pre" or "Na početku" for the reminder setting; "30 min before" or "At the start" in English. */
fun reminderOffsetLabel(minutes: Int): String =
    if (minutes == 0) tr("Na početku", "At the start") else tr("${durationLabel(minutes.toLong())} pre", "${durationLabel(minutes.toLong())} before")

/** Title and text of an activity reminder notification. */
data class ActivityReminderContent(val title: String, val text: String)

/** "⚽ Fudbal u 17:30" / "Ana · za 30 min · Sportska hala" for a reminder posted at [now]. */
fun activityReminderContent(child: Child, reminder: ActivityReminder, now: LocalDateTime): ActivityReminderContent {
    val activity = reminder.activity
    val day = when (reminder.start.toLocalDate()) {
        now.toLocalDate() -> ""
        now.toLocalDate().plusDays(1) -> tr("sutra ", "tomorrow ")
        else -> tr("${reminder.start.toLocalDate().label().lowercase()} ", "on ${reminder.start.toLocalDate().label()} ")
    }
    val title = "${activity.kind.emoji()} ${activity.title} $day" + tr("u ", "at ") + reminder.start.toLocalTime().label()
    val minutes = minutesUntil(now, reminder.start)
    val text = listOfNotNull(
        child.name.ifBlank { null },
        if (minutes <= 0) tr("počinje sada", "starts now") else tr("za ${durationLabel(minutes)}", "in ${durationLabel(minutes)}"),
        activity.place.ifBlank { null },
        activity.note.ifBlank { null },
    ).joinToString(" · ")
    return ActivityReminderContent(title, text)
}

/** Whole minutes from [from] to [to], rounded to the nearest minute so "za 30 min" is not shown as "za 29 min". */
private fun minutesUntil(from: LocalDateTime, to: LocalDateTime): Long =
    (java.time.Duration.between(from, to).seconds + 30) / 60
