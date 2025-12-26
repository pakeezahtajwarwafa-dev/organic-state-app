package com.example.organicstate

import android.app.Application
import com.google.firebase.FirebaseApp

class OrganicStateApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
    }
}