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

    @Test
    fun cancelledSessionIsSkippedOnlyThatWeek() {
        val cancelled = football.withChange(wednesday, SessionChange(wednesday.toEpochDay()))
        val ana = child.copy(activities = listOf(cancelled, choir))
        assertEquals(listOf(choir), ana.activitiesOn(wednesday))
        assertEquals(SessionState.CANCELLED, ana.sessionsOn(wednesday).single { it.activity.id == "football" }.state)
        // The next Monday and Wednesday football is back.
        assertEquals(listOf(cancelled), ana.activitiesOn(wednesday.plusDays(5)))
        assertEquals(listOf(choir, cancelled), ana.activitiesOn(wednesday.plusDays(7)))
        // No reminder for the cancelled session: the next one is Monday 19 Oct at 17:00.
        assertEquals(LocalDate.of(2026, 10, 19).atTime(17, 0), ana.nextActivityReminder(wednesday.atTime(12, 0)))
    }

    @Test
    fun movedSessionShowsOnTheNewDayWithItsTimeAndReminder() {
        // Wednesday's football moved to Thursday 18:00–19:30.
        val thursday = wednesday.plusDays(1)
        val moved = football.withChange(wednesday, SessionChange(wednesday.toEpochDay(), thursday.toEpochDay(), 18 * 60, 19 * 60 + 30))
        val ana = child.copy(activities = listOf(moved))
        assertEquals(emptyList<Extracurricular>(), ana.activitiesOn(wednesday))
        assertEquals(SessionState.MOVED_AWAY, ana.sessionsOn(wednesday).single().state)
        val session = ana.sessionsOn(thursday).single()
        assertEquals(SessionState.MOVED_HERE, session.state)
        assertEquals(wednesday, session.regularDay)
        assertEquals(18 * 60, ana.activitiesOn(thursday).single().startMinute)
        assertEquals(thursday.atTime(17, 30), ana.nextActivityReminder(wednesday.atTime(12, 0)))
        // The regular schedule is unchanged, and the week after it is Wednesday again.
        assertEquals(17 * 60 + 30, ana.activities.single().startMinute)
        assertEquals(17 * 60 + 30, ana.activitiesOn(wednesday.plusDays(7)).single().startMinute)
        assertEquals(emptyList<Extracurricular>(), ana.activitiesOn(thursday.plusDays(7)))
    }

    @Test
    fun sessionMovedToAnotherTimeOnTheSameDayShowsOnce() {
        val later = football.withChange(wednesday, SessionChange(wednesday.toEpochDay(), wednesday.toEpochDay(), 19 * 60, 20 * 60))
        val ana = child.copy(activities = listOf(later))
        val sessions = ana.sessionsOn(wednesday)
        assertEquals(listOf(SessionState.MOVED_HERE), sessions.map { it.state })
        assertEquals(wednesday.atTime(18, 30), ana.nextActivityReminder(wednesday.atTime(12, 0)))
    }

    @Test
    fun sessionMovedOntoARegularDayGivesTwoSessions() {
        // Monday 19 Oct moved to Wednesday 21 Oct at 10:00, which already has its own football at 17:30.
        val monday = LocalDate.of(2026, 10, 19)
        val nextWednesday = monday.plusDays(2)
        val moved = football.withChange(monday, SessionChange(monday.toEpochDay(), nextWednesday.toEpochDay(), 10 * 60, 11 * 60))
        val ana = child.copy(activities = listOf(moved))
        assertEquals(listOf(10 * 60, 17 * 60 + 30), ana.activitiesOn(nextWednesday).map { it.startMinute })
        val due = ana.activityRemindersDue(nextWednesday.atTime(0, 0), nextWednesday.atTime(23, 59))
        assertEquals(listOf(nextWednesday.atTime(10, 0), nextWednesday.atTime(17, 30)), due.map { it.start })
    }

    @Test
    fun restoringASessionAndDroppingPastChanges() {
        val change = SessionChange(wednesday.toEpochDay())
        val cancelled = football.withChange(wednesday, change)
        assertEquals(football, cancelled.withChange(wednesday, null))
        // A second change of the same session replaces the first.
        assertEquals(1, cancelled.withChange(wednesday, change.copy(movedToEpochDay = wednesday.plusDays(1).toEpochDay())).changes.size)

        val ana = child.copy(activities = listOf(cancelled))
        assertEquals(listOf(change), ana.withoutPastActivities(wednesday).activities.single().changes)
        assertEquals(emptyList<SessionChange>(), ana.withoutPastActivities(wednesday.plusDays(1)).activities.single().changes)
    }

    @Test
    fun upcomingRegularDaysSkipBreaksForPausedActivities() {
        assertEquals(
            listOf(wednesday, wednesday.plusDays(5), wednesday.plusDays(7)),
            child.upcomingRegularDays(football, wednesday, 3),
        )
        // Choir pauses in the autumn break (11–13 Nov 2026), so Wednesday 11 Nov is skipped.
        assertEquals(LocalDate.of(2026, 11, 18), child.upcomingRegularDays(choir, LocalDate.of(2026, 11, 5), 2).first())
        assertEquals(emptyList<LocalDate>(), child.upcomingRegularDays(camp, wednesday))
    }
}
