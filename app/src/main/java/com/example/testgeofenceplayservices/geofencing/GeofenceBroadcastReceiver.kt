package com.example.testgeofenceplayservices.geofencing

import android.content.*
import android.location.Location
import android.os.SystemClock
import android.util.Log
import com.example.testgeofenceplayservices.data.GeofenceTriggerEvent
import com.example.testgeofenceplayservices.data.GeofenceTriggerEventStore
import com.example.testgeofenceplayservices.diagnostics.GeofenceDiagnosticsLogger
import com.google.android.gms.location.*
import java.util.Locale

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
        val idsWithDetails = if (requestIds.isEmpty()) {
            "none"
        } else {
            requestIds.joinToString(separator = ", ") { requestId ->
                formatGeofenceIdWithDetails(requestId)
            }
        }
        val triggeringLocation = geofencingEvent.triggeringLocation
        val eventReceivedAtEpochMs = System.currentTimeMillis()
        val eventReceivedElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()

        Log.i(TAG, "Geofence transition=$transition, ids=$idsWithDetails")
        GeofenceDiagnosticsLogger.logTriggerEventDiagnostics(
            context = context,
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
                context = context,
                requestId = requestId,
                triggeringLatitude = triggeringLocation.latitude,
                triggeringLongitude = triggeringLocation.longitude,
            )
        }

        Log.i(
            TAG,
            "Triggering location: ${formatLocationForLog(triggeringLocation)}"
        )

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
        context: Context,
        requestId: String,
        triggeringLatitude: Double,
        triggeringLongitude: Double,
    ) {
        val definition = PoznanDabrowskiegoGeofences.findDefinition(requestId)
        if (definition == null) {
            Log.w(TAG, "No geofence definition found for requestId=$requestId")
            return
        }

        val distanceResult = FloatArray(1)
        Location.distanceBetween(
            triggeringLatitude,
            triggeringLongitude,
            definition.latitude,
            definition.longitude,
            distanceResult,
        )
        val distanceMeters = distanceResult[0]
        val inside = distanceMeters <= definition.radiusMeters
        val distanceToRadiusRatio =
            if (definition.radiusMeters > 0f) distanceMeters / definition.radiusMeters else Float.NaN
        val distanceFormatted = String.format(Locale.US, "%.2f", distanceMeters)
        val ratioFormatted = String.format(Locale.US, "%.3f", distanceToRadiusRatio)
        GeofenceDiagnosticsLogger.logTriggeredGeofenceDistance(
            requestId = requestId,
            distanceMeters = distanceMeters,
            radiusMeters = definition.radiusMeters,
        )

        Log.i(
            TAG,
            "requestId=$requestId distanceMeters=$distanceFormatted " +
                "radiusMeters=${definition.radiusMeters} " +
                "distanceToRadiusRatio=$ratioFormatted inside=$inside"
        )
    }

    companion object {

        private const val TAG = "GeofenceReceiver"
    }

    private fun formatGeofenceIdWithDetails(requestId: String): String {
        val definition = PoznanDabrowskiegoGeofences.findDefinition(requestId)
            ?: return "$requestId(lat=unknown, lon=unknown, radius=unknown)"
        val latitude = String.format(Locale.US, "%.6f", definition.latitude)
        val longitude = String.format(Locale.US, "%.6f", definition.longitude)
        val radius = String.format(Locale.US, "%.1f", definition.radiusMeters)
        return "$requestId(lat=$latitude, lon=$longitude, radius=${radius}m)"
    }

    private fun formatLocationForLog(location: Location?): String {
        if (location == null) return "null"
        val accuracy = if (location.hasAccuracy()) location.accuracy.toString() else "n/a"
        return "lat=${location.latitude}, lon=${location.longitude}, accuracy=$accuracy, time=${location.time}"
    }
}
