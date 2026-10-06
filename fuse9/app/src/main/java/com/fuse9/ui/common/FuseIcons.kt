package com.fuse9.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * FUSE9's own icon set. One grammar for all of them: 24-unit grid, 1.6 stroke, round caps
 * and joins, 2-unit corner radius, geometry kept on whole or half units.
 */
object FuseIcons {
    private const val STROKE = 1.6f

    private fun icon(name: String, vararg paths: PathBuilder.() -> Unit, fills: List<PathBuilder.() -> Unit> = emptyList()): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (p in paths) b.path(
            stroke = SolidColor(Color.Black), strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = p,
        )
        for (f in fills) b.path(fill = SolidColor(Color.Black), pathBuilder = f)
        return b.build()
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    val Back = icon("back", { moveTo(14.5f, 6f); lineTo(8.5f, 12f); lineTo(14.5f, 18f) })

    val Undo = icon(
        "undo",
        { moveTo(8f, 6.5f); lineTo(4.5f, 10f); lineTo(8f, 13.5f) },
        { moveTo(4.5f, 10f); lineTo(14f, 10f); arcTo(4.5f, 4.5f, 0f, false, true, 14f, 19f); lineTo(10f, 19f) },
    )

    val Erase = icon(
        "erase",
        { moveTo(9.5f, 18.5f); lineTo(5f, 14f); lineTo(13.5f, 5.5f); lineTo(19f, 11f); lineTo(11.5f, 18.5f); close() },
        { moveTo(8.5f, 10f); lineTo(14.5f, 16f) },
        { moveTo(11.5f, 18.5f); lineTo(19f, 18.5f) },
    )

    val Notes = icon(
        "notes",
        { moveTo(5f, 19f); lineTo(5.8f, 15.2f); lineTo(15.5f, 5.5f); lineTo(18.5f, 8.5f); lineTo(8.8f, 18.2f); close() },
        { moveTo(13.5f, 7.5f); lineTo(16.5f, 10.5f) },
    )

    /** The seal: a ring with four lock teeth around a solid core. */
    val Seal = icon(
        "seal",
        { circle(12f, 12f, 6.5f) },
        { moveTo(12f, 3f); lineTo(12f, 5.5f) },
        { moveTo(12f, 18.5f); lineTo(12f, 21f) },
        { moveTo(3f, 12f); lineTo(5.5f, 12f) },
        { moveTo(18.5f, 12f); lineTo(21f, 12f) },
        fills = listOf({ circle(12f, 12f, 2.2f) }),
    )

    val Hint = icon(
        "hint",
        { circle(10.5f, 10.5f, 5.5f) },
        { moveTo(14.5f, 14.5f); lineTo(19.5f, 19.5f) },
        fills = listOf({ circle(10.5f, 10.5f, 1.4f) }),
    )

    val Pause = icon("pause", { moveTo(9f, 6.5f); lineTo(9f, 17.5f) }, { moveTo(15f, 6.5f); lineTo(15f, 17.5f) })

    val Play = icon("play", { moveTo(8.5f, 6f); lineTo(18f, 12f); lineTo(8.5f, 18f); close() })

    val Sliders = icon(
        "settings",
        { moveTo(4.5f, 7f); lineTo(19.5f, 7f) }, { moveTo(4.5f, 12f); lineTo(19.5f, 12f) }, { moveTo(4.5f, 17f); lineTo(19.5f, 17f) },
        fills = listOf({ circle(9f, 7f, 2f) }, { circle(15.5f, 12f, 2f) }, { circle(7.5f, 17f, 2f) }),
    )

    val Stats = icon(
        "stats",
        { moveTo(6f, 19f); lineTo(6f, 13f) }, { moveTo(12f, 19f); lineTo(12f, 6f) }, { moveTo(18f, 19f); lineTo(18f, 10f) },
    )

    val Close = icon("close", { moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f) }, { moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f) })

    val Check = icon("check", { moveTo(5.5f, 12.5f); lineTo(10f, 17f); lineTo(18.5f, 7.5f) })

    val Bug = icon(
        "debug",
        { circle(12f, 13f, 5f) },
        { moveTo(12f, 8f); lineTo(12f, 18f) },
        { moveTo(4f, 13f); lineTo(7f, 13f) }, { moveTo(17f, 13f); lineTo(20f, 13f) },
        { moveTo(9.5f, 8.5f); lineTo(8f, 5.5f) }, { moveTo(14.5f, 8.5f); lineTo(16f, 5.5f) },
    )
}
