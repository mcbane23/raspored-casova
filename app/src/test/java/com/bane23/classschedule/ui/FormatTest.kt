package com.bane23.classschedule.ui

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
}
