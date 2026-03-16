package com.example.testgeofenceplayservices.geofencing

import com.google.android.gms.location.Geofence

object PoznanGeofences {

    data class GeofenceDefinition(
        val requestId: String,
        val latitude: Double,
        val longitude: Double,
        val radiusMeters: Float,
    )

    private val centerPoints: List<Pair<Double, Double>> = listOf(
        52.419300022511244 to 16.89956311671255,
        52.41992906246642 to 16.895362170931158
    )

    private val radiiMeters = floatArrayOf(30f, 35f, 40f, 45f, 60f, 80f)
    private val definitions: List<GeofenceDefinition> by lazy { buildDefinitions() }
    const val transitionTypesMask: Int =
        Geofence.GEOFENCE_TRANSITION_ENTER or
            Geofence.GEOFENCE_TRANSITION_EXIT or
            Geofence.GEOFENCE_TRANSITION_DWELL
    const val expirationDurationMs: Long = Geofence.NEVER_EXPIRE

    fun buildGeofences(): List<Geofence> =
        definitions.map { definition ->
            Geofence.Builder()
                .setRequestId(definition.requestId)
                .setCircularRegion(definition.latitude, definition.longitude, definition.radiusMeters)
                .setTransitionTypes(transitionTypesMask)
                .setExpirationDuration(expirationDurationMs)
                .setLoiteringDelay(30_000)
                .build()
        }

    fun getAllDefinitions(): List<GeofenceDefinition> = definitions

    fun findDefinition(requestId: String): GeofenceDefinition? {
        return definitions.firstOrNull { it.requestId == requestId }
    }

    private fun buildDefinitions(): List<GeofenceDefinition> =
        centerPoints.mapIndexed { index, point ->
            val (latitude, longitude) = point
            val radius = radiiMeters[index % radiiMeters.size]

            GeofenceDefinition(
                requestId = "geofence_${index + 1}",
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radius,
            )
        }
}
