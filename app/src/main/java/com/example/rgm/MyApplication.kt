package com.pushkar.RGM

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Manually initializing Firebase because the database URL is missing from google-services.json
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:566231843923:android:00e1fb49e7a1813c54b2a6")
                    .setApiKey("AIzaSyDRf_H1JZnk6ofbz3BBVd1OA0e0-cJViV4")
                    .setProjectId("pushkar2b")
                    .setDatabaseUrl("https://pushkar2b-default-rtdb.firebaseio.com/")
                    .setStorageBucket("pushkar2b.firebasestorage.app")
                    .build()
                
                FirebaseApp.initializeApp(this, options)
                Log.d("MyApplication", "Firebase initialized with manual options")
            }
        } catch (e: Exception) {
            Log.e("MyApplication", "Firebase initialization failed", e)
        }
    }
}
