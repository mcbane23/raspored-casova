package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.withDayChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MorningReminderTest {
    private val timetable = Timetable.defaultFor(Shift.MORNING)
        .withLesson(DayOfWeek.WEDNESDAY, 0, "Matematika")
        .withLesson(DayOfWeek.WEDNESDAY, 1, "Srpski jezik")

    private val wednesday = LocalDate.of(2026, 10, 14)

    private val child = Child(
        name = "Mila",
        shiftMode = ShiftMode.FIXED_MORNING,
        referenceEpochDay = wednesday.toEpochDay(),
        morning = timetable,
    )

    @Test
    fun namesTheFirstClassAndListsTheDay() {
        val content = morningReminderContent(child, wednesday)!!
        assertEquals("☀️ Mila, prvi čas u 08:00", content.title)
        assertEquals("Da li je neki čas pomeren ili otkazan? Proveri plan za danas.", content.text)
        assertEquals(
            "Da li je neki čas pomeren ili otkazan? Proveri plan za danas.\n• 08:00 Matematika\n• 08:50 Srpski jezik",
            content.bigText,
        )
    }

    @Test
    fun saysWhenTheDayWasAlreadyChanged() {
        val content = morningReminderContent(child.withDayChange(wednesday, 0, ""), wednesday)!!
        assertEquals("☀️ Mila, prvi čas u 08:50", content.title)
        assertEquals("Raspored je danas izmenjen. Proveri da li je sve tačno.", content.text)
    }

    @Test
    fun nothingOnDaysWithoutClasses() {
        assertNull(morningReminderContent(child, wednesday.plusDays(1)))
        assertNull(morningReminderContent(child, LocalDate.of(2026, 10, 17)))
    }
}
