package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class SchoolCalendarTest {
    private val timetable = Timetable.defaultFor(Shift.MORNING)
        .withLesson(DayOfWeek.TUESDAY, 0, "Matematika")
        .withLesson(DayOfWeek.FRIDAY, 0, "Likovno")
        .withLesson(DayOfWeek.MONDAY, 0, "Srpski jezik")
        .withLesson(DayOfWeek.WEDNESDAY, 0, "Engleski")

    private val child = Child(
        name = "Test",
        shiftMode = ShiftMode.FIXED_MORNING,
        referenceEpochDay = LocalDate.of(2026, 9, 1).toEpochDay(),
        morning = timetable,
    )

    private fun subjects(child: Child, date: LocalDate) = child.lessonsOn(date).map { it.subject }

    @Test
    fun breaksAndHolidaysHaveNoClasses() {
        assertEquals(DayInfo.NoClasses("Jesenji raspust"), child.dayInfo(LocalDate.of(2026, 11, 11)))
        assertEquals(DayInfo.NoClasses("Zimski raspust"), child.dayInfo(LocalDate.of(2027, 1, 11)))
        assertEquals(DayInfo.NoClasses("Savindan, nema nastave"), child.dayInfo(LocalDate.of(2027, 1, 27)))
        assertEquals(emptyList<String>(), subjects(child, LocalDate.of(2027, 5, 4)))
        assertEquals(DayInfo.NoClasses("Vikend"), child.dayInfo(LocalDate.of(2026, 10, 17)))
    }

    @Test
    fun regularSchoolDayUsesItsOwnTimetable() {
        assertEquals(listOf("Engleski"), subjects(child, LocalDate.of(2026, 10, 14)))
    }

    @Test
    fun swappedDayUsesFridayTimetable() {
        val tuesday = LocalDate.of(2026, 11, 10)
        assertEquals(DayInfo.SchoolDay(DayOfWeek.FRIDAY, "Nastava po rasporedu za petak"), child.dayInfo(tuesday))
        assertEquals(listOf("Likovno"), subjects(child, tuesday))
    }

    @Test
    fun vojvodinaHasEarlierWinterBreak() {
        val vojvodina = child.copy(region = Region.VOJVODINA)
        assertEquals(DayInfo.NoClasses("Zimski raspust"), vojvodina.dayInfo(LocalDate.of(2026, 12, 24)))
        assertEquals(listOf("Srpski jezik"), subjects(vojvodina, LocalDate.of(2027, 1, 11)))
        assertTrue(child.dayInfo(LocalDate.of(2026, 12, 24)) is DayInfo.SchoolDay)
    }

    @Test
    fun finalGradeEndsClassesEarlier() {
        val lastFriday = LocalDate.of(2027, 6, 4)
        assertEquals(listOf("Likovno"), subjects(child, lastFriday))
        assertEquals(emptyList<String>(), subjects(child.copy(finalGrade = true), lastFriday))
        assertTrue(child.dayInfo(LocalDate.of(2027, 6, 14)) is DayInfo.NoClasses)
    }

    @Test
    fun customEventAddsDayOff() {
        val slava = LocalDate.of(2026, 10, 20)
        val withSlava = child.copy(
            customEvents = listOf(CalendarEvent(slava.toEpochDay(), slava.toEpochDay(), "Školska slava")),
        )
        assertEquals(listOf("Matematika"), subjects(child, slava))
        assertEquals(DayInfo.NoClasses("Školska slava"), withSlava.dayInfo(slava))
    }

    @Test
    fun nextDayOffSkipsPastEvents() {
        assertEquals("Jesenji raspust", child.nextDayOff(LocalDate.of(2026, 10, 13))?.title)
        assertEquals("Jesenji raspust", child.nextDayOff(LocalDate.of(2026, 11, 13))?.title)
        assertEquals("Zimski raspust", child.nextDayOff(LocalDate.of(2026, 11, 14))?.title)
    }

    @Test
    fun oneDayChangeStillWorksOnSwappedDay() {
        val tuesday = LocalDate.of(2026, 11, 10)
        val changed = child.withDayChange(tuesday, 0, "Muzičko")
        assertEquals(listOf("Muzičko"), subjects(changed, tuesday))
    }
}
