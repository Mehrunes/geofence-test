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
        52.410668 to 16.912540, // Most Teatralny (start)
        52.411565 to 16.909247,
        52.412315 to 16.905835,
        52.413056 to 16.902419,
        52.413800 to 16.899006,
        52.414548 to 16.895593,
        52.43871447 to 16.932027,
        52.4408028 to 16.93179448,
        52.435443179710134 to 16.92701340794936,
        52.43363939360835 to 16.929675313549932
        /*        52.413760 to 16.898970,
                52.414349 to 16.896761,
                52.414796 to 16.894460,
                52.415299 to 16.892188,
                52.415856 to 16.889961,
                52.416277 to 16.887650,
                52.416775 to 16.885375,
                52.417278 to 16.883102,
                52.418025 to 16.881019,
                52.418819 to 16.878979,
                52.419612 to 16.876937,
                52.420384 to 16.874875,
                52.421354 to 16.873098,
                52.422043 to 16.870973,*/
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
