package com.umkt.ucopy

import android.app.Application
import com.umkt.ucopy.utils.CrashHandler

class UCopyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }
}
