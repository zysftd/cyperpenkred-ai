package com.cyperpunkred.ai

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CyberpunkRedApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e("CyberCrash", "FATAL on ${thread.name}", throwable)
                previous?.uncaughtException(thread, throwable)
            }
        }
    }
}
