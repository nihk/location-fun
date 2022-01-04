package nick.template.ui

import android.content.Context
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import nick.template.location.LocationClient
import nick.template.location.LocationData

class MainViewModel(
    private val client: LocationClient,
    private val handle: SavedStateHandle
) : ViewModel() {

    private val events = MutableSharedFlow<Event>()
    val viewStates: Flow<ViewState> = events
        .share()
        .toResults()
        .scan(ViewState()) { viewState, result ->
            when (result) {
                is Result.GotLocationResult -> viewState.copy(
                    count = viewState.count + 1,
                    locationData = result.locationData
                )
                else -> viewState
            }

        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1_000L),
            initialValue = ViewState()
        )

    fun processEvent(event: Event) {
        viewModelScope.launch {
            events.emit(event)
        }
    }

    private fun <T> Flow<T>.share(): SharedFlow<T> {
        return shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily
        )
    }

    private fun Flow<Event>.toResults(): Flow<Result> {
        return merge(
            filterIsInstance<Event.GetLocationData>().toLocationResults()
        )
    }

    private fun Flow<Event.GetLocationData>.toLocationResults(): Flow<Result> {
        return distinctUntilChanged().flatMapLatest { event ->
            if (event.start) {
                client.locationData().mapLatest { locationData -> Result.GotLocationResult(locationData) }
            } else {
                flowOf(Result.NoOpResult)
            }
        }
    }

    class Factory @Inject constructor(
        @ApplicationContext private val context: Context,
        private val client: LocationClient
    ) {
        fun create(owner: SavedStateRegistryOwner): AbstractSavedStateViewModelFactory {
            return object : AbstractSavedStateViewModelFactory(owner, null) {
                override fun <T : ViewModel?> create(
                    key: String,
                    modelClass: Class<T>,
                    handle: SavedStateHandle
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(client, handle) as T
                }
            }
        }
    }
}

sealed class Event {
    data class GetLocationData(val start: Boolean) : Event()
}

sealed class Result {
    data class GotLocationResult(val locationData: LocationData) : Result()
    object NoOpResult : Result()
}

data class ViewState(
    val count: Int = 0,
    val locationData: LocationData? = null
)
