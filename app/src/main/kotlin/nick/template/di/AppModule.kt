package nick.template.di

import android.app.Application
import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nick.template.location.AndroidLocationStates
import nick.template.location.GooglePlayLocationClient
import nick.template.location.LocationClient
import nick.template.location.LocationStates

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    companion object {
        @Provides
        fun locationManager(application: Application): LocationManager {
            return application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        @Provides
        fun locationClient(application: Application): FusedLocationProviderClient {
            return LocationServices.getFusedLocationProviderClient(application)
        }
    }

    @Binds
    abstract fun locationStates(locationStates: AndroidLocationStates): LocationStates

    @Binds
    abstract fun locationClient(locationClient: GooglePlayLocationClient): LocationClient
}
