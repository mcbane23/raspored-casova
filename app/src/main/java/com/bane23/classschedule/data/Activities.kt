package com.bane23.classschedule.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** What kind of extracurricular activity it is; only picks the emoji and the suggested title. */
@Serializable
enum class ActivityKind { SPORT, SWIMMING, DANCE, MUSIC, ART, LANGUAGE, TRIP, CAMP, OTHER }

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
) {
    val weekly: Boolean get() = days.isNotEmpty()
    val start: LocalTime get() = LocalTime.of(startMinute / 60, startMinute % 60)
    val end: LocalTime get() = LocalTime.of(endMinute / 60, endMinute % 60)
    val firstDay: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)
    val lastDay: LocalDate get() = LocalDate.ofEpochDay(maxOf(startEpochDay, endEpochDay))

    fun repeatsOn(day: DayOfWeek): Boolean = day.value in days
}

/** Whether [date] falls in a school break or holiday for this child (weekends alone do not count). */
private fun Child.isBreak(date: LocalDate): Boolean = calendarEvents().any { it.noClasses && it.covers(date) }

/** Whether [activity] takes place (at least partly) on [date]. */
fun Child.hasActivityOn(activity: Extracurricular, date: LocalDate): Boolean =
    if (activity.weekly) activity.repeatsOn(date.dayOfWeek) && !(activity.pausedOnBreaks && isBreak(date))
    else date >= activity.firstDay && date <= activity.lastDay

/** Whether [activity] begins on [date]; a one-off over several days begins only on its first day. */
fun Child.activityStartsOn(activity: Extracurricular, date: LocalDate): Boolean =
    if (activity.weekly) hasActivityOn(activity, date) else date == activity.firstDay

/** The child's activities on [date], earliest first (an activity continuing from an earlier day comes first). */
fun Child.activitiesOn(date: LocalDate): List<Extracurricular> =
    activities.filter { hasActivityOn(it, date) }
        .sortedWith(compareBy({ activityStartsOn(it, date) }, { it.startMinute }, { it.title }))

/** The weekly activities on [day] of the regular week, earliest first. */
fun Child.weeklyActivitiesOn(day: DayOfWeek): List<Extracurricular> =
    activities.filter { it.repeatsOn(day) }.sortedBy { it.startMinute }

/** One reminder: [activity] starts at [start], and the notification is due at [at]. */
data class ActivityReminder(val activity: Extracurricular, val start: LocalDateTime, val at: LocalDateTime)

/** Reminders of activities starting on [date] (those with the reminder on). */
private fun Child.remindersFor(date: LocalDate): List<ActivityReminder> =
    activities.filter { it.reminderEnabled && activityStartsOn(it, date) }.map {
        val start = date.atTime(it.start)
        ActivityReminder(it, start, start.minusMinutes(it.reminderMinutesBefore.toLong()))
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

/** Drops one-off activities that ended before [today], so the list does not grow forever. */
fun Child.withoutPastActivities(today: LocalDate): Child =
    copy(activities = activities.filter { it.weekly || it.lastDay >= today })
