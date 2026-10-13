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
        assertNotEquals(child.id, decoded.id)
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
}
