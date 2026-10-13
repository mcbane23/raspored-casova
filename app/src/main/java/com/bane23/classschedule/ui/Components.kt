package com.bane23.classschedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.ExtraClass
import kotlinx.coroutines.delay
import java.time.LocalDateTime

// Subject and avatar colors are the Catppuccin Mocha pastel accents, used as soft tints behind emoji.

/** Emoji and color that make a subject recognizable at a glance, even for children who don't read well yet. */
data class SubjectStyle(val emoji: String, val color: Color)

private val SUBJECT_STYLES: List<Pair<String, SubjectStyle>> = listOf(
    "srpski" to SubjectStyle("📖", Color(0xFFF38BA8)),
    "matemat" to SubjectStyle("🔢", Color(0xFF89B4FA)),
    "engleski" to SubjectStyle("🇬🇧", Color(0xFF74C7EC)),
    "strani" to SubjectStyle("🗣️", Color(0xFF74C7EC)),
    "svet oko nas" to SubjectStyle("🌱", Color(0xFFA6E3A1)),
    "priroda" to SubjectStyle("🌍", Color(0xFFA6E3A1)),
    "likovn" to SubjectStyle("🎨", Color(0xFFF5C2E7)),
    "muzič" to SubjectStyle("🎵", Color(0xFFCBA6F7)),
    "muzic" to SubjectStyle("🎵", Color(0xFFCBA6F7)),
    "fizičk" to SubjectStyle("⚽", Color(0xFFFAB387)),
    "fizick" to SubjectStyle("⚽", Color(0xFFFAB387)),
    "istorij" to SubjectStyle("🏛️", Color(0xFFF9E2AF)),
    "geograf" to SubjectStyle("🗺️", Color(0xFF94E2D5)),
    "biolog" to SubjectStyle("🦋", Color(0xFFA6E3A1)),
    "fizika" to SubjectStyle("🧲", Color(0xFFB4BEFE)),
    "hemij" to SubjectStyle("🧪", Color(0xFF89DCEB)),
    "tehni" to SubjectStyle("🛠️", Color(0xFFBAC2DE)),
    "informat" to SubjectStyle("💻", Color(0xFF89B4FA)),
    "verska" to SubjectStyle("🕊️", Color(0xFFF2CDCD)),
    "građansk" to SubjectStyle("🤝", Color(0xFF94E2D5)),
    "gradjansk" to SubjectStyle("🤝", Color(0xFF94E2D5)),
    "odeljensk" to SubjectStyle("🏫", Color(0xFFCBA6F7)),
)

private val FALLBACK_COLORS = listOf(
    Color(0xFF89B4FA), Color(0xFFF38BA8), Color(0xFFA6E3A1), Color(0xFFFAB387),
    Color(0xFFCBA6F7), Color(0xFF94E2D5), Color(0xFFF5C2E7), Color(0xFFF9E2AF),
)

/** Style for a lesson text; optional classes ("Dopunska: Matematika") take their subject's style. */
fun subjectStyle(lesson: String): SubjectStyle {
    val subject = ExtraClass.parse(lesson).second.trim().lowercase()
    SUBJECT_STYLES.firstOrNull { subject.contains(it.first) }?.let { return it.second }
    val color = FALLBACK_COLORS[Math.floorMod(subject.hashCode(), FALLBACK_COLORS.size)]
    return SubjectStyle("📚", color)
}

/** Emoji for a calendar event, picked from words in its title. */
fun eventEmoji(title: String): String {
    val t = title.lowercase()
    return when {
        "zimski" in t -> "❄️"
        "letnji" in t -> "☀️"
        "proleć" in t -> "🌷"
        "jesenji" in t -> "🍂"
        "sretenj" in t -> "🇷🇸"
        "savindan" in t || "sava" in t -> "📜"
        "ispit" in t -> "✏️"
        "dan škole" in t -> "🏫"
        "slava" in t -> "🕯️"
        "raspust" in t -> "🏖️"
        "rasporedu" in t -> "🔁"
        else -> "📅"
    }
}

/** Rounded tile with a subject's emoji on a soft tint of its color. */
@Composable
fun SubjectBadge(lesson: String, size: Dp = 48.dp) {
    val style = subjectStyle(lesson)
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(style.color.copy(alpha = if (isDarkTheme()) 0.22f else 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(style.emoji, fontSize = (size.value * 0.46f).sp)
    }
}

/** Colored circle with the first letter of a child's name. */
@Composable
fun ChildAvatar(name: String, size: Dp = 36.dp) {
    val color = FALLBACK_COLORS[Math.floorMod(name.hashCode(), FALLBACK_COLORS.size)]
    Box(
        Modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color(0xFF1E1E2E),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** White card with soft corners used for every grouped block in the app. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = color,
        shadowElevation = 1.dp,
    ) {
        Column(
            (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(contentPadding),
            content = content,
        )
    }
}

/** Large page title with an optional muted subtitle; [compact] is a smaller version for the busy Today tab. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier.padding(top = if (compact) 4.dp else 8.dp, bottom = if (compact) 0.dp else 8.dp)) {
        Text(title, style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Text(
                subtitle,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Small uppercase-free label above a group of items. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = 8.dp),
    )
}

/** Muted helper line under a section. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Two or more options in one rounded track; the selected one is a raised white pill. */
@Composable
fun <T> PillToggle(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.surfaceBright else Color.Transparent,
                shadowElevation = if (isSelected) 1.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Small rounded tag, e.g. "Prepodne" or "Nema nastave". */
@Composable
fun Tag(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** Friendly empty state: a big emoji, a short title and a hint about what to do next. */
@Composable
fun EmptyState(emoji: String, title: String, hint: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, fontSize = 56.sp)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** True when the app is drawn with the dark palette (by the phone setting or the in-app theme choice). */
@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** The current time, refreshed every 15 seconds so "now" highlights and countdowns stay correct. */
@Composable
fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = LocalDateTime.now()
        }
    }
    return now
}
