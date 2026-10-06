package com.fuse9.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fuse9.BuildConfig
import com.fuse9.settings.CountStyle
import com.fuse9.settings.Settings
import com.fuse9.settings.ThemeChoice
import com.fuse9.ui.common.ScreenHeader
import com.fuse9.ui.common.FuseSlider
import com.fuse9.ui.common.Segmented
import com.fuse9.ui.common.Toggle
import com.fuse9.ui.theme.FuseText
import com.fuse9.ui.theme.LocalPalette

@Composable
fun SettingsScreen(settings: Settings, onChange: ((Settings) -> Settings) -> Unit, onBack: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(p.page).statusBarsPadding().navigationBarsPadding()) {
        ScreenHeader("Settings", onBack)
        Column(Modifier.padding(horizontal = 28.dp).verticalScroll(rememberScrollState())) {
            Section("Sound & touch")
            Toggle("Sound", settings.sound) { v -> onChange { it.copy(sound = v) } }
            if (settings.sound) {
                FuseSlider(settings.sfxVolume, label = "Sound volume") { v -> onChange { it.copy(sfxVolume = v) } }
            }
            Toggle("Haptics", settings.haptics) { v -> onChange { it.copy(haptics = v) } }

            Section("Look")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Theme", style = FuseText.Body, color = p.ink, modifier = Modifier.weight(1f))
                Segmented(listOf("System", "Paper", "Graphite"), settings.theme.ordinal) { i -> onChange { it.copy(theme = ThemeChoice.entries[i]) } }
            }
            Toggle("High contrast", settings.highContrast) { v -> onChange { it.copy(highContrast = v) } }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Seal counts", style = FuseText.Body, color = p.ink)
                    Text("How open cells show nearby seals", style = FuseText.Small, color = p.inkSoft)
                }
                Segmented(listOf("Dots", "Numbers"), settings.countStyle.ordinal) { i -> onChange { it.copy(countStyle = CountStyle.entries[i]) } }
            }

            Section("Play")
            Toggle("Clear notes automatically", settings.autoCleanNotes, "Remove a digit from notes when it is placed nearby") { v -> onChange { it.copy(autoCleanNotes = v) } }
            Toggle("Highlight matching digits", settings.highlightSameDigit) { v -> onChange { it.copy(highlightSameDigit = v) } }
            Toggle("Show timer", settings.showTimer) { v -> onChange { it.copy(showTimer = v) } }

            Spacer(Modifier.height(28.dp))
            Text("FUSE9 ${BuildConfig.VERSION_NAME}", style = FuseText.Small, color = p.inkFaint)
            Text("Typefaces: Manrope, Fraunces (SIL Open Font License)", style = FuseText.Small, color = p.inkFaint)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    val p = LocalPalette.current
    Spacer(Modifier.height(18.dp))
    Text(title.uppercase(), style = FuseText.Label, color = p.inkSoft)
    Spacer(Modifier.height(4.dp))
}
