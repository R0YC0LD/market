package com.fuse9.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fuse9.settings.Settings
import com.fuse9.settings.ThemeChoice

object FuseText {
    val Title = TextStyle(fontFamily = FuseFonts.Serif, fontWeight = FontWeight(400), fontSize = 44.sp, letterSpacing = 1.sp)
    val Word = TextStyle(fontFamily = FuseFonts.Serif, fontWeight = FontWeight(400), fontSize = 40.sp)
    val Heading = TextStyle(fontFamily = FuseFonts.Serif, fontWeight = FontWeight(400), fontSize = 26.sp)
    val Item = TextStyle(fontFamily = FuseFonts.Sans, fontWeight = FontWeight.Medium, fontSize = 19.sp, letterSpacing = 0.2.sp)
    val Body = TextStyle(fontFamily = FuseFonts.Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
    val Small = TextStyle(fontFamily = FuseFonts.Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.4.sp)
    val Label = TextStyle(fontFamily = FuseFonts.Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.2.sp)
    val Numeric = TextStyle(fontFamily = FuseFonts.Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.5.sp)
}

@Composable
fun FuseTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    val base = if (dark) Palettes.Graphite else Palettes.Paper
    val palette = if (settings.highContrast) Palettes.highContrast(base) else base
    val scheme = if (dark) darkColorScheme(
        primary = palette.accent, background = palette.page, surface = palette.board,
        onBackground = palette.ink, onSurface = palette.ink, onPrimary = palette.page,
    ) else lightColorScheme(
        primary = palette.accent, background = palette.page, surface = palette.board,
        onBackground = palette.ink, onSurface = palette.ink, onPrimary = palette.page,
    )
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = Typography(bodyLarge = FuseText.Body), content = content)
    }
}
