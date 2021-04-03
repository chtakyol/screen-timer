package com.example.screentimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.annotation.Nullable
import com.example.screentimer.receiever.TimerExpiredReceiver
import com.example.screentimer.util.NotificationUtil
import com.example.screentimer.util.PrefUtil
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var deviceManger: DevicePolicyManager
        fun setAlarm(context: Context, nowSeconds : Long, secondsRemaining: Long): Long{
            val wakeUpTime = (nowSeconds + secondsRemaining) * 1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        // TODO: set repeating alarm and trigger TimeTickReceiver

        fun removeAlarm(context: Context){
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        fun lockPhone() {
            deviceManger.lockNow()
        }

        val nowSeconds: Long
            get() = Calendar.getInstance().timeInMillis / 1000
    }

    enum class TimerState{
        Stopped, Paused, Running
    }

    private lateinit var timer : CountDownTimer
    private var timerLengthSeconds = 0L
    private var timerState = TimerState.Stopped

    private var secondRemaining = 0L

    private val enableResult = 1

    lateinit var compName: ComponentName
    lateinit var btnEnable: Button
    lateinit var btnLock: Button
    lateinit var seekBar: SeekBar
    lateinit var minTextView : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnLock = findViewById(R.id.btnLock)
        seekBar = findViewById(R.id.seekBar)
        minTextView = findViewById(R.id.minTextView)

        btnLock.setOnClickListener {
            startTimer()
            timerState = TimerState.Running
            updateButtons()
        }

        title = "KotlinApp"
        btnEnable = findViewById(R.id.btnEnable)

        deviceManger = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdmin::class.java)

        val active = deviceManger.isAdminActive(compName)
        if (active) {
            btnEnable.text = "Disable"
            btnLock.visibility = View.VISIBLE
        }
        else {
            btnEnable.text = "Enable"
            btnLock.visibility = View.GONE
        }

        seekBar.max = 60
        seekBar.setOnSeekBarChangeListener(object :
        SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                secondRemaining = progress.toLong()
                updateCountDownUI()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    override fun onResume() {
        super.onResume()
        initTimer()

        removeAlarm(this)
        NotificationUtil.hideTimerNotification(this)
    }

    override fun onPause() {
        super.onPause()

        if (timerState == TimerState.Running){
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds, secondRemaining)
            NotificationUtil.showTimerRunning(this, secondRemaining)
        }
        else if (timerState == TimerState.Paused){
            NotificationUtil.showTimerPaused(this)
        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    private fun initTimer(){
        timerState = PrefUtil.getTimerState(context = this)

        if (timerState == TimerState.Stopped){
            setNewTimerLength()
        }
        else{
            setPreviousTimerLength()
        }
        secondRemaining = if (timerState == TimerState.Running || timerState == TimerState.Paused)
            PrefUtil.getSecondsRemaining(context = this)
        else
            timerLengthSeconds

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)

        if (alarmSetTime > 0){
            secondRemaining -= nowSeconds - alarmSetTime
        }

        if (secondRemaining <= 0) {
            onTimerFinished()
        }
        else if (timerState == TimerState.Running){
            startTimer()
        }

        updateButtons()
        updateCountDownUI()
    }

    private fun onTimerFinished(){
        timerState = TimerState.Stopped

        setNewTimerLength()

        //progress_countdown.progress = 0

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondRemaining = timerLengthSeconds

        updateButtons()
        updateCountDownUI()

        lockPhone()
    }

    private fun startTimer(){
        timerState = TimerState.Paused
        timer = object : CountDownTimer(secondRemaining * 1000, 1000){
            override fun onFinish() {
                onTimerFinished()
            }
            override fun onTick(millisUntilFinished: Long) {
                secondRemaining = millisUntilFinished / 1000
                updateCountDownUI()
                updateNotification()
            }
        }.start()
    }

    private fun setNewTimerLength(){
        val lengthInMinutes = PrefUtil.getTimerLenght(this)
        timerLengthSeconds = (lengthInMinutes * 60L)
        //progress_countdown.max=timerLengthSecond.toInt()
    }

    private fun setPreviousTimerLength(){
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(this)
        //progress_countdown.max=timerLenghtSeconds.toInt()
    }

    private fun updateCountDownUI(){
        val minutesUntilFinished = secondRemaining / 60
        val secondsInMinuteUntilFinished = secondRemaining - minutesUntilFinished * 60
        val secondStr = secondsInMinuteUntilFinished.toString()
        minTextView.text = "$minutesUntilFinished:${
            if (secondStr.length == 2 ) secondStr
            else "0" + secondStr}"
        //progress_countdown.progress = (timerLengthSeconds - secondsRemaining).toInt()
    }

    private fun updateButtons(){
        NotificationUtil.showTimerRunning(this, secondRemaining)
    }

    private fun updateNotification(){
        NotificationUtil.showTimerRunning(this, secondRemaining)
    }

    fun enablePhone(view: View) {
        val active = deviceManger.isAdminActive(compName)
        if (active) {
            deviceManger.removeActiveAdmin(compName)
            btnEnable.text = "Enable"
            btnLock.visibility = View.GONE
        }
        else {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You should enable the app!")
            startActivityForResult(intent, enableResult)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            enableResult -> {
                if (resultCode == RESULT_OK) {
                    btnEnable.text = "Disable"
                    btnLock.visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                            applicationContext, "Failed!",
                            Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }

}