package de.devfest.hamburg.twozerommo

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.util.*


class MainView(context: Context) : View(context) {
    val numCellTypes = 21
    val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val bitmapCell = arrayOfNulls<BitmapDrawable>(numCellTypes)
    val game: MainGame
    //Internal variables
    private val paint = Paint()
    var hasSaveState = false
    var continueButtonEnabled = false
    var startingX: Int = 0
    var startingY: Int = 0
    var endingX: Int = 0
    var endingY: Int = 0
    //Icon variables
    var sYIcons: Int = 0
    var sXHighScore: Int = 0
    var sXUndo: Int = 0
    var iconSize: Int = 0
    //Misc
    internal var refreshLastTime = true
    //Timing
    private var lastFPSTime = System.nanoTime()
    //Text
    private var titleTextSize: Float = 0.toFloat()
    private var bodyTextSize: Float = 0.toFloat()
    private var headerTextSize: Float = 0.toFloat()
    private var instructionsTextSize: Float = 0.toFloat()
    private var logTextSize: Float = 0.toFloat()
    private var gameOverTextSize: Float = 0.toFloat()
    //Layout variables
    private var cellSize = 0
    private var textSize = 0f
    private var cellTextSize = 0f
    private var gridWidth = 0
    private var textPaddingSize: Int = 0
    private var iconPaddingSize: Int = 0
    //Assets
    lateinit private var backgroundRectangle: Drawable
    lateinit private var lightUpRectangle: Drawable
    lateinit private var fadeRectangle: Drawable
    private var background: Bitmap? = null
    private var loseGameOverlay: BitmapDrawable? = null
    private var winGameContinueOverlay: BitmapDrawable? = null
    private var winGameFinalOverlay: BitmapDrawable? = null
    //Text variables
    private var sYAll: Int = 0
    private var titleStartYAll: Int = 0
    private var bodyStartYAll: Int = 0
    private var eYAll: Int = 0
    private var titleWidthHighScore: Int = 0
    private var titleWidthScore: Int = 0

    //score variables
    private val scoreUpdates = ArrayList<ScoreUpdate>()

    data class DrawableCellProps(val drawableRes: Int, val color: Int)

    var cellRectangles: Array<DrawableCellProps> = emptyArray()

    init {
        setupRemoteConfig()
        //Loading resources
        game = MainGame(context, this)
        try {
            //Getting assets
            backgroundRectangle = getDrawable(R.drawable.background_rectangle)
            lightUpRectangle = getDrawable(R.drawable.light_up_rectangle)
            fadeRectangle = getDrawable(R.drawable.fade_rectangle)
            this.setBackgroundColor(resources.getColor(R.color.background))
            val font = Typeface.createFromAsset(resources.assets, "ClearSans-Bold.ttf")
            paint.typeface = font
            paint.isAntiAlias = true
        } catch (e: Exception) {
            Log.e(TAG, "Error getting assets?", e)
        }

        setOnTouchListener(InputListener(this))

        game.newGame()
    }

    private fun resetCellRectangles() {
        cellRectangles = arrayOf(
                DrawableCellProps(R.drawable.cell_rectangle, Color.parseColor(remoteConfig.getString(remoteTheme[0]))),
                DrawableCellProps(R.drawable.cell_rectangle_2, Color.parseColor(remoteConfig.getString(remoteTheme[1]))),
                DrawableCellProps(R.drawable.cell_rectangle_4, Color.parseColor(remoteConfig.getString(remoteTheme[2]))),
                DrawableCellProps(R.drawable.cell_rectangle_8, Color.parseColor(remoteConfig.getString(remoteTheme[3]))),
                DrawableCellProps(R.drawable.cell_rectangle_16, Color.parseColor(remoteConfig.getString(remoteTheme[4]))),
                DrawableCellProps(R.drawable.cell_rectangle_32, Color.parseColor(remoteConfig.getString(remoteTheme[5]))),
                DrawableCellProps(R.drawable.cell_rectangle_64, Color.parseColor(remoteConfig.getString(remoteTheme[6]))),
                DrawableCellProps(R.drawable.cell_rectangle_128, Color.parseColor(remoteConfig.getString(remoteTheme[7]))),
                DrawableCellProps(R.drawable.cell_rectangle_256, Color.parseColor(remoteConfig.getString(remoteTheme[8]))),
                DrawableCellProps(R.drawable.cell_rectangle_512, Color.parseColor(remoteConfig.getString(remoteTheme[9]))),
                DrawableCellProps(R.drawable.cell_rectangle_1024, Color.parseColor(remoteConfig.getString(remoteTheme[10]))),
                DrawableCellProps(R.drawable.cell_rectangle_2048, Color.parseColor(remoteConfig.getString(remoteTheme[11]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12]))),
                DrawableCellProps(R.drawable.cell_rectangle_4096, Color.parseColor(remoteConfig.getString(remoteTheme[12])))
        )
    }

    private fun setupRemoteConfig() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build()
        remoteConfig.setConfigSettings(configSettings)
        remoteConfig.setDefaults(R.xml.remote_config_defaults)
        resetCellRectangles()

        remoteConfig.fetch().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("RemoteConfig", "Fetched remote data")
                remoteConfig.activateFetched()
                resetCellRectangles()
                invalidate(startingX, startingY, endingX, endingY)
            } else {
                Log.e("RemoteConfig", "Failed to fetch remote config data")
            }
        }
    }

    public override fun onDraw(canvas: Canvas) {
        //Reset the transparency of the screen

        canvas.drawBitmap(background, 0f, 0f, paint)

        drawScoreText(canvas)
        drawScoreLog(canvas)

        drawCells(canvas)

        if (!game.isActive) {
            drawEndGameState(canvas)
        }

        if (!game.canContinue()) {
            drawEndlessText(canvas)
        }

        if (game.isUsersTurn) {
            drawRemainingTime(canvas)
        } else {
            drawWaitingForTurnText(canvas)
        }


        //Refresh the screen if there is still an animation running
        if (game.aGrid.isAnimationActive) {
            invalidate(startingX, startingY, endingX, endingY)
            tick()
            //Refresh one last time on game end.
        } else if (!game.isActive && refreshLastTime) {
            invalidate()
            refreshLastTime = false
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(width, height, oldW, oldH)
        getLayout(width, height)
        createBitmapCells()
        createBackgroundBitmap(width, height)
        createOverlays()
    }

    private fun getDrawable(resId: Int): Drawable {
        return resources.getDrawable(resId)
    }

    private fun drawDrawable(canvas: Canvas, draw: Drawable, startingX: Int, startingY: Int, endingX: Int, endingY: Int) {
        draw.setBounds(startingX, startingY, endingX, endingY)
        draw.draw(canvas)
    }

    private fun drawCellText(canvas: Canvas, value: Int) {
        val textShiftY = centerText()
        if (value >= 8) {
            paint.color = resources.getColor(R.color.text_white)
        } else {
            paint.color = resources.getColor(R.color.text_black)
        }
        canvas.drawText("" + value, (cellSize / 2).toFloat(), (cellSize / 2 - textShiftY).toFloat(), paint)
    }

    private fun drawScoreText(canvas: Canvas) {
        //Drawing the score text: Ver 2
        paint.textSize = bodyTextSize
        paint.textAlign = Paint.Align.CENTER

        val bodyWidthScore = paint.measureText("" + game.score).toInt()

        val textWidthScore = Math.max(titleWidthHighScore, bodyWidthScore) + textPaddingSize * 2

        val textMiddleScore = textWidthScore / 2

        val eXScore = endingX
        val sXScore = eXScore - textWidthScore

        //Outputting scores box
        backgroundRectangle.setBounds(sXScore, sYAll, eXScore, eYAll)
        backgroundRectangle.draw(canvas)
        paint.textSize = titleTextSize
        paint.color = resources.getColor(R.color.text_brown)
        canvas.drawText(resources.getString(R.string.score), (sXScore + textMiddleScore).toFloat(), titleStartYAll.toFloat(), paint)
        paint.textSize = bodyTextSize
        paint.color = resources.getColor(R.color.text_white)
        canvas.drawText(game.score.toString(), (sXScore + textMiddleScore).toFloat(), bodyStartYAll.toFloat(), paint)
    }

    private fun drawHighScoreButton(canvas: Canvas) {
        drawDrawable(canvas,
                backgroundRectangle,
                sXHighScore,
                sYIcons, sXHighScore + iconSize,
                sYIcons + iconSize
        )

        drawDrawable(canvas,
                getDrawable(R.drawable.ic_view_headline_white_24dp),
                sXHighScore + iconPaddingSize,
                sYIcons + iconPaddingSize,
                sXHighScore + iconSize - iconPaddingSize,
                sYIcons + iconSize - iconPaddingSize
        )
    }

    private fun drawHeader(canvas: Canvas) {
        paint.textSize = headerTextSize
        paint.color = resources.getColor(R.color.text_black)
        paint.textAlign = Paint.Align.LEFT
        val textShiftY = centerText() * 2
        val headerStartY = sYAll - textShiftY
        canvas.drawText(resources.getString(R.string.header), startingX.toFloat(), headerStartY.toFloat(), paint)
    }

    private fun drawInstructions(canvas: Canvas) {
        paint.textSize = instructionsTextSize
        paint.textAlign = Paint.Align.LEFT
        val textShiftY = centerText() * 2
        canvas.drawText(resources.getString(R.string.instructions),
                startingX.toFloat(), (endingY - textShiftY + textPaddingSize).toFloat(), paint)
    }

    private fun drawBackground(canvas: Canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY, endingX, endingY)
    }

    //Renders the set of 16 background squares.
    private fun drawBackgroundGrid(canvas: Canvas) {
        val resources = resources
        val backgroundCell = getDrawable(R.drawable.cell_rectangle)
        // Outputting the game grid
        for (xx in 0 until game.numSquaresX) {
            for (yy in 0 until game.numSquaresY) {
                val sX = startingX + gridWidth + (cellSize + gridWidth) * xx
                val eX = sX + cellSize
                val sY = startingY + gridWidth + (cellSize + gridWidth) * yy
                val eY = sY + cellSize

                drawDrawable(canvas, backgroundCell, sX, sY, eX, eY)
            }
        }
    }

    private fun drawCells(canvas: Canvas) {
        paint.textSize = textSize
        paint.textAlign = Paint.Align.CENTER
        // Outputting the individual cells
        for (xx in 0 until game.numSquaresX) {
            for (yy in 0 until game.numSquaresY) {
                val sX = startingX + gridWidth + (cellSize + gridWidth) * xx
                val eX = sX + cellSize
                val sY = startingY + gridWidth + (cellSize + gridWidth) * yy
                val eY = sY + cellSize

                val currentTile = game.grid?.getCellContent(xx, yy)
                if (currentTile != null) {
                    //Get and represent the value of the tile
                    val value = currentTile.value
                    val index = log2(value)

                    //Check for any active animations
                    val aArray = game.aGrid.getAnimationCell(xx, yy)
                    var animated = false
                    for (i in aArray!!.indices.reversed()) {
                        val aCell = aArray[i]
                        //If this animation is not active, skip it
                        if (aCell.animationType == MainGame.SPAWN_ANIMATION) {
                            animated = true
                        }
                        if (!aCell.isActive) {
                            continue
                        }

                        if (aCell.animationType == MainGame.SPAWN_ANIMATION) { // Spawning animation
                            val percentDone = aCell.percentageDone
                            val textScaleSize = percentDone.toFloat()
                            paint.textSize = textSize * textScaleSize

                            val cellScaleSize = cellSize / 2 * (1 - textScaleSize)
                            bitmapCell[index]?.setBounds((sX + cellScaleSize).toInt(), (sY + cellScaleSize).toInt(), (eX - cellScaleSize).toInt(), (eY - cellScaleSize).toInt())
                            bitmapCell[index]?.draw(canvas)
                        } else if (aCell.animationType == MainGame.MERGE_ANIMATION) { // Merging Animation
                            val percentDone = aCell.percentageDone
                            val textScaleSize = (1.0 + INITIAL_VELOCITY * percentDone
                                    + MERGING_ACCELERATION.toDouble() * percentDone * percentDone / 2).toFloat()
                            paint.textSize = textSize * textScaleSize

                            val cellScaleSize = cellSize / 2 * (1 - textScaleSize)
                            bitmapCell[index]?.setBounds((sX + cellScaleSize).toInt(), (sY + cellScaleSize).toInt(), (eX - cellScaleSize).toInt(), (eY - cellScaleSize).toInt())
                            bitmapCell[index]?.draw(canvas)
                        } else if (aCell.animationType == MainGame.MOVE_ANIMATION) {  // Moving animation
                            val percentDone = aCell.percentageDone
                            var tempIndex = index
                            if (aArray.size >= 2) {
                                tempIndex = tempIndex - 1
                            }
                            val previousX = aCell.extras!![0]
                            val previousY = aCell.extras!![1]
                            val currentX = currentTile.x
                            val currentY = currentTile.y
                            val dX = ((currentX - previousX).toDouble() * (cellSize + gridWidth).toDouble() * (percentDone - 1) * 1.0).toInt()
                            val dY = ((currentY - previousY).toDouble() * (cellSize + gridWidth).toDouble() * (percentDone - 1) * 1.0).toInt()
                            bitmapCell[tempIndex]?.setBounds(sX + dX, sY + dY, eX + dX, eY + dY)
                            bitmapCell[tempIndex]?.draw(canvas)
                        }
                        animated = true
                    }

                    //No active animations? Just draw the cell
                    if (!animated) {
                        bitmapCell[index]?.setBounds(sX, sY, eX, eY)
                        bitmapCell[index]?.draw(canvas)
                    }
                }
            }
        }
    }

    private fun drawRemainingTime(canvas: Canvas) {
        return
        paint.textSize = instructionsTextSize
        paint.textAlign = Paint.Align.CENTER

        val center = centerText()
        paint.color = resources.getColor(R.color.text_white)
        canvas.drawRect(startingX.toFloat(), sYIcons.toFloat(), sXHighScore.toFloat(), (sYIcons - center * 2).toFloat(), paint)

        paint.color = resources.getColor(R.color.text_black)
        canvas.drawText("%.2fs left".format((System.currentTimeMillis() - game.turnStartTime) / 1000f), (startingX + (sXHighScore - startingX) / 2).toFloat(), (sYIcons - center * 3).toFloat(), paint)

    }

    private fun drawWaitingForTurnText(canvas: Canvas) {
        paint.textSize = instructionsTextSize
        paint.textAlign = Paint.Align.CENTER

        val center = centerText()
        paint.color = resources.getColor(R.color.text_white)
        canvas.drawRect(startingX.toFloat(), sYIcons.toFloat(), sXHighScore.toFloat(), (sYIcons - center * 2).toFloat(), paint)

        paint.color = resources.getColor(R.color.text_black)
        canvas.drawText("Wait for your turn", (startingX + (sXHighScore - startingX) / 2).toFloat(), (sYIcons - center * 3).toFloat(), paint)
    }

    private fun drawScoreLog(canvas: Canvas) {
        paint.textSize = logTextSize
        paint.textAlign = Paint.Align.LEFT

        paint.color = resources.getColor(R.color.text_white)
        canvas.drawRect(startingX.toFloat(), 0f, (endingX * 0.7f).toInt().toFloat(), eYAll.toFloat(), paint)

        paint.color = resources.getColor(R.color.text_black)

        val rowHeight = centerText() * 3
        for (i in scoreUpdates.indices.reversed()) {
            val score = scoreUpdates[i]
            val textY = eYAll + rowHeight * i
            if (textY < 0)
                continue
            canvas.drawText("+" + score.score.toInt().toString() + " " + score.name, startingX.toFloat(), textY.toFloat(), paint)
        }
    }

    private fun drawEndGameState(canvas: Canvas) {
        var alphaChange = 1.0
        continueButtonEnabled = false
        for (animation in game.aGrid.globalAnimation) {
            if (animation.animationType == MainGame.FADE_GLOBAL_ANIMATION) {
                alphaChange = animation.percentageDone
            }
        }
        var displayOverlay: BitmapDrawable? = null
        if (game.gameWon()) {
            if (game.canContinue()) {
                continueButtonEnabled = true
                displayOverlay = winGameContinueOverlay
            } else {
                displayOverlay = winGameFinalOverlay
            }
        } else if (game.gameLost()) {
            displayOverlay = loseGameOverlay
        }

        if (displayOverlay != null) {
            displayOverlay.setBounds(startingX, startingY, endingX, endingY)
            displayOverlay.alpha = (255 * alphaChange).toInt()
            displayOverlay.draw(canvas)
        }
    }

    private fun drawEndlessText(canvas: Canvas) {
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = bodyTextSize
        paint.color = resources.getColor(R.color.text_black)
        canvas.drawText(resources.getString(R.string.endless), startingX.toFloat(), (sYIcons - centerText() * 2).toFloat(), paint)
    }

    private fun createEndGameStates(canvas: Canvas, win: Boolean, showButton: Boolean) {
        val width = endingX - startingX
        val length = endingY - startingY
        val middleX = width / 2
        val middleY = length / 2
        if (win) {
            lightUpRectangle.alpha = 127
            drawDrawable(canvas, lightUpRectangle, 0, 0, width, length)
            lightUpRectangle.alpha = 255
            paint.color = resources.getColor(R.color.text_white)
            paint.alpha = 255
            paint.textSize = gameOverTextSize
            paint.textAlign = Paint.Align.CENTER
            val textBottom = middleY - centerText()
            canvas.drawText(resources.getString(R.string.you_win), middleX.toFloat(), textBottom.toFloat(), paint)
            paint.textSize = bodyTextSize
            val text = if (showButton)
                resources.getString(R.string.go_on)
            else
                resources.getString(R.string.for_now)
            canvas.drawText(text, middleX.toFloat(), (textBottom + textPaddingSize * 2 - centerText() * 2).toFloat(), paint)
        } else {
            fadeRectangle.alpha = 127
            drawDrawable(canvas, fadeRectangle, 0, 0, width, length)
            fadeRectangle.alpha = 255
            paint.color = resources.getColor(R.color.text_black)
            paint.alpha = 255
            paint.textSize = gameOverTextSize
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(resources.getString(R.string.game_over), middleX.toFloat(), (middleY - centerText()).toFloat(), paint)
        }
    }

    private fun createBackgroundBitmap(width: Int, height: Int) {
        background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(background)
        //        drawHeader(canvas);
        drawHighScoreButton(canvas)
        drawBackground(canvas)
        drawBackgroundGrid(canvas)
        drawInstructions(canvas)
    }

    private fun createBitmapCells() {
        val resources = resources
        paint.textAlign = Paint.Align.CENTER
        for (xx in 1 until bitmapCell.size) {
            val value = Math.pow(2.0, xx.toDouble()).toInt()
            paint.textSize = cellTextSize
            val tempTextSize = cellTextSize * cellSize.toFloat() * 0.9f / Math.max(cellSize * 0.9f, paint.measureText(value.toString()))
            paint.textSize = tempTextSize
            val bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawDrawable(canvas, getThemedTile(xx), 0, 0, cellSize, cellSize)
            drawCellText(canvas, value)
            bitmapCell[xx] = BitmapDrawable(resources, bitmap)
        }
    }

    private fun getThemedTile(tilePosition: Int): Drawable {
        val tile = cellRectangles[tilePosition]
        val drawable = getDrawable(tile.drawableRes)
        drawable.setTint(tile.color)
        return drawable
    }

    private fun createOverlays() {
        val resources = resources
        //Initialize overlays
        var bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bitmap)
        createEndGameStates(canvas, true, true)
        winGameContinueOverlay = BitmapDrawable(resources, bitmap)
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        createEndGameStates(canvas, true, false)
        winGameFinalOverlay = BitmapDrawable(resources, bitmap)
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap)
        createEndGameStates(canvas, false, false)
        loseGameOverlay = BitmapDrawable(resources, bitmap)
    }

    private fun tick() {
        val currentTime = System.nanoTime()
        game.aGrid.tickAll(currentTime - lastFPSTime)
        lastFPSTime = currentTime
    }

    fun resyncTime() {
        lastFPSTime = System.nanoTime()
    }

    private fun getLayout(width: Int, height: Int) {
        cellSize = Math.min(width / (game.numSquaresX + 1), height / (game.numSquaresY + 3))
        gridWidth = cellSize / 7
        val screenMiddleX = width / 2
        val screenMiddleY = height / 2
        val boardMiddleY = screenMiddleY + cellSize / 2
        iconSize = cellSize / 2

        //Grid Dimensions
        val halfNumSquaresX = game.numSquaresX / 2.0
        val halfNumSquaresY = game.numSquaresY / 2.0
        startingX = (screenMiddleX.toDouble() - (cellSize + gridWidth) * halfNumSquaresX - (gridWidth / 2).toDouble()).toInt()
        endingX = (screenMiddleX.toDouble() + (cellSize + gridWidth) * halfNumSquaresX + (gridWidth / 2).toDouble()).toInt()
        startingY = (boardMiddleY.toDouble() - (cellSize + gridWidth) * halfNumSquaresY - (gridWidth / 2).toDouble()).toInt()
        endingY = (boardMiddleY.toDouble() + (cellSize + gridWidth) * halfNumSquaresY + (gridWidth / 2).toDouble()).toInt()

        val widthWithPadding = (endingX - startingX).toFloat()

        // Text Dimensions
        paint.textSize = cellSize.toFloat()
        textSize = cellSize * cellSize / Math.max(cellSize.toFloat(), paint.measureText("0000"))

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 1000f
        instructionsTextSize = Math.min(
                1000f * (widthWithPadding / paint.measureText(resources.getString(R.string.instructions))),
                textSize / 1.5f
        )
        logTextSize = instructionsTextSize * 0.6f
        gameOverTextSize = Math.min(
                Math.min(
                        1000f * ((widthWithPadding - gridWidth * 2) / paint.measureText(resources.getString(R.string.game_over))),
                        textSize * 2
                ),
                1000f * ((widthWithPadding - gridWidth * 2) / paint.measureText(resources.getString(R.string.you_win)))
        )

        paint.textSize = cellSize.toFloat()
        cellTextSize = textSize
        titleTextSize = textSize / 3
        bodyTextSize = (textSize / 1.5).toInt().toFloat()
        headerTextSize = textSize * 2
        textPaddingSize = (textSize / 3).toInt()
        iconPaddingSize = (textSize / 5).toInt()

        paint.textSize = titleTextSize

        var textShiftYAll = centerText()
        //static variables
        sYAll = (startingY - cellSize * 1.5).toInt()
        titleStartYAll = (sYAll.toFloat() + textPaddingSize.toFloat() + titleTextSize / 2 - textShiftYAll).toInt()
        bodyStartYAll = (titleStartYAll.toFloat() + textPaddingSize.toFloat() + titleTextSize / 2 + bodyTextSize / 2).toInt()

        titleWidthHighScore = paint.measureText(resources.getString(R.string.high_score)).toInt()
        titleWidthScore = paint.measureText(resources.getString(R.string.score)).toInt()
        paint.textSize = bodyTextSize
        textShiftYAll = centerText()
        eYAll = (bodyStartYAll.toFloat() + textShiftYAll.toFloat() + bodyTextSize / 2 + textPaddingSize.toFloat()).toInt()

        sYIcons = (startingY + eYAll) / 2 - iconSize / 2
        sXHighScore = endingX - iconSize
        resyncTime()
    }

    private fun centerText(): Int {
        return ((paint.descent() + paint.ascent()) / 2).toInt()
    }

    companion object {

        //Internal Constants
        internal val BASE_ANIMATION_TIME = 100000000L
        private val TAG = MainView::class.java.simpleName
        private val MERGING_ACCELERATION = (-0.5).toFloat()
        private val INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4

        private val remoteTheme = listOf(
                "empty_cell_background",
                "cell_value_2_background",
                "cell_value_4_background",
                "cell_value_8_background",
                "cell_value_16_background",
                "cell_value_32_background",
                "cell_value_64_background",
                "cell_value_128_background",
                "cell_value_256_background",
                "cell_value_512_background",
                "cell_value_1024_background",
                "cell_value_2048_background",
                "cell_value_4096_background")

        private fun log2(n: Int): Int {
            if (n <= 0) throw IllegalArgumentException()
            return 31 - Integer.numberOfLeadingZeros(n)
        }
    }

}