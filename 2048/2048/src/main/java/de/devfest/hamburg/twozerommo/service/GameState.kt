package de.devfest.hamburg.twozerommo.service

import java.util.*

data class GameState(
    var grid: List<Int>? = null,
    var lastMove: Int = 0) {
}