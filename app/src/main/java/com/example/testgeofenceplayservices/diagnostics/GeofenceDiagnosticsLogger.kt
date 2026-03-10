package com.example.testgeofenceplayservices.diagnostics

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import com.example.testgeofenceplayservices.PlayServicesVersionProvider
import com.example.testgeofenceplayservices.geofencing.PoznanDabrowskiegoGeofences
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes
import java.util.Locale
import kotlin.math.abs

object GeofenceDiagnosticsLogger {

    private const val TAG = "GeofenceDiagnostics"

    data class RegistrationParams(
        val initialTrigger: Int,
        val transitionTypes: Int,
        val expirationDurationMs: Long,
    )

    fun logEnvironmentSnapshot(context: Context, reason: String) {
        val playServicesInfo = PlayServicesVersionProvider.getPlayServicesInfo(context)
        Log.i(
            TAG,
            "environment reason=$reason " +
                "playServicesVersionName=${playServicesInfo?.versionName ?: "unknown"} " +
                "playServicesVersionCode=${playServicesInfo?.versionCode ?: "unknown"} " +
                "androidApi=${Build.VERSION.SDK_INT} " +
                "manufacturer=${Build.MANUFACTURER} " +
                "model=${Build.MODEL}"
        )
    }

    fun logGeofenceRegistrationRequest(
        context: Context,
        geofenceDefinitions: List<PoznanDabrowskiegoGeofences.GeofenceDefinition>,
        params: RegistrationParams,
    ) {
        logEnvironmentSnapshot(context, reason = "registration_request")
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
        context: Context,
        transition: String,
        requestIds: List<String>,
        triggeringLocation: Location?,
        eventReceivedAtEpochMs: Long,
        eventReceivedElapsedRealtimeNanos: Long,
    ) {
        logEnvironmentSnapshot(context, reason = "trigger_event")
        Log.i(
            TAG,
            "trigger_event_summary transition=$transition " +
                "requestIds=${requestIds.ifEmpty { listOf("none") }.joinToString(",")} " +
                "eventReceivedAtEpochMs=$eventReceivedAtEpochMs " +
                "eventReceivedElapsedRealtimeNanos=$eventReceivedElapsedRealtimeNanos"
        )

        if (triggeringLocation == null) {
            Log.w(TAG, "trigger_event_location missing=true")
            return
        }

        val accuracy =
            if (triggeringLocation.hasAccuracy()) formatFloat(triggeringLocation.accuracy) else "n/a"
        Log.i(
            TAG,
            "trigger_event_location provider=${triggeringLocation.provider} " +
                "lat=${formatDouble(triggeringLocation.latitude)} " +
                "lon=${formatDouble(triggeringLocation.longitude)} " +
                "accuracyMeters=$accuracy " +
                "locationTimeEpochMs=${triggeringLocation.time}"
        )
    }

    fun logTriggeredGeofenceDistance(
        requestId: String,
        distanceMeters: Float,
        radiusMeters: Float,
    ) {
        val inside = distanceMeters <= radiusMeters
        val ratio = if (radiusMeters > 0f) distanceMeters / radiusMeters else Float.NaN
        val distanceToBoundaryMeters = abs(radiusMeters - distanceMeters)
        val status = if (inside) "INSIDE" else "OUTSIDE"
        val statusEmoji = if (inside) "✅" else "❌"
        val boundaryLabel = if (inside) "insideByMeters" else "outsideByMeters"
        Log.i(
            TAG,
            "trigger_event_distance $statusEmoji status=$status requestId=$requestId " +
                "distanceToCenterMeters=${formatFloat(distanceMeters)} " +
                "radiusMeters=${formatFloat(radiusMeters)} " +
                "distanceToRadiusRatio=${formatFloat(ratio)} " +
                "$boundaryLabel=${formatFloat(distanceToBoundaryMeters)}"
        )
    }

    private fun formatFloat(value: Float): String = String.format(Locale.US, "%.3f", value)

    private fun formatDouble(value: Double): String = String.format(Locale.US, "%.6f", value)
}
