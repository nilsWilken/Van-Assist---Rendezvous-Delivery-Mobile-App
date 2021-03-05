package de.dpd.vanassist.comboxExampleUI

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.dpd.vanassist.databinding.FragmentBleBinding


class BleFragment : Fragment() {

    private val viewModel by viewModels<BleViewModel> {
        InjectorUtils.provideBleViewModelFactory(requireContext())
    }

    private var _binding: FragmentBleBinding? = null

    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentBleBinding.inflate(inflater, container, false)

        val view = binding.root

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
    }
}