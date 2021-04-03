package com.example.screentimer.receiever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.screentimer.util.NotificationUtil
import com.example.screentimer.util.PrefUtil

class TimerTicReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val secondRemaining = PrefUtil.getSecondsRemaining(context)
        NotificationUtil.showTimerRunning(context, secondRemaining)
    }
}