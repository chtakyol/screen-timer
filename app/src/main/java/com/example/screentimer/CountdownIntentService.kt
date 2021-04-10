package com.example.screentimer

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.example.screentimer.util.NotificationUtil
import com.example.screentimer.util.PrefUtil
import kotlin.concurrent.timer

class CountdownIntentService : IntentService("CountdownIntentService") {
    init {
        instance = this
    }

    companion object {
        private lateinit var instance: CountdownIntentService

        var isRunning = false

        fun stopService(){
            Log.d("CountdownService", "Service is stopping.")
            instance.stopSelf()
            isRunning = false
        }
    }

    private var secondRemaining = 0L

    override fun onHandleIntent(intent: Intent?) {
        try {
            isRunning = true
            secondRemaining = PrefUtil.getSecondsRemaining(this)
            while (isRunning){
                Log.d("CountdownService",
                        "Service is running ${secondRemaining.toString()}")
                NotificationUtil.showTimerRunning(this, secondRemaining)
                secondRemaining -= 1
                if (secondRemaining == 0L){
                    stopService()
                    MainActivity.lockPhone()
                    PrefUtil.setTimerState(MainActivity.TimerState.Stopped, this)
                }
                PrefUtil.setSecondsRemaining(secondRemaining, this)
                Thread.sleep(1000)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}