package nick.template.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import nick.template.R
import nick.template.databinding.MainFragmentBinding
import javax.inject.Inject

class MainFragment @Inject constructor(
    private val factory: MainViewModel.Factory
) : Fragment(R.layout.main_fragment) {
    private val viewModel: MainViewModel by viewModels { factory.create(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = MainFragmentBinding.bind(view)
    }
}
