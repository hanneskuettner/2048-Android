package de.devfest.hamburg.twozerommo.service

import java.util.*

data class Move(
    val gameState: Array<IntArray>,
    val direction: Int,
    val additionalScore: Int,
    val index: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Move) return false

        if (!Arrays.equals(gameState, other.gameState)) return false
        if (direction != other.direction) return false
        if (additionalScore != other.additionalScore) return false
        if (index != other.index) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(gameState)
        result = 31 * result + direction
        result = 31 * result + additionalScore
        result = 31 * result + index
        return result
    }
}