package com.bane23.classschedule.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ShareCodec
import com.bane23.classschedule.data.tr
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/** Shows [child]'s timetable as a QR code for another phone to scan. */
@Composable
fun QrCodeDialog(child: Child, onDismiss: () -> Unit) {
    // Null when the data does not fit in one QR code (a very long list of activities or events).
    val bitmap = remember(child) { runCatching { qrBitmap(ShareCodec.encode(child), 720) }.getOrNull() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Raspored: ${child.name}", "Timetable: ${child.name}")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (bitmap == null) {
                    Text(
                        tr(
                            "Raspored ima previše podataka za jedan QR kod. Obriši aktivnosti ili događaje koji više nisu " +
                                "potrebni pa pokušaj ponovo.",
                            "The timetable has too much data for one QR code. Delete activities or events you no longer " +
                                "need and try again.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Image(bitmap.asImageBitmap(), contentDescription = tr("QR kod rasporeda", "Timetable QR code"), Modifier.size(280.dp))
                    Text(
                        tr(
                            "Na drugom telefonu: Podešavanja → Uvezi raspored sa QR koda. Deli se raspored, zvona, smene, " +
                                "kalendar, predmeti, aktivnosti i šta se nosi; jednodnevne izmene i podsetnici ostaju samo ovde.",
                            "On the other phone: Settings → Import a timetable from a QR code. The timetable, bells, shifts, " +
                                "calendar, subjects, activities and what to bring are shared; one-day changes and reminders stay here only.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(colors = accentTextButton(), onClick = onDismiss) { Text(tr("Zatvori", "Close")) } },
    )
}

/**
 * Scans a timetable QR code from another phone; the result is added as a new child or replaces [replaceable].
 * With [onImported] (the first-run setup) it replaces [replaceable] without asking and then calls it.
 * Returns the function that opens the scanner.
 */
@Composable
fun rememberQrImport(viewModel: AppViewModel, replaceable: Child?, onImported: (() -> Unit)? = null): () -> Unit {
    var scanned by remember { mutableStateOf<Child?>(null) }
    var scanFailed by remember { mutableStateOf(false) }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@rememberLauncherForActivityResult
        val decoded = ShareCodec.decode(text)
        when {
            decoded == null -> scanFailed = true
            onImported != null && replaceable != null -> {
                viewModel.importChild(decoded, replaceId = replaceable.id)
                onImported()
            }
            else -> scanned = decoded
        }
    }

    scanned?.let { shared ->
        AlertDialog(
            onDismissRequest = { scanned = null },
            title = { Text(tr("Uvezi raspored: ${shared.name}", "Import timetable: ${shared.name}")) },
            text = {
                Text(
                    if (replaceable == null) tr("Raspored će biti dodat kao novo dete.", "The timetable will be added as a new child.")
                    else tr(
                        "Možeš ga dodati kao novo dete ili njime zameniti raspored za ${replaceable.name} " +
                            "(ime, škola, raspored, zvona, smene, kalendar i predmeti).",
                        "You can add it as a new child or use it to replace the timetable of ${replaceable.name} " +
                            "(name, school, timetable, bells, shifts, calendar and subjects).",
                    ),
                )
            },
            confirmButton = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    TextButton(colors = accentTextButton(), onClick = {
                        viewModel.importChild(shared, replaceId = null)
                        scanned = null
                    }) { Text(tr("Dodaj kao novo dete", "Add as a new child")) }
                    if (replaceable != null) {
                        TextButton(colors = accentTextButton(), onClick = {
                            viewModel.importChild(shared, replaceId = replaceable.id)
                            scanned = null
                        }) { Text(tr("Zameni za ${replaceable.name}", "Replace ${replaceable.name}")) }
                    }
                    TextButton(colors = accentTextButton(), onClick = { scanned = null }) { Text(tr("Odustani", "Cancel")) }
                }
            },
        )
    }

    if (scanFailed) {
        AlertDialog(
            onDismissRequest = { scanFailed = false },
            title = { Text(tr("Nepoznat QR kod", "Unknown QR code")) },
            text = { Text(tr("Ovaj QR kod nije raspored iz aplikacije Raspored časova.", "This QR code is not a timetable from the Class schedule app.")) },
            confirmButton = { TextButton(colors = accentTextButton(), onClick = { scanFailed = false }) { Text(tr("U redu", "OK")) } },
        )
    }

    return {
        scanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt(tr("Skeniraj QR kod rasporeda", "Scan the timetable QR code"))
                .setBeepEnabled(false)
                .setOrientationLocked(false),
        )
    }
}

private fun qrBitmap(text: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 2)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) Color.BLACK else Color.WHITE }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
