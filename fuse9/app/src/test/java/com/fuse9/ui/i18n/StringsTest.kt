package com.fuse9.ui.i18n

import com.fuse9.game.Action
import com.fuse9.game.GameMode
import com.fuse9.game.GameState
import com.fuse9.game.GameStatus
import com.fuse9.game.HintAction
import com.fuse9.game.HintEngine
import com.fuse9.game.Look
import com.fuse9.game.LookAt
import com.fuse9.game.MoveEngine
import com.fuse9.game.PuzzleSource
import com.fuse9.game.Reason
import com.fuse9.game.TutorialGuide
import com.fuse9.game.Verdict
import com.fuse9.game.Why
import com.fuse9.game.play
import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.PuzzleGenerator
import com.fuse9.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class StringsTest {
    private val both = listOf(English, Turkish)

    /** The caption under the board holds three short lines; keep every generated line inside it. */
    private val captionLimit = 150

    private fun clean(s: String, what: String) {
        assertTrue("$what is blank", s.isNotBlank())
        assertTrue("$what has a template leak: $s", listOf("null", "{", "}", "$", "-1").none { it in s })
    }

    @Test
    fun everyEnumIsWordedInBothLanguages() {
        for (t in both) {
            Difficulty.entries.forEach { clean(t.difficulty(it), "difficulty $it") }
            GameMode.entries.forEach { clean(t.mode(it), "mode $it"); clean(t.modeDescription(it), "mode desc $it") }
            Verdict.entries.forEach { clean(t.verdict(it), "verdict $it"); clean(t.verdictLine(it), "verdict line $it") }
            for (why in Why.entries) clean(t.reason(Reason(why, unit = 3, cell = 40, digit = 7)), "reason $why")
            for (at in LookAt.entries) clean(t.look(Look(at, unit = 20, cell = 40)), "look $at")
            for (k in TutorialGuide.Kind.entries) {
                clean(t.guide(TutorialGuide.Line(k, digit = 5, reasons = listOf(Reason(Why.SAFE)))), "guide $k")
            }
            assertEquals(4, t.rules.size)
            assertEquals(3, t.themeOptions.size)
            assertEquals(AppLanguage.entries.size, t.languageOptions.size)
        }
    }

    @Test
    fun turkishIsActuallyTurkish() {
        assertNotEquals(English.newPuzzle, Turkish.newPuzzle)
        assertNotEquals(English.reason(Reason(Why.ONLY_FIT, digit = 3)), Turkish.reason(Reason(Why.ONLY_FIT, digit = 3)))
        // Locale-aware upper case: the dotted capital İ.
        assertEquals("İLK TAHTA", Turkish.upper(Turkish.firstBoard))
        assertEquals("İPUCU", Turkish.upper(Turkish.hint))
        assertEquals("FIRST BOARD", English.upper(English.firstBoard))
    }

    @Test
    fun turkishUnitEndings() {
        assertEquals("4. satıra bak.", Turkish.look(Look(LookAt.UNIT, unit = 3)))
        assertEquals("7. sütuna bak.", Turkish.look(Look(LookAt.UNIT, unit = 15)))
        assertEquals("5. kutuya bak.", Turkish.look(Look(LookAt.UNIT, unit = 22)))
        assertEquals("5. kutunun çevresine bak.", Turkish.look(Look(LookAt.AROUND_BOX, unit = 22)))
        assertTrue(Turkish.reason(Reason(Why.HIDDEN_SINGLE, unit = 9, digit = 7)).startsWith("1. sütunda 7 rakamının"))
    }

    @Test
    fun languageChoice() {
        assertEquals(Turkish, stringsFor(AppLanguage.TURKISH, Locale.ENGLISH))
        assertEquals(English, stringsFor(AppLanguage.ENGLISH, Locale.forLanguageTag("tr-TR")))
        assertEquals(Turkish, stringsFor(AppLanguage.SYSTEM, Locale.forLanguageTag("tr-TR")))
        assertEquals(English, stringsFor(AppLanguage.SYSTEM, Locale.GERMAN))
    }

    @Test
    fun realHintsFitTheCaptionInBothLanguages() {
        val engine = MoveEngine()
        for (d in Difficulty.entries) for (seed in listOf(3L, 77L)) {
            var s = GameState.new(PuzzleGenerator.generate(seed, d), GameMode.ZEN)
            var guard = 0
            while (s.status == GameStatus.PLAYING && guard++ < 120) {
                val h = HintEngine.find(s)!!
                for (t in both) {
                    val look = t.look(h.look)
                    val why = t.reasons(h.reasons)
                    clean(look, "look"); clean(why, "why")
                    assertTrue("${t.locale} too long (${why.length}): $why", why.length <= captionLimit)
                }
                s = if (h.action == HintAction.SEAL) engine.play(s, Action.SealAt(h.cell)).state
                else engine.play(s, Action.Select(h.cell), Action.Digit(h.digit)).state
            }
        }
    }

    @Test
    fun tutorialLinesFitTheCaptionInBothLanguages() {
        val engine = MoveEngine()
        var s = GameState.new(PuzzleGenerator.generate(PuzzleSource.TUTORIAL_SEED, Difficulty.EASY), GameMode.ZEN, tutorial = true)
        val guide = TutorialGuide()
        var guard = 0
        while (!guide.finished && guard++ < 80) {
            guide.line(s)?.let { line -> for (t in both) assertTrue("${t.locale}: ${t.guide(line)}", t.guide(line).length <= captionLimit) }
            if (guide.step == TutorialGuide.Step.LOOK) { val o = engine.reduce(s, Action.Select(s.puzzle.givens.first())); s = o.state; o.events.forEach(guide::onEvent); continue }
            val h = HintEngine.find(s, if (guide.step == TutorialGuide.Step.MORE) HintAction.SEAL else null)!!
            val o = if (h.action == HintAction.SEAL) engine.play(s, Action.SealAt(h.cell)) else engine.play(s, Action.Select(h.cell), Action.Digit(h.digit))
            s = o.state
            o.events.forEach(guide::onEvent)
        }
        assertTrue(guide.finished)
    }
}
