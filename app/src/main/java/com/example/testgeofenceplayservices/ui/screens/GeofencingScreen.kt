package com.example.testgeofenceplayservices.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.testgeofenceplayservices.data.GeofenceTriggerEvent
import com.example.testgeofenceplayservices.geofencing.PoznanGeofences
import com.example.testgeofenceplayservices.ui.format.formatEventTime
import com.example.testgeofenceplayservices.ui.theme.TestGeofencePlayServicesTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import java.util.Locale
import kotlin.math.roundToInt

private sealed interface MapOverlaySelection {
    data class Geofence(val requestId: String) : MapOverlaySelection
    data class TriggeringLocation(val eventTimeEpochMillis: Long) : MapOverlaySelection
}

@Composable
fun GeofencingScreen(
    statusText: String,
    playServicesVersionText: String,
    geofenceDefinitions: List<PoznanGeofences.GeofenceDefinition>,
    triggerEvents: List<GeofenceTriggerEvent>,
    isUserLocationEnabled: Boolean,
    onStartClick: () -> Unit,
    onClearEventsClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMap: Boolean = true,
) {
    val latestTriggerEvent = triggerEvents.lastOrNull()
    var showClearEventsDialog by remember { mutableStateOf(false) }

    var selectedOverlay by remember(latestTriggerEvent?.eventTimeEpochMillis, triggerEvents.size) {
        mutableStateOf<MapOverlaySelection?>(
            latestTriggerEvent?.let { MapOverlaySelection.TriggeringLocation(it.eventTimeEpochMillis) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Geofencing Poznan: ${PoznanGeofences.getAllDefinitions().size}")
        Text(text = "Google Play services: $playServicesVersionText")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = onStartClick) {
                Text(text = "Start geofencing")
            }
            Button(
                onClick = { showClearEventsDialog = true },
                enabled = triggerEvents.isNotEmpty(),
            ) {
                Text(text = "Clear events")
            }
        }
        Text(text = "Status: $statusText")

        if (showMap) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                GeofencingMap(
                    geofenceDefinitions = geofenceDefinitions,
                    triggerEvents = triggerEvents,
                    isUserLocationEnabled = isUserLocationEnabled,
                    onOverlaySelected = { selectedOverlay = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        when (val overlay = selectedOverlay) {
            is MapOverlaySelection.TriggeringLocation -> {
                val selectedEvent = triggerEvents.lastOrNull {
                    it.eventTimeEpochMillis == overlay.eventTimeEpochMillis
                }
                if (selectedEvent != null) {
                    TriggeringLocationInfoCard(
                        triggerEvent = selectedEvent,
                        geofenceDefinitions = geofenceDefinitions,
                    )
                }
            }

            is MapOverlaySelection.Geofence -> {
                val geofence = geofenceDefinitions.firstOrNull { it.requestId == overlay.requestId }
                if (geofence != null) {
                    GeofenceInfoCard(
                        geofence = geofence,
                        isLastTriggered = latestTriggerEvent?.geofenceRequestIds?.contains(geofence.requestId) == true,
                    )
                }
            }

            null -> {
                Text(
                    text = if (triggerEvents.isNotEmpty()) {
                        "Tap an orange circle to view details of the selected trigger."
                    } else {
                        "After a geofence trigger, a triggeringLocation circle appears with accuracy radius."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (showClearEventsDialog) {
        AlertDialog(
            onDismissRequest = { showClearEventsDialog = false },
            title = { Text(text = "Clear saved events?") },
            text = {
                Text(
                    text = "This will remove all saved geofence trigger events from the app."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearEventsDialog = false
                        onClearEventsClick()
                    }
                ) {
                    Text(text = "Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearEventsDialog = false }) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@Composable
fun BackgroundLocationPermissionDialog(
    onGoToSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = "Background location required") },
        text = {
            Text(
                text =
                    "Geofencing requires background location permission to detect " +
                        "enter/exit events when the app is not active."
            )
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) {
                Text(text = "Go to settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = "Cancel")
            }
        },
    )
}

@Composable
private fun GeofencingMap(
    geofenceDefinitions: List<PoznanGeofences.GeofenceDefinition>,
    triggerEvents: List<GeofenceTriggerEvent>,
    isUserLocationEnabled: Boolean,
    onOverlaySelected: (MapOverlaySelection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialCenter = geofenceDefinitions.firstOrNull()?.let {
        LatLng(it.latitude, it.longitude)
    } ?: LatLng(52.4164, 16.8856)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 14.5f)
    }

    val latestTriggerEvent = triggerEvents.maxByOrNull { it.eventTimeEpochMillis }

    LaunchedEffect(latestTriggerEvent?.eventTimeEpochMillis) {
        val event = latestTriggerEvent ?: return@LaunchedEffect
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(event.triggerLatitude, event.triggerLongitude),
                16f,
            )
        )
    }

    val triggeredIds = latestTriggerEvent?.geofenceRequestIds?.toSet().orEmpty()

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapClick = { onOverlaySelected(null) },
        properties = MapProperties(
            isMyLocationEnabled = isUserLocationEnabled,
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = isUserLocationEnabled,
        ),
    ) {
        geofenceDefinitions.forEach { geofence ->
            val center = LatLng(geofence.latitude, geofence.longitude)
            val isTriggered = geofence.requestId in triggeredIds

            Circle(
                center = center,
                radius = geofence.radiusMeters.toDouble(),
                strokeColor = if (isTriggered) Color(0xFF1565C0) else Color(0xFF2E7D32),
                fillColor = if (isTriggered) Color(0x331565C0) else Color(0x332E7D32),
                strokeWidth = if (isTriggered) 5f else 3f,
                clickable = true,
                zIndex = 1f,
                onClick = { _ ->
                    onOverlaySelected(MapOverlaySelection.Geofence(geofence.requestId))
                },
            )
        }

        val latestEventTime = latestTriggerEvent?.eventTimeEpochMillis
        triggerEvents
            .sortedBy { it.eventTimeEpochMillis }
            .forEach { event ->
                val triggerLatLng = LatLng(event.triggerLatitude, event.triggerLongitude)
                val isLatest = event.eventTimeEpochMillis == latestEventTime

                Circle(
                    center = triggerLatLng,
                    radius = event.accuracyMeters.toDouble().coerceAtLeast(5.0),
                    strokeColor = if (isLatest) Color(0xFFD84315) else Color(0xFFEF6C00),
                    fillColor = if (isLatest) Color(0x33F4511E) else Color(0x1AF57C00),
                    strokeWidth = if (isLatest) 5f else 3f,
                    clickable = true,
                    zIndex = if (isLatest) 3f else 2f,
                    onClick = { _ ->
                        onOverlaySelected(MapOverlaySelection.TriggeringLocation(event.eventTimeEpochMillis))
                    },
                )

                Marker(
                    state = MarkerState(position = triggerLatLng),
                    title = "Triggering location ${formatEventTime(event.eventTimeEpochMillis)}",
                    snippet = "${event.transition}, accuracy: ${event.accuracyMeters.roundToInt()} m",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        transitionToMarkerHue(event.transition)
                    ),
                    onClick = { _ ->
                        onOverlaySelected(MapOverlaySelection.TriggeringLocation(event.eventTimeEpochMillis))
                        false
                    },
                )
            }
    }
}

@Composable
private fun TriggeringLocationInfoCard(
    triggerEvent: GeofenceTriggerEvent,
    geofenceDefinitions: List<PoznanGeofences.GeofenceDefinition>,
    modifier: Modifier = Modifier,
) {
    val geofenceText = triggerEvent.geofenceRequestIds.joinToString(", ") { requestId ->
        val geofence = geofenceDefinitions.firstOrNull { it.requestId == requestId }
        if (geofence == null) {
            requestId
        } else {
            "$requestId (${formatLatLon(geofence.latitude, geofence.longitude)}, ${geofence.radiusMeters.roundToInt()} m)"
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "TriggeringLocation (${formatLatLon(triggerEvent.triggerLatitude, triggerEvent.triggerLongitude)})",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = "Geofence: $geofenceText")
            Text(text = "Transition: ${triggerEvent.transition}")
            Text(text = "Time: ${formatEventTime(triggerEvent.eventTimeEpochMillis)}")
            Text(text = "Accuracy: ${"%.1f".format(Locale.US, triggerEvent.accuracyMeters)} m")
        }
    }
}

@Composable
private fun GeofenceInfoCard(
    geofence: PoznanGeofences.GeofenceDefinition,
    isLastTriggered: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${geofence.requestId} (${
                    formatLatLon(
                        geofence.latitude,
                        geofence.longitude
                    )
                }, ${geofence.radiusMeters.roundToInt()} m)",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (isLastTriggered) {
                    "Status: triggered in the latest event."
                } else {
                    "Status: not triggered in the latest event."
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GeofencingScreenPreview() {
    TestGeofencePlayServicesTheme {
        GeofencingScreen(
            statusText = "Preview",
            playServicesVersionText = "260235035",
            geofenceDefinitions = PoznanGeofences.getAllDefinitions(),
            triggerEvents = listOf(
                GeofenceTriggerEvent(
                    geofenceRequestIds = listOf("dabrowskiego_5"),
                    transition = "ENTER",
                    triggerLatitude = 52.412807,
                    triggerLongitude = 16.891729,
                    accuracyMeters = 14f,
                    eventTimeEpochMillis = 1_740_390_000_000L,
                ),
                GeofenceTriggerEvent(
                    geofenceRequestIds = listOf("dabrowskiego_6"),
                    transition = "EXIT",
                    triggerLatitude = 52.413255,
                    triggerLongitude = 16.901242,
                    accuracyMeters = 12f,
                    eventTimeEpochMillis = 1_740_390_120_000L,
                ),
            ),
            isUserLocationEnabled = true,
            onStartClick = {},
            onClearEventsClick = {},
            showMap = false,
        )
    }
}

private fun transitionToMarkerHue(transition: String): Float {
    return when (transition.trim().uppercase(Locale.US)) {
        "ENTER" -> BitmapDescriptorFactory.HUE_GREEN
        "DWELL" -> BitmapDescriptorFactory.HUE_BLUE
        "EXIT" -> BitmapDescriptorFactory.HUE_RED
        else -> BitmapDescriptorFactory.HUE_ORANGE
    }
}

private fun formatLatLon(latitude: Double, longitude: Double): String {
    return "lat=${"%.6f".format(Locale.US, latitude)}, lon=${"%.6f".format(Locale.US, longitude)}"
}
