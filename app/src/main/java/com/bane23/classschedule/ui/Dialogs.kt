package com.bane23.classschedule.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.bane23.classschedule.data.ExtraClass

/** Asks for a line of text, e.g. a subject or a child's name. [extraAction] adds a third button such as "cancel class". */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    extraAction: Pair<String, () -> Unit>? = null,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = true)
                if (extraAction != null) {
                    TextButton(onClick = extraAction.second) { Text(extraAction.first) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Sačuvaj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeInputDialog(title: String, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinute / 60, initialMinute = initialMinute % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Sačuvaj") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    )
}

/**
 * Picks a subject from [subjects] in a drop-down list, or accepts a typed one; typing filters the list. Chips
 * mark the class as optional remedial ("Dopunska") or advanced ("Dodatna") teaching for that subject.
 * [onConfirm] receives the full lesson text. [extraAction] adds a button such as "cancel class".
 */
@Composable
fun SubjectPickerDialog(
    title: String,
    initial: String,
    subjects: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    extraAction: Pair<String, () -> Unit>? = null,
) {
    val (initialKind, initialSubject) = remember(initial) { ExtraClass.parse(initial) }
    var kind by remember { mutableStateOf(initialKind) }
    var text by remember { mutableStateOf(initialSubject) }
    var expanded by remember { mutableStateOf(initial.isEmpty()) }
    val matches = subjects.filter {
        text.isBlank() || text == initialSubject || it.contains(text.trim(), ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = kind == null, onClick = { kind = null }, label = { Text("Redovan") })
                    ExtraClass.entries.forEach { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.prefix) })
                    }
                }
                Box {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            expanded = true
                        },
                        label = { Text("Predmet") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Izaberi sa spiska")
                            }
                        },
                    )
                    DropdownMenu(
                        expanded = expanded && matches.isNotEmpty(),
                        onDismissRequest = { expanded = false },
                        properties = PopupProperties(focusable = false),
                        modifier = Modifier.heightIn(max = 280.dp),
                    ) {
                        matches.forEach { subject ->
                            DropdownMenuItem(
                                text = { Text(subject) },
                                onClick = {
                                    text = subject
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                if (kind != null) {
                    Text(
                        "Neobavezan čas iz izabranog predmeta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (extraAction != null) {
                    TextButton(onClick = extraAction.second) { Text(extraAction.first) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val subject = text.trim()
                onConfirm(if (subject.isEmpty()) "" else kind?.lesson(subject) ?: subject)
            }) { Text("Sačuvaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    )
}
