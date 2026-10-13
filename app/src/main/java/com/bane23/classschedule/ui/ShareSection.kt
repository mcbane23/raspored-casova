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
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/** Shows [child]'s timetable as a QR code for another phone to scan. */
@Composable
fun QrCodeDialog(child: Child, onDismiss: () -> Unit) {
    val bitmap = remember(child) { qrBitmap(ShareCodec.encode(child), 720) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Raspored: ${child.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(bitmap.asImageBitmap(), contentDescription = "QR kod rasporeda", Modifier.size(280.dp))
                Text(
                    "Na drugom telefonu: Podešavanja → Uvezi raspored sa QR koda. Deli se raspored, zvona, smene, " +
                        "kalendar, predmeti i šta se nosi; jednodnevne izmene i alarm ostaju samo ovde.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zatvori") } },
    )
}

/**
 * Scans a timetable QR code from another phone; the result is added as a new child or replaces [replaceable].
 * Returns the function that opens the scanner.
 */
@Composable
fun rememberQrImport(viewModel: AppViewModel, replaceable: Child?): () -> Unit {
    var scanned by remember { mutableStateOf<Child?>(null) }
    var scanFailed by remember { mutableStateOf(false) }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@rememberLauncherForActivityResult
        val decoded = ShareCodec.decode(text)
        if (decoded == null) scanFailed = true else scanned = decoded
    }

    scanned?.let { shared ->
        AlertDialog(
            onDismissRequest = { scanned = null },
            title = { Text("Uvezi raspored: ${shared.name}") },
            text = {
                Text(
                    if (replaceable == null) "Raspored će biti dodat kao novo dete."
                    else "Možeš ga dodati kao novo dete ili njime zameniti raspored za ${replaceable.name} " +
                        "(ime, škola, raspored, zvona, smene, kalendar i predmeti).",
                )
            },
            confirmButton = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    TextButton(onClick = {
                        viewModel.importChild(shared, replaceId = null)
                        scanned = null
                    }) { Text("Dodaj kao novo dete") }
                    if (replaceable != null) {
                        TextButton(onClick = {
                            viewModel.importChild(shared, replaceId = replaceable.id)
                            scanned = null
                        }) { Text("Zameni za ${replaceable.name}") }
                    }
                    TextButton(onClick = { scanned = null }) { Text("Odustani") }
                }
            },
        )
    }

    if (scanFailed) {
        AlertDialog(
            onDismissRequest = { scanFailed = false },
            title = { Text("Nepoznat QR kod") },
            text = { Text("Ovaj QR kod nije raspored iz aplikacije Raspored časova.") },
            confirmButton = { TextButton(onClick = { scanFailed = false }) { Text("U redu") } },
        )
    }

    return {
        scanner.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Skeniraj QR kod rasporeda")
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
