package com.bane23.classschedule.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** When the last class on [date] ends, or null if there are no classes that day. */
fun Child.classesEndOn(date: LocalDate): LocalTime? = lessonsOn(date).lastOrNull()?.period?.end

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
