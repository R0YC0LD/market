package com.fuse9.ui.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fuse9.game.GameState
import com.fuse9.puzzle.Difficulty
import com.fuse9.ui.common.Hairline
import com.fuse9.ui.common.MenuItem
import com.fuse9.ui.common.quietClick
import com.fuse9.ui.game.formatTime
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette

data class MenuInfo(val saved: GameState?, val dailyDone: Boolean, val dailyLabel: String, val streak: Int)

@Composable
fun MenuScreen(
    info: MenuInfo,
    onContinue: () -> Unit,
    onNew: (Difficulty) -> Unit,
    onDaily: () -> Unit,
    onModes: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onRules: () -> Unit,
) {
    val p = LocalPalette.current
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(p.page).statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.9f))
        LogoMark(1f, Modifier.size(52.dp))
        Spacer(Modifier.height(18.dp))
        Text("FUSE9", style = FuseText.Title, color = p.ink)
        Spacer(Modifier.height(4.dp))
        Text("Sudoku, with nine sealed cells", style = FuseText.Small, color = p.inkSoft)
        Spacer(Modifier.weight(0.7f))

        info.saved?.let { s ->
            MenuItem("Continue", "${s.puzzle.difficulty.label} · ${formatTime(s.elapsedMillis)} · ${s.sealsFound}/9 seals", emphasis = true, onClick = onContinue)
        }
        MenuItem("New Puzzle", emphasis = info.saved == null, onClick = { picking = !picking })
        AnimatedVisibility(picking, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (d in Difficulty.entries) {
                    Text(
                        d.label, style = FuseText.Small, color = p.accent,
                        modifier = Modifier.quietClick(label = "New ${d.label} puzzle") { picking = false; onNew(d) }.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp)); Hairline(); Spacer(Modifier.height(10.dp))
        MenuItem("Daily", if (info.dailyDone) "Done today${if (info.streak > 0) " · streak ${info.streak}" else ""}" else info.dailyLabel, onClick = onDaily)
        MenuItem("Modes", onClick = onModes)
        Spacer(Modifier.height(10.dp)); Hairline(); Spacer(Modifier.height(10.dp))
        MenuItem("Stats", onClick = onStats)
        MenuItem("Settings", onClick = onSettings)
        Spacer(Modifier.weight(1f))
        Text("How to play", style = FuseText.Small, color = p.inkSoft, modifier = Modifier.quietClick(label = "How to play", onClick = onRules).padding(12.dp))
        Spacer(Modifier.height(8.dp))
    }
}
