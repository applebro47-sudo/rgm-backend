package com.pushkar.RGM

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseUtils {
    private const val DB_URL = "https://pushkar2b-default-rtdb.firebaseio.com/"

    val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance(DB_URL).reference
    }
}
