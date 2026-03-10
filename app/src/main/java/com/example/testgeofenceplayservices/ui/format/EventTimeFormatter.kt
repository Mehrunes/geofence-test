package com.example.testgeofenceplayservices.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatEventTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
