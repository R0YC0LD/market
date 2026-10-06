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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.fuse9.game.GameMode
import com.fuse9.puzzle.Difficulty
import com.fuse9.ui.common.ScreenHeader
import com.fuse9.ui.common.quietClick
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette

@Composable
fun ModesScreen(onBack: () -> Unit, onStart: (GameMode, Difficulty) -> Unit) {
    val p = LocalPalette.current
    var open by rememberSaveable { mutableStateOf<GameMode?>(null) }
    Column(Modifier.fillMaxSize().background(p.page).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Modes", onBack)
        Column(Modifier.padding(horizontal = 28.dp)) {
            for (mode in GameMode.entries.filter { it != GameMode.DAILY }) {
                Column(
                    Modifier.fillMaxWidth().alpha(if (mode.available) 1f else 0.38f)
                        .quietClick(enabled = mode.available, label = mode.label) { open = if (open == mode) null else mode }
                        .padding(vertical = 14.dp),
                ) {
                    Text(mode.label, style = FuseText.Item, color = p.ink)
                    Spacer(Modifier.height(2.dp))
                    Text(mode.description, style = FuseText.Small, color = p.inkSoft)
                    if (open == mode) {
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (d in Difficulty.entries) Text(
                                d.label, style = FuseText.Small, color = p.accent,
                                modifier = Modifier.quietClick(label = "${mode.label} ${d.label}") { onStart(mode, d) }.padding(vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
