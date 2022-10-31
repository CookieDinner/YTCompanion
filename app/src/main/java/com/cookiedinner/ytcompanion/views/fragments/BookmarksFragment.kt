package com.cookiedinner.ytcompanion.views.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.FragmentBookmarksBinding
import com.cookiedinner.ytcompanion.models.BookmarkedVideo
import com.cookiedinner.ytcompanion.models.FabData
import com.cookiedinner.ytcompanion.views.fragments.adapters.BookmarkedVideosAdapter
import com.cookiedinner.ytcompanion.views.viewmodels.BookmarksViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookmarksViewModel by viewModels()
    private val activityViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        setupUI()
        setupListeners()
        return binding.root
    }


    private fun setupUI() {
        val text = SpannableString(binding.textBookmarks.text)
        text.setSpan(ImageSpan(requireContext(), R.drawable.ic_baseline_add_24), 6, 7, DynamicDrawableSpan.ALIGN_BASELINE)
        binding.textBookmarks.text = text

        val test = ArrayList<BookmarkedVideo>()
        for (i in 1..20) {
            test.add(BookmarkedVideo("Test $i", ""))
        }

        val bookmarksRecycler = binding.recyclerView
        bookmarksRecycler.layoutManager = LinearLayoutManager(activity)
        bookmarksRecycler.adapter = BookmarkedVideosAdapter(test)
    }

    private fun setupListeners() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}