package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PackingListTest {
    private val timetable = Timetable.defaultFor(Shift.MORNING)
        .withLesson(DayOfWeek.WEDNESDAY, 0, "Matematika")
        .withLesson(DayOfWeek.WEDNESDAY, 1, "Fizičko i zdravstveno vaspitanje")
        .withLesson(DayOfWeek.WEDNESDAY, 2, "Dopunska: Matematika")
        .withLesson(DayOfWeek.MONDAY, 0, "Srpski jezik")

    private val tuesday = LocalDate.of(2026, 10, 13)
    private val wednesday = tuesday.plusDays(1)

    private val child = Child(
        name = "Mila",
        shiftMode = ShiftMode.FIXED_MORNING,
        referenceEpochDay = tuesday.toEpochDay(),
        morning = timetable,
    )

    @Test
    fun subjectsAreListedOnceWithoutOptionalPrefix() {
        assertEquals(listOf("Matematika", "Fizičko i zdravstveno vaspitanje"), child.packSubjects(wednesday))
    }

    @Test
    fun nextSchoolDaySkipsEmptyDaysAndWeekend() {
        assertEquals(wednesday, child.nextSchoolDayAfter(tuesday))
        // Thursday and Friday have no classes here, so after Wednesday comes Monday.
        assertEquals(LocalDate.of(2026, 10, 19), child.nextSchoolDayAfter(wednesday))
    }

    @Test
    fun parentNoteWinsOverSuggestion() {
        assertEquals("patike i sportska oprema", child.packNote("Fizičko i zdravstveno vaspitanje"))
        val custom = child.withPackNote("Fizičko i zdravstveno vaspitanje", "patike")
        assertEquals("patike", custom.packNote("Fizičko i zdravstveno vaspitanje"))
        assertEquals("", custom.withPackNote("Matematika", " ").packNote("Matematika"))
    }

    @Test
    fun ticksBelongToOneDay() {
        val packed = child.withPacked(wednesday, "Matematika", true)
        assertTrue(packed.isPacked(wednesday, "Matematika"))
        assertFalse(packed.isPacked(wednesday.plusDays(5), "Matematika"))

        val nextWeek = packed.withPacked(wednesday.plusDays(7), "Srpski jezik", true)
        assertEquals(listOf("Srpski jezik"), nextWeek.packed)
    }
}
