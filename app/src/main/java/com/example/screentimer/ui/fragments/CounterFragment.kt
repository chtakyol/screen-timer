package com.example.screentimer.ui.fragments

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.screentimer.R
import com.example.screentimer.other.Constants.ACTION_PAUSE
import com.example.screentimer.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.screentimer.other.TrackingUtilities
import com.example.screentimer.receiver.DeviceAdmin
import com.example.screentimer.services.TrackingService
import com.example.screentimer.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_counter.*
import timber.log.Timber

@AndroidEntryPoint
class CounterFragment : Fragment(R.layout.fragment_counter) {

    private val enableResult = 1

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false

    private var curTimeInMillis = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Device admin policy permission request.
        // todo: refactor to method keep clean amk!
        val deviceManger = activity?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val compName = ComponentName(requireContext(), DeviceAdmin::class.java)
        val active = deviceManger.isAdminActive(compName)

        if (!active) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You should enable the app!")
            startActivityForResult(intent, enableResult)
        }

        Timber.d("active return!" + active)
        //-------------------------------------------------
        btnToggleRun.setOnClickListener {
            toggleRun()
        }

        subscribeToObservers()
    }



    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedTime = TrackingUtilities.getFormattedStopWatchTime(curTimeInMillis, true)
            tvTimer.text = formattedTime
        })


    }

    private fun toggleRun(){
        // todo replace wih image buttons
        if(isTracking){
            sendCommandToService(ACTION_PAUSE, 20L)
        }
        else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE, 20L)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking){
            btnToggleRun.text = "Start"
            btnFinishRun.visibility= View.VISIBLE
        }
        else{
            btnToggleRun.text = "Stop"
            btnFinishRun.visibility = View.GONE
        }
    }

    private fun sendCommandToService(action: String, duration: Long){
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            it.putExtra("duration", duration)
            requireContext().startService(it)
        }
    }
}