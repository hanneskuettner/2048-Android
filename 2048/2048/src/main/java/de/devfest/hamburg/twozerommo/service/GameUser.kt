package de.devfest.hamburg.twozerommo.service

data class GameUser (
    val uid: String,
    var active: Boolean,
    var name: String,
    var score: Int
)