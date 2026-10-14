package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.withPacked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PackReminderTest {
    private val timetable = Timetable.defaultFor(Shift.MORNING)
        .withLesson(DayOfWeek.WEDNESDAY, 0, "Matematika")
        .withLesson(DayOfWeek.WEDNESDAY, 1, "Fizičko i zdravstveno vaspitanje")

    private val tuesday = LocalDate.of(2026, 10, 13)
    private val wednesday = tuesday.plusDays(1)

    private val child = Child(
        name = "Mila",
        shiftMode = ShiftMode.FIXED_MORNING,
        referenceEpochDay = tuesday.toEpochDay(),
        morning = timetable,
    )

    @Test
    fun listsTomorrowsSubjectsWithWhatToBring() {
        val content = packReminderContent(child, tuesday)!!
        assertEquals("🎒 Mila, spakuj torbu", content.title)
        assertEquals("Za sutra: Matematika, Fizičko i zdravstveno vaspitanje", content.text)
        assertEquals(
            "Za sutra:\n• Matematika\n• Fizičko i zdravstveno vaspitanje: patike i sportska oprema",
            content.bigText,
        )
    }

    @Test
    fun leavesOutWhatIsAlreadyPacked() {
        val packed = child.withPacked(wednesday, "Matematika", true)
        assertEquals("Za sutra: Fizičko i zdravstveno vaspitanje", packReminderContent(packed, tuesday)!!.text)
        assertNull(packReminderContent(packed.withPacked(wednesday, "Fizičko i zdravstveno vaspitanje", true), tuesday))
    }
}
