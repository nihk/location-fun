package nick.template.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import nick.template.R
import nick.template.databinding.MainFragmentBinding
import javax.inject.Inject
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import nick.template.location.LocationData

class MainFragment @Inject constructor(
    private val factory: MainViewModel.Factory
) : Fragment(R.layout.main_fragment) {
    private val viewModel: MainViewModel by viewModels { factory.create(this) }

    private val locationRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        viewModel.tryEmittingLocationData()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = MainFragmentBinding.bind(view)

        viewModel.viewStates
            .flowWithLifecycle(viewLifecycleOwner.lifecycle)
            .onEach { viewState ->
                when (viewState.locationData) {
                    is LocationData.Available -> {
                        with(viewState.locationData) {
                            binding.locationData.text = "lat: $latitude\nlng: $longitude\nacc: $accuracy"
                        }
                    }
                }
                binding.count.text = viewState.count.toString()
                Log.d("asdf", "count: ${viewState.count}")
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        binding.enableLocation.setOnClickListener {
            locationRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
