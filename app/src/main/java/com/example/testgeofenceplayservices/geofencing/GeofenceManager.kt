package com.example.testgeofenceplayservices.geofencing

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.PendingIntentCompat
import com.example.testgeofenceplayservices.diagnostics.GeofenceDiagnosticsLogger
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(context: Context) {

    data class RegistrationResult(
        val success: Boolean,
        val message: String,
    )

    val geofenceDefinitions: List<PoznanDabrowskiegoGeofences.GeofenceDefinition> by lazy {
        PoznanDabrowskiegoGeofences.getAllDefinitions()
    }

    private val appContext = context.applicationContext
    private val geofencingClient by lazy { LocationServices.getGeofencingClient(appContext) }
    private val geofencePendingIntent: PendingIntent by lazy {
        PendingIntentCompat.getBroadcast(
            appContext,
            0,
            Intent(appContext, GeofenceBroadcastReceiver::class.java).apply {
                action = GeofenceActions.ACTION_GEOFENCE_EVENT
            },
            PendingIntent.FLAG_UPDATE_CURRENT,
            true
        )!!
    }

    @Suppress("MissingPermission")
    fun registerGeofences(
        onResult: (RegistrationResult) -> Unit,
    ) {
        val geofences = PoznanDabrowskiegoGeofences.buildGeofences()
        val initialTriggerMask =
            GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(initialTriggerMask)
            .addGeofences(geofences)
            .build()

        GeofenceDiagnosticsLogger.logGeofenceRegistrationRequest(
            context = appContext,
            geofenceDefinitions = geofenceDefinitions,
            params = GeofenceDiagnosticsLogger.RegistrationParams(
                initialTrigger = initialTriggerMask,
                transitionTypes = PoznanDabrowskiegoGeofences.transitionTypesMask,
                expirationDurationMs = PoznanDabrowskiegoGeofences.expirationDurationMs,
            ),
        )

        @Suppress("MissingPermission")
        fun addGeofences() {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
                .addOnSuccessListener {
                    GeofenceDiagnosticsLogger.logGeofenceOperationResult(
                        operation = "addGeofences",
                        success = true,
                    )
                    onResult(
                        RegistrationResult(
                            success = true,
                            message = "Added ${geofences.size} geofences on Dabrowskiego Street.",
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    GeofenceDiagnosticsLogger.logGeofenceOperationResult(
                        operation = "addGeofences",
                        success = false,
                        throwable = exception,
                    )
                    onResult(
                        RegistrationResult(
                            success = false,
                            message = "Failed to add geofences: ${exception.localizedMessage ?: "unknown"}",
                        )
                    )
                }
        }

        @Suppress("MissingPermission")
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                GeofenceDiagnosticsLogger.logGeofenceOperationResult(
                    operation = "removeGeofences",
                    success = true,
                )
                addGeofences()
            }
            .addOnFailureListener { exception ->
                GeofenceDiagnosticsLogger.logGeofenceOperationResult(
                    operation = "removeGeofences",
                    success = false,
                    throwable = exception,
                )
                // Continue with registration to preserve existing behavior.
                addGeofences()
            }
    }
}
