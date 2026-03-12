package com.example.testgeofenceplayservices.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.example.testgeofenceplayservices.PlayServicesVersionProvider
import com.example.testgeofenceplayservices.data.GeofenceTriggerEvent
import com.example.testgeofenceplayservices.data.GeofenceTriggerEventStore
import com.example.testgeofenceplayservices.diagnostics.GeofenceDiagnosticsLogger
import com.example.testgeofenceplayservices.geofencing.GeofenceManager

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>()
    private val geofenceManager = GeofenceManager(appContext)

    val geofenceDefinitions by lazy { geofenceManager.geofenceDefinitions }
    val playServicesVersionText: String by lazy {
        PlayServicesVersionProvider.getPlayServicesVersion(appContext) ?: "unavailable"
    }

    var statusText by mutableStateOf("Tap the button to start geofencing.")
        private set
    var triggerEvents by mutableStateOf<List<GeofenceTriggerEvent>>(emptyList())
        private set
    var showBackgroundPermissionDialog by mutableStateOf(false)
        private set

    private var shouldStartAfterPermission = false

    fun onAppStarted() {
        refreshTriggerEvents()
    }

    fun onTriggerEventsUpdated() {
        refreshTriggerEvents()
    }

    fun onStartGeofencingRequested() {
        shouldStartAfterPermission = true
        GeofenceDiagnosticsLogger.logSessionInfoOnce(appContext, reason = "start_geofencing_click")
    }

    fun onForegroundPermissionDenied() {
        statusText = "Precise location permission not granted."
        shouldStartAfterPermission = false
    }

    fun onBackgroundPermissionRequired() {
        showBackgroundPermissionDialog = true
    }

    fun onBackgroundDialogGoToSettings() {
        showBackgroundPermissionDialog = false
    }

    fun onBackgroundDialogCancel() {
        showBackgroundPermissionDialog = false
        statusText = "Background location is required for geofencing."
        shouldStartAfterPermission = false
    }

    fun onBackgroundPermissionResult(granted: Boolean) {
        if (granted && shouldStartAfterPermission) {
            startGeofencing()
            return
        }
        statusText = "Background location permission not granted."
        shouldStartAfterPermission = false
    }

    fun shouldStartAfterPermission(): Boolean = shouldStartAfterPermission

    fun startGeofencing() {
        geofenceManager.registerGeofences { result ->
            statusText = result.message
            shouldStartAfterPermission = false
        }
    }

    fun clearTriggerEvents() {
        GeofenceTriggerEventStore.clearAll(appContext)
        triggerEvents = emptyList()
        statusText = "Saved trigger events were cleared."
    }

    private fun refreshTriggerEvents() {
        val events = GeofenceTriggerEventStore.loadAll(appContext)
        triggerEvents = events
        val latest = events.lastOrNull() ?: return
        statusText = "Trigger ${latest.transition} at ${com.example.testgeofenceplayservices.ui.format.formatEventTime(latest.eventTimeEpochMillis)}."
    }
}
