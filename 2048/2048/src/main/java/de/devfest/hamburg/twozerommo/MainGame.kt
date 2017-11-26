package de.devfest.hamburg.twozerommo

import android.content.Context
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import de.devfest.hamburg.twozerommo.service.*
import java.util.*
import android.widget.Toast
import com.google.firebase.database.ChildEventListener



class MainGame(private val mContext: Context, private val mView: MainView) {
    val TAG = "MainGame"
    var gameState = GAME_NORMAL
    var lastGameState = GAME_NORMAL
    private var bufferGameState = GAME_NORMAL
    internal val numSquaresX = 4
    internal val numSquaresY = 4
    lateinit var grid: Grid
    private var isGridSetUp = false
    lateinit var aGrid: AnimationGrid
    var canUndo: Boolean = false
    var score: Long = 0
    private var _highScore: Long = 0
    var lastScore: Long = 0
    var isUsersTurn = false
    var turnStartTime: Long = 0
    var turnLastMoveIndex = 0
    var startup: Boolean = true
    lateinit var user: GameUser
    val TURN_TIME = 5f

    private var bufferScore: Long = 0

    var highScore: Long
        get() {
            val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
            return settings.getLong(HIGH_SCORE, -1)
        }
        set(value: Long) {
            _highScore = value
        }

    val isActive: Boolean
        get() = !(gameWon() || gameLost())

    init {
        endingMaxValue = Math.pow(2.0, (mView.numCellTypes - 1).toDouble()).toInt()



        val auth = FirebaseAuth.getInstance()

        GameDatabase.users.document(auth.currentUser!!.uid).get()
                .addOnCompleteListener({ document ->
                    if (document.isSuccessful) {
                        user = document.result.toObject(GameUser::class.java)
                    } else {
                        user = GameUser(auth.currentUser!!.uid, true, auth.currentUser!!.displayName!!, 0)
                    }
                    user.active = true
                    score = user.score.toLong()
                    GameDatabase.updateUser(user)
                    setupListeners()
                })

    }

