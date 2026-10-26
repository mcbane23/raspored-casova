package com.bane23.classschedule.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/** Keeps all app data in one small JSON file in the app's private storage. Use [get] for the shared instance. */
class ChildRepository private constructor(context: Context) {
    private val dir = context.filesDir
    private val file = AtomicFile(File(dir, "data.json"))
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    /** One write at a time, so two quick saves can never mix their contents in the file. */
    private val writeLock = Mutex()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData {
        val text = runCatching { file.readFully().toString(Charsets.UTF_8) }.getOrNull()
        val stored = text?.let { runCatching { json.decodeFromString(AppData.serializer(), it) }.getOrNull() }
        if (stored != null && stored.children.isNotEmpty()) {
            val today = LocalDate.now()
            val current = stored.migrated()
            return current.copy(children = current.children.map { it.withoutPastChanges(today).withoutPastActivities(today) })
        }
        // A file that exists but cannot be read is kept aside instead of being overwritten by the next save.
        if (text != null) runCatching { File(dir, "data.json.bad").writeText(text) }
        // First launch: start with one child so the app is usable right away.
        val first = Child(name = DEFAULT_CHILD_NAME, referenceEpochDay = LocalDate.now().toEpochDay())
        return AppData(children = listOf(first), selectedChildId = first.id, dataVersion = CURRENT_DATA_VERSION)
    }

    suspend fun update(transform: (AppData) -> AppData) {
        _data.update(transform)
        withContext(Dispatchers.IO) {
            writeLock.withLock {
                // Always the newest data, so the file never goes back to an older state when saves overlap.
                val bytes = json.encodeToString(AppData.serializer(), _data.value).toByteArray(Charsets.UTF_8)
                val out = file.startWrite()
                try {
                    out.write(bytes)
                    file.finishWrite(out)
                } catch (_: Exception) {
                    file.failWrite(out)
                }
            }
        }
    }

    companion object {
        @Volatile private var instance: ChildRepository? = null

        fun get(context: Context): ChildRepository = instance ?: synchronized(this) {
            instance ?: ChildRepository(context.applicationContext).also { instance = it }
        }
    }
}
