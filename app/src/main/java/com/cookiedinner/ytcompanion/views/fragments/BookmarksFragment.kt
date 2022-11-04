package com.cookiedinner.ytcompanion.views.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.FragmentBookmarksBinding
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.views.fragments.adapters.BookmarkedVideosAdapter
import com.cookiedinner.ytcompanion.views.fragments.adapters.BookmarkedVideosAdapterInterface
import com.cookiedinner.ytcompanion.views.viewmodels.BookmarksViewModel
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import com.google.android.material.snackbar.Snackbar


class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookmarksViewModel by viewModels()
    private val activityViewModel: MainActivityViewModel by activityViewModels()
    private val bookmarkedVideosList: MutableList<BookmarkedVideo> = mutableListOf()
    private lateinit var bookmarksRecycler: RecyclerView
    private lateinit var adapter: BookmarkedVideosAdapter

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
        val text = SpannableString(binding.textEmptyBookmarks.text)
        text.setSpan(ImageSpan(requireContext(), R.drawable.ic_baseline_add_24), 6, 7, DynamicDrawableSpan.ALIGN_BASELINE)
        binding.textEmptyBookmarks.text = text

        bookmarksRecycler = binding.recyclerView
        bookmarksRecycler.layoutManager = LinearLayoutManager(activity)
        adapter = BookmarkedVideosAdapter(bookmarkedVideosList, object: BookmarkedVideosAdapterInterface {
            override fun downloadButtonPressed(id: Int) {
                TODO("Not yet implemented")
            }

            override fun deleteButtonPressed(id: Int) {
                activityViewModel.deleteFromBookmarks(requireActivity(), id)
            }

        })
        bookmarksRecycler.adapter = adapter

        bookmarksRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                    if (!recyclerView.canScrollVertically(1) && recyclerView.canScrollVertically(-1)) {
                        activityViewModel.notifyFab(isVisible = false)
                    } else {
                        activityViewModel.notifyFab(isVisible = true)
                    }
            }
        })

        activityViewModel.getAllBookmarks(requireActivity()).observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.textEmptyBookmarks.visibility = View.GONE
                it.forEach { bookmark ->
                    bookmarkedVideosList.add(bookmark)
                    adapter.notifyItemInserted(bookmarkedVideosList.size - 1)
                }
            }
        }
    }

    private fun setupListeners() {
        activityViewModel.liveDataSnackbar.observe(viewLifecycleOwner) {
            val message = it.contentIfNotHandled
            if (message != null) {
                val snack = Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
                val params = snack.view.layoutParams as CoordinatorLayout.LayoutParams
                params.gravity = Gravity.TOP
                snack.view.layoutParams = params
                snack.show()
            }
        }
        activityViewModel.liveDataBookmarkInserted.observe(viewLifecycleOwner) {
            Log.d("Tests", "insert")
            val insertedBookmark = it.contentIfNotHandled
            if (insertedBookmark != null) {
                bookmarkedVideosList.add(insertedBookmark)
                if (bookmarkedVideosList.isNotEmpty()) {
                    binding.textEmptyBookmarks.visibility = View.GONE
                }
                adapter.notifyItemInserted(bookmarkedVideosList.size - 1)
            }
        }
        activityViewModel.liveDataBookmarkDeleted.observe(viewLifecycleOwner) {
            Log.d("Tests", "delete")
            val deletedId = it.contentIfNotHandled
            if (deletedId != null) {
                val index = bookmarkedVideosList.indexOfFirst { it.id == deletedId }
                bookmarkedVideosList.removeAt(index)
                if (bookmarkedVideosList.isEmpty()) {
                    binding.textEmptyBookmarks.visibility = View.VISIBLE
                }
                adapter.notifyItemRemoved(index)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}