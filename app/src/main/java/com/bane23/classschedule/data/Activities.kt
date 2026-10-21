package com.bane23.classschedule.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** What kind of extracurricular activity it is; only picks the emoji and the suggested title. */
@Serializable
enum class ActivityKind { SPORT, SWIMMING, DANCE, MUSIC, DRAMA, ART, LANGUAGE, TRIP, CAMP, OTHER }

/**
 * One session of a weekly activity that does not happen as usual: the session regularly on [epochDay] is cancelled
 * ([movedToEpochDay] null) or moved to [movedToEpochDay] from [startMinute] to [endMinute] (possibly the same day at
 * another time). The regular weekly schedule is not touched, so the following weeks stay as they were.
 */
@Serializable
data class SessionChange(
    val epochDay: Long,
    val movedToEpochDay: Long? = null,
    val startMinute: Int = 0,
    val endMinute: Int = 0,
) {
    val cancelled: Boolean get() = movedToEpochDay == null
    val regularDay: LocalDate get() = LocalDate.ofEpochDay(epochDay)
    val movedTo: LocalDate? get() = movedToEpochDay?.let(LocalDate::ofEpochDay)
}

/** Default minutes between an activity reminder and the start of the activity. */
const val DEFAULT_ACTIVITY_REMINDER_MINUTES = 30

/**
 * An extracurricular activity of one child (sport, choir, folklore, a school trip, ...). It either repeats every
 * week on [days] (ISO numbers, 1 = Monday) or, when [days] is empty, happens once from [startEpochDay] to
 * [endEpochDay]. Times are minutes since midnight; for a one-off over several days the activity starts at
 * [startMinute] on the first day and ends at [endMinute] on the last.
 */
@Serializable
data class Extracurricular(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val kind: ActivityKind = ActivityKind.OTHER,
    val days: List<Int> = emptyList(),
    val startEpochDay: Long = 0,
    val endEpochDay: Long = 0,
    val startMinute: Int,
    val endMinute: Int,
    val place: String = "",
    val note: String = "",
    /** A weekly activity that does not take place during school breaks and holidays. */
    val pausedOnBreaks: Boolean = false,
    /** A one-off (a trip, a school camp) on whose days there are no classes; it then counts as a day off. */
    val noClasses: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderMinutesBefore: Int = DEFAULT_ACTIVITY_REMINDER_MINUTES,
    /** Single sessions of a weekly activity that were cancelled or moved, at most one per regular day. */
    val changes: List<SessionChange> = emptyList(),
) {
    val weekly: Boolean get() = days.isNotEmpty()
    val start: LocalTime get() = LocalTime.of(startMinute / 60, startMinute % 60)
    val end: LocalTime get() = LocalTime.of(endMinute / 60, endMinute % 60)
    val firstDay: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)
    val lastDay: LocalDate get() = LocalDate.ofEpochDay(maxOf(startEpochDay, endEpochDay))

    fun repeatsOn(day: DayOfWeek): Boolean = day.value in days

    /** The change of the session regularly on [date], if any. */
    fun changeOn(date: LocalDate): SessionChange? = changes.find { it.epochDay == date.toEpochDay() }

    /** This activity with the session regularly on [date] changed as [change] says; null restores it. */
    fun withChange(date: LocalDate, change: SessionChange?): Extracurricular =
        copy(changes = changes.filterNot { it.epochDay == date.toEpochDay() } + listOfNotNull(change))
}

/** How a session of an activity on a given day relates to the regular schedule. */
enum class SessionState {
    /** As planned (also every one-off activity). */
    REGULAR,

    /** Cancelled this once. */
    CANCELLED,

    /** Moved away from this day; it takes place on [SessionChange.movedTo]. */
    MOVED_AWAY,

    /** Moved here from its regular day. */
    MOVED_HERE,
}

/**
 * One session of [activity] on [date]. For a moved session [activity] carries the moved times, so labels and
 * reminders follow the change; [regularDay] is the day it was planned for, which identifies its [SessionChange].
 */
data class ActivitySession(
    val activity: Extracurricular,
    val date: LocalDate,
    val regularDay: LocalDate,
    val state: SessionState,
) {
    /** Whether the activity actually takes place in this session. */
    val takesPlace: Boolean get() = state == SessionState.REGULAR || state == SessionState.MOVED_HERE
}

/** Whether [date] falls in a school break or holiday for this child (weekends alone do not count). */
private fun Child.isBreak(date: LocalDate): Boolean = calendarEvents().any { it.noClasses && it.covers(date) }

/** Whether a weekly [activity] regularly has a session on [date] (before any one-time change). */
private fun Child.regularlyOn(activity: Extracurricular, date: LocalDate): Boolean =
    activity.repeatsOn(date.dayOfWeek) && !(activity.pausedOnBreaks && isBreak(date))

/**
 * The sessions of [activity] on [date], including cancelled and moved-away ones so they can still be shown. A weekly
 * activity can have two sessions on a day when another day's session was moved there.
 */
fun Child.sessionsOf(activity: Extracurricular, date: LocalDate): List<ActivitySession> {
    if (!activity.weekly) {
        return if (date >= activity.firstDay && date <= activity.lastDay) {
            listOf(ActivitySession(activity, date, activity.firstDay, SessionState.REGULAR))
        } else {
            emptyList()
        }
    }
    val sessions = mutableListOf<ActivitySession>()
    if (regularlyOn(activity, date)) {
        val change = activity.changeOn(date)
        when {
            change == null -> sessions += ActivitySession(activity, date, date, SessionState.REGULAR)
            change.cancelled -> sessions += ActivitySession(activity, date, date, SessionState.CANCELLED)
            // Moved to another time on the same day: only the moved session is shown.
            change.movedTo != date -> sessions += ActivitySession(activity, date, date, SessionState.MOVED_AWAY)
        }
    }
    activity.changes.filter { it.movedTo == date && regularlyOn(activity, it.regularDay) }.forEach {
        val moved = activity.copy(startMinute = it.startMinute, endMinute = it.endMinute)
        sessions += ActivitySession(moved, date, it.regularDay, SessionState.MOVED_HERE)
    }
    return sessions
}

/** Whether [activity] begins on [date]; a one-off over several days begins only on its first day. */
fun Child.activityStartsOn(activity: Extracurricular, date: LocalDate): Boolean =
    if (activity.weekly) hasActivityOn(activity, date) else date == activity.firstDay

/** Whether [session] begins on its date; a one-off over several days begins only on its first day. */
private fun ActivitySession.startsHere(): Boolean = activity.weekly || date == activity.firstDay

/**
 * Every session of the child's activities on [date], cancelled and moved-away ones included, earliest first (an
 * activity continuing from an earlier day comes first).
 */
fun Child.sessionsOn(date: LocalDate): List<ActivitySession> =
    activities.flatMap { sessionsOf(it, date) }
        .sortedWith(compareBy({ it.startsHere() }, { it.activity.startMinute }, { it.activity.title }))

/** Whether [activity] takes place (at least partly) on [date]. */
fun Child.hasActivityOn(activity: Extracurricular, date: LocalDate): Boolean =
    sessionsOf(activity, date).any { it.takesPlace }

/**
 * The activities taking place on [date], earliest first (an activity continuing from an earlier day comes first). A
 * moved session comes with its moved times.
 */
fun Child.activitiesOn(date: LocalDate): List<Extracurricular> =
    sessionsOn(date).filter { it.takesPlace }.map { it.activity }

/** The next [count] regular days of a weekly [activity] from [from] on, for picking a session to change. */
fun Child.upcomingRegularDays(activity: Extracurricular, from: LocalDate, count: Int = 6): List<LocalDate> =
    if (!activity.weekly) emptyList()
    else generateSequence(from) { it.plusDays(1) }.take(366).filter { regularlyOn(activity, it) }.take(count).toList()

/** The weekly activities on [day] of the regular week, earliest first. */
fun Child.weeklyActivitiesOn(day: DayOfWeek): List<Extracurricular> =
    activities.filter { it.repeatsOn(day) }.sortedBy { it.startMinute }

/** One reminder: [activity] starts at [start], and the notification is due at [at]. */
data class ActivityReminder(val activity: Extracurricular, val start: LocalDateTime, val at: LocalDateTime)

/** Reminders of activities starting on [date] (those with the reminder on). */
private fun Child.remindersFor(date: LocalDate): List<ActivityReminder> =
    sessionsOn(date).filter { it.takesPlace && it.startsHere() && it.activity.reminderEnabled }.map {
        val start = date.atTime(it.activity.start)
        ActivityReminder(it.activity, start, start.minusMinutes(it.activity.reminderMinutesBefore.toLong()))
    }

/**
 * The next activity reminder strictly after [now], looking up to [daysAhead] days ahead. A reminder can fall on the
 * day before the activity (e.g. two hours before a 01:00 start), so the search starts one day back and, once a day
 * has a reminder, the following day is checked too.
 */
fun Child.nextActivityReminder(now: LocalDateTime, daysAhead: Int = 120): LocalDateTime? {
    fun upcoming(date: LocalDate) = remindersFor(date).map { it.at }.filter { it.isAfter(now) }
    for (offset in -1..daysAhead) {
        val date = now.toLocalDate().plusDays(offset.toLong())
        val found = upcoming(date)
        if (found.isNotEmpty()) return (found + upcoming(date.plusDays(1))).min()
    }
    return null
}

/**
 * Reminders due in [from]..[to] (both inclusive), so an alarm that fired a few minutes late still posts every
 * reminder it stood for.
 */
fun Child.activityRemindersDue(from: LocalDateTime, to: LocalDateTime): List<ActivityReminder> {
    val reminders = mutableListOf<ActivityReminder>()
    var date = from.toLocalDate().minusDays(1)
    while (date <= to.toLocalDate().plusDays(1)) {
        reminders += remindersFor(date).filter { !it.at.isBefore(from) && !it.at.isAfter(to) }
        date = date.plusDays(1)
    }
    return reminders.sortedBy { it.at }
}

/** Adds [activity], or replaces the one with the same id. */
fun Child.withActivity(activity: Extracurricular): Child =
    if (activities.any { it.id == activity.id }) copy(activities = activities.map { if (it.id == activity.id) activity else it })
    else copy(activities = activities + activity)

/** Drops one-off activities and session changes that ended before [today], so the lists do not grow forever. */
fun Child.withoutPastActivities(today: LocalDate): Child =
    copy(
        activities = activities.filter { it.weekly || it.lastDay >= today }.map { activity ->
            val current = activity.changes.filter { it.regularDay >= today || (it.movedTo ?: it.regularDay) >= today }
            if (current.size == activity.changes.size) activity else activity.copy(changes = current)
        },
    )
