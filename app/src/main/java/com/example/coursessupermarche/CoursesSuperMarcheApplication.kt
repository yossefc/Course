package com.example.coursessupermarche

import android.app.Application
import com.example.coursessupermarche.utils.NetworkMonitor
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp  // Cette annotation est essentielle pour Hilt
class CoursesSuperMarcheApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialiser le moniteur réseau dès le démarrage de l'application
        NetworkMonitor.init(applicationContext)
    }
}