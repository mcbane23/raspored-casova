package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DayStatusTest {
    // 1st class 08:00–08:45, 2nd 08:50–09:35, 3rd 09:55–10:40.
    private val periods = Timetable.defaultFor(Shift.MORNING).periods
    private val lessons = listOf("Srpski jezik", "Matematika", "Engleski jezik").mapIndexed { i, s -> Lesson(i, periods[i], s) }

    @Test
    fun noLessonsHasNoStatus() {
        assertNull(dayStatus(emptyList(), LocalTime.of(9, 0)))
    }

    @Test
    fun beforeFirstClassCountsDown() {
        assertEquals(DayStatus.Before(lessons[0], 30), dayStatus(lessons, LocalTime.of(7, 30)))
    }

    @Test
    fun duringClassShowsTimeLeftAndNext() {
        val status = dayStatus(lessons, LocalTime.of(10, 20)) as DayStatus.InClass
        assertEquals(lessons[2], status.lesson)
        assertNull(status.next)
        assertEquals(20, status.minutesLeft)
    }

    @Test
    fun bigBreakPointsAtNextClass() {
        val status = dayStatus(lessons, LocalTime.of(9, 45)) as DayStatus.Break
        assertEquals(lessons[2], status.next)
        assertEquals(10, status.minutes)
        assertTrue(status.progress in 0.4f..0.6f)
    }

    @Test
    fun afterLastClassIsDone() {
        assertEquals(DayStatus.Done, dayStatus(lessons, LocalTime.of(10, 40)))
    }
}
