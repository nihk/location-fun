package nick.template.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest

interface LocationClient {
    fun locationData(): Flow<LocationData>
}

class GooglePlayLocationClient @Inject constructor(
    private val client: FusedLocationProviderClient,
    private val states: LocationStates
) : LocationClient {
    override fun locationData(): Flow<LocationData> = states.states()
        .filter { locationState ->
            locationState.hasLocationPermissions && locationState.isLocationEnabled
        }
        .flatMapLatest { locationDataAssumesPermissions() }

    @SuppressLint("MissingPermission")
    private fun locationDataAssumesPermissions() = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationAvailability(availability: LocationAvailability) {
                Log.d("asdf", "onLocationAvailability: ${availability.isLocationAvailable}")
                if (!availability.isLocationAvailable) {
                    trySend(LocationData.NotAvailable)
                }
            }

            override fun onLocationResult(result: LocationResult) {
                trySend(result.lastLocation.toAvailable())
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 5_000L
            fastestInterval = 2_500L
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        client.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            Log.d("asdf", "removeLocationUpdates")
            client.removeLocationUpdates(callback)
        }
    }

    private fun Location.toAvailable(): LocationData.Available {
        return LocationData.Available(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy
        )
    }
}

sealed class LocationData {
    data class Available(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    ) : LocationData()

    object NotAvailable : LocationData()
}
