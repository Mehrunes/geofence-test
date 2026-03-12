package com.example.testgeofenceplayservices.geofencing

import android.content.*
import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.example.testgeofenceplayservices.data.GeofenceTriggerEvent
import com.example.testgeofenceplayservices.data.GeofenceTriggerEventStore
import com.example.testgeofenceplayservices.diagnostics.GeofenceDiagnosticsLogger
import com.google.android.gms.location.*

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GeofenceActions.ACTION_GEOFENCE_EVENT) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            val error = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing error: $error")
            return
        }

        val transition = transitionToName(geofencingEvent.geofenceTransition)
        val requestIds = geofencingEvent.triggeringGeofences
            ?.map { it.requestId }
            .orEmpty()
        val triggeringLocation = geofencingEvent.triggeringLocation
        val eventReceivedAtEpochMs = System.currentTimeMillis()
        val eventReceivedElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

        GeofenceDiagnosticsLogger.logTriggerEventDiagnostics(
            transition = transition,
            requestIds = requestIds,
            triggeringLocation = triggeringLocation,
            eventReceivedAtEpochMs = eventReceivedAtEpochMs,
            eventReceivedElapsedRealtimeNanos = eventReceivedElapsedRealtimeNanos,
        )

        if (triggeringLocation == null) {
            Log.w(TAG, "No triggeringLocation available - cannot calculate distance.")
            return
        }

        requestIds.forEach { requestId ->
            logDistanceFromCenter(
                transition = transition,
                requestId = requestId,
                triggeringLocation = triggeringLocation,
            )
        }

        GeofenceTriggerEventStore.append(
            context = context,
            event = GeofenceTriggerEvent(
                geofenceRequestIds = requestIds,
                transition = transition,
                triggerLatitude = triggeringLocation.latitude,
                triggerLongitude = triggeringLocation.longitude,
                accuracyMeters = triggeringLocation.accuracy,
                eventTimeEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun transitionToName(transition: Int): String {
        return when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
            else -> "UNKNOWN($transition)"
        }
    }

    private fun logDistanceFromCenter(
        transition: String,
        requestId: String,
        triggeringLocation: Location,
    ) {
        val definition = PoznanDabrowskiegoGeofences.findDefinition(requestId)
        if (definition == null) {
            Log.w(TAG, "No geofence definition found for requestId=$requestId")
            return
        }

        val distanceResult = FloatArray(1)
        Location.distanceBetween(
            triggeringLocation.latitude,
            triggeringLocation.longitude,
            definition.latitude,
            definition.longitude,
            distanceResult,
        )
        val distanceMeters = distanceResult[0]
        GeofenceDiagnosticsLogger.logTriggeredGeofenceEvent(
            transition = transition,
            definition = definition,
            triggeringLocation = triggeringLocation,
            distanceMeters = distanceMeters,
        )
    }

    companion object {

        private const val TAG = "GeofenceReceiver"
    }
}
