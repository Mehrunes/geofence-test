package com.example.testgeofenceplayservices.data

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object GeofenceTriggerEventStore {

    const val ACTION_TRIGGER_EVENT_UPDATED =
        "com.example.testgeofenceplayservices.action.TRIGGER_EVENT_UPDATED"

    private const val prefsName = "geofence_trigger_events"
    private const val keyEventsJson = "events_json"
    private const val maxStoredEvents = 100

    fun append(context: Context, event: GeofenceTriggerEvent) {
        val updated = (loadAll(context) + event)
            .sortedBy { it.eventTimeEpochMillis }
            .takeLast(maxStoredEvents)

        writeAll(context, updated)
        notifyUpdated(context)
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit {
            remove(keyEventsJson)
        }
        notifyUpdated(context)
    }

    fun loadAll(context: Context): List<GeofenceTriggerEvent> {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val raw = prefs.getString(keyEventsJson, null)

        return if (!raw.isNullOrBlank()) {
            runCatching { decode(raw) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
    }

    private fun writeAll(context: Context, events: List<GeofenceTriggerEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject().apply {
                    put("geofenceRequestIds", JSONArray(event.geofenceRequestIds))
                    put("transition", event.transition)
                    put("triggerLatitude", event.triggerLatitude)
                    put("triggerLongitude", event.triggerLongitude)
                    put("accuracyMeters", event.accuracyMeters.toDouble())
                    put("eventTimeEpochMillis", event.eventTimeEpochMillis)
                }
            )
        }

        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit { putString(keyEventsJson, array.toString()) }
    }

    private fun decode(raw: String): List<GeofenceTriggerEvent> {
        val array = JSONArray(raw)
        val events = mutableListOf<GeofenceTriggerEvent>()
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            val idsJson = json.optJSONArray("geofenceRequestIds") ?: JSONArray()
            val ids = buildList {
                for (idIndex in 0 until idsJson.length()) {
                    val id = idsJson.optString(idIndex)
                    if (!id.isNullOrBlank()) add(id)
                }
            }
            val transition = json.optString("transition")
            val latitude = json.optDouble("triggerLatitude")
            val longitude = json.optDouble("triggerLongitude")
            val accuracy = json.optDouble("accuracyMeters").toFloat()
            val eventTime = json.optLong("eventTimeEpochMillis")

            if (
                ids.isNotEmpty() &&
                transition.isNotBlank() &&
                !latitude.isNaN() &&
                !longitude.isNaN() &&
                accuracy > 0f &&
                eventTime > 0L
            ) {
                events.add(
                    GeofenceTriggerEvent(
                        geofenceRequestIds = ids,
                        transition = transition,
                        triggerLatitude = latitude,
                        triggerLongitude = longitude,
                        accuracyMeters = accuracy,
                        eventTimeEpochMillis = eventTime,
                    )
                )
            }
        }
        return events.sortedBy { it.eventTimeEpochMillis }
    }

    private fun notifyUpdated(context: Context) {
        context.sendBroadcast(Intent(ACTION_TRIGGER_EVENT_UPDATED).setPackage(context.packageName))
    }
}
