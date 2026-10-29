package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class FormatTest {
    private val thursday = LocalDate.of(2026, 10, 29)

    @Test
    fun accusativeAfterZa() {
        assertEquals("ponedeljak", DayOfWeek.MONDAY.accusative())
        assertEquals("sredu", DayOfWeek.WEDNESDAY.accusative())
        assertEquals("subotu", DayOfWeek.SATURDAY.accusative())
        assertEquals("nedelju", DayOfWeek.SUNDAY.accusative())
    }

    @Test
    fun spokenDropsEmoji() {
        assertEquals("Bravo!", spoken("🎉 Bravo!"))
        assertEquals("Zatim Srpski u 09:35", spoken("Zatim 📖 Srpski u 09:35"))
        assertEquals("Engleski", spoken("🇬🇧 Engleski"))
        assertEquals("Pre podne", spoken("☀️ Pre podne"))
    }

    @Test
    fun packHintNamesTheNextSchoolDay() {
        assertEquals(" Sutra spakuj torbu.", packHint(thursday, thursday.plusDays(1)))
        assertEquals(" Za ponedeljak spakuj torbu.", packHint(thursday.plusDays(1), thursday.plusDays(4)))
        // The last day before a break, or no school day ahead.
        assertEquals("", packHint(thursday, thursday.plusDays(11)))
        assertEquals("", packHint(thursday, null))
    }

    @Test
    fun datesInTheRightCase() {
        assertEquals("sredu, 28. oktobar", LocalDate.of(2026, 10, 28).accusativeLabel())
        assertEquals("13. novembra", LocalDate.of(2026, 11, 13).genitiveLabel())
        assertEquals("15. januara", LocalDate.of(2027, 1, 15).genitiveLabel())
        assertEquals("svaku sredu", DayOfWeek.WEDNESDAY.every())
        assertEquals("svaki ponedeljak", DayOfWeek.MONDAY.every())
    }

    @Test
    fun subjectPlural() {
        assertEquals("1 predmet", subjectsCount(1))
        assertEquals("19 predmeta", subjectsCount(19))
        assertEquals("21 predmet", subjectsCount(21))
        assertEquals("11 predmeta", subjectsCount(11))
    }

    @Test
    fun shiftSummaryNamesTheComingWeekOnAWeekend() {
        // Week of Monday 26 October 2026 is the morning shift.
        val weekly = Child(name = "Ana", referenceEpochDay = LocalDate.of(2026, 10, 26).toEpochDay(), referenceShift = Shift.MORNING)
        assertEquals("ove nedelje pre podne", shiftSummary(weekly, thursday))
        assertEquals("od ponedeljka popodne", shiftSummary(weekly, LocalDate.of(2026, 10, 31)))
        assertEquals("od ponedeljka popodne", shiftSummary(weekly, LocalDate.of(2026, 11, 1)))
        assertEquals("ove nedelje popodne", shiftSummary(weekly, LocalDate.of(2026, 11, 2)))

        val monthly = weekly.copy(shiftMode = ShiftMode.MONTHLY)
        assertEquals("ovog meseca pre podne", shiftSummary(monthly, LocalDate.of(2026, 10, 31)))
        assertEquals("uvek popodne", shiftSummary(weekly.copy(shiftMode = ShiftMode.FIXED_AFTERNOON), thursday))
    }
}
