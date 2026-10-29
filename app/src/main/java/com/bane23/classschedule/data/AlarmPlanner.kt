package com.bane23.classschedule.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** When the last class on [date] ends, or null if there are no classes that day. */
fun Child.classesEndOn(date: LocalDate): LocalTime? = lessonsOn(date).maxOfOrNull { it.period.end }

/** When the first class on [date] starts, or null if there are no classes that day. */
fun Child.classesStartOn(date: LocalDate): LocalTime? = lessonsOn(date).minOfOrNull { it.period.start }

/**
 * The next time strictly after [now] the parent should be told that classes are ending, looking up to [daysAhead]
 * days ahead (long breaks are skipped). Null when the alarm is off or no classes are found.
 */
fun Child.nextAlarm(now: LocalDateTime, daysAhead: Int = 120): LocalDateTime? {
    if (!alarmEnabled) return null
    for (offset in 0..daysAhead) {
        val date = now.toLocalDate().plusDays(offset.toLong())
        val end = classesEndOn(date) ?: continue
        val alarm = date.atTime(end).minusMinutes(alarmMinutesBefore.toLong())
        if (alarm.isAfter(now)) return alarm
    }
    return null
}

/**
 * The next packing reminder strictly after [now]: at [Child.packReminderMinute] on each day whose following day has
 * classes, looking up to [daysAhead] days ahead. Null when the reminder is off or no classes are found.
 */
fun Child.nextPackReminder(now: LocalDateTime, daysAhead: Int = 120): LocalDateTime? {
    if (!packReminderEnabled) return null
    val time = LocalTime.of(packReminderMinute / 60, packReminderMinute % 60)
    for (offset in 0..daysAhead) {
        val date = now.toLocalDate().plusDays(offset.toLong())
        if (lessonsOn(date.plusDays(1)).isEmpty()) continue
        val reminder = date.atTime(time)
        if (reminder.isAfter(now)) return reminder
    }
    return null
}

/**
 * The next morning reminder strictly after [now]: [Child.morningReminderMinutesBefore] minutes before the first class
 * of each day with classes (with the shift, the school calendar and one-day changes applied), looking up to
 * [daysAhead] days ahead. Null when the reminder is off or no classes are found.
 */
fun Child.nextMorningReminder(now: LocalDateTime, daysAhead: Int = 120): LocalDateTime? {
    if (!morningReminderEnabled) return null
    for (offset in 0..daysAhead) {
        val date = now.toLocalDate().plusDays(offset.toLong())
        val start = classesStartOn(date) ?: continue
        val reminder = date.atTime(start).minusMinutes(morningReminderMinutesBefore.toLong())
        if (reminder.isAfter(now)) return reminder
    }
    return null
}
