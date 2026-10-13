package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class AlarmPlannerTest {
    // Morning shift: 1st class 08:00–08:45, 2nd 08:50–09:35, 3rd 09:55–10:40.
    private val timetable = Timetable.defaultFor(Shift.MORNING)
        .withLesson(DayOfWeek.WEDNESDAY, 0, "Matematika")
        .withLesson(DayOfWeek.WEDNESDAY, 2, "Srpski jezik")
        .withLesson(DayOfWeek.THURSDAY, 0, "Fizičko")

    private val wednesday = LocalDate.of(2026, 10, 14)

    private val child = Child(
        name = "Test",
        shiftMode = ShiftMode.FIXED_MORNING,
        referenceEpochDay = wednesday.toEpochDay(),
        morning = timetable,
        alarmEnabled = true,
    )

    @Test
    fun alarmAtEndOfLastClassToday() {
        assertEquals(wednesday.atTime(10, 40), child.nextAlarm(wednesday.atTime(7, 0)))
    }

    @Test
    fun alarmMovesToNextSchoolDayAfterClassesEnd() {
        assertEquals(wednesday.plusDays(1).atTime(8, 45), child.nextAlarm(wednesday.atTime(10, 40)))
    }

    @Test
    fun minutesBeforeShiftsTheAlarm() {
        val early = child.copy(alarmMinutesBefore = 10)
        assertEquals(wednesday.atTime(10, 30), early.nextAlarm(wednesday.atTime(7, 0)))
    }

    @Test
    fun cancelledLastClassMovesAlarmEarlier() {
        val changed = child.withDayChange(wednesday, 2, "")
        assertEquals(wednesday.atTime(8, 45), changed.nextAlarm(wednesday.atTime(7, 0)))
    }

    @Test
    fun skipsBreaksAndDisabledAlarm() {
        // Autumn break is 11–13 Nov 2026; the next Wednesday with classes is 18 Nov.
        val inBreak = LocalDateTime.of(2026, 11, 11, 7, 0)
        assertEquals(LocalDate.of(2026, 11, 18).atTime(10, 40), child.nextAlarm(inBreak.plusDays(1)))
        assertNull(child.copy(alarmEnabled = false).nextAlarm(wednesday.atTime(7, 0)))
    }
}
