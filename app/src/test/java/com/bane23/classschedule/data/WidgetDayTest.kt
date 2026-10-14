package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WidgetDayTest {
    // Wednesday: three classes, 08:00–10:40; Monday: one class. Thursday and Friday are empty.
    private val timetable = Timetable.defaultFor(Shift.MORNING)
        .withLesson(DayOfWeek.WEDNESDAY, 0, "Matematika")
        .withLesson(DayOfWeek.WEDNESDAY, 1, "Fizičko i zdravstveno vaspitanje")
        .withLesson(DayOfWeek.WEDNESDAY, 2, "Srpski jezik")
        .withLesson(DayOfWeek.MONDAY, 0, "Srpski jezik")

    private val tuesday = LocalDate.of(2026, 10, 13)
    private val wednesday = tuesday.plusDays(1)
    private val monday = LocalDate.of(2026, 10, 19)

    private val child = Child(
        name = "Mila",
        shiftMode = ShiftMode.FIXED_MORNING,
        referenceEpochDay = tuesday.toEpochDay(),
        morning = timetable,
    )

    @Test
    fun showsTodayUntilTheLastClassEnds() {
        val day = child.widgetDay(wednesday.atTime(9, 0))!!
        assertEquals(wednesday, day.date)
        assertEquals(listOf("Matematika", "Fizičko i zdravstveno vaspitanje", "Srpski jezik"), day.lessons.map { it.subject })
    }

    @Test
    fun movesOnToTheNextSchoolDayAfterClasses() {
        assertEquals(monday, child.widgetDay(wednesday.atTime(10, 40))!!.date)
    }

    @Test
    fun dayWithoutClassesShowsTheNextOne() {
        assertEquals(wednesday, child.widgetDay(tuesday.atTime(7, 0))!!.date)
    }

    @Test
    fun countsWhatIsPackedForTheNextSchoolDay() {
        val day = child.withPacked(wednesday, "Matematika", true).widgetDay(tuesday.atTime(19, 0))!!
        assertEquals(wednesday, day.packDay)
        assertEquals(1, day.packed)
        assertEquals(3, day.toPack)
    }

    @Test
    fun noTimetableMeansNothingToShow() {
        val empty = child.copy(morning = Timetable.defaultFor(Shift.MORNING))
        assertEquals(null, empty.widgetDay(wednesday.atTime(9, 0)))
        assertFalse(empty.hasTimetable())
        assertTrue(child.hasTimetable())
    }

    @Test
    fun refreshesWhenClassesEndThenAtMidnight() {
        assertEquals(wednesday.atTime(10, 40), child.nextWidgetRefresh(wednesday.atTime(9, 0)))
        assertEquals(wednesday.plusDays(1).atStartOfDay(), child.nextWidgetRefresh(wednesday.atTime(11, 0)))
        assertEquals(wednesday.atStartOfDay(), child.nextWidgetRefresh(tuesday.atTime(12, 0)))
    }
}
