package com.bane23.classschedule.data

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Base64
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Packs a child's timetable, calendar settings, subjects and extracurricular activities into a short text for a QR
 * code, and back. One-day changes and alarm settings stay on the sharing phone; activities arrive with their
 * reminders off, since each phone decides which reminders it wants.
 */
object ShareCodec {
    private const val PREFIX = "RASPORED1:"
    private const val MAX_DECODED_BYTES = 256 * 1024
    private val json = Json { encodeDefaults = false; ignoreUnknownKeys = true }

    /** The child without what stays on each phone: one-day changes, alarm and reminder settings, packing ticks. */
    private fun Child.withoutPhoneSettings(): Child = copy(
        overrides = emptyList(),
        alarmEnabled = false,
        alarmMinutesBefore = 0,
        alarmSound = false,
        alarmSilent = false,
        packReminderEnabled = false,
        packReminderMinute = DEFAULT_PACK_REMINDER_MINUTE,
        packReminderSound = false,
        packReminderSilent = false,
        packedForEpochDay = 0,
        packed = emptyList(),
        activities = activities.map { it.copy(reminderEnabled = false) },
        activityReminderSound = false,
        activityReminderSilent = false,
    )

    fun encode(child: Child): String {
        val raw = json.encodeToString(Child.serializer(), child.withoutPhoneSettings()).toByteArray(Charsets.UTF_8)
        val deflater = Deflater(Deflater.BEST_COMPRESSION).apply {
            setInput(raw)
            finish()
        }
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return PREFIX + Base64.getEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    /** The shared child, or null if [text] is not a code made by this app. Imported children get a new id. */
    fun decode(text: String): Child? = runCatching {
        require(text.startsWith(PREFIX))
        val compressed = Base64.getDecoder().decode(text.removePrefix(PREFIX).trim())
        val inflater = Inflater().apply { setInput(compressed) }
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buffer)
            require(n > 0 || !(inflater.needsInput() || inflater.needsDictionary())) { "truncated" }
            out.write(buffer, 0, n)
            require(out.size() <= MAX_DECODED_BYTES) { "too large" }
        }
        inflater.end()
        val child = json.decodeFromString(Child.serializer(), out.toString(Charsets.UTF_8.name()))
        // A code made elsewhere could carry times or dates the app cannot show; such a code is refused as a whole.
        require(child.isValid()) { "invalid values" }
        child.withoutPhoneSettings().copy(id = UUID.randomUUID().toString())
    }.getOrNull()

    private val SANE_DAYS = LocalDate.of(1900, 1, 1).toEpochDay()..LocalDate.of(2200, 12, 31).toEpochDay()

    private fun minuteOfDay(minute: Int) = minute in 0 until 24 * 60

    private fun Child.isValid(): Boolean =
        referenceEpochDay in SANE_DAYS &&
            (morning.periods + afternoon.periods).all { minuteOfDay(it.startMinute) && minuteOfDay(it.endMinute) } &&
            customEvents.all { e ->
                e.startEpochDay in SANE_DAYS && e.endEpochDay in SANE_DAYS && (e.timetableDay == null || e.timetableDay in 1..7)
            } &&
            activities.all { a ->
                minuteOfDay(a.startMinute) && minuteOfDay(a.endMinute) &&
                    a.days.all { it in 1..7 } &&
                    a.startEpochDay in SANE_DAYS && a.endEpochDay in SANE_DAYS &&
                    a.reminderMinutesBefore in 0..24 * 60 &&
                    a.changes.all { c ->
                        c.epochDay in SANE_DAYS && (c.movedToEpochDay == null || c.movedToEpochDay in SANE_DAYS) &&
                            minuteOfDay(c.startMinute) && minuteOfDay(c.endMinute)
                    }
            }
}
