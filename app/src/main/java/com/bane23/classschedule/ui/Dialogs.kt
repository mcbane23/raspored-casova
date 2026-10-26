package com.bane23.classschedule.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.ExtraClass

/** Asks for a line of text, e.g. a subject or a child's name. [extraAction] adds a red button such as "delete subject". */
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
                    TextButton(onClick = extraAction.second) { Text(extraAction.first, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(colors = accentTextButton(), onClick = { onConfirm(text) }) { Text("Sačuvaj") } },
        dismissButton = { TextButton(colors = accentTextButton(), onClick = onDismiss) { Text("Odustani") } },
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
        confirmButton = { TextButton(colors = accentTextButton(), onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Sačuvaj") } },
        dismissButton = { TextButton(colors = accentTextButton(), onClick = onDismiss) { Text("Odustani") } },
    )
}

/**
 * Bottom sheet with a big tile for every subject in [subjects]: one tap picks it. Chips above mark the class as
 * optional remedial ("Dopunska") or advanced ("Dodatna") teaching. "+ Novi predmet" types a subject that is not in
 * the list. [onConfirm] receives the full lesson text; [clearLabel] adds a button that sends "" (no class).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectPickerSheet(
    overline: String,
    initial: String,
    subjects: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    clearLabel: String? = null,
) {
    val (initialKind, initialSubject) = remember(initial) { ExtraClass.parse(initial) }
    var kind by remember { mutableStateOf(initialKind) }
    var typing by remember { mutableStateOf(false) }
    val options = remember(subjects, initialSubject) {
        if (initialSubject.isBlank() || subjects.any { it.equals(initialSubject, ignoreCase = true) }) subjects
        else listOf(initialSubject) + subjects
    }
    val haptic = LocalHapticFeedback.current
    fun pick(subject: String) {
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        onConfirm(kind?.lesson(subject) ?: subject.trim())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column {
                Text(overline, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Izaberi predmet", style = MaterialTheme.typography.titleLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf<ExtraClass?>(null) + ExtraClass.entries).forEach { k ->
                    val selected = kind == k
                    Surface(
                        onClick = { kind = k },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        Text(
                            k?.prefix ?: "Redovan",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
            options.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { subject ->
                        val style = subjectStyle(subject)
                        val selected = subject.equals(initialSubject, ignoreCase = true)
                        Surface(
                            onClick = { pick(subject) },
                            modifier = Modifier.weight(1f).heightIn(min = 72.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = tintOf(style.color),
                            border = if (selected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null,
                        ) {
                            Column(
                                Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(style.emoji, fontSize = 24.sp)
                                Text(
                                    subject,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (clearLabel != null) {
                    SheetButton(clearLabel, MaterialTheme.colorScheme.error, Modifier.weight(1f)) { onConfirm("") }
                }
                SheetButton("+ Novi predmet", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { typing = true }
            }
        }
    }

    if (typing) {
        TextInputDialog(
            title = "Novi predmet",
            label = "Naziv predmeta",
            initial = "",
            onDismiss = { typing = false },
            onConfirm = {
                typing = false
                if (it.isNotBlank()) pick(it)
            },
        )
    }
}

@Composable
private fun SheetButton(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}
