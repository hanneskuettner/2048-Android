package de.devfest.hamburg.twozerommo

import java.util.ArrayList

class Grid(sizeX: Int, sizeY: Int) {

    val field: Array<Array<Tile?>>
    val undoField: Array<Array<Tile?>>
    private val bufferField: Array<Array<Tile?>>

    private val availableCells: MutableList<Cell>
        get() {
            val availableCells = ArrayList<Cell>()
            for (xx in this.field.indices) {
                for (yy in 0 until this.field[0].size) {
                    if (this.field[xx][yy] == null) {
                        availableCells.add(Cell(xx, yy))
                    }
                }
            }
            return availableCells
        }

    val isCellsAvailable: Boolean
        get() = availableCells.size >= 1

    init {
        field = Array(sizeX) { arrayOfNulls<Tile>(sizeY) }
        undoField = Array(sizeX) { arrayOfNulls<Tile>(sizeY) }
        bufferField = Array(sizeX) { arrayOfNulls<Tile>(sizeY) }
        clearGrid()
        clearUndoGrid()
    }

    fun randomAvailableCell(): Cell? {
        val availableCells = availableCells
        return if (availableCells.size >= 1) {
            availableCells[Math.floor(Math.random() * availableCells.size).toInt()]
        } else null
    }

    fun isCellAvailable(cell: Cell): Boolean {
        return !isCellOccupied(cell)
    }

    fun isCellOccupied(cell: Cell?): Boolean {
        return getCellContent(cell) != null
    }

    fun getCellContent(cell: Cell?): Tile? {
        return if (cell != null && isCellWithinBounds(cell)) {
            field[cell.x][cell.y]
        } else {
            null
        }
    }

    fun getCellContent(x: Int, y: Int): Tile? {
        return if (isCellWithinBounds(x, y)) {
            field[x][y]
        } else {
            null
        }
    }

    fun isCellWithinBounds(cell: Cell): Boolean {
        return (0 <= cell.x && cell.x < field.size
                && 0 <= cell.y && cell.y < field[0].size)
    }

    private fun isCellWithinBounds(x: Int, y: Int): Boolean {
        return (0 <= x && x < field.size
                && 0 <= y && y < field[0].size)
    }

    fun insertTile(tile: Tile) {
        field[tile.x][tile.y] = tile
    }

    fun removeTile(tile: Tile) {
        field[tile.x][tile.y] = null
    }

    fun saveTiles() {
        for (xx in bufferField.indices) {
            for (yy in 0 until bufferField[0].size) {
                if (bufferField[xx][yy] == null) {
                    undoField[xx][yy] = null
                } else {
                    undoField[xx][yy] = Tile(xx, yy, bufferField[xx][yy]!!.value)
                }
            }
        }
    }

    fun prepareSaveTiles() {
        for (xx in field.indices) {
            for (yy in 0 until field[0].size) {
                if (field[xx][yy] == null) {
                    bufferField[xx][yy] = null
                } else {
                    bufferField[xx][yy] = Tile(xx, yy, field[xx][yy]!!.value)
                }
            }
        }
    }

    fun revertTiles() {
        for (xx in undoField.indices) {
            for (yy in 0 until undoField[0].size) {
                if (undoField[xx][yy] == null) {
                    field[xx][yy] = null
                } else {
                    field[xx][yy] = Tile(xx, yy, undoField[xx][yy]!!.value)
                }
            }
        }
    }

    fun clearGrid() {
        for (xx in field.indices) {
            for (yy in 0 until field[0].size) {
                field[xx][yy] = null
            }
        }
    }

    private fun clearUndoGrid() {
        for (xx in field.indices) {
            for (yy in 0 until field[0].size) {
                undoField[xx][yy] = null
            }
        }
    }
}
