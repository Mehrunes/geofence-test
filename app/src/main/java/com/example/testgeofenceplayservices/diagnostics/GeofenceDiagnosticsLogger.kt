package com.example.testgeofenceplayservices.diagnostics

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import com.example.testgeofenceplayservices.PlayServicesVersionProvider
import com.example.testgeofenceplayservices.geofencing.PoznanGeofences
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes
import java.util.Locale
import kotlin.math.abs

object GeofenceDiagnosticsLogger {

    private const val TAG = "GeofenceDiagnostics"
    private var hasLoggedSessionInfo = false

    data class RegistrationParams(
        val initialTrigger: Int,
        val transitionTypes: Int,
        val expirationDurationMs: Long,
    )

    @Synchronized
    fun logSessionInfoOnce(context: Context, reason: String) {
        if (hasLoggedSessionInfo) return
        hasLoggedSessionInfo = true

        val playServicesInfo = PlayServicesVersionProvider.getPlayServicesInfo(context)
        Log.i(
            TAG,
            "session_start reason=$reason " +
                "playServicesVersionName=${playServicesInfo?.versionName ?: "unknown"} " +
                "playServicesVersionCode=${playServicesInfo?.versionCode ?: "unknown"} " +
                "androidApi=${Build.VERSION.SDK_INT} " +
                "manufacturer=${Build.MANUFACTURER} " +
                "model=${Build.MODEL}"
        )
    }

    fun logGeofenceRegistrationRequest(
        geofenceDefinitions: List<PoznanGeofences.GeofenceDefinition>,
        params: RegistrationParams,
    ) {
        Log.i(
            TAG,
            "registration_request_summary count=${geofenceDefinitions.size} " +
                "initialTrigger=${params.initialTrigger} transitionTypes=${params.transitionTypes} " +
                "expirationDurationMs=${params.expirationDurationMs}"
        )

        geofenceDefinitions.forEach { geofence ->
            Log.i(
                TAG,
                "registration_geofence requestId=${geofence.requestId} " +
                    "lat=${formatDouble(geofence.latitude)} lon=${formatDouble(geofence.longitude)} " +
                    "radiusMeters=${formatFloat(geofence.radiusMeters)}"
            )
        }
    }

    fun logGeofenceOperationResult(
        operation: String,
        success: Boolean,
        throwable: Throwable? = null,
    ) {
        if (success) {
            Log.i(TAG, "$operation success=true")
            return
        }

        val apiException = throwable as? ApiException
        val statusCode = apiException?.statusCode
        val statusName = statusCode?.let { GeofenceStatusCodes.getStatusCodeString(it) } ?: "unknown"
        val message = throwable?.localizedMessage ?: "unknown"
        Log.e(
            TAG,
            "$operation success=false statusCode=${statusCode ?: "n/a"} statusName=$statusName message=$message"
        )
    }

    fun logTriggerEventDiagnostics(
        transition: String,
        requestIds: List<String>,
        triggeringLocation: Location?,
        eventReceivedAtEpochMs: Long,
        eventReceivedElapsedRealtimeNanos: Long,
    ) {
        Log.i(
            TAG,
            "trigger_event transition=$transition " +
                "requestIds=${requestIds.ifEmpty { listOf("none") }.joinToString(",")} " +
                "receivedAtEpochMs=$eventReceivedAtEpochMs " +
                "receivedElapsedRealtimeNanos=$eventReceivedElapsedRealtimeNanos"
        )

        if (triggeringLocation == null) {
            Log.w(TAG, "trigger_event location unavailable")
        }
    }

    fun logTriggeredGeofenceEvent(
        transition: String,
        definition: PoznanGeofences.GeofenceDefinition,
        triggeringLocation: Location,
        distanceMeters: Float,
    ) {
        val radiusMeters = definition.radiusMeters
        val inside = distanceMeters <= radiusMeters
        val distanceToBoundaryMeters = abs(radiusMeters - distanceMeters)
        val status = if (inside) "INSIDE" else "OUTSIDE"
        val statusEmoji = if (inside) "✅" else "❌"
        val boundaryText =
            if (inside) {
                "inside geofence by ${formatFloat(distanceToBoundaryMeters)}m"
            } else {
                "outside geofence by ${formatFloat(distanceToBoundaryMeters)}m"
            }
        val accuracy =
            if (triggeringLocation.hasAccuracy()) "${formatFloat(triggeringLocation.accuracy)}m" else "n/a"
        Log.i(
            TAG,
            "$statusEmoji $transition ${definition.requestId}: " +
                "geofence(lat=${formatDouble(definition.latitude)}, lon=${formatDouble(definition.longitude)}, radius=${formatFloat(radiusMeters)}m), " +
                "triggered at location(lat=${formatDouble(triggeringLocation.latitude)}, lon=${formatDouble(triggeringLocation.longitude)}, accuracy=$accuracy, time=${triggeringLocation.time}), " +
                "${formatFloat(distanceMeters)}m from center, $boundaryText ($status)"
        )
    }

    private fun formatFloat(value: Float): String = String.format(Locale.US, "%.3f", value)

    private fun formatDouble(value: Double): String = String.format(Locale.US, "%.6f", value)
}
