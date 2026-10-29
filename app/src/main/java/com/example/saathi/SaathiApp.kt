package com.example.saathi

import android.app.Application
import com.example.saathi.worker.InactivityWorker
import com.google.firebase.database.FirebaseDatabase

class SaathiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase Realtime Database persistence
        val database = FirebaseDatabase.getInstance("https://saathi-5f51d-default-rtdb.firebaseio.com/")
        try {
            database.setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Persistence must be enabled before any other usage of the database
        }

        scheduleInactivityCheckWorker()
    }

    private fun scheduleInactivityCheckWorker() {
        try {
            InactivityWorker.scheduleNextMinuteCheck(this)
        } catch (e: Exception) {
            // Ignore error
        }
    }
}
