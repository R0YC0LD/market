package com.fuse9.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paper and graphite, one ink accent, a brick for danger and a warm brass for success.
 * Dark is rebalanced rather than inverted: the board stays a touch lighter than the page so
 * hidden tiles, counts and the accent keep their separation.
 */
@Immutable
data class FusePalette(
    val page: Color,
    val board: Color,
    val tile: Color,
    val tileEdge: Color,
    val tileSafe: Color,
    val lineThin: Color,
    val lineThick: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val peerTint: Color,
    val danger: Color,
    val dangerSoft: Color,
    val success: Color,
    val seal: Color,
    val isDark: Boolean,
)

object Palettes {
    val Paper = FusePalette(
        page = Color(0xFFF2EEE5),
        board = Color(0xFFFBF8F2),
        tile = Color(0xFFE9E3D7),
        tileEdge = Color(0xFFD8D0C1),
        tileSafe = Color(0xFFF1ECE2),
        lineThin = Color(0xFFDCD5C8),
        lineThick = Color(0xFF6E685E),
        ink = Color(0xFF26251F),
        inkSoft = Color(0xFF6B665C),
        inkFaint = Color(0xFFA39D91),
        accent = Color(0xFF2F4E7A),
        accentSoft = Color(0x1F2F4E7A),
        peerTint = Color(0x0D26251F),
        danger = Color(0xFFA24B3C),
        dangerSoft = Color(0x26A24B3C),
        success = Color(0xFFA9853C),
        seal = Color(0xFF3B3A34),
        isDark = false,
    )

    val Graphite = FusePalette(
        page = Color(0xFF181715),
        board = Color(0xFF211F1C),
        tile = Color(0xFF2D2A26),
        tileEdge = Color(0xFF15140F),
        tileSafe = Color(0xFF26241F),
        lineThin = Color(0xFF34312C),
        lineThick = Color(0xFF8A8376),
        ink = Color(0xFFEDE7DA),
        inkSoft = Color(0xFFADA697),
        inkFaint = Color(0xFF6E685D),
        accent = Color(0xFF9CB5DA),
        accentSoft = Color(0x269CB5DA),
        peerTint = Color(0x0FEDE7DA),
        danger = Color(0xFFD9826F),
        dangerSoft = Color(0x2ED9826F),
        success = Color(0xFFD6B56E),
        seal = Color(0xFFD9D2C3),
        isDark = true,
    )

    fun highContrast(base: FusePalette): FusePalette = if (base.isDark) base.copy(
        page = Color(0xFF000000), board = Color(0xFF0C0C0B), tile = Color(0xFF2E2E2B), tileSafe = Color(0xFF1A1A18),
        lineThin = Color(0xFF5A5A55), lineThick = Color(0xFFFFFFFF), ink = Color(0xFFFFFFFF), inkSoft = Color(0xFFE0E0DA),
        inkFaint = Color(0xFFB0B0A8), accent = Color(0xFFB9D0FF), danger = Color(0xFFFF9C88), seal = Color(0xFFFFFFFF),
    ) else base.copy(
        page = Color(0xFFFFFFFF), board = Color(0xFFFFFFFF), tile = Color(0xFFD9D4C8), tileSafe = Color(0xFFF0EDE6),
        lineThin = Color(0xFFA8A296), lineThick = Color(0xFF000000), ink = Color(0xFF000000), inkSoft = Color(0xFF33312C),
        inkFaint = Color(0xFF6B675E), accent = Color(0xFF173A70), danger = Color(0xFF8A2414), seal = Color(0xFF000000),
    )
}

val LocalPalette = staticCompositionLocalOf { Palettes.Paper }
