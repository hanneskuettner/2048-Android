package com.tpcstld.twozerogame

open class Cell(x: Int, y: Int) {
    var x: Int = 0
        internal set
    var y: Int = 0
        internal set

    init {
        this.x = x
        this.y = y
    }
}
