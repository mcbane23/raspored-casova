package com.bane23.classschedule.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.LanguageStore
import com.bane23.classschedule.data.AppLanguage
import com.bane23.classschedule.data.Lang

/** The language's own name with its flag, the same in either app language so it can always be found. */
fun AppLanguage.label(): String = when (this) {
    AppLanguage.SERBIAN -> "🇷🇸 Srpski"
    AppLanguage.ENGLISH -> "🇬🇧 English"
}

/**
 * The very first screen: pick the app's language. [initial] (the phone's language when the app has it) is selected
 * at the start; the texts on this screen follow the selection, so each reader sees it in their own language.
 */
@Composable
fun LanguageScreen(initial: AppLanguage, onChoose: (AppLanguage) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(initial) }
    val english = selected == AppLanguage.ENGLISH
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("🌍", fontSize = 64.sp)
        Text(
            if (english) "Choose a language" else "Izaberi jezik",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            if (english) "You can change it later in Settings." else "Možeš ga promeniti kasnije u podešavanjima.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.padding(top = 4.dp))
        AppLanguage.entries.forEach { language ->
            val isSelected = language == selected
            Surface(
                selected = isSelected,
                onClick = { selected = language },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = if (isSelected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(language.label(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = isSelected, onClick = null)
                }
            }
        }
        Spacer(Modifier.padding(top = 4.dp))
        BigButton(if (english) "Continue" else "Nastavi", Modifier.fillMaxWidth()) { onChoose(selected) }
    }
}

/** The "Jezik · Language" settings card; picking the other language redraws the app in it. */
@Composable
fun LanguageGroup() {
    val activity = LocalContext.current.findActivity()
    // Both words, so someone who picked the wrong language by mistake still recognizes the card.
    SettingsGroup(if (Lang.english) "Language · Jezik" else "Jezik · Language") {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PillToggle(
                AppLanguage.entries,
                Lang.current,
                { it.label() },
                { language -> if (activity != null && language != Lang.current) LanguageStore.choose(activity, language) },
                onCard = true,
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
