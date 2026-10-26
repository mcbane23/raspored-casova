package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ShareCodecTest {
    private val child = Child(
        name = "Ana",
        school = "OŠ Vuk Karadžić, 3-2",
        shiftMode = ShiftMode.WEEKLY,
        referenceEpochDay = LocalDate.of(2026, 10, 12).toEpochDay(),
        referenceShift = Shift.AFTERNOON,
        morning = Timetable.defaultFor(Shift.MORNING)
            .withLesson(DayOfWeek.TUESDAY, 0, "Srpski jezik")
            .withLesson(DayOfWeek.TUESDAY, 5, ExtraClass.REMEDIAL.lesson("Srpski jezik")),
        afternoon = Timetable.defaultFor(Shift.AFTERNOON).withPreClass(true)
            .withLesson(DayOfWeek.TUESDAY, 6, ExtraClass.REMEDIAL.lesson("Matematika")),
        overrides = listOf(DayOverride(LocalDate.of(2026, 10, 14).toEpochDay(), listOf("Likovna kultura"))),
        region = Region.VOJVODINA,
        customEvents = listOf(CalendarEvent(1, 1, "Dan škole", id = "e1")),
        alarmEnabled = true,
        alarmSilent = true,
        activities = listOf(
            Extracurricular(
                id = "a1",
                title = "Folklor",
                kind = ActivityKind.DANCE,
                days = listOf(2, 4),
                startMinute = 18 * 60,
                endMinute = 19 * 60 + 30,
                place = "Dom kulture",
            ),
        ),
        activityReminderSound = true,
    )

    @Test
    fun roundTripKeepsTimetableAndDropsPrivateSettings() {
        val decoded = ShareCodec.decode(ShareCodec.encode(child))!!

        assertEquals(child.name, decoded.name)
        assertEquals(child.school, decoded.school)
        assertEquals(child.morning, decoded.morning)
        assertEquals(child.afternoon, decoded.afternoon)
        assertEquals(child.shiftMode, decoded.shiftMode)
        assertEquals(child.referenceShift, decoded.referenceShift)
        assertEquals(child.referenceEpochDay, decoded.referenceEpochDay)
        assertEquals(child.region, decoded.region)
        assertEquals(child.customEvents, decoded.customEvents)
        assertEquals(child.subjects, decoded.subjects)
        assertEquals(emptyList<DayOverride>(), decoded.overrides)
        assertEquals(false, decoded.alarmEnabled)
        assertEquals(false, decoded.alarmSilent)
        assertNotEquals(child.id, decoded.id)
    }

    @Test
    fun activitiesAreSharedWithRemindersOff() {
        val decoded = ShareCodec.decode(ShareCodec.encode(child))!!

        assertEquals(child.activities.map { it.copy(reminderEnabled = false) }, decoded.activities)
        assertEquals(false, decoded.activityReminderSound)
    }

    @Test
    fun codeIsSmallEnoughForQr() {
        // A version 25 QR code at low error correction holds about 1,800 bytes.
        assertTrue(ShareCodec.encode(child).length < 1800)
    }

    @Test
    fun rejectsForeignOrBrokenText() {
        assertNull(ShareCodec.decode("https://example.com"))
        assertNull(ShareCodec.decode("RASPORED1:not-base64!!"))
        assertNull(ShareCodec.decode(ShareCodec.encode(child).dropLast(10)))
    }

    @Test
    fun rejectsTimesTheAppCannotShow() {
        val badBell = child.copy(morning = child.morning.copy(periods = listOf(Period(2000, 2045))))
        assertNull(ShareCodec.decode(ShareCodec.encode(badBell)))
        val badActivity = child.copy(activities = listOf(child.activities.first().copy(startMinute = -5)))
        assertNull(ShareCodec.decode(ShareCodec.encode(badActivity)))
        val badSwap = child.copy(customEvents = listOf(CalendarEvent(1, 1, "Zamena", noClasses = false, timetableDay = 9)))
        assertNull(ShareCodec.decode(ShareCodec.encode(badSwap)))
        val badDate = child.copy(referenceEpochDay = Long.MAX_VALUE)
        assertNull(ShareCodec.decode(ShareCodec.encode(badDate)))
    }
}
