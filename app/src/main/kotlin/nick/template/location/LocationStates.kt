package nick.template.location

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

data class LocationState(
    val isLocationEnabled: Boolean,
    val hasLocationPermissions: Boolean
)

interface LocationStates {
    fun states(): Flow<LocationState>
    fun state(): LocationState
}

// Note: there's no way to listen to location permission changes
class AndroidLocationStates @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationManager: LocationManager
) : LocationStates {
    override fun states(): Flow<LocationState> {
        val locationStates = callbackFlow {
            val broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action != LocationManager.PROVIDERS_CHANGED_ACTION) {
                        return
                    }

                    trySend(createLocationState())
                }
            }

            context.registerReceiver(
                broadcastReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            )

            awaitClose {
                Log.d("asdf", "unregistering location receiver")
                context.unregisterReceiver(broadcastReceiver)
            }
        }

        return locationStates
            .onStart { emit(createLocationState()) }
            .distinctUntilChanged()
    }

    override fun state(): LocationState {
        return createLocationState()
    }

    private fun createLocationState(): LocationState {
        return LocationState(
            isLocationEnabled = LocationManagerCompat.isLocationEnabled(locationManager),
            hasLocationPermissions = LOCATION_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    companion object {
        private val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
}
