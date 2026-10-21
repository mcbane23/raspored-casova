package com.bane23.classschedule.ui

import com.bane23.classschedule.data.ActivityKind
import com.bane23.classschedule.data.ActivityReminder
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Extracurricular
import com.bane23.classschedule.data.SessionChange
import com.bane23.classschedule.data.sessionsOn
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ActivityFormatTest {
    private val wednesday = LocalDate.of(2026, 10, 14)
    private val child = Child(name = "Ana", referenceEpochDay = wednesday.toEpochDay())

    private val football = Extracurricular(
        title = "Fudbal",
        kind = ActivityKind.SPORT,
        days = listOf(DayOfWeek.MONDAY.value, DayOfWeek.WEDNESDAY.value),
        startMinute = 17 * 60 + 30,
        endMinute = 19 * 60,
        place = "Sportska hala",
    )

    private val camp = Extracurricular(
        title = "Rekreativna nastava",
        kind = ActivityKind.CAMP,
        startEpochDay = LocalDate.of(2026, 10, 19).toEpochDay(),
        endEpochDay = LocalDate.of(2026, 10, 23).toEpochDay(),
        startMinute = 8 * 60,
        endMinute = 15 * 60,
    )

    @Test
    fun scheduleLabels() {
        assertEquals("Pon, Sre · 17:30–19:00", football.scheduleLabel())
        assertEquals("19–23. oktobar · od 08:00 do 15:00", camp.scheduleLabel())
        val trip = camp.copy(endEpochDay = camp.startEpochDay)
        assertEquals("Ponedeljak, 19. oktobar · 08:00–15:00", trip.scheduleLabel())
    }

    @Test
    fun timeLabelFollowsTheDayOfAMultiDayActivity() {
        val monday = LocalDate.of(2026, 10, 19)
        assertEquals("17:30–19:00", football.timeLabelOn(wednesday))
        assertEquals("od 08:00", camp.timeLabelOn(monday))
        assertEquals("ceo dan", camp.timeLabelOn(monday.plusDays(2)))
        assertEquals("do 15:00", camp.timeLabelOn(monday.plusDays(4)))
    }

    @Test
    fun reminderNotificationText() {
        val start = wednesday.atTime(17, 30)
        val reminder = ActivityReminder(football, start, start.minusMinutes(30))
        val content = activityReminderContent(child, reminder, start.minusMinutes(30))
        assertEquals("⚽ Fudbal u 17:30", content.title)
        assertEquals("Ana · za 30 min · Sportska hala", content.text)

        val early = activityReminderContent(child, ActivityReminder(football, start, start.minusHours(20)), start.minusHours(20))
        assertEquals("⚽ Fudbal sutra u 17:30", early.title)
        assertEquals("Ana · za 20 h · Sportska hala", early.text)
    }

    @Test
    fun reminderOffsets() {
        assertEquals("30 min pre", reminderOffsetLabel(30))
        assertEquals("1 h pre", reminderOffsetLabel(60))
        assertEquals("2 h pre", reminderOffsetLabel(120))
    }

    @Test
    fun changedSessionLabels() {
        val thursday = wednesday.plusDays(1)
        assertEquals("otkazano", SessionChange(wednesday.toEpochDay()).label())
        assertEquals(
            "pomereno na četvrtak, 15. oktobar u 18:00",
            SessionChange(wednesday.toEpochDay(), thursday.toEpochDay(), 18 * 60, 19 * 60).label(),
        )
        assertEquals("pomereno na 19:00", SessionChange(wednesday.toEpochDay(), wednesday.toEpochDay(), 19 * 60, 20 * 60).label())

        val moved = football.withChange(wednesday, SessionChange(wednesday.toEpochDay(), thursday.toEpochDay(), 18 * 60, 19 * 60))
        val ana = child.copy(activities = listOf(moved))
        val away = ana.sessionsOn(wednesday).single()
        assertEquals("Pomereno", away.tag())
        assertEquals("pomereno na četvrtak, 15. oktobar u 18:00", away.detail())
        val here = ana.sessionsOn(thursday).single()
        assertEquals("redovno: sreda, 14. oktobar", here.detail())
        assertEquals("18:00–19:00", here.activity.timeLabelOn(thursday))
        assertEquals(null, ana.copy(activities = listOf(football)).sessionsOn(wednesday).single().tag())
    }

    @Test
    fun dramaHasItsOwnKind() {
        assertEquals("🎭 Gluma", "${ActivityKind.DRAMA.emoji()} ${ActivityKind.DRAMA.label()}")
        assertEquals(false, ActivityKind.DRAMA.usuallyOnce())
    }
}
