package de.devfest.hamburg.twozerommo

internal class AnimationCell(x: Int, y: Int, val animationType: Int, private val animationTime: Long, private val delayTime: Long, val extras: IntArray?) : Cell(x, y) {
    private var timeElapsed: Long = 0

    val percentageDone: Double
        get() = Math.max(0.0, 1.0 * (timeElapsed - delayTime) / animationTime)

    val isActive: Boolean
        get() = timeElapsed >= delayTime

    fun tick(timeElapsed: Long) {
        this.timeElapsed = this.timeElapsed + timeElapsed
    }

    fun animationDone(): Boolean {
        return animationTime + delayTime < timeElapsed
    }

}
