package com.washmycar.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class GeoPosition(val latitude: Double, val longitude: Double)

/** Position GPS via FusedLocationProvider (Google Play Services). */
@Singleton
class LocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) {
    companion object {
        /** Position par défaut : centre de Tunis (permission refusée ou position indisponible). */
        val TUNIS = GeoPosition(36.8065, 10.1815)

        /** Rectangle englobant approximatif de la Tunisie. */
        fun isInTunisia(p: GeoPosition) = p.latitude in 30.0..37.6 && p.longitude in 7.4..11.7
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * Dernière position connue si elle date de moins de 2 minutes (réponse immédiate),
     * sinon nouvelle mesure (délai max. 8 s), sinon null.
     */
    @SuppressLint("MissingPermission") // vérifiée par hasPermission()
    suspend fun current(): GeoPosition? {
        if (!hasPermission()) return null
        return runCatching {
            val last = client.lastLocation.await()
            val recent = last != null && System.currentTimeMillis() - last.time < 120_000
            if (recent) last else withTimeoutOrNull(8_000) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                    .await()
            } ?: last
        }.getOrNull()?.let { GeoPosition(it.latitude, it.longitude) }
    }
}
