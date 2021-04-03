package com.example.screentimer.receiever

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.screentimer.MainActivity
import com.example.screentimer.util.NotificationUtil
import com.example.screentimer.util.PrefUtil

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationUtil.showTimerExpired(context)

        PrefUtil.setTimerState(MainActivity.TimerState.Stopped, context)
        PrefUtil.setAlarmSetTime(0, context)

        MainActivity.lockPhone()
    }
}