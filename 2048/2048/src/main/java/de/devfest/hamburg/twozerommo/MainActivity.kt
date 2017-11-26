package de.devfest.hamburg.twozerommo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.ResultCodes
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance();
    lateinit private var view: MainView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser != null) {
            setupGameUI(savedInstanceState)
        }
        firebaseSignIn()
    }

    private fun firebaseSignIn() {
        val providers = mutableListOf<AuthUI.IdpConfig>(AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build());
        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
                RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == ResultCodes.OK) {
                Log.d("FirebaseAuth", "Authenticated user ${auth.currentUser?.displayName} with uid ${auth.currentUser?.uid}")
                setupGameUI(null)
            } else {
                Log.e("FirebaseAuth", "Shit's fucked yo!")
            }
        }
    }

    private fun setupGameUI(savedInstanceState: Bundle?) {
        view = MainView(this)

        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        view.hasSaveState = settings.getBoolean("save_state", false)

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("hasState")) {
                load()
            }
        }
        setContentView(view)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            //Do nothing
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view.game.move(2)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view.game.move(0)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view.game.move(3)
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view.game.move(1)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.putBoolean("hasState", true)
        save()
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
        save()
    }

    private fun save() {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = settings.edit()
        val field = view.game.grid?.field
        val undoField = view.game.grid?.undoField
        editor.putInt(WIDTH, field?.size ?: 0)
        editor.putInt(HEIGHT, field?.size ?: 0)
        for (xx in field?.indices ?: IntRange(0, 0)) {
            for (yy in 0 until (field?.get(0)?.size ?: 0)) {
                if (field!![xx][yy] != null) {
                    editor.putInt(xx.toString() + " " + yy, field[xx][yy]!!.value)
                } else {
                    editor.putInt(xx.toString() + " " + yy, 0)
                }

                if (undoField!![xx][yy] != null) {
                    editor.putInt(UNDO_GRID + xx + " " + yy, undoField[xx][yy]!!.value)
                } else {
                    editor.putInt(UNDO_GRID + xx + " " + yy, 0)
                }
            }
        }
        editor.putLong(SCORE, view.game.score)
        editor.putLong(HIGH_SCORE, view.game.getHighScore())
        editor.putLong(UNDO_SCORE, view.game.lastScore)
        editor.putBoolean(CAN_UNDO, view.game.canUndo)
        editor.putInt(GAME_STATE, view.game.gameState)
        editor.putInt(UNDO_GAME_STATE, view.game.lastGameState)
        editor.commit()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        //Stopping all animations
        view.game.aGrid.cancelAnimations()

        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        for (xx in view.game.grid!!.field.indices) {
            for (yy in 0 until view.game.grid!!.field[0].size) {
                val value = settings.getInt(xx.toString() + " " + yy, -1)
                if (value > 0) {
                    view.game.grid!!.field[xx][yy] = Tile(xx, yy, value)
                } else if (value == 0) {
                    view.game.grid!!.field[xx][yy] = null
                }

                val undoValue = settings.getInt(UNDO_GRID + xx + " " + yy, -1)
                if (undoValue > 0) {
                    view.game.grid!!.undoField[xx][yy] = Tile(xx, yy, undoValue)
                } else if (value == 0) {
                    view.game.grid!!.undoField[xx][yy] = null
                }
            }
        }

        view.game.score = settings.getLong(SCORE, view.game.score)
        view.game.highScore = settings.getLong(HIGH_SCORE, view.game.getHighScore())
        view.game.lastScore = settings.getLong(UNDO_SCORE, view.game.lastScore)
        view.game.canUndo = settings.getBoolean(CAN_UNDO, view.game.canUndo)
        view.game.gameState = settings.getInt(GAME_STATE, view.game.gameState)
        view.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view.game.lastGameState)
    }

    companion object {

        private val WIDTH = "width"
        private val HEIGHT = "height"
        private val SCORE = "score"
        private val HIGH_SCORE = "high score temp"
        private val UNDO_SCORE = "undo score"
        private val CAN_UNDO = "can undo"
        private val UNDO_GRID = "undo"
        private val GAME_STATE = "game state"
        private val UNDO_GAME_STATE = "undo game state"
        private val RC_SIGN_IN = 999
    }
}
