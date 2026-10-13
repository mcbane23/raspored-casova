package com.bane23.classschedule.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.CalendarEvent
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.shiftOn
import com.bane23.classschedule.data.withDayChange
import com.bane23.classschedule.data.withoutDayChange
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChildRepository(application)
    val data: StateFlow<AppData> = repository.data

    private fun save(transform: (AppData) -> AppData) {
        viewModelScope.launch { repository.update(transform) }
    }

    private fun updateChild(id: String, transform: (Child) -> Child) = save { data ->
        data.copy(children = data.children.map { if (it.id == id) transform(it) else it })
    }

    fun selectChild(id: String) = save { it.copy(selectedChildId = id) }

    fun addChild(name: String) {
        val child = Child(name = name.trim().ifEmpty { "Dete" }, referenceEpochDay = LocalDate.now().toEpochDay())
        save { it.copy(children = it.children + child, selectedChildId = child.id) }
    }

    fun deleteChild(id: String) = save { data ->
        val remaining = data.children.filterNot { it.id == id }
        if (remaining.isEmpty()) return@save data
        val selected = data.selectedChildId.takeIf { sel -> remaining.any { it.id == sel } } ?: remaining.first().id
        data.copy(children = remaining, selectedChildId = selected)
    }

    fun renameChild(id: String, name: String) = updateChild(id) { it.copy(name = name) }

    fun setSchool(id: String, school: String) = updateChild(id) { it.copy(school = school) }

    /** Changes the rotation while keeping today's shift what it currently is. */
    fun setShiftMode(id: String, mode: ShiftMode) = updateChild(id) { child ->
        val today = LocalDate.now()
        child.copy(shiftMode = mode, referenceEpochDay = today.toEpochDay(), referenceShift = child.shiftOn(today))
    }

    /** Sets which shift the child is in this week (or month); the rotation continues from there. */
    fun setCurrentShift(id: String, shift: Shift) = updateChild(id) {
        it.copy(referenceEpochDay = LocalDate.now().toEpochDay(), referenceShift = shift)
    }

    fun setTimetable(id: String, shift: Shift, timetable: Timetable) = updateChild(id) {
        it.withTimetable(shift, timetable)
    }

    /** Changes one period on one date only, e.g. a substitute subject, or "" for a cancelled class. */
    fun changeDay(id: String, date: LocalDate, index: Int, subject: String) = updateChild(id) {
        it.withDayChange(date, index, subject)
    }

    fun resetDay(id: String, date: LocalDate) = updateChild(id) { it.withoutDayChange(date) }

    fun setRegion(id: String, region: Region) = updateChild(id) { it.copy(region = region) }

    fun setFinalGrade(id: String, finalGrade: Boolean) = updateChild(id) { it.copy(finalGrade = finalGrade) }

    fun addEvent(id: String, event: CalendarEvent) = updateChild(id) { it.copy(customEvents = it.customEvents + event) }

    fun deleteEvent(id: String, eventId: String) = updateChild(id) { child ->
        child.copy(customEvents = child.customEvents.filterNot { it.id == eventId })
    }
}
