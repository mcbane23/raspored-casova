package com.bane23.classschedule.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilledTonalButton
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

/** Share the selected child's timetable as a QR code, or scan one from another phone. */
@Composable
fun ShareSection(child: Child, viewModel: AppViewModel) {
    var showing by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf<Child?>(null) }
    var scanFailed by remember { mutableStateOf(false) }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val text = result.contents ?: return@rememberLauncherForActivityResult
        val decoded = ShareCodec.decode(text)
        if (decoded == null) scanFailed = true else scanned = decoded
    }

    Hint("Pokaži QR kod detetu ili drugom roditelju; oni ga skeniraju u svojoj aplikaciji.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = { showing = true }, modifier = Modifier.weight(1f)) { Text("Prikaži QR") }
        FilledTonalButton(modifier = Modifier.weight(1f), onClick = {
            scanner.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt("Skeniraj QR kod rasporeda")
                    .setBeepEnabled(false)
                    .setOrientationLocked(false),
            )
        }) { Text("Skeniraj QR") }
    }

    if (showing) {
        val bitmap = remember(child) { qrBitmap(ShareCodec.encode(child), 720) }
        AlertDialog(
            onDismissRequest = { showing = false },
            title = { Text("Raspored: ${child.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(bitmap.asImageBitmap(), contentDescription = "QR kod rasporeda", Modifier.size(280.dp))
                    Text(
                        "Na drugom telefonu: Podešavanja → Skeniraj QR. Deli se raspored, zvona, smene, " +
                            "kalendar i predmeti; jednodnevne izmene i alarm ostaju samo ovde.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showing = false }) { Text("Zatvori") } },
        )
    }

    scanned?.let { shared ->
        AlertDialog(
            onDismissRequest = { scanned = null },
            title = { Text("Uvezi raspored: ${shared.name}") },
            text = {
                Text(
                    "Možeš ga dodati kao novo dete ili njime zameniti raspored za ${child.name} " +
                        "(ime, škola, raspored, zvona, smene, kalendar i predmeti).",
                )
            },
            confirmButton = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    TextButton(onClick = {
                        viewModel.importChild(shared, replaceId = null)
                        scanned = null
                    }) { Text("Dodaj kao novo dete") }
                    TextButton(onClick = {
                        viewModel.importChild(shared, replaceId = child.id)
                        scanned = null
                    }) { Text("Zameni za ${child.name}") }
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
}

private fun qrBitmap(text: String, size: Int): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 2)
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) Color.BLACK else Color.WHITE }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
