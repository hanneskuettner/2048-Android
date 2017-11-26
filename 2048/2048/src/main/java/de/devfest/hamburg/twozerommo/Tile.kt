package de.devfest.hamburg.twozerommo

class Tile : Cell {
    val value: Int
    var mergedFrom: Array<Tile>? = null

    constructor(x: Int, y: Int, value: Int) : super(x, y) {
        this.value = value
    }

    constructor(cell: Cell, value: Int) : super(cell.x, cell.y) {
        this.value = value
    }

    fun updatePosition(cell: Cell) {
        this.x = cell.x
        this.y = cell.y
    }
}
