package com.bane23.classschedule.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ExtraClass
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Emoji and color that make a subject recognizable at a glance, even for children who don't read well yet. */
data class SubjectStyle(val emoji: String, val color: Color)

// Saturated subject colors: shown as soft tints behind emoji, and in full on the "now" card.
private val SUBJECT_STYLES: List<Pair<String, SubjectStyle>> = listOf(
    "srpski" to SubjectStyle("📖", Color(0xFFE5484D)),
    "matemat" to SubjectStyle("🔢", Color(0xFF3E7BFA)),
    "engleski" to SubjectStyle("🇬🇧", Color(0xFF0E9CC0)),
    "strani" to SubjectStyle("🗣️", Color(0xFF0E9CC0)),
    "svet oko nas" to SubjectStyle("🌱", Color(0xFF2FA86A)),
    "priroda" to SubjectStyle("🌍", Color(0xFF2FA86A)),
    "likovn" to SubjectStyle("🎨", Color(0xFFD6409F)),
    "muzič" to SubjectStyle("🎵", Color(0xFF8E4EC6)),
    "muzic" to SubjectStyle("🎵", Color(0xFF8E4EC6)),
    "fizičk" to SubjectStyle("⚽", Color(0xFFF07F1A)),
    "fizick" to SubjectStyle("⚽", Color(0xFFF07F1A)),
    "istorij" to SubjectStyle("🏛️", Color(0xFFC2911A)),
    "geograf" to SubjectStyle("🗺️", Color(0xFF21A0A0)),
    "biolog" to SubjectStyle("🦋", Color(0xFF45A350)),
    "fizika" to SubjectStyle("🧲", Color(0xFF6E56CF)),
    "hemij" to SubjectStyle("🧪", Color(0xFF0B95B5)),
    "tehni" to SubjectStyle("🛠️", Color(0xFF6B7A90)),
    "informat" to SubjectStyle("💻", Color(0xFF5B6CF0)),
    "verska" to SubjectStyle("🕊️", Color(0xFFA9805A)),
    "građansk" to SubjectStyle("🤝", Color(0xFF1A9E99)),
    "gradjansk" to SubjectStyle("🤝", Color(0xFF1A9E99)),
    "odeljensk" to SubjectStyle("🏫", Color(0xFF7563D8)),
)

private val FALLBACK_COLORS = listOf(
    Color(0xFFE5484D), Color(0xFF3E7BFA), Color(0xFF2FA86A), Color(0xFFF07F1A),
    Color(0xFF8E4EC6), Color(0xFF0E9CC0), Color(0xFFD6409F), Color(0xFFC2911A),
)

/** Colors of the full-width status card when no subject sets it. */
val AccentOrange = Color(0xFFE8620C)
val BreakTeal = Color(0xFF14A38B)
val DoneGreen = Color(0xFF2FA86A)
val HolidayAmber = Color(0xFFE98B2A)

/** Short subject names for places where only a few letters fit (the week table, the stops on Today). */
private val SHORT_NAMES = listOf(
    "srpski" to "Srp", "matemat" to "Mat", "engleski" to "Eng", "strani" to "Str", "svet oko nas" to "SON",
    "priroda" to "PiD", "likovn" to "Lik", "muzič" to "Muz", "muzic" to "Muz", "fizičk" to "Fiz", "fizick" to "Fiz",
    "istorij" to "Ist", "geograf" to "Geo", "biolog" to "Bio", "fizika" to "Fizika", "hemij" to "Hem", "tehni" to "Teh",
    "informat" to "Inf", "verska" to "Ver", "građansk" to "Građ", "gradjansk" to "Građ", "odeljensk" to "ČOS",
)

/** "Mat" for "Matematika" or "Dopunska: Matematika"; unknown subjects keep their first four letters. */
fun shortName(lesson: String): String {
    val name = ExtraClass.parse(lesson).second.trim()
    return SHORT_NAMES.firstOrNull { name.lowercase().contains(it.first) }?.second ?: name.take(4)
}

/**
 * A horizontal swipe on this element calls [onPrevious] (swipe right) or [onNext] (swipe left), e.g. to change the
 * day. Drags consumed by a scrolling child (the stops row) don't count.
 */
fun Modifier.swipeBetween(onPrevious: () -> Unit, onNext: () -> Unit): Modifier = pointerInput(onPrevious, onNext) {
    val threshold = 72.dp.toPx()
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragEnd = {
            when {
                total > threshold -> onPrevious()
                total < -threshold -> onNext()
            }
        },
        onHorizontalDrag = { _, amount -> total += amount },
    )
}

/** Style for a lesson text; optional classes ("Dopunska: Matematika") take their subject's style. */
fun subjectStyle(lesson: String): SubjectStyle {
    val subject = ExtraClass.parse(lesson).second.trim().lowercase()
    SUBJECT_STYLES.firstOrNull { subject.contains(it.first) }?.let { return it.second }
    val color = FALLBACK_COLORS[Math.floorMod(subject.hashCode(), FALLBACK_COLORS.size)]
    return SubjectStyle("📚", color)
}

/** Avatar color for a child, stable for a given name. */
fun childColor(name: String): Color = listOf(
    Color(0xFFD6409F), Color(0xFF3E7BFA), Color(0xFF2FA86A), Color(0xFF8E4EC6), Color(0xFFF07F1A), Color(0xFF0E9CC0),
)[Math.floorMod(name.trim().hashCode(), 6)]

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

/** A soft tint of [color] on the card surface; stronger in the dark theme so it stays visible. */
@Composable
fun tintOf(color: Color, light: Float = 0.18f, dark: Float = 0.30f): Color =
    lerp(MaterialTheme.colorScheme.surfaceContainerLowest, color, if (isDarkTheme()) dark else light)

/** Background of the big status card: the color itself, slightly deepened (light) or blended into the night (dark). */
@Composable
fun heroColorOf(color: Color): Color =
    if (isDarkTheme()) lerp(MaterialTheme.colorScheme.surfaceContainerLowest, color, 0.42f) else lerp(color, Color.Black, 0.16f)

/** Rounded tile with a subject's emoji on a tint of its color; [done] shows a check instead. */
@Composable
fun SubjectBadge(lesson: String, size: Dp = 48.dp, done: Boolean = false, shape: RoundedCornerShape = RoundedCornerShape(size / 3)) {
    val style = subjectStyle(lesson)
    Box(
        Modifier.size(size).clip(shape).background(tintOf(style.color, 0.30f, 0.38f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (done) "✓" else style.emoji, fontSize = (size.value * 0.48f).sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Colored circle with the first letter of a child's name. */
@Composable
fun ChildAvatar(name: String, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(childColor(name)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color.White,
            fontSize = (size.value * 0.46f).sp,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Flat card on the tinted background, used for every grouped block in the app. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = color) {
        Column(
            (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/** Large page title with an optional muted line above it (e.g. the date). */
@Composable
fun ScreenHeader(title: String, overline: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        if (overline != null) {
            Text(overline, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Small uppercase label above a group of items. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 6.dp, top = 6.dp),
    )
}

/** Muted helper line. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/**
 * Two or more options in one rounded track. On the page background the selected option is filled with the accent;
 * inside a card ([onCard]) the track is the page color and the selected option a raised card-colored pill.
 */
@Composable
fun <T> PillToggle(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    onCard: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .fillMaxWidth()
            .clip(if (onCard) MaterialTheme.shapes.small else CircleShape)
            .background(if (onCard) colors.background else colors.surfaceContainerLowest)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                shape = if (onCard) RoundedCornerShape(11.dp) else CircleShape,
                color = when {
                    !isSelected -> Color.Transparent
                    onCard -> colors.surfaceBright
                    else -> colors.primary
                },
                shadowElevation = if (isSelected && onCard) 1.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        color = when {
                            !isSelected -> colors.onSurfaceVariant
                            onCard -> colors.onSurface
                            else -> colors.onPrimary
                        },
                    )
                }
            }
        }
    }
}

/** Small rounded tag, e.g. "Nema nastave". */
@Composable
fun Tag(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        modifier = modifier.clip(CircleShape).background(container).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Friendly empty state: a big emoji, a short title, a hint about what to do next and optionally a button that does it. */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, fontSize = 52.sp)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.height(6.dp))
            BigButton(action, onClick = onAction)
        }
    }
}

/** Filled accent button for the one main action on a screen, e.g. "Upiši raspored". */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Round button that opens the parents' settings. */
@Composable
fun SettingsButton(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Settings, contentDescription = "Podešavanja", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Round back button for the settings pages. */
@Composable
fun BackButton(onClick: () -> Unit) {
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad", tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/**
 * Top row of a main tab: name pills to switch between children (only with two or more, so a child can tap their
 * own name), or [title] with one child; [trailing] sits on the right.
 */
@Composable
fun ChildTopRow(
    children: List<Child>,
    selectedId: String,
    onSelect: (String) -> Unit,
    title: String,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (children.size > 1) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                children.forEach { c ->
                    val selected = c.id == selectedId
                    Surface(
                        onClick = { onSelect(c.id) },
                        shape = CircleShape,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            ChildAvatar(c.name, size = 30.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                c.name.ifBlank { "Bez imena" },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        } else {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

/**
 * The big colored card at the top of a screen: an [eyebrow] line, a large [emoji] and [title], a [detail] line and,
 * optionally, a ring counting down [ringMinutes] with [ringProgress] of the time already gone.
 */
@Composable
fun HeroCard(
    color: Color,
    eyebrow: String,
    emoji: String,
    title: String,
    detail: String?,
    ringMinutes: Long? = null,
    ringProgress: Float = 0f,
    footer: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = heroColorOf(color),
        contentColor = Color.White,
    ) {
        Column(
            (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelLarge, letterSpacing = 0.8.sp, color = Color.White.copy(alpha = 0.9f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 46.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    if (detail != null) {
                        Text(detail, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.92f))
                    }
                }
                if (ringMinutes != null) {
                    Spacer(Modifier.width(8.dp))
                    CountdownRing(ringMinutes, ringProgress)
                }
            }
            if (footer != null) {
                Text(
                    footer,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** Ring that empties as time passes, with the minutes left in the middle. */
@Composable
private fun CountdownRing(minutes: Long, progress: Float) {
    val left by animateFloatAsState(1f - progress.coerceIn(0f, 1f), tween(600), label = "ring")
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(58.dp)) {
            val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, useCenter = false, style = stroke)
            drawArc(Color.White, -90f, 360f * left, useCenter = false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (minutes > 99) "${minutes / 60}h" else "$minutes", style = MaterialTheme.typography.titleLarge, color = Color.White, lineHeight = 20.sp)
            if (minutes <= 99) Text("min", style = MaterialTheme.typography.labelSmall, color = Color.White, lineHeight = 11.sp)
        }
    }
}

/** Soft banner for notes such as a swapped timetable day or a one-day change. */
@Composable
fun NoteBanner(emoji: String, text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
        )
        action?.invoke()
    }
}

/** Dashed outline button for "add" actions at the end of a list. */
@Composable
fun DashedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.large)
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
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
