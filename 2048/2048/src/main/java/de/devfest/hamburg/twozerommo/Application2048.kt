package de.devfest.hamburg.twozerommo

import android.app.Application
import com.google.firebase.FirebaseApp

class Application2048(): Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
