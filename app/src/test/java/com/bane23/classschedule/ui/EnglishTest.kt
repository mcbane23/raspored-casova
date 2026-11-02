package com.bane23.classschedule.ui

import com.bane23.classschedule.data.AppLanguage
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DayInfo
import com.bane23.classschedule.data.Lang
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.calendarEvents
import com.bane23.classschedule.data.dayInfo
import com.bane23.classschedule.data.lessonLabel
import com.bane23.classschedule.data.packNote
import com.bane23.classschedule.data.subjectLabel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** The English texts; saved data (subjects, lessons) stays Serbian and is only translated on screen. */
class EnglishTest {
    private val tuesday = LocalDate.of(2026, 10, 13)

    @Before
    fun english() {
        Lang.current = AppLanguage.ENGLISH
    }

    @After
    fun serbian() {
        Lang.current = AppLanguage.SERBIAN
    }

    @Test
    fun datesAndDays() {
        assertEquals("Wednesday, October 14", tuesday.plusDays(1).label())
        assertEquals("Monday", DayOfWeek.MONDAY.accusative())
        assertEquals("every Wednesday", DayOfWeek.WEDNESDAY.every())
        assertEquals("November 13", LocalDate.of(2026, 11, 13).genitiveLabel())
        assertEquals("November 11–13", dateRangeLabel(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 13)))
        assertEquals("December 31 – January 15", dateRangeLabel(LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 15)))
        assertEquals("October 2026", tuesday.monthYearLabel())
        assertEquals("Oct", tuesday.monthShortLabel())
        assertEquals("Wednesday", "Wednesday".inSentence())
    }

    @Test
    fun counts() {
        assertEquals("1 class", lessonsCount(1))
        assertEquals("5 classes", lessonsCount(5))
        assertEquals("21 subjects", subjectsCount(21))
        assertEquals("1 day", daysCount(1))
        assertEquals("in 21 days", inDaysLabel(21))
        assertEquals("Class 1", Timetable.defaultFor(Shift.MORNING).periodLabel(0))
    }

    @Test
    fun shiftSummary() {
        val child = Child(name = "Mila", shiftMode = ShiftMode.FIXED_AFTERNOON, referenceEpochDay = tuesday.toEpochDay())
        assertEquals("always afternoons", shiftSummary(child, tuesday))
    }

    @Test
    fun builtInSubjectsAreTranslatedOnScreenOnly() {
        assertEquals("Mathematics", subjectLabel("Matematika"))
        assertEquals("Remedial: Mathematics", lessonLabel("Dopunska: Matematika"))
        assertEquals("Chess club", subjectLabel("Chess club"))
        assertEquals("Math", shortName("Matematika"))
        assertEquals("PE", shortName("Fizičko i zdravstveno vaspitanje"))
        val child = Child(name = "Mila", referenceEpochDay = tuesday.toEpochDay())
        assertEquals("sneakers and sports gear", child.packNote("Fizičko i zdravstveno vaspitanje"))
    }

    @Test
    fun officialCalendarInEnglish() {
        val child = Child(name = "Mila", referenceEpochDay = tuesday.toEpochDay(), region = Region.CENTRAL_SERBIA)
        assertEquals(DayInfo.NoClasses("Autumn break"), child.dayInfo(LocalDate.of(2026, 11, 12)))
        assertEquals(DayInfo.NoClasses("Weekend"), child.dayInfo(LocalDate.of(2026, 10, 17)))
        assertEquals("Winter break", child.calendarEvents().first { it.id == "2627-winter" }.title)
        Lang.current = AppLanguage.SERBIAN
        assertEquals("Zimski raspust", child.calendarEvents().first { it.id == "2627-winter" }.title)
    }

    @Test
    fun languageCodes() {
        assertEquals(AppLanguage.SERBIAN, AppLanguage.fromCode("sr-Latn-RS"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en-US"))
        assertEquals(null, AppLanguage.fromCode("de"))
    }
}
