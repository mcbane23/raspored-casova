package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ExtraClassesTest {
    private val wednesday = LocalDate.of(2026, 10, 14)

    // Pre-class remedial Math at 07:10, then two regular classes, then advanced English in the 3rd period.
    private val timetable = Timetable.defaultFor(Shift.MORNING).withPreClass(true)
        .withLesson(DayOfWeek.WEDNESDAY, 0, "Dopunska: Matematika")
        .withLesson(DayOfWeek.WEDNESDAY, 1, "Srpski jezik")
        .withLesson(DayOfWeek.WEDNESDAY, 2, "Istorija")
        .withLesson(DayOfWeek.WEDNESDAY, 3, "Dodatna: Engleski jezik")

    private val child = Child(
        name = "Mila",
        shiftMode = ShiftMode.FIXED_MORNING,
        referenceEpochDay = wednesday.toEpochDay(),
        morning = timetable,
    )

    @Test
    fun asksAboutEveryOptionalClassUntilAnswered() {
        assertEquals(listOf(0, 3), child.extraClassesToAsk(wednesday).map { it.index })
        val going = child.withExtraClassAnswer(wednesday, 3, attends = true)
        assertEquals(listOf(0), going.extraClassesToAsk(wednesday).map { it.index })
        assertEquals(child.classesEndOn(wednesday), going.classesEndOn(wednesday))
    }

    @Test
    fun skippingMovesTheStartAndEndOfTheDay() {
        val skipped = child
            .withExtraClassAnswer(wednesday, 0, attends = false)
            .withExtraClassAnswer(wednesday, 3, attends = false)
        assertTrue(skipped.extraClassesToAsk(wednesday).isEmpty())
        assertEquals(timetable.periods[1].start, skipped.classesStartOn(wednesday))
        assertEquals(timetable.periods[2].end, skipped.classesEndOn(wednesday))
        // Other days keep their optional classes.
        assertEquals(listOf(0, 3), skipped.extraClassesToAsk(wednesday.plusWeeks(1)).map { it.index })
    }

    @Test
    fun changingTheAnswerToNotGoingCancelsTheClass() {
        val changed = child.withExtraClassAnswer(wednesday, 3, attends = true).withExtraClassAnswer(wednesday, 3, attends = false)
        assertTrue(changed.attendingExtra.isEmpty())
        assertEquals(timetable.periods[2].end, changed.classesEndOn(wednesday))
    }

    @Test
    fun endedClassesAreNotAskedAbout() {
        val afterPreClass = timetable.periods[0].end.plusMinutes(1)
        assertEquals(listOf(3), child.extraClassesToAsk(wednesday, afterPreClass).map { it.index })
        assertTrue(child.extraClassesToAsk(wednesday, LocalTime.of(23, 0)).isEmpty())
    }

    @Test
    fun pastAnswersAreDropped() {
        val going = child.withExtraClassAnswer(wednesday, 3, attends = true)
        assertTrue(going.withoutPastChanges(wednesday.plusDays(1)).attendingExtra.isEmpty())
        assertEquals(1, going.withoutPastChanges(wednesday).attendingExtra.size)
    }
}
