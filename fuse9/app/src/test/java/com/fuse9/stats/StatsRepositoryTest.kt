package com.fuse9.stats

import com.fuse9.game.Fixtures
import com.fuse9.game.GameMode
import com.fuse9.game.GameStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class StatsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun recordsSolvesAndBestTimes() {
        val repo = StatsRepository(tmp.root)
        val won = Fixtures.game().copy(status = GameStatus.WON, elapsedMillis = 300_000)
        repo.record(won)
        repo.record(won.copy(elapsedMillis = 200_000, mistakes = 1))
        repo.record(won.copy(status = GameStatus.LOST))
        val s = repo.stats.value
        assertEquals(3, s.played)
        assertEquals(2, s.solved)
        assertEquals(1, s.perfect)
        val tier = s.tiers[won.puzzle.difficulty]!!
        assertEquals(200_000, tier.bestMillis)
        assertEquals(250_000, tier.averageMillis)
        // Survives a reload.
        assertEquals(s, StatsRepository(tmp.root).stats.value)
    }

    @Test
    fun dailyStreakCountsConsecutiveDaysOnce() {
        val repo = StatsRepository(tmp.root)
        val daily = Fixtures.game(GameMode.DAILY).copy(status = GameStatus.WON)
        val d1 = LocalDate.of(2026, 10, 1)
        repo.record(daily, d1)
        repo.record(daily, d1) // same day twice counts once
        repo.record(daily, d1.plusDays(1))
        assertEquals(2, repo.stats.value.dailyStreak)
        assertEquals(2, repo.currentStreak(d1.plusDays(2)))
        assertEquals(0, repo.currentStreak(d1.plusDays(3)))
        repo.record(daily, d1.plusDays(5))
        assertEquals(1, repo.stats.value.dailyStreak)
        assertEquals(2, repo.stats.value.bestDailyStreak)
    }
}
