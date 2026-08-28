package com.cyperpunkred.ai

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler

@HiltAndroidApp
class CyberpunkRedApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CyberCrash", "FATAL on ${thread.name}", throwable)
            try {
                previous?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}
