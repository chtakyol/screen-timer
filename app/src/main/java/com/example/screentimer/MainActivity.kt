package com.example.screentimer

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.health.TimerStat
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.screentimer.databinding.ActivityMainBinding
import com.example.screentimer.receiever.TimerExpiredReceiver
import com.example.screentimer.receiever.TimerTickReceiver
import com.example.screentimer.util.NotificationUtil
import com.example.screentimer.util.PrefUtil
import com.skumar.flexibleciruclarseekbar.CircularSeekBar
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    companion object {
        lateinit var deviceManger: DevicePolicyManager
        fun lockPhone() {
            deviceManger.lockNow()
        }
    }

    enum class TimerState{
        Stopped, Paused, Running
    }

    private var secondRemaining = 0L
    private lateinit var timer : CountDownTimer
    private var timerLengthSeconds = 0L
    private var timerState = TimerState.Stopped
    private val enableResult = 1
    lateinit var compName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureSeekBar()

        binding.btnLock.setOnClickListener {
            startTimer()
        }

        binding.mCircularSeekBar.setOnCircularSeekBarChangeListener(object : CircularSeekBar.OnCircularSeekBarChangeListener {
            override fun onProgressChanged(CircularSeekBar: CircularSeekBar, progress: Float, fromUser: Boolean) {
                secondRemaining = progress.toLong()
                updateCountDownUI()
            }

            override fun onStartTrackingTouch(CircularSeekBar: CircularSeekBar) {

            }

            override fun onStopTrackingTouch(CircularSeekBar: CircularSeekBar) {

            }
        })

        title = "KotlinApp"
        deviceManger = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdmin::class.java)
        val active = deviceManger.isAdminActive(compName)
        if (active) {
            binding.btnEnable.text = "Disable"
            binding.btnLock.visibility = View.VISIBLE
        }
        else {
            binding.btnEnable.text = "Enable"
            binding.btnLock.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        initTimer()
        NotificationUtil.hideTimerNotification(this)
    }

    override fun onPause() {

        when (timerState) {
            TimerState.Running -> {
                timer.cancel()
                NotificationUtil.showTimerRunning(this, secondRemaining)
                Intent(this, CountdownIntentService::class.java).also {
                    startService(it)
                }
            }
            TimerState.Paused -> {
                NotificationUtil.showTimerPaused(this)
            }
            TimerState.Stopped -> {
            }
        }
        Log.d("MainActivity",
                "pause ${secondRemaining.toString()}")
        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondRemaining, this)
        PrefUtil.setTimerState(timerState, this)
        super.onPause()
    }

    private fun initTimer(){
        timerState = PrefUtil.getTimerState(this)
        when (timerState) {
            TimerState.Running -> {
                CountdownIntentService.stopService()
                secondRemaining = PrefUtil.getSecondsRemaining(this)
                updateCountDownUI()
                startTimer()
            }
            TimerState.Paused -> {
                secondRemaining = PrefUtil.getSecondsRemaining(this)
                updateCountDownUI()
            }
            TimerState.Stopped -> {
            }
        }
    }

    private fun onTimerFinished(){
        timerState = TimerState.Stopped
        PrefUtil.setTimerState(timerState, this)
        lockPhone()
    }

    private fun startTimer(){
        timerState = TimerState.Running
        PrefUtil.setTimerState(timerState, this)
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

    private fun updateCountDownUI(){

        val minutesUntilFinished = secondRemaining / 60
        val secondsInMinuteUntilFinished = secondRemaining - minutesUntilFinished * 60
        val secondStr = secondsInMinuteUntilFinished.toString()
        binding.minTextView.setText("$secondStr")
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
            binding.btnEnable.text = "Enable"
            binding.btnLock.visibility = View.GONE
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
                    binding.btnEnable.text = "Disable"
                    binding.btnLock.visibility = View.VISIBLE
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

    private fun configureSeekBar(){
        binding.mCircularSeekBar.setDrawMarkings(false)
        binding.mCircularSeekBar.setRoundedEdges(true)
        binding.mCircularSeekBar.setIsGradient(false)
        binding.mCircularSeekBar.arcColor = ContextCompat.getColor(this, R.color.lightPurple)
        binding.mCircularSeekBar.arcThickness = 48
//        binding.mCircularSeekBar.progress = progressValue
        binding.mCircularSeekBar.valueStep = 1
    }

}
