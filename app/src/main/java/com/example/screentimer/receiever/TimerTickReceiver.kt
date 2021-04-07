package com.example.screentimer.receiever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.screentimer.util.NotificationUtil
import com.example.screentimer.util.PrefUtil

class TimerTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val secondRemaining = PrefUtil.getSecondsRemaining(context)
        NotificationUtil.showTimerRunning(context, secondRemaining)
        Log.d("Tick", secondRemaining.toString())
    }
}