package com.umkt.ucopy.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.umkt.ucopy.ui.crash.CrashActivity

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val stackTrace = Log.getStackTraceString(throwable)

            val intent = Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("crash_log", stackTrace)
            }
            context.startActivity(intent)
            
            // Give it a moment to start the Activity before killing the process
            Thread.sleep(300) 
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}
