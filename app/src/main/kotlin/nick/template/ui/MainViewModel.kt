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

    private val events = MutableSharedFlow<Unit>()
    val viewStates: Flow<ViewState> = events
        .onStart { emit(Unit) }
        .flatMapLatest { client.locationData() }
        // Note: ViewState as is won't survive resubscriptions
        .scan(ViewState()) { viewState, data ->
            viewState.copy(
                count = viewState.count + 1,
                locationData = data
            )
        }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000L),
            replay = 1
        )

    fun tryEmittingLocationData() {
        viewModelScope.launch {
            events.emit(Unit)
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

data class ViewState(
    val count: Int = 0,
    val locationData: LocationData? = null
)
