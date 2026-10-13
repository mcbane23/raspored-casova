package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.DayOfWeek

class TimetableTest {
    private val afternoon = Timetable.defaultFor(Shift.AFTERNOON)
        .withLesson(DayOfWeek.TUESDAY, 0, "Srpski jezik")
        .withLesson(DayOfWeek.TUESDAY, 1, "Matematika")

    @Test
    fun preClassIsAddedBeforeFirstClassAndKeepsSubjectsInPlace() {
        val withPre = afternoon.withPreClass(true)

        assertEquals(afternoon.periods.size + 1, withPre.periods.size)
        assertEquals(Period(13 * 60 + 10, 13 * 60 + 55), withPre.periods.first())
        assertEquals(listOf("", "Srpski jezik", "Matematika"), withPre.lessonsFor(DayOfWeek.TUESDAY))
        assertEquals(emptyList<String>(), withPre.lessonsFor(DayOfWeek.MONDAY))
        assertEquals("Pretčas", withPre.periodLabel(0))
        assertEquals("1. čas", withPre.periodLabel(1))
        assertEquals("P", withPre.periodNumber(0))
        assertEquals("1. čas", afternoon.periodLabel(0))
    }

    @Test
    fun removingPreClassRestoresTimetable() {
        val back = afternoon.withPreClass(true)
            .withLesson(DayOfWeek.TUESDAY, 0, "Engleski jezik")
            .withPreClass(false)

        assertFalse(back.hasPreClass)
        assertEquals(afternoon.periods, back.periods)
        assertEquals(listOf("Srpski jezik", "Matematika"), back.lessonsFor(DayOfWeek.TUESDAY))
    }

    @Test
    fun extraClassRoundTrip() {
        val lesson = ExtraClass.REMEDIAL.lesson("Matematika")
        assertEquals("Dopunska: Matematika", lesson)
        assertEquals(ExtraClass.REMEDIAL to "Matematika", ExtraClass.parse(lesson))
        assertEquals(ExtraClass.ADVANCED to "Fizika", ExtraClass.parse("Dodatna: Fizika"))
        assertEquals(null to "Srpski jezik", ExtraClass.parse("Srpski jezik"))
    }

    @Test
    fun preClassCountsForEndOfDay() {
        val child = Child(
            name = "Test",
            shiftMode = ShiftMode.FIXED_AFTERNOON,
            referenceEpochDay = 0,
            afternoon = afternoon.withPreClass(true).withLesson(DayOfWeek.TUESDAY, 0, "Engleski jezik"),
        )
        val tuesday = java.time.LocalDate.of(2026, 10, 13)
        val lessons = child.lessonsOn(tuesday)
        assertEquals(listOf("Engleski jezik", "Srpski jezik", "Matematika"), lessons.map { it.subject })
        assertEquals(13 * 60 + 10, lessons.first().period.startMinute)
    }
}
