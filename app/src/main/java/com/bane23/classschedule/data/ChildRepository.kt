package com.bane23.classschedule.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/** Keeps all app data in one small JSON file in the app's private storage. */
class ChildRepository(context: Context) {
    private val file = File(context.filesDir, "data.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData {
        val stored = runCatching { json.decodeFromString(AppData.serializer(), file.readText()) }.getOrNull()
        if (stored != null && stored.children.isNotEmpty()) {
            val today = LocalDate.now()
            return stored.copy(children = stored.children.map { it.withoutPastChanges(today) })
        }
        // First launch: start with one child so the app is usable right away.
        val first = Child(name = "Dete", referenceEpochDay = LocalDate.now().toEpochDay())
        return AppData(children = listOf(first), selectedChildId = first.id)
    }

    suspend fun update(transform: (AppData) -> AppData) {
        _data.update(transform)
        val snapshot = _data.value
        withContext(Dispatchers.IO) {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(AppData.serializer(), snapshot))
            tmp.renameTo(file)
        }
    }
}
