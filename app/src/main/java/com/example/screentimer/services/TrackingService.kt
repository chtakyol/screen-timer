package com.example.screentimer.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.screentimer.R
import com.example.screentimer.other.Constants.ACTION_PAUSE
import com.example.screentimer.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.screentimer.other.Constants.ACTION_STOP
import com.example.screentimer.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.screentimer.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.screentimer.other.Constants.NOTIFICATION_ID
import com.example.screentimer.other.Constants.TIMER_UPDATE_INTERVAL
import com.example.screentimer.other.TrackingUtilities
import com.example.screentimer.receiver.DeviceAdmin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.sql.Time
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService() {
    var isFirst = true
    var serviceKilled = false

    private val timerRunInSeconds = MutableLiveData<Long>()
    private val countDownTimeInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var curNotificationBuilder: NotificationCompat.Builder

    companion object{
        val timeRunInMillis = MutableLiveData<Long>()
        val countDownInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
    }

    private fun postInitialValues(){
        isTracking.postValue(false)
        timerRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
        countDownTimeInSeconds.postValue(0L)
        countDownInMillis.postValue(1L)
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        postInitialValues()

        isTracking.observe(this, Observer {
            updateNotificationState(it)
        })
    }

    private fun killService(){
        serviceKilled = true
        isFirst = true
        pauseService()
        postInitialValues()

        stopForeground(true)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirst){
                        val duration = it.getLongExtra("duration", 0L)
                        durationInSecond = duration / 1000L
                        durationInMillis = duration
                        countDownTimeInSeconds.postValue(durationInSecond)
                        countDownInMillis.postValue(durationInMillis)
                        startForegroundService()
                        isFirst = false
                    }
                    else{
                        Timber.d("Resuming service!")
                        //startTimer()
                        startCountdownTimer()
                    }
                }

                ACTION_PAUSE -> {
                    pauseService()
                    Timber.d("Paused service!")
                }

                ACTION_STOP -> {
                    killService()
                    Timber.d("Stopped service!")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerIsEnabled = false
    }

    private fun updateNotificationState(isTracking: Boolean){
        val notificationActionText = if (isTracking) "Pause" else "Resume"
        val pendingIntent = if  (isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE
            }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notificationManager =  getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
        if(!serviceKilled){
            curNotificationBuilder = baseNotificationBuilder
                    .addAction(R.drawable.ic_settings, notificationActionText, pendingIntent)
            notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
        }
    }

    private var isTimerIsEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L
    private var durationInSecond = 0L
    private var durationInMillis = 0L

    private fun startTimer(){
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerIsEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!){
                lapTime = System.currentTimeMillis() - timeStarted
                timeRunInMillis.postValue(timeRun + lapTime)
                if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L){
                    timerRunInSeconds.postValue(timerRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    private fun startCountdownTimer(){
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerIsEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!){
                lapTime = System.currentTimeMillis() - timeStarted
                timeRunInMillis.postValue(timeRun + lapTime)

                if (countDownInMillis.value!! >= 0){
                    countDownInMillis.postValue(durationInMillis - lapTime)
                    if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L){
                        timerRunInSeconds.postValue(timerRunInSeconds.value!! + 1)
                        countDownTimeInSeconds.postValue(countDownTimeInSeconds.value!! - 1)
                        lastSecondTimestamp += 1000L
                    }
                    delay(TIMER_UPDATE_INTERVAL)
                }
                else{
                    lockScreen()
                    killService()
                    break
                }
            }
            timeRun += lapTime
        }
    }

    private fun lockScreen(){
        Timber.d("from service")
        val deviceManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        deviceManager.lockNow()
    }

    private fun startForegroundService() {
        //startTimer()
        startCountdownTimer()
        isTracking.postValue(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }

        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        countDownTimeInSeconds.observe(this, Observer {
            if(!serviceKilled){
                val notification = curNotificationBuilder
                        .setContentText(TrackingUtilities.getFormattedStopWatchTime(it * 1000L))
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager){
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}