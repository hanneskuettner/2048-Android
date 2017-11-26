package de.devfest.hamburg.twozerommo.service

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

object GameDatabase {
    var db = FirebaseFirestore.getInstance()
    var users = db.collection("users")
    init {

    }

    fun updateUser(user: GameUser) {
        users.document(user.uid).set(user)
    }
}