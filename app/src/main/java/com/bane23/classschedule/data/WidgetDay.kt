package com.bane23.classschedule.data

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * What the home-screen widget shows: one school day's [lessons] and how much of the bag is packed for the next school
 * day ([packed] of [toPack] subjects, for [packDay]).
 */
data class WidgetDay(
    val date: LocalDate,
    val lessons: List<Lesson>,
    val packDay: LocalDate?,
    val packed: Int,
    val toPack: Int,
)

/**
 * Today while it still has classes ahead or in progress, otherwise the next day with classes (null when there is none
 * within two weeks, e.g. during a long break).
 */
fun Child.widgetDay(now: LocalDateTime): WidgetDay? {
    val today = now.toLocalDate()
    val todayEnd = classesEndOn(today)
    val date = if (todayEnd != null && now.toLocalTime() < todayEnd) today else nextSchoolDayAfter(today) ?: return null
    // Same day the Today tab packs for: the school day after today.
    val packDay = nextSchoolDayAfter(today)
    val subjects = packDay?.let { packSubjects(it) }.orEmpty()
    val packed = if (packDay == null) 0 else subjects.count { isPacked(packDay, it) }
    return WidgetDay(date, lessonsOn(date), packDay, packed, subjects.size)
}

/** When the widget next changes on its own: when today's classes end (it moves on to the next day), or at midnight. */
fun Child.nextWidgetRefresh(now: LocalDateTime): LocalDateTime {
    val today = now.toLocalDate()
    val end = classesEndOn(today)?.let { today.atTime(it) }
    return if (end != null && end.isAfter(now)) end else today.plusDays(1).atStartOfDay()
}

/** Whether any class has been entered in either shift's timetable. */
fun Child.hasTimetable(): Boolean = (morning.lessons + afternoon.lessons).any { day -> day.any { it.isNotBlank() } }
