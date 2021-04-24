package com.example.screentimer.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.screentimer.R
import com.example.screentimer.other.Constants.ACTION_PAUSE
import com.example.screentimer.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.screentimer.other.TrackingUtilities
import com.example.screentimer.services.TrackingService
import com.example.screentimer.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_counter.*
import timber.log.Timber

@AndroidEntryPoint
class CounterFragment : Fragment(R.layout.fragment_counter) {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false

    private var curTimeInMillis = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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