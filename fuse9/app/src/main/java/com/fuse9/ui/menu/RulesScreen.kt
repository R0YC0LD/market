package com.fuse9.ui.menu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fuse9.ui.common.ScreenHeader
import com.fuse9.ui.theme.FusePalette
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette
import com.fuse9.ui.i18n.LocalStrings

/** Four rules, each with a tiny drawing. That's the whole manual. */
@Composable
fun RulesScreen(onBack: () -> Unit) {
    val p = LocalPalette.current
    val t = LocalStrings.current
    val drawings: List<DrawScope.(FusePalette) -> Unit> = listOf({ grid(it) }, { seals(it) }, { dots(it) }, { link(it) })
    Column(Modifier.fillMaxSize().background(p.page).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(t.howToPlay, onBack)
        Column(Modifier.padding(horizontal = 28.dp).verticalScroll(rememberScrollState())) {
            t.rules.forEachIndexed { i, (title, body) -> Rule(title, body, drawings[i]) }
            Spacer(Modifier.height(10.dp))
            Text(t.rulesFooter, style = FuseText.Body, color = p.inkSoft)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Rule(title: String, body: String, draw: DrawScope.(FusePalette) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(64.dp)) { draw(p) }
        Spacer(Modifier.width(18.dp))
        Column {
            Text(title, style = FuseText.Item, color = p.ink)
            Spacer(Modifier.height(3.dp))
            Text(body, style = FuseText.Small, color = p.inkSoft)
        }
    }
}

private fun DrawScope.frame(p: FusePalette) {
    val s = size.minDimension
    val c = s / 3
    for (i in 1..2) {
        drawLine(p.lineThin, Offset(i * c, 0f), Offset(i * c, s), 1.dp.toPx())
        drawLine(p.lineThin, Offset(0f, i * c), Offset(s, i * c), 1.dp.toPx())
    }
    drawRect(p.lineThick, style = Stroke(1.4.dp.toPx()))
}

private fun DrawScope.ring(p: FusePalette, center: Offset, r: Float) {
    drawCircle(p.seal, r, center, style = Stroke(1.3.dp.toPx()))
    drawCircle(p.seal, r * 0.35f, center)
}

private fun DrawScope.grid(p: FusePalette) {
    frame(p)
    val c = size.minDimension / 3
    for (i in 0 until 9) drawCircle(p.inkSoft, c * 0.08f, Offset((i % 3) * c + c / 2, (i / 3) * c + c / 2))
}

private fun DrawScope.seals(p: FusePalette) {
    frame(p)
    val c = size.minDimension / 3
    listOf(0 to 1, 1 to 2, 2 to 0).forEach { (r, k) -> ring(p, Offset(k * c + c / 2, r * c + c / 2), c * 0.26f) }
}

private fun DrawScope.dots(p: FusePalette) {
    frame(p)
    val c = size.minDimension / 3
    ring(p, Offset(c / 2, c / 2), c * 0.26f)
    ring(p, Offset(2 * c + c / 2, 2 * c + c / 2), c * 0.26f)
    val center = Offset(1.5f * c, 1.5f * c)
    drawCircle(p.inkSoft, c * 0.06f, center + Offset(-c * 0.12f, c * 0.3f))
    drawCircle(p.inkSoft, c * 0.06f, center + Offset(c * 0.12f, c * 0.3f))
}

private fun DrawScope.link(p: FusePalette) {
    frame(p)
    val c = size.minDimension / 3
    val seal = Offset(c / 2, c / 2)
    drawCircle(p.seal, c * 0.3f, seal, style = Stroke(1.3.dp.toPx()))
    drawCircle(p.accent, c * 0.1f, seal)
    drawCircle(p.accentSoft, c * 0.34f, Offset(2.5f * c, 1.5f * c))
    drawCircle(p.accent, c * 0.1f, Offset(2.5f * c, 1.5f * c))
}
