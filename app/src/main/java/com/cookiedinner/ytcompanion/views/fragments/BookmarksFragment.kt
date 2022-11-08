package com.cookiedinner.ytcompanion.views.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.FragmentBookmarksBinding
import com.cookiedinner.ytcompanion.utilities.SwipeToDeleteCallback
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.utilities.getCurrentDate
import com.cookiedinner.ytcompanion.utilities.observeFresh
import com.cookiedinner.ytcompanion.utilities.showSnackbar
import com.cookiedinner.ytcompanion.views.fragments.adapters.BookmarkedVideosAdapter
import com.cookiedinner.ytcompanion.views.fragments.adapters.BookmarkedVideosAdapterInterface
import com.cookiedinner.ytcompanion.views.viewmodels.BookmarksViewModel
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar


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
        enableSwipeToDelete()
        return binding.root
    }

    private fun setupUI() {
        val text = SpannableString(binding.textEmptyBookmarks.text)
        text.setSpan(ImageSpan(requireContext(), R.drawable.ic_baseline_add_24), 6, 7, DynamicDrawableSpan.ALIGN_BASELINE)
        binding.textEmptyBookmarks.text = text

        binding.dimLayout.setOnClickListener {
            activityViewModel.hidePopup()
        }

        bookmarksRecycler = binding.recyclerView
        bookmarksRecycler.layoutManager = LinearLayoutManager(activity)
        adapter = BookmarkedVideosAdapter(bookmarkedVideosList, object: BookmarkedVideosAdapterInterface {
            override fun downloadButtonPressed(bookmarkedVideo: BookmarkedVideo, pressedButton: MaterialButton) {
                activityViewModel.liveDataGiveFabLocation.observeFresh(viewLifecycleOwner) {
                    val fabPositionY = it.contentIfNotHandled
                    if (fabPositionY != null) {
                        val location = intArrayOf(0, 0)
                        pressedButton.getLocationInWindow(location)
                        var possibleTranslationY = location[1].toFloat() - 112
                        Log.d("Tests", "$possibleTranslationY --- ${fabPositionY * 0.55}")
                        if (possibleTranslationY > (fabPositionY * 0.52)) {
                            possibleTranslationY = (fabPositionY * 0.52).toFloat()
                        }
                        binding.downloadPopup.translationY = possibleTranslationY
                        binding.downloadPopup.setOnClickListener {
                            morphDownloadButton(pressedButton)
                        }
                        morphDownloadButton(pressedButton)
                    }
                }
                activityViewModel.askForFabPosition()
            }

            override fun cardPressed(bookmarkedVideo: BookmarkedVideo) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmarkedVideo.videoUrl))
                startActivity(browserIntent)
            }

        })
        bookmarksRecycler.adapter = adapter

//        bookmarksRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                super.onScrollStateChanged(recyclerView, newState)
//                    if (!recyclerView.canScrollVertically(1) && recyclerView.canScrollVertically(-1)) {
//                        activityViewModel.notifyFab(isVisible = false)
//                    } else {
//                        activityViewModel.notifyFab(isVisible = true)
//                    }
//            }
//        })

//        activityViewModel.getAllBookmarks(requireActivity()).observe(viewLifecycleOwner) {
//            if (it.isNotEmpty()) {
//                binding.textEmptyBookmarks.visibility = View.GONE
//                it.forEach { bookmark ->
//                    bookmarkedVideosList.add(bookmark)
//                    adapter.notifyItemInserted(bookmarkedVideosList.size - 1)
//                }
//            }
//        }
        activityViewModel.populateBookmarksList(requireActivity()).observe(viewLifecycleOwner) {
            if (binding.textEmptyBookmarks.isVisible)
                binding.textEmptyBookmarks.visibility = View.GONE
            bookmarkedVideosList.add(it)
            adapter.notifyItemInserted(bookmarkedVideosList.size - 1)
        }
    }

    private fun morphDownloadButton(pressedButton: MaterialButton) {
        if (pressedButton.isVisible)
            activityViewModel.hidePopup()
        val views = listOf(pressedButton, binding.downloadPopup).sortedBy { !it.isVisible }
        val transform = MaterialContainerTransform().apply {
            startView = views.first()
            endView = views.last()
            addTarget(views.last())
            scrimColor = Color.TRANSPARENT
        }

        TransitionManager.beginDelayedTransition(binding.root, transform)
        views.first().visibility = View.INVISIBLE
        views.last().visibility = View.VISIBLE
        if (pressedButton.isVisible) {
            binding.dimLayout.visibility = View.GONE
            activityViewModel.notifyPopupOpened(false)
        } else {
            binding.dimLayout.visibility = View.VISIBLE
            activityViewModel.notifyPopupOpened(true) {
                morphDownloadButton(pressedButton)
            }
        }
    }

    private fun setupListeners() {
        activityViewModel.liveDataBookmarkInserted.observe(viewLifecycleOwner) {
            val insertedBookmark = it.contentIfNotHandled
            if (insertedBookmark != null) {
                var index = bookmarkedVideosList.indexOfFirst { it.modificationDate < insertedBookmark.modificationDate }
                if (index == -1)
                    index = bookmarkedVideosList.size
                bookmarkedVideosList.add(index, insertedBookmark)
                bookmarksRecycler.scrollToPosition(index)
                if (bookmarkedVideosList.isNotEmpty()) {
                    binding.textEmptyBookmarks.visibility = View.GONE
                }
                adapter.notifyItemInserted(index)
            }
        }
        activityViewModel.liveDataBookmarkDeleted.observe(viewLifecycleOwner) {
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

    private fun enableSwipeToDelete() {
        val swipeToDeleteCallback = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = bookmarkedVideosList[position]
                activityViewModel.deleteFromBookmarks(requireActivity(), item.id)
                requireActivity().showSnackbar("Removed video from the list", actionText = "UNDO") {
                    activityViewModel.insertIntoBookmarks(requireActivity(), item)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(bookmarksRecycler)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}