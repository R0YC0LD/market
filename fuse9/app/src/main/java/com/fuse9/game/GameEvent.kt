package com.fuse9.game

/** Things that just happened. The UI turns these into motion, sound and haptics. */
sealed interface GameEvent {
    data class Selected(val cell: Int) : GameEvent
    data class Placed(val cell: Int, val digit: Int) : GameEvent
    data class WrongDigit(val cell: Int, val digit: Int) : GameEvent
    data class Defused(val cell: Int, val digit: Int) : GameEvent
    data class Tripped(val cell: Int, val digit: Int) : GameEvent
    data class WrongSeal(val cell: Int) : GameEvent
    data class NoteToggled(val cell: Int, val digit: Int, val added: Boolean) : GameEvent
    data class SuspectToggled(val cell: Int, val on: Boolean) : GameEvent
    /** Cells newly proven safe beside a zero, in domino order. */
    data class Ripple(val origin: Int, val cells: List<Int>) : GameEvent
    data class UnitCompleted(val unit: Int, val origin: Int) : GameEvent
    data class Undone(val changed: List<Int>) : GameEvent
    data class ModeChanged(val mode: InputMode) : GameEvent
    data object Won : GameEvent
    data object Lost : GameEvent
    data object Rejected : GameEvent
}
