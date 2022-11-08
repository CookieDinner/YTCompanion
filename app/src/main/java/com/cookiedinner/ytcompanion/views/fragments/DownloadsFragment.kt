package com.cookiedinner.ytcompanion.views.fragments

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.FragmentDownloadsBinding
import com.cookiedinner.ytcompanion.models.FabData
import com.cookiedinner.ytcompanion.views.viewmodels.DownloadsViewModel

class DownloadsFragment : Fragment() {

    private var _binding: FragmentDownloadsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DownloadsViewModel by viewModels()
    private val activityViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)

        setupUI()
        setupListeners()
        return binding.root
    }

    private fun setupUI() {
        val text = SpannableString(binding.textDownloads.text)
        text.setSpan(ImageSpan(requireContext(), R.drawable.ic_baseline_download_24), 6, 7, DynamicDrawableSpan.ALIGN_BASELINE)
        binding.textDownloads.text = text
    }

    private fun setupListeners() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}