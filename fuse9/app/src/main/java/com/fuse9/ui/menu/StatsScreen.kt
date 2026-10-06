package com.fuse9.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fuse9.puzzle.Difficulty
import com.fuse9.stats.Stats
import com.fuse9.ui.common.Hairline
import com.fuse9.ui.common.ScreenHeader
import com.fuse9.ui.game.formatTime
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette
import com.fuse9.ui.i18n.LocalStrings

@Composable
fun StatsScreen(stats: Stats, streak: Int, onBack: () -> Unit) {
    val p = LocalPalette.current
    val t = LocalStrings.current
    Column(Modifier.fillMaxSize().background(p.page).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader(t.stats, onBack)
        Column(Modifier.padding(horizontal = 28.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Figure(stats.solved.toString(), t.solvedLabel)
                Figure(stats.perfect.toString(), t.cleanLabel)
                Figure(streak.toString(), t.dailyStreakLabel)
            }
            Hairline(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Line(t.sealsDefused, stats.sealsDefused.toString())
            Line(t.totalStrikes, stats.totalMistakes.toString())
            Line(t.bestDailyStreak, stats.bestDailyStreak.toString())
            Line(t.hardestSolved, stats.hardestSolved?.let { t.difficulty(it) } ?: "—")
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("", Modifier.weight(1f))
                Text(t.upper(t.colSolved), style = FuseText.Label, color = p.inkSoft, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                Text(t.upper(t.colBest), style = FuseText.Label, color = p.inkSoft, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                Text(t.upper(t.colAverage), style = FuseText.Label, color = p.inkSoft, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
            }
            for (d in Difficulty.entries) {
                val tier = stats.tiers[d]
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                    Text(t.difficulty(d), style = FuseText.Body, color = p.ink, modifier = Modifier.weight(1f))
                    Text((tier?.solved ?: 0).toString(), style = FuseText.Body, color = p.ink, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                    Text(tier?.takeIf { it.solved > 0 }?.let { formatTime(it.bestMillis) } ?: "—", style = FuseText.Body, color = p.inkSoft, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                    Text(tier?.takeIf { it.solved > 0 }?.let { formatTime(it.averageMillis) } ?: "—", style = FuseText.Body, color = p.inkSoft, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun Figure(value: String, label: String) {
    val p = LocalPalette.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = FuseText.Word, color = p.ink)
        Text(LocalStrings.current.upper(label), style = FuseText.Label, color = p.inkSoft)
    }
}

@Composable
private fun Line(label: String, value: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, style = FuseText.Body, color = p.inkSoft, modifier = Modifier.weight(1f))
        Text(value, style = FuseText.Body, color = p.ink)
    }
}
