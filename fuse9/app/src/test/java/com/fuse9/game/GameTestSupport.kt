package com.fuse9.game

import com.fuse9.puzzle.Difficulty
import com.fuse9.puzzle.Puzzle
import com.fuse9.puzzle.PuzzleGenerator

object Fixtures {
    val puzzle: Puzzle by lazy { PuzzleGenerator.generate(2024, Difficulty.MEDIUM) }
    fun game(mode: GameMode = GameMode.CLASSIC) = GameState.new(puzzle, mode)
    fun firstHidden(state: GameState, seal: Boolean): Int =
        (0 until 81).first { state.cells[it].status == CellStatus.HIDDEN && state.truth.isSeal[it] == seal }
}

fun MoveEngine.play(state: GameState, vararg actions: Action): Outcome {
    var s = state
    val events = ArrayList<GameEvent>()
    for (a in actions) { val o = reduce(s, a); s = o.state; events += o.events }
    return Outcome(s, events)
}
