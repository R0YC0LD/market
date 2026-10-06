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

@Composable
fun StatsScreen(stats: Stats, streak: Int, onBack: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(p.page).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Stats", onBack)
        Column(Modifier.padding(horizontal = 28.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Figure(stats.solved.toString(), "solved")
                Figure(stats.perfect.toString(), "clean")
                Figure(streak.toString(), "daily streak")
            }
            Hairline(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Line("Seals defused", stats.sealsDefused.toString())
            Line("Total strikes", stats.totalMistakes.toString())
            Line("Best daily streak", stats.bestDailyStreak.toString())
            Line("Hardest solved", stats.hardestSolved?.label ?: "—")
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text("", Modifier.weight(1f))
                Text("SOLVED", style = FuseText.Label, color = p.inkSoft, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                Text("BEST", style = FuseText.Label, color = p.inkSoft, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                Text("AVERAGE", style = FuseText.Label, color = p.inkSoft, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
            }
            for (d in Difficulty.entries) {
                val t = stats.tiers[d]
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                    Text(d.label, style = FuseText.Body, color = p.ink, modifier = Modifier.weight(1f))
                    Text((t?.solved ?: 0).toString(), style = FuseText.Body, color = p.ink, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                    Text(t?.takeIf { it.solved > 0 }?.let { formatTime(it.bestMillis) } ?: "—", style = FuseText.Body, color = p.inkSoft, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                    Text(t?.takeIf { it.solved > 0 }?.let { formatTime(it.averageMillis) } ?: "—", style = FuseText.Body, color = p.inkSoft, modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
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
        Text(label.uppercase(), style = FuseText.Label, color = p.inkSoft)
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
