package com.example.screentimer

import android.app.IntentService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.JobIntentService
import com.example.screentimer.util.NotificationUtil
import com.example.screentimer.util.PrefUtil
import kotlin.concurrent.timer

class CountdownIntentService : Service() {
    companion object {
        const val JOBID = 1
    }

    private var secondRemaining = 0L
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            isRunning = true
            secondRemaining = PrefUtil.getSecondsRemaining(this)
            while (isRunning){
                Log.d("CountdownService",
                        "Service is running ${secondRemaining.toString()}")
                NotificationUtil.showTimerRunning(this, secondRemaining)
                secondRemaining -= 1
                if (secondRemaining == 0L){
                    MainActivity.lockPhone()
                    PrefUtil.setTimerState(MainActivity.TimerState.Stopped, this)
                    this.stopSelf()
                }
                PrefUtil.setSecondsRemaining(secondRemaining, this)
                Thread.sleep(1000)
            }
        }.start()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CountdownService",
                "Service is destroyed")
        isRunning = false

    }

    override fun onBind(intent: Intent?): IBinder? = null
}