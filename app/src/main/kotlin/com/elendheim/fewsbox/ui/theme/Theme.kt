package com.elendheim.fewsbox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark mode first: this palette is the flagship look and the app ships with
// it regardless of system setting. A light variant can come later if it ever
// earns its keep.

val Ink = Color(0xFF14141A)
val Panel = Color(0xFF1D1D26)
val PanelRaised = Color(0xFF26242F)
val TextBright = Color(0xFFEAE7EE)
val TextMuted = Color(0xFF8B8797)
val Accent = Color(0xFFFF7B93)       // the logo pink
val AccentDim = Color(0xFF3A2531)
val HpGreen = Color(0xFF6FCF97)
val HpLow = Color(0xFFE0B05E)
val ShieldBlue = Color(0xFF4AA3FF)
val EnergyGold = Color(0xFFFFD166)
val DangerRed = Color(0xFFE5484D)

private val FewsBoxDark = darkColorScheme(
    primary = Accent,
    onPrimary = Ink,
    secondary = ShieldBlue,
    onSecondary = Ink,
    background = Ink,
    onBackground = TextBright,
    surface = Panel,
    onSurface = TextBright,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = TextMuted,
    error = DangerRed
)

@Composable
fun FewsBoxTheme(content: @Composable () -> Unit) {
    // isSystemInDarkTheme() is intentionally ignored for now — dark first.
    MaterialTheme(
        colorScheme = FewsBoxDark,
        content = content
    )
}
