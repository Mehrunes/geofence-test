package com.example.testgeofenceplayservices.data

data class GeofenceTriggerEvent(
    val geofenceRequestIds: List<String>,
    val transition: String,
    val triggerLatitude: Double,
    val triggerLongitude: Double,
    val accuracyMeters: Float,
    val eventTimeEpochMillis: Long,
)
