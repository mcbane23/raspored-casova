package com.bane23.classschedule.data

import java.time.Duration
import java.time.LocalTime

/** Where a school day stands at a given time; drives the big card at the top of the Today and Family screens. */
sealed interface DayStatus {
    /** Classes have not started; [minutes] until the first one. */
    data class Before(val first: Lesson, val minutes: Long) : DayStatus

    /** [lesson] is in progress with [minutesLeft] to go; [next] is the following lesson, if any. */
    data class InClass(val lesson: Lesson, val next: Lesson?, val minutesLeft: Long, val progress: Float) : DayStatus

    /** Between two lessons; [next] starts in [minutes], [progress] is how much of the break has passed. */
    data class Break(val next: Lesson, val minutes: Long, val progress: Float) : DayStatus

    /** The last lesson has ended. */
    data object Done : DayStatus
}

/** Status of a day with [lessons] (sorted by time) at [now]; null when there are no lessons. */
fun dayStatus(lessons: List<Lesson>, now: LocalTime): DayStatus? {
    val first = lessons.firstOrNull() ?: return null
    if (now < first.period.start) return DayStatus.Before(first, minutesBetween(now, first.period.start))
    lessons.forEachIndexed { i, lesson ->
        val start = lesson.period.start
        val end = lesson.period.end
        if (now >= start && now < end) {
            val total = minutesBetween(start, end).coerceAtLeast(1)
            val left = minutesBetween(now, end)
            return DayStatus.InClass(lesson, lessons.getOrNull(i + 1), left, 1f - left.toFloat() / total)
        }
        val next = lessons.getOrNull(i + 1)
        if (next != null && now >= end && now < next.period.start) {
            val total = minutesBetween(end, next.period.start).coerceAtLeast(1)
            val left = minutesBetween(now, next.period.start)
            return DayStatus.Break(next, left, 1f - left.toFloat() / total)
        }
    }
    return DayStatus.Done
}

/** Whole minutes from [from] to [to], rounded up so a countdown never shows "0 min" early. */
fun minutesBetween(from: LocalTime, to: LocalTime): Long =
    ((Duration.between(from, to).seconds + 59) / 60).coerceAtLeast(0)