    fun setupListeners() {

        GameDatabase.game.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val state = dataSnapshot.getValue(GameState::class.java)
                if (state != null) {
                    if (!isUsersTurn) {
                        move(state.lastMove, true)
                        grid.fromMatrix(state.grid!!)
                    } else if (startup) {
                        grid.fromMatrix(state.grid!!)
                        mView.invalidate()
                    }
                } else {
                    newGame()
                }
                startup = false
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })


        GameDatabase.queue.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (data in dataSnapshot.children) {
                    isUsersTurn =  data.getValue(GameUser::class.java)?.uid == user.uid
                    break
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    fun newGame() {
       // if (grid == null) {
            grid = Grid(numSquaresX, numSquaresY)
        //} else {
//            prepareUndoState()
//            saveUndoState()
//            grid?.clearGrid()
//        }
        aGrid = AnimationGrid(numSquaresX, numSquaresY)
        highScore = getHighScore()
        if (score >= highScore) {
            highScore = score
            recordHighScore()
        }
        score = 0
        gameState = GAME_NORMAL
        addStartTiles()
        mView.refreshLastTime = true
        mView.resyncTime()
        mView.invalidate()
        isUsersTurn = true // TODO Remove
        isGridSetUp = true
    }

    internal fun startTurn() {
        isUsersTurn = true
        turnStartTime = System.currentTimeMillis()
        GameFunctionsService.startTurn(user)
    }

    private fun addStartTiles() {
        val startTiles = 2
        for (xx in 0 until startTiles) {
            this.addRandomTile()
        }
    }

    private fun addRandomTile() {
        if (grid?.isCellsAvailable == true) {
            val value = if (Math.random() < 0.9) 2 else 4
            val tile = Tile(grid!!.randomAvailableCell()!!, value)
            spawnTile(tile)
        }
    }

    private fun spawnTile(tile: Tile) {
        grid?.insertTile(tile)
        aGrid.startAnimation(tile.x, tile.y, SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null) //Direction: -1 = EXPANDING
    }

    private fun recordHighScore() {
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        val editor = settings.edit()
        editor.putLong(HIGH_SCORE, highScore)
        editor.commit()
    }

    internal fun getHighScore(): Long {
        val settings = PreferenceManager.getDefaultSharedPreferences(mContext)
        return settings.getLong(HIGH_SCORE, -1)
    }

    private fun prepareTiles() {
        if (grid?.field != null) {
            for (array in grid!!.field) {
                for (tile in array) {
                    if (grid!!.isCellOccupied(tile)) {
                        tile?.mergedFrom = null
                    }
                }
            }
        }
    }

    private fun moveTile(tile: Tile, cell: Cell) {
        grid?.field?.get(tile.x)?.set(tile.y, null)
        grid?.field?.get(cell.x)?.set(cell.y, tile)
        tile.updatePosition(cell)
    }

    private fun saveUndoState() {
        grid?.saveTiles()
        canUndo = true
        lastScore = bufferScore
        lastGameState = bufferGameState
    }

    private fun prepareUndoState() {
        grid?.prepareSaveTiles()
        bufferScore = score
        bufferGameState = gameState
    }

    fun revertUndoState() {
        if (canUndo) {
            canUndo = false
            aGrid.cancelAnimations()
            grid?.revertTiles()
            score = lastScore
            gameState = lastGameState
            mView.refreshLastTime = true
            mView.invalidate()
        }
    }

    fun gameWon(): Boolean {
        return gameState > 0 && gameState % 2 != 0
    }

    fun gameLost(): Boolean {
        return gameState == GAME_LOST
    }

    fun move(direction: Int, animateOnly: Boolean = false) {
        if (isGridSetUp) {
            aGrid.cancelAnimations()
            // 0: up, 1: right, 2: down, 3: left
            if (!isActive) {
                return
            }
            prepareUndoState()
            val vector = getVector(direction)
            val traversalsX = buildTraversalsX(vector)
            val traversalsY = buildTraversalsY(vector)
            var moved = false

            prepareTiles()

            var gainedScore = 0
            for (xx in traversalsX) {
                for (yy in traversalsY) {
                    val cell = Cell(xx, yy)
                    val tile = grid?.getCellContent(cell)

                    if (tile != null) {
                        val positions = findFarthestPosition(cell, vector)
                        val next = grid?.getCellContent(positions[1])

                        if (next != null && next.value == tile.value && next.mergedFrom == null) {
                            val merged = Tile(positions[1], tile.value * 2)
                            val temp = arrayOf(tile, next)
                            merged.mergedFrom = temp

                            grid?.insertTile(merged)
                            grid?.removeTile(tile)

                            // Converge the two tiles' positions
                            tile.updatePosition(positions[1])

                            val extras = intArrayOf(xx, yy)
                            aGrid.startAnimation(merged.x, merged.y, MOVE_ANIMATION,
                                    MOVE_ANIMATION_TIME, 0, extras) //Direction: 0 = MOVING MERGED
                            aGrid.startAnimation(merged.x, merged.y, MERGE_ANIMATION,
                                    SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null)

                        if (!animateOnly) {// Update the score
                            gainedScore += merged.value
                            score = score + merged.value
                            // TODO score update listener
                            highScore = Math.max(score, highScore)
                        }
                    } else {
                        moveTile(tile, positions[0])
                        val extras = intArrayOf(xx, yy, 0)
                        aGrid.startAnimation(positions[0].x, positions[0].y, MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras) //Direction: 1 = MOVING NO MERGE
                    }

                        if (!positionsEqual(cell, tile)) {
                            moved = true
                        }
                    }
                }
            }

        if (moved && !animateOnly) {
            saveUndoState()
            addRandomTile()
            checkLose()
            GameFunctionsService.move(Move(GameState(grid.toMatrix(), direction), gainedScore, turnLastMoveIndex++), user)
                    ?.subscribe({ t ->
                        Log.d(TAG, t.toString())
                    }, {err -> Log.e("NetworkError", err.message)})
        }
        mView.resyncTime()
        mView.invalidate()}
    }

    private fun checkLose() {
        if (!movesAvailable() && !gameWon()) {
            gameState = GAME_LOST
            endGame()
        }
    }

    private fun endGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION, NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null)
        if (score >= highScore) {
            highScore = score
            recordHighScore()
        }
    }

    private fun getVector(direction: Int): Cell {
        val map = arrayOf(Cell(0, -1), // up
                Cell(1, 0), // right
                Cell(0, 1), // down
                Cell(-1, 0)  // left
        )
        return map[direction]
    }

    private fun buildTraversalsX(vector: Cell): List<Int> {
        val traversals = ArrayList<Int>()

        for (xx in 0 until numSquaresX) {
            traversals.add(xx)
        }
        if (vector.x == 1) {
            Collections.reverse(traversals)
        }

        return traversals
    }

    private fun buildTraversalsY(vector: Cell): List<Int> {
        val traversals = ArrayList<Int>()

        for (xx in 0 until numSquaresY) {
            traversals.add(xx)
        }
        if (vector.y == 1) {
            Collections.reverse(traversals)
        }

        return traversals
    }

    private fun findFarthestPosition(cell: Cell, vector: Cell): Array<Cell> {
        var previous: Cell
        var nextCell = Cell(cell.x, cell.y)
        do {
            previous = nextCell
            nextCell = Cell(previous.x + vector.x,
                    previous.y + vector.y)
        } while (grid?.isCellWithinBounds(nextCell) == true && grid?.isCellAvailable(nextCell) == true)

        return arrayOf(previous, nextCell)
    }

    private fun movesAvailable(): Boolean {
        return grid?.isCellsAvailable == true || tileMatchesAvailable()
    }

    private fun tileMatchesAvailable(): Boolean {
        var tile: Tile?

        for (xx in 0 until numSquaresX) {
            for (yy in 0 until numSquaresY) {
                tile = grid?.getCellContent(Cell(xx, yy))

                if (tile != null) {
                    for (direction in 0..3) {
                        val vector = getVector(direction)
                        val cell = Cell(xx + vector.x, yy + vector.y)

                        val other = grid?.getCellContent(cell)

                        if (other != null && other.value == tile.value) {
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun positionsEqual(first: Cell, second: Cell): Boolean {
        return first.x == second.x && first.y == second.y
    }

    private fun winValue(): Int {
        return if (!canContinue()) {
            endingMaxValue
        } else {
            startingMaxValue
        }
    }

    fun setEndlessMode() {
        gameState = GAME_ENDLESS
        mView.invalidate()
        mView.refreshLastTime = true
    }

    fun canContinue(): Boolean {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON)
    }

    companion object {

        val SPAWN_ANIMATION = -1
        val MOVE_ANIMATION = 0
        val MERGE_ANIMATION = 1

        val FADE_GLOBAL_ANIMATION = 0
        private val MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME
        private val SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME
        private val NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME
        private val NOTIFICATION_ANIMATION_TIME: Long= MainView.BASE_ANIMATION_TIME * 5
        private val startingMaxValue = 2048
        //Odd state = game is not active
        //Even state = game is active
        //Win state = active state + 1
        private val GAME_WIN = 1
        private val GAME_LOST = -1
        private val GAME_NORMAL = 0
        private val GAME_ENDLESS = 2
        private val GAME_ENDLESS_WON = 3
        private val HIGH_SCORE = "high score"
        private var endingMaxValue: Int = 0
    }
}
