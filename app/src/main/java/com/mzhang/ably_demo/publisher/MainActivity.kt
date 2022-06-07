package com.mzhang.ably_demo.publisher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.*
import com.mzhang.ably_demo.R
import kotlinx.android.synthetic.main.activity_publishing_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : PublisherServiceActivity() {
    private lateinit var appPreferences: AppPreferences
    private var trackablesUpdateJob: Job? = null

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publishing_main)
        appPreferences = AppPreferences.getInstance(this)

        PermissionsHelper.requestLocationPermission(this)
        publisherServiceSwitch.setOnClickListener { onServiceSwitchClick(publisherServiceSwitch.isChecked) }
        addTrackableFab.setOnClickListener {         addTrackableClicked() }
    }

    override fun onStart() {
        super.onStart()
        updateLocationSourceMethodInfo()
    }

    private fun updateLocationSourceMethodInfo() {
//        locationSourceMethodTextView.text = getString(appPreferences.getLocationSource().displayNameResourceId)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionsHelper.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            onLocationPermissionGranted = { showLongToast(R.string.info_permission_granted) }
        )
    }

    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        indicatePublisherServiceIsOn()
        publisherService.publisher?.let { publisher ->

            trackablesUpdateJob = publisher.trackables
                .onEach {
//                    trackablesAdapter.trackables = it.toList()
                    if (it.isEmpty() && publisherService.publisher != null) {
//                        hideTrackablesList()
                        try {
                            publisherService.publisher?.stop()
                            publisherService.publisher = null
                        } catch (e: Exception) {
                            showLongToast(R.string.error_stopping_publisher_failed)
                        }
                    } else {
//                        addTrackableClicked()
//                        showTrackablesList()
                    }
                }
                .launchIn(scope)
        }
    }

    override fun onPublisherServiceDisconnected() {
        indicatePublisherServiceIsOff()
        trackablesUpdateJob?.cancel()
    }

    private fun indicatePublisherServiceIsOn() {
        publisherServiceSwitch.isChecked = true
    }

    private fun indicatePublisherServiceIsOff() {
        publisherServiceSwitch.isChecked = false
    }

    private fun onServiceSwitchClick(isSwitchingOn: Boolean) {
        if (isSwitchingOn) {
            startAndBindPublisherService()
        } else {
            stopPublisherService()
//            if (trackablesAdapter.trackables.isEmpty()) {
//                stopPublisherService()
//            } else {
//                showCannotStopServiceDialog()
//                indicatePublisherServiceIsOn()
//            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun addTrackableClicked() {
        getTrackableId().let { trackableId ->
            if (!PermissionsHelper.hasFineOrCoarseLocationPermissionGranted(this)) {
                PermissionsHelper.requestLocationPermission(this)
                return
            }
            if (trackableId.isNotEmpty()) {
//                showLoading()
                if (isPublisherServiceStarted()) {
                    publisherService?.let { publisherService ->
                        scope.launch(CoroutineExceptionHandler { _, _ -> onAddTrackableFailed() }) {
                            if (!publisherService.isPublisherStarted) {
                                startPublisher(publisherService)
                            }
                            publisherService.publisher!!.track(createTrackable(trackableId))
                            showTrackableDetailsScreen(trackableId)
                            finish()
                        }
                    }
                } else {
                    onAddTrackableFailed(R.string.error_publisher_service_not_started)
                }
            } else {
                showLongToast(R.string.error_no_trackable_id)
            }
        }
    }

    private fun showTrackableDetailsScreen(trackableId: String) {
        startActivity(
            Intent(this, TrackableDetailsActivity::class.java).apply {
                putExtra(TRACKABLE_ID_EXTRA, trackableId)
            }
        )
    }

    private fun createTrackable(trackableId: String): Trackable =
        Trackable(
            trackableId,
            constraints = DefaultResolutionConstraints(
                DefaultResolutionSet(createResolution()),
                DefaultProximity(spatial = 1.0),
                batteryLevelThreshold = 10.0f,
                lowBatteryMultiplier = 2.0f
            )
        )

    private fun createResolution(): Resolution {
        return Resolution(
            Accuracy.BALANCED,
            1000,
            1.0
        )
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private suspend fun startPublisher(publisherService: PublisherService) {
        publisherService.startPublisher()
    }

    private fun getTrackableId(): String = "trackableId1"

    private fun onAddTrackableFailed(@StringRes messageResourceId: Int = R.string.error_trackable_adding_failed) {
        showLongToast(messageResourceId)
//        hideLoading()
    }


}