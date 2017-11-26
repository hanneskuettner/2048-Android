package de.devfest.hamburg.twozerommo.service

import java.util.*

data class Move(
    val gameState: GameState,
    val additionalScore: Int,
    val index: Int
) {
}