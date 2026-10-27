package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class TimetableSnapshotTest {
    private val child = Child(
        id = "ana",
        name = "Ana",
        referenceEpochDay = 0,
        morning = Timetable.defaultFor(Shift.MORNING)
            .withLesson(DayOfWeek.MONDAY, 0, "Srpski jezik")
            .withLesson(DayOfWeek.MONDAY, 1, "Matematika"),
    )
    private val snapshot = TimetableSnapshot.of(listOf(child))

    private fun Child.edited(transform: (Timetable) -> Timetable) = withTimetable(Shift.MORNING, transform(morning))

    @Test
    fun noChangesRightAfterEditingStarts() {
        assertFalse(snapshot.hasChanges(listOf(child)))
    }

    @Test
    fun changedSubjectIsAChangeAndDiscardBringsItBack() {
        val edited = child.edited { it.withLesson(DayOfWeek.MONDAY, 1, "Engleski jezik") }

        assertTrue(snapshot.hasChanges(listOf(edited)))
        val (id, shift, restored) = snapshot.changes(listOf(edited)).single()
        assertEquals("ana", id)
        assertEquals(Shift.MORNING, shift)
        assertEquals(child.morning, restored)
    }

    @Test
    fun changingBackByHandIsNoChange() {
        val edited = child
            .edited { it.withLesson(DayOfWeek.MONDAY, 1, "Engleski jezik") }
            .edited { it.withLesson(DayOfWeek.MONDAY, 1, "Matematika") }

        assertFalse(snapshot.hasChanges(listOf(edited)))
    }

    @Test
    fun bellChangesAloneAreNoChangeAndStayWhenDiscarding() {
        val earlier = child.edited { it.withPeriodStart(0, 7 * 60 + 45)!! }
        assertFalse(snapshot.hasChanges(listOf(earlier)))

        val edited = earlier.edited { it.withLesson(DayOfWeek.FRIDAY, 0, "Fizičko") }
        val restored = snapshot.changes(listOf(edited)).single().third
        assertEquals(earlier.morning.periods, restored.periods)
        assertEquals(child.morning.lessons, restored.lessons)
    }

    @Test
    fun preClassAddedOnTheBellsPageKeepsSubjectsInPlace() {
        val withPre = child.edited { it.withPreClass(true) }
        assertFalse(snapshot.hasChanges(listOf(withPre)))

        val edited = withPre.edited { it.withLesson(DayOfWeek.MONDAY, 1, "Likovno") }
        val restored = snapshot.changes(listOf(edited)).single().third
        assertEquals(listOf("", "Srpski jezik", "Matematika"), restored.lessonsFor(DayOfWeek.MONDAY))
        assertTrue(restored.hasPreClass)
    }

    @Test
    fun survivesEncoding() {
        assertEquals(snapshot, TimetableSnapshot.decode(snapshot.encode()))
    }
}
