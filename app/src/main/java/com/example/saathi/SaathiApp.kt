package com.example.saathi

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class SaathiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase Realtime Database persistence
        // This saves the data to the local file storage automatically
        // so the app works offline and only downloads new messages (delta sync).
        val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
        try {
            database.setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Persistence must be enabled before any other usage of the database
        }
    }
}
