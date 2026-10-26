package com.example

import android.app.Application
import android.util.Log
import com.example.data.CrashTracker

class MockGpsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("MockGpsApp", "MockGpsApplication onCreate: Initializing Global Exception & Crash Handler")
        CrashTracker.init(this)
        CrashTracker.updateState("Application Launched")
    }
}
