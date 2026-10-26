package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class BellEditTest {
    private val morning = Timetable.defaultFor(Shift.MORNING)

    @Test
    fun movingAStartMovesTheLaterClassesToo() {
        val moved = morning.withPeriodStart(0, 7 * 60 + 30)!!
        assertEquals(Period(450, 495), moved.periods[0])
        // 2nd class keeps its 5-minute break, the big break after it stays 20 minutes.
        assertEquals(Period(500, 545), moved.periods[1])
        assertEquals(morning.periods[6].startMinute - 30, moved.periods[6].startMinute)
    }

    @Test
    fun movingTheThirdClassLeavesTheFirstTwo() {
        val moved = morning.withPeriodStart(2, morning.periods[2].startMinute + 10)!!
        assertEquals(morning.periods.take(2), moved.periods.take(2))
        assertEquals(morning.periods[6].startMinute + 10, moved.periods[6].startMinute)
    }

    @Test
    fun aStartBeforeThePreviousEndIsRefused() {
        assertNull(morning.withPeriodStart(1, morning.periods[0].endMinute - 1))
    }

    @Test
    fun thePreClassMovesAlone() {
        val withPre = morning.withPreClass(true)
        val moved = withPre.withPeriodStart(0, withPre.periods[0].startMinute - 15)!!
        assertEquals(withPre.periods.drop(1), moved.periods.drop(1))
        // ...and may not run into the 1st class.
        assertNull(withPre.withPeriodStart(0, withPre.periods[1].startMinute - 10))
    }

    @Test
    fun anEndMustBeAfterTheStartAndBeforeTheNextClass() {
        assertNull(morning.withPeriodEnd(0, morning.periods[0].startMinute))
        assertNull(morning.withPeriodEnd(0, morning.periods[1].startMinute + 1))
        assertEquals(Period(480, 520), morning.withPeriodEnd(0, 520)!!.periods[0])
    }

    @Test
    fun classesMayNotRunPastMidnight() {
        val afternoon = Timetable.defaultFor(Shift.AFTERNOON)
        assertNull(afternoon.withPeriodStart(0, 23 * 60))
    }

    @Test
    fun removingTheLastPeriodRemovesItsClasses() {
        val full = morning.withLesson(DayOfWeek.MONDAY, 6, "Informatika").withLesson(DayOfWeek.MONDAY, 0, "Srpski jezik")
        val trimmed = full.withoutLastPeriod()
        assertEquals(6, trimmed.periods.size)
        assertEquals(listOf("Srpski jezik"), trimmed.lessonsFor(DayOfWeek.MONDAY))
    }

    @Test
    fun defaultBellsKeepThePreClassAndTheNumberOfClasses() {
        val custom = morning.withPreClass(true)
            .withLesson(DayOfWeek.MONDAY, 0, "Engleski jezik")
            .copy(periods = morning.withPreClass(true).periods + Period(15 * 60, 15 * 60 + 45))
            .withPeriodStart(1, 8 * 60 + 30)!!
        val reset = custom.withDefaultBells(Shift.MORNING)
        assertEquals(custom.periods.size, reset.periods.size)
        assertEquals(listOf("Engleski jezik"), reset.lessonsFor(DayOfWeek.MONDAY))
        assertEquals(8 * 60, reset.periods[1].startMinute)
        assertEquals(Period(8 * 60 - 50, 8 * 60 - 5), reset.periods[0])
        assertEquals(true, reset.bellsInOrder())
    }

    @Test
    fun lastClassEndsAtTheLatestBell() {
        val child = Child(
            name = "Ana",
            shiftMode = ShiftMode.FIXED_MORNING,
            referenceEpochDay = 0,
            morning = morning.withLesson(DayOfWeek.MONDAY, 0, "A").withLesson(DayOfWeek.MONDAY, 1, "B")
                .copy(periods = listOf(Period(600, 645), Period(480, 525)) + morning.periods.drop(2)),
        )
        assertEquals(java.time.LocalTime.of(10, 45), child.classesEndOn(LocalDate.of(2026, 10, 26)))
    }

    @Test
    fun oneDayChangesFollowThePreClass() {
        val monday = LocalDate.of(2026, 10, 26)
        val child = Child(
            name = "Ana",
            shiftMode = ShiftMode.FIXED_MORNING,
            referenceEpochDay = monday.toEpochDay(),
            morning = morning.withLesson(DayOfWeek.MONDAY, 0, "Srpski jezik").withLesson(DayOfWeek.MONDAY, 1, "Matematika"),
        ).withDayChange(monday, 0, "")
        val withPre = child.withTimetableKeepingChanges(Shift.MORNING, child.morning.withPreClass(true))
        assertEquals(listOf("Matematika"), withPre.lessonsOn(monday).map { it.subject })
        assertEquals(2, withPre.lessonsOn(monday).single().index)
        val back = withPre.withTimetableKeepingChanges(Shift.MORNING, withPre.morning.withPreClass(false))
        assertEquals(child.overrides, back.overrides)
    }

    @Test
    fun onAWeekendTheShiftIsSetForTheComingWeek() {
        val sunday = LocalDate.of(2026, 11, 1)
        assertEquals(LocalDate.of(2026, 11, 2), sunday.upcomingSchoolWeekDay())
        assertEquals(LocalDate.of(2026, 11, 2), sunday.minusDays(1).upcomingSchoolWeekDay())
        assertEquals(sunday.minusDays(2), sunday.minusDays(2).upcomingSchoolWeekDay())
        val monthly = Child(name = "Ana", shiftMode = ShiftMode.MONTHLY, referenceEpochDay = 0)
        assertEquals(sunday, monthly.currentShiftDate(sunday))
    }

    @Test
    fun bellsAlreadyOutOfOrderCanBeFixedOneAtATime() {
        // Saved by an older version that did not check neighbours: the 1st class runs into the 2nd.
        val broken = morning.copy(periods = listOf(Period(510, 555)) + morning.periods.drop(1))
        val fixed = broken.withPeriodStart(0, 8 * 60)!!
        assertEquals(true, fixed.bellsInOrder())
        assertEquals(Period(530, 560), broken.withPeriodEnd(1, 560)!!.periods[1])
    }
}
