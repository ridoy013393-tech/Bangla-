package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context, timeoutMs: Long = 7_000L): CapturedLocation? {
        if (!hasLocationPermission(context)) return null
        val manager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val fresh = withTimeoutOrNull(timeoutMs) {
            requestFreshLocation(context, manager)
        }
        val best = fresh ?: getBestLastKnownLocation(manager)
        return best?.let { CapturedLocation(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocation(
        context: Context,
        manager: LocationManager
    ): Location? {
        val provider = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                manager.isProviderEnabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        return suspendCancellableCoroutine { cont ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    manager.getCurrentLocation(
                        provider,
                        signal,
                        context.mainExecutor
                    ) { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                }
                cont.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
                runCatching {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(manager: LocationManager): Location? {
        val providers = runCatching { manager.getProviders(true) }.getOrNull().orEmpty()
        var best: Location? = null
        for (p in providers) {
            val loc = runCatching { manager.getLastKnownLocation(p) }.getOrNull() ?: continue
            if (best == null || loc.time > best.time) {
                best = loc
            }
        }
        return best
    }

    fun formatCoordinates(lat: Double, lng: Double): String {
        val ns = if (lat >= 0) "N" else "S"
        val ew = if (lng >= 0) "E" else "W"
        return String.format(Locale.US, "%.4f° %s, %.4f° %s", abs(lat), ns, abs(lng), ew)
    }

    fun openInGoogleMaps(context: Context, lat: Double, lng: Double, label: String): Boolean {
        val encodedLabel = Uri.encode(label.ifBlank { "Bangla Chat Location" })
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val openedGeo = runCatching {
            context.startActivity(mapIntent)
            true
        }.getOrDefault(false)
        if (openedGeo) return true

        val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        }.getOrDefault(false)
    }
}
