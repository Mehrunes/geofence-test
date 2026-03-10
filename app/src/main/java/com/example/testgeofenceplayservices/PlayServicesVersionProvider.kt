package com.example.testgeofenceplayservices

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat

/**
 * Provides installed Google Play services package version details.
 *
 * Returns `null` when Play services are not installed or package lookup fails.
 */
object PlayServicesVersionProvider {

    data class PlayServicesInfo(
        val versionName: String?,
        val versionCode: Long,
    )

    fun getPlayServicesInfo(context: Context): PlayServicesInfo? {
        val packageInfo = getPackageInfo(context) ?: return null
        return PlayServicesInfo(
            versionName = packageInfo.versionName,
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
        )
    }

    /** Version name of `com.google.android.gms` (for example 25.35.38), or `null` if unavailable. */
    fun getPlayServicesVersion(context: Context): String? = getPlayServicesInfo(context)?.versionName

    private fun getPackageInfo(context: Context): PackageInfo? = runCatching {
        context.packageManager.getPackageInfo("com.google.android.gms", 0)
    }.getOrElse { null }
}
