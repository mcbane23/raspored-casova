package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ActivitiesTest {
    private val wednesday = LocalDate.of(2026, 10, 14)

    // Football on Monday and Wednesday 17:30–19:00, reminder 30 minutes before.
    private val football = Extracurricular(
        id = "football",
        title = "Fudbal",
        kind = ActivityKind.SPORT,
        days = listOf(DayOfWeek.MONDAY.value, DayOfWeek.WEDNESDAY.value),
        startMinute = 17 * 60 + 30,
        endMinute = 19 * 60,
    )

    // Choir on Wednesday 16:00–17:00, without a reminder, paused during breaks.
    private val choir = Extracurricular(
        id = "choir",
        title = "Hor",
        kind = ActivityKind.MUSIC,
        days = listOf(DayOfWeek.WEDNESDAY.value),
        startMinute = 16 * 60,
        endMinute = 17 * 60,
        pausedOnBreaks = true,
        reminderEnabled = false,
    )

    // School camp from Monday 19 to Friday 23 October, leaving at 08:00 and back at 15:00.
    private val camp = Extracurricular(
        id = "camp",
        title = "Rekreativna nastava",
        kind = ActivityKind.CAMP,
        startEpochDay = LocalDate.of(2026, 10, 19).toEpochDay(),
        endEpochDay = LocalDate.of(2026, 10, 23).toEpochDay(),
        startMinute = 8 * 60,
        endMinute = 15 * 60,
        reminderMinutesBefore = 60,
    )

    private val child = Child(
        name = "Ana",
        referenceEpochDay = wednesday.toEpochDay(),
        activities = listOf(football, choir, camp),
    )

    @Test
    fun weeklyActivitiesShowOnTheirDaysEarliestFirst() {
        assertEquals(listOf(choir, football), child.activitiesOn(wednesday))
        assertEquals(listOf(football), child.activitiesOn(wednesday.minusDays(2)))
        assertEquals(emptyList<Extracurricular>(), child.activitiesOn(wednesday.plusDays(1)))
        assertEquals(listOf(choir, football), child.weeklyActivitiesOn(DayOfWeek.WEDNESDAY))
    }

    @Test
    fun pausedActivitySkipsSchoolBreaks() {
        // Autumn break is 11–13 Nov 2026: choir pauses, football goes on.
        val inBreak = LocalDate.of(2026, 11, 11)
        assertEquals(listOf(football), child.activitiesOn(inBreak))
    }

    @Test
    fun oneOffOverSeveralDaysShowsOnEveryDayButStartsOnce() {
        val monday = LocalDate.of(2026, 10, 19)
        assertTrue(child.hasActivityOn(camp, monday.plusDays(2)))
        assertFalse(child.hasActivityOn(camp, monday.plusDays(5)))
        assertTrue(child.activityStartsOn(camp, monday))
        assertFalse(child.activityStartsOn(camp, monday.plusDays(1)))
        // The camp continues from Monday, so on Wednesday it comes before the afternoon activities.
        assertEquals(listOf(camp, choir, football), child.activitiesOn(monday.plusDays(2)))
    }

    @Test
    fun nextReminderIsBeforeTheNextStartWithTheReminderOn() {
        // Choir has no reminder, so the next one is football at 17:00.
        assertEquals(wednesday.atTime(17, 0), child.nextActivityReminder(wednesday.atTime(12, 0)))
        // After that: camp on Monday 19 Oct at 07:00, before football's Monday reminder.
        assertEquals(LocalDate.of(2026, 10, 19).atTime(7, 0), child.nextActivityReminder(wednesday.atTime(17, 0)))
        assertNull(child.copy(activities = listOf(choir)).nextActivityReminder(wednesday.atTime(12, 0)))
    }

    @Test
    fun reminderCanFallOnTheDayBefore() {
        val night = football.copy(days = listOf(DayOfWeek.THURSDAY.value), startMinute = 30, endMinute = 90, reminderMinutesBefore = 60)
        val withNight = child.copy(activities = listOf(night))
        assertEquals(wednesday.atTime(23, 30), withNight.nextActivityReminder(wednesday.atTime(12, 0)))
    }

    @Test
    fun dueRemindersCoverALateAlarm() {
        val due = child.activityRemindersDue(wednesday.atTime(17, 0), wednesday.atTime(17, 14))
        assertEquals(listOf("football"), due.map { it.activity.id })
        assertEquals(wednesday.atTime(17, 30), due.single().start)
        assertEquals(emptyList<ActivityReminder>(), child.activityRemindersDue(wednesday.atTime(17, 1), wednesday.atTime(17, 14)))
    }

    @Test
    fun saveReplacesByIdAndPastOneOffsAreDropped() {
        val renamed = child.withActivity(football.copy(title = "Košarka"))
        assertEquals(3, renamed.activities.size)
        assertEquals("Košarka", renamed.activities.first { it.id == "football" }.title)
        assertEquals(4, child.withActivity(football.copy(id = "new")).activities.size)

        val afterCamp = child.withoutPastActivities(LocalDate.of(2026, 10, 24))
        assertEquals(listOf("football", "choir"), afterCamp.activities.map { it.id })
        assertEquals(3, child.withoutPastActivities(LocalDate.of(2026, 10, 23)).activities.size)
    }

    @Test
    fun oneOffWithoutClassesIsADayOff() {
        val ana = child.copy(shiftMode = ShiftMode.FIXED_MORNING, morning = Timetable.defaultFor(Shift.MORNING).withLesson(DayOfWeek.TUESDAY, 0, "Matematika"))
        val tuesday = LocalDate.of(2026, 10, 20)
        assertEquals(1, ana.lessonsOn(tuesday).size)

        val away = ana.withActivity(camp.copy(noClasses = true))
        assertEquals(DayInfo.NoClasses("Rekreativna nastava"), away.dayInfo(tuesday))
        assertEquals(emptyList<Lesson>(), away.lessonsOn(tuesday))
        // The camp is still listed as an activity, and the day after it classes are back.
        assertEquals(listOf("camp"), away.activitiesOn(tuesday).filter { !it.weekly }.map { it.id })
        assertEquals(1, away.lessonsOn(LocalDate.of(2026, 10, 27)).size)
    }
}
