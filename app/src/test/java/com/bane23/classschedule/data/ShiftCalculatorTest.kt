package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ShiftCalculatorTest {
    // Wednesday 2026-10-14 is in the week of Monday 2026-10-12.
    private val reference = LocalDate.of(2026, 10, 14)

    private fun child(mode: ShiftMode, shift: Shift = Shift.MORNING) =
        Child(name = "Test", shiftMode = mode, referenceEpochDay = reference.toEpochDay(), referenceShift = shift)

    @Test
    fun weeklyAlternatesByWeek() {
        val child = child(ShiftMode.WEEKLY)
        assertEquals(Shift.MORNING, child.shiftOn(LocalDate.of(2026, 10, 12)))
        assertEquals(Shift.MORNING, child.shiftOn(LocalDate.of(2026, 10, 18)))
        assertEquals(Shift.AFTERNOON, child.shiftOn(LocalDate.of(2026, 10, 19)))
        assertEquals(Shift.MORNING, child.shiftOn(LocalDate.of(2026, 10, 26)))
        assertEquals(Shift.AFTERNOON, child.shiftOn(LocalDate.of(2026, 10, 9)))
    }

    @Test
    fun monthlyAlternatesByMonth() {
        val child = child(ShiftMode.MONTHLY, Shift.AFTERNOON)
        assertEquals(Shift.AFTERNOON, child.shiftOn(LocalDate.of(2026, 10, 1)))
        assertEquals(Shift.MORNING, child.shiftOn(LocalDate.of(2026, 11, 30)))
        assertEquals(Shift.AFTERNOON, child.shiftOn(LocalDate.of(2026, 12, 1)))
        assertEquals(Shift.MORNING, child.shiftOn(LocalDate.of(2026, 9, 15)))
    }

    @Test
    fun fixedShiftNeverChanges() {
        assertEquals(Shift.AFTERNOON, child(ShiftMode.FIXED_AFTERNOON).shiftOn(LocalDate.of(2027, 3, 3)))
        assertEquals(Shift.MORNING, child(ShiftMode.FIXED_MORNING).shiftOn(LocalDate.of(2027, 3, 10)))
    }

    @Test
    fun lessonsOnSkipsFreePeriodsAndWeekends() {
        val timetable = Timetable.defaultFor(Shift.MORNING)
            .withLesson(DayOfWeek.WEDNESDAY, 0, "Matematika")
            .withLesson(DayOfWeek.WEDNESDAY, 2, "Srpski jezik")
        val child = child(ShiftMode.FIXED_MORNING).copy(morning = timetable)

        val lessons = child.lessonsOn(reference)
        assertEquals(listOf("Matematika", "Srpski jezik"), lessons.map { it.subject })
        assertEquals(listOf(0, 2), lessons.map { it.index })
        assertEquals(8 * 60, lessons.first().period.startMinute)
        assertEquals(emptyList<Lesson>(), child.lessonsOn(LocalDate.of(2026, 10, 17)))
    }

    private fun childWithWednesday(vararg subjects: String): Child {
        var timetable = Timetable.defaultFor(Shift.MORNING)
        subjects.forEachIndexed { i, s -> timetable = timetable.withLesson(DayOfWeek.WEDNESDAY, i, s) }
        return child(ShiftMode.FIXED_MORNING).copy(morning = timetable)
    }

    @Test
    fun dayChangeSubstitutesSubjectOnlyOnThatDay() {
        val child = childWithWednesday("Engleski", "Matematika")
            .withDayChange(reference, 0, "Srpski jezik")

        assertEquals(listOf("Srpski jezik", "Matematika"), child.lessonsOn(reference).map { it.subject })
        assertEquals(listOf("Engleski", "Matematika"), child.lessonsOn(reference.plusWeeks(1)).map { it.subject })
    }

    @Test
    fun cancellingLastClassShortensTheDay() {
        val child = childWithWednesday("Matematika", "Fizičko")
            .withDayChange(reference, 1, "")

        val lessons = child.lessonsOn(reference)
        assertEquals(listOf("Matematika"), lessons.map { it.subject })
        assertEquals(1, child.overrides.size)
    }

    @Test
    fun revertingChangeRemovesOverride() {
        val changed = childWithWednesday("Engleski").withDayChange(reference, 0, "Srpski jezik")
        assertEquals(0, changed.withDayChange(reference, 0, "Engleski").overrides.size)
        assertEquals(0, changed.withoutDayChange(reference).overrides.size)
        assertEquals(0, changed.withoutPastChanges(reference.plusDays(1)).overrides.size)
        assertEquals(1, changed.withoutPastChanges(reference).overrides.size)
    }

    @Test
    fun clearingLastLessonTrimsDay() {
        val timetable = Timetable.defaultFor(Shift.MORNING)
            .withLesson(DayOfWeek.MONDAY, 1, "Muzičko")
            .withLesson(DayOfWeek.MONDAY, 1, " ")
        assertEquals(emptyList<String>(), timetable.lessonsFor(DayOfWeek.MONDAY))
    }

    @Test
    fun defaultBellScheduleHasLongBreakAfterSecondClass() {
        val periods = Timetable.defaultFor(Shift.AFTERNOON).periods
        assertEquals(14 * 60, periods[0].startMinute)
        assertEquals(14 * 60 + 45 + 5, periods[1].startMinute)
        assertEquals(periods[1].endMinute + 20, periods[2].startMinute)
    }

    @Test
    fun dayOffAddedLaterWinsOverDayChange() {
        val changed = childWithWednesday("Engleski", "Matematika").withDayChange(reference, 0, "Srpski jezik")
        val dayOff = CalendarEvent(reference.toEpochDay(), reference.toEpochDay(), "Dan škole")
        val withDayOff = changed.copy(customEvents = listOf(dayOff))

        assertEquals(emptyList<Lesson>(), withDayOff.lessonsOn(reference))
        assertEquals(null, withDayOff.classesEndOn(reference))
        // The change is kept, so it counts again once the day off is removed.
        assertEquals(1, withDayOff.overrides.size)
        assertEquals(listOf("Srpski jezik", "Matematika"), withDayOff.copy(customEvents = emptyList()).lessonsOn(reference).map { it.subject })
    }
}
