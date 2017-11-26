package de.devfest.hamburg.twozerommo.service

data class GameUser (
    val uid: String="",
    var active: Boolean=false,
    var name: String="",
    var score: Int=0
)