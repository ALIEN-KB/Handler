package com.alienkb.handler.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/** Handler colours: near-black glass with neon mint, violet and amber accents. */
object HC {
    val Bg = Color(0xFF07090D)
    val Surface1 = Color(0xFF0F131A)
    val Surface2 = Color(0xFF161B24)
    val Surface3 = Color(0xFF1D2430)
    val Stroke = Color(0xFF232A36)
    val Mint = Color(0xFF5EF2C6)
    val Violet = Color(0xFFA594FF)
    val Amber = Color(0xFFFFC266)
    val Rose = Color(0xFFFF6B81)
    val Sky = Color(0xFF6CC7FF)
    val TextHi = Color(0xFFE8ECF3)
    val TextLo = Color(0xFF8A94A6)
}

val Mono = FontFamily.Monospace

private val Scheme = darkColorScheme(
    primary = HC.Mint,
    onPrimary = HC.Bg,
    primaryContainer = Color(0xFF0E3A31),
    onPrimaryContainer = HC.Mint,
    secondary = HC.Violet,
    onSecondary = HC.Bg,
    tertiary = HC.Amber,
    onTertiary = HC.Bg,
    background = HC.Bg,
    onBackground = HC.TextHi,
    surface = HC.Bg,
    onSurface = HC.TextHi,
    surfaceVariant = HC.Surface2,
    onSurfaceVariant = HC.TextLo,
    surfaceContainerLowest = HC.Bg,
    surfaceContainerLow = HC.Surface1,
    surfaceContainer = HC.Surface1,
    surfaceContainerHigh = HC.Surface2,
    surfaceContainerHighest = HC.Surface3,
    outline = HC.Stroke,
    outlineVariant = HC.Stroke,
    error = HC.Rose,
    onError = HC.Bg,
)

@Composable
fun HandlerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
