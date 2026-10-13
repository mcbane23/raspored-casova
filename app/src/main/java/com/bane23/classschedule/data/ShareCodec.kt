package com.bane23.classschedule.data

import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Packs a child's timetable, calendar settings and subjects into a short text for a QR code, and back.
 * One-day changes and alarm settings stay on the sharing phone.
 */
object ShareCodec {
    private const val PREFIX = "RASPORED1:"
    private const val MAX_DECODED_BYTES = 256 * 1024
    private val json = Json { encodeDefaults = false; ignoreUnknownKeys = true }

    fun encode(child: Child): String {
        val shared = child.copy(overrides = emptyList(), alarmEnabled = false, alarmMinutesBefore = 0)
        val raw = json.encodeToString(Child.serializer(), shared).toByteArray(Charsets.UTF_8)
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
        json.decodeFromString(Child.serializer(), out.toString(Charsets.UTF_8.name()))
            .copy(id = UUID.randomUUID().toString())
    }.getOrNull()
}
