package com.fuse9.puzzle

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A finished puzzle. Stores its full answer so a saved game never depends on the
 * generator staying byte-identical across versions.
 */
@Serializable
data class Puzzle(
    val seed: Long,
    val difficulty: Difficulty,
    val generatorVersion: Int,
    val solution: List<Int>,
    val seals: List<Int>,
    val givens: List<Int>,
    val score: Int = 0,
    val maxLevel: Int = 0,
    val zeroCascade: Boolean = false,
    val generationMillis: Long = 0,
    val solverSteps: Int = 0,
    val usage: Map<Technique, Int> = emptyMap(),
) {
    @Transient val truth: Truth = Truth(solution.toIntArray(), seals.toIntArray())
    @Transient val rules: Rules = Rules(zeroCascade)
    val id: String get() = "v$generatorVersion-${difficulty.name.lowercase()}-${java.lang.Long.toHexString(seed)}"
}
