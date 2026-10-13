package com.bane23.classschedule.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
