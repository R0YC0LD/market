package com.fuse9.game

import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.Puzzle
import com.fuse9.puzzle.PuzzleGenerator
import com.fuse9.puzzle.Rng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Where boards come from. Generation runs off the main thread, and the next board of the
 * tier just played is prepared in the background so "Next" is instant.
 */
class PuzzleSource(private val scope: CoroutineScope) {
    private val prefetched = HashMap<Difficulty, Deferred<Puzzle>>()

    suspend fun next(difficulty: Difficulty): Puzzle {
        val ready = synchronized(prefetched) { prefetched.remove(difficulty) }
        val puzzle = ready?.await() ?: generate(freshSeed(), difficulty)
        prefetch(difficulty)
        return puzzle
    }

    fun prefetch(difficulty: Difficulty) = synchronized(prefetched) {
        if (difficulty !in prefetched) prefetched[difficulty] = scope.async(Dispatchers.Default) { PuzzleGenerator.generate(freshSeed(), difficulty) }
    }

    suspend fun generate(seed: Long, difficulty: Difficulty): Puzzle =
        withContext(Dispatchers.Default) { PuzzleGenerator.generate(seed, difficulty) }

    suspend fun daily(date: LocalDate = LocalDate.now()): Puzzle = generate(dailySeed(date), dailyDifficulty(date))

    /** The first board a new player sees: fixed, gentle, chosen for a clear opening. */
    suspend fun tutorial(): Puzzle = generate(TUTORIAL_SEED, Difficulty.EASY)

    private fun freshSeed(): Long = System.nanoTime() xor (Math.random() * Long.MAX_VALUE).toLong()

    companion object {
        const val TUTORIAL_SEED = 0xF05E9L

        fun dailySeed(date: LocalDate): Long = Rng.seedOf("fuse9-daily-$date")

        /** The week climbs: gentle on Monday, Master on Sunday. */
        fun dailyDifficulty(date: LocalDate): Difficulty = when (date.dayOfWeek.value) {
            1 -> Difficulty.EASY
            2, 3 -> Difficulty.MEDIUM
            4, 5 -> Difficulty.HARD
            6 -> Difficulty.EXPERT
            else -> Difficulty.MASTER
        }
    }
}
