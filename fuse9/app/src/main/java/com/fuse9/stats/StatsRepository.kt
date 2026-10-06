package com.fuse9.stats

import com.fuse9.game.GameMode
import com.fuse9.game.GameState
import com.fuse9.game.GameStatus
import com.fuse9.game.Scoring
import com.fuse9.game.Verdict
import com.fuse9.persistence.FuseJson
import com.fuse9.persistence.writeAtomically
import com.fuse9.puzzle.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.io.File
import java.time.LocalDate

@Serializable
data class TierStats(val solved: Int = 0, val bestMillis: Long = 0, val totalMillis: Long = 0) {
    val averageMillis: Long get() = if (solved == 0) 0 else totalMillis / solved
}

@Serializable
data class Stats(
    val played: Int = 0,
    val solved: Int = 0,
    val perfect: Int = 0,
    val totalMistakes: Int = 0,
    val sealsDefused: Int = 0,
    val tiers: Map<Difficulty, TierStats> = emptyMap(),
    val dailyStreak: Int = 0,
    val bestDailyStreak: Int = 0,
    val lastDaily: String? = null,
    val dailiesDone: List<String> = emptyList(),
    val tutorialDone: Boolean = false,
) {
    val hardestSolved: Difficulty? get() = tiers.filterValues { it.solved > 0 }.keys.maxByOrNull { it.ordinal }
}

class StatsRepository(dir: File) {
    private val file = File(dir, "stats.json")
    private val _stats = MutableStateFlow(load())
    val stats: StateFlow<Stats> = _stats

    private fun load(): Stats = runCatching {
        if (file.exists()) FuseJson.decodeFromString(Stats.serializer(), file.readText()) else Stats()
    }.getOrDefault(Stats())

    @Synchronized
    private fun update(transform: (Stats) -> Stats) {
        val next = transform(_stats.value)
        _stats.value = next
        runCatching { writeAtomically(file, FuseJson.encodeToString(Stats.serializer(), next)) }
    }

    fun markTutorialDone() = update { it.copy(tutorialDone = true) }

    fun record(state: GameState, today: LocalDate = LocalDate.now()) = update { s ->
        val summary = Scoring.summarize(state)
        val won = state.status == GameStatus.WON
        var next = s.copy(
            played = s.played + 1,
            solved = s.solved + if (won) 1 else 0,
            perfect = s.perfect + if (summary.verdict == Verdict.CLEAN) 1 else 0,
            totalMistakes = s.totalMistakes + state.mistakes,
            sealsDefused = s.sealsDefused + state.stats.sealsDefused,
        )
        if (won) {
            val d = state.puzzle.difficulty
            val t = s.tiers[d] ?: TierStats()
            val best = if (t.bestMillis == 0L) state.elapsedMillis else minOf(t.bestMillis, state.elapsedMillis)
            next = next.copy(tiers = s.tiers + (d to TierStats(t.solved + 1, best, t.totalMillis + state.elapsedMillis)))
        }
        if (state.mode == GameMode.DAILY && won) {
            val key = today.toString()
            if (key !in s.dailiesDone) {
                val continues = s.lastDaily == today.minusDays(1).toString()
                val streak = if (continues) s.dailyStreak + 1 else 1
                next = next.copy(
                    dailyStreak = streak, bestDailyStreak = maxOf(streak, s.bestDailyStreak), lastDaily = key,
                    dailiesDone = (s.dailiesDone + key).takeLast(400),
                )
            }
        }
        next
    }

    /** The streak shown today: it lapses once a day is skipped. */
    fun currentStreak(today: LocalDate = LocalDate.now()): Int {
        val s = _stats.value
        val last = s.lastDaily ?: return 0
        return if (last == today.toString() || last == today.minusDays(1).toString()) s.dailyStreak else 0
    }
}
