package de.devfest.hamburg.twozerommo.service

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore





object GameDatabase {
    val TAG = "GameDatabase"
    var firestore = FirebaseFirestore.getInstance()
    var realDb = FirebaseDatabase.getInstance()

    var users = firestore.collection("users")

    var game = realDb.getReference("game")
    var queue = realDb.getReference("queue")


    init {

    }

    fun updateUser(user: GameUser) {
        users.document(user.uid).set(user)
    }
}