package com.bane23.classschedule.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Which shift a child attends on a given date. */
fun Child.shiftOn(date: LocalDate): Shift {
    val reference = LocalDate.ofEpochDay(referenceEpochDay)
    val periodsApart = when (shiftMode) {
        ShiftMode.FIXED_MORNING -> return Shift.MORNING
        ShiftMode.FIXED_AFTERNOON -> return Shift.AFTERNOON
        ShiftMode.WEEKLY -> ChronoUnit.WEEKS.between(reference.mondayOfWeek(), date.mondayOfWeek())
        ShiftMode.MONTHLY -> ChronoUnit.MONTHS.between(reference.withDayOfMonth(1), date.withDayOfMonth(1))
    }
    return if (Math.floorMod(periodsApart, 2L) == 0L) referenceShift else referenceShift.other()
}

/**
 * The regular subjects for [date] from the timetable, one per period ("" for a free period). Empty on days without
 * classes (weekends, breaks, holidays); on a swapped day it uses the timetable of the day named by the calendar.
 */
fun Child.regularSubjectsOn(date: LocalDate): List<String> = when (val info = dayInfo(date)) {
    is DayInfo.NoClasses -> emptyList()
    is DayInfo.SchoolDay -> timetable(shiftOn(date)).lessonsFor(info.timetableDay)
}

fun Child.overrideOn(date: LocalDate): DayOverride? = overrides.find { it.epochDay == date.toEpochDay() }

/** The subjects for [date], with any one-off change for that day applied. */
fun Child.subjectsOn(date: LocalDate): List<String> = overrideOn(date)?.lessons ?: regularSubjectsOn(date)

/** A lesson actually held on a date: its period number (0-based), bell times and subject. */
data class Lesson(val index: Int, val period: Period, val subject: String)

/** The lessons a child has on [date], with bell times, the school calendar and one-off changes applied. */
fun Child.lessonsOn(date: LocalDate): List<Lesson> {
    val periods = timetable(shiftOn(date)).periods
    return subjectsOn(date).mapIndexedNotNull { i, subject ->
        val period = periods.getOrNull(i)
        if (period == null || subject.isBlank()) null else Lesson(i, period, subject)
    }
}

/**
 * Returns this child with the subject of period [index] on [date] changed for that day only ("" cancels it).
 * A change that makes the day match the regular timetable again removes the override.
 */
fun Child.withDayChange(date: LocalDate, index: Int, subject: String): Child {
    val current = subjectsOn(date)
    val padded = current + List((index + 1 - current.size).coerceAtLeast(0)) { "" }
    val updated = padded.toMutableList().also { it[index] = subject.trim() }.dropLastWhile { it.isBlank() }
    val others = overrides.filterNot { it.epochDay == date.toEpochDay() }
    val regular = regularSubjectsOn(date).dropLastWhile { it.isBlank() }
    return copy(overrides = if (updated == regular) others else others + DayOverride(date.toEpochDay(), updated))
}

/**
 * Returns this child with [timetable] as the timetable of [shift]. When the pre-class is turned on or off, the one-day
 * changes on that shift's days move along with the classes, as [Timetable.withPreClass] moves the regular ones.
 */
fun Child.withTimetableKeepingChanges(shift: Shift, timetable: Timetable): Child {
    val before = timetable(shift).hasPreClass
    val updated = withTimetable(shift, timetable)
    if (before == timetable.hasPreClass) return updated
    return updated.copy(
        overrides = overrides.map { change ->
            if (shiftOn(LocalDate.ofEpochDay(change.epochDay)) != shift || change.lessons.isEmpty()) change
            else if (timetable.hasPreClass) change.copy(lessons = listOf("") + change.lessons)
            else change.copy(lessons = change.lessons.drop(1).dropLastWhile { it.isBlank() })
        },
    )
}

/** Returns this child with the one-off change for [date] removed. */
fun Child.withoutDayChange(date: LocalDate): Child =
    copy(overrides = overrides.filterNot { it.epochDay == date.toEpochDay() })

/** Drops one-off changes for days before [today]; they no longer affect anything. */
fun Child.withoutPastChanges(today: LocalDate): Child =
    copy(overrides = overrides.filter { it.epochDay >= today.toEpochDay() })

fun Shift.other(): Shift = if (this == Shift.MORNING) Shift.AFTERNOON else Shift.MORNING

fun LocalDate.mondayOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * The day whose shift counts as "this week's" for a weekly rotation: on a weekend that is the coming Monday, since
 * parents set the shift up for the school week ahead.
 */
fun LocalDate.upcomingSchoolWeekDay(): LocalDate =
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) with(TemporalAdjusters.next(DayOfWeek.MONDAY)) else this

/** The date whose shift the settings and the week view call the current one; see [upcomingSchoolWeekDay]. */
fun Child.currentShiftDate(today: LocalDate): LocalDate =
    if (shiftMode == ShiftMode.WEEKLY) today.upcomingSchoolWeekDay() else today
