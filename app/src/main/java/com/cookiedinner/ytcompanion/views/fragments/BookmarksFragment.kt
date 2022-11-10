package com.cookiedinner.ytcompanion.views.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.anggrayudi.storage.SimpleStorage
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.*
import com.anggrayudi.storage.media.MediaType
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.FragmentBookmarksBinding
import com.cookiedinner.ytcompanion.utilities.*
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.views.fragments.adapters.BookmarkedVideosAdapter
import com.cookiedinner.ytcompanion.views.fragments.adapters.BookmarkedVideosAdapterInterface
import com.cookiedinner.ytcompanion.views.viewmodels.BookmarksViewModel
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.transition.MaterialContainerTransform
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.Unregistrar
import java.io.File


class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BookmarksViewModel by viewModels()
    private val activityViewModel: MainActivityViewModel by activityViewModels()
    private val bookmarkedVideosList: MutableList<BookmarkedVideo> = mutableListOf()
    private lateinit var bookmarksRecycler: RecyclerView
    private lateinit var adapter: BookmarkedVideosAdapter
    private var keyboardListener: Unregistrar? = null

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
                rowDownloadButtonClicked(bookmarkedVideo, pressedButton)
            }

            override fun cardPressed(bookmarkedVideo: BookmarkedVideo) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmarkedVideo.videoUrl))
                startActivity(browserIntent)
            }
        }, activityViewModel, requireActivity())
        bookmarksRecycler.adapter = adapter

        activityViewModel.populateBookmarksList(requireActivity()).observe(viewLifecycleOwner) {
            if (binding.textEmptyBookmarks.isVisible)
                binding.textEmptyBookmarks.visibility = View.GONE
            bookmarkedVideosList.add(it)
            adapter.notifyItemInserted(bookmarkedVideosList.size - 1)
        }
    }

    private fun setupListeners() {
        activityViewModel.liveDataBookmarkInserted.observe(viewLifecycleOwner) {
            val insertedBookmark = it.contentIfNotHandled
            if (insertedBookmark != null) {
                var index = bookmarkedVideosList.indexOfFirst { it.modificationDate < insertedBookmark.modificationDate }
                if (index == -1)
                    index = bookmarkedVideosList.size
                val recyclerLayoutManager = bookmarksRecycler.layoutManager as LinearLayoutManager
                val firstVisibleRow = recyclerLayoutManager.findFirstVisibleItemPosition()
                val lastVisibleRow = recyclerLayoutManager.findLastVisibleItemPosition()
                if (index - 1 !in firstVisibleRow..lastVisibleRow)
                    bookmarksRecycler.scrollToPosition(index)
                bookmarkedVideosList.add(index, insertedBookmark)
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

    private fun morphDownloadButton(pressedButton: MaterialButton) {
        keyboardListener?.unregister()
        if (pressedButton.isVisible)
            activityViewModel.hidePopup()
        val views = listOf(pressedButton, binding.downloadPopup).sortedBy { !it.isVisible }
        val transform = MaterialContainerTransform().apply {
            startView = views.first()
            endView = views.last()
            addTarget(views.last())
            scrimColor = Color.TRANSPARENT
            isElevationShadowEnabled = false
        }
        TransitionManager.beginDelayedTransition(binding.root, transform)
        views.first().visibility = View.INVISIBLE
        views.last().visibility = View.VISIBLE
        if (pressedButton.isVisible) {
            activityViewModel.notifyFab(isVisible = true)
            binding.dimLayout.visibility = View.GONE
            activityViewModel.notifyPopupOpened(false)
            activityViewModel.hideBottomNavigationView(false)
        } else {
            activityViewModel.notifyFab(isVisible = false)
            activityViewModel.hideBottomNavigationView(true)
            binding.dimLayout.visibility = View.VISIBLE
            activityViewModel.notifyPopupOpened(true) {
                /** I hate this workaround so much that it hurts my soul, but whatever... it works */
                viewModel.closePopup(requireActivity()) {
                    morphDownloadButton(pressedButton)
                }
            }
        }
    }

    private fun rowDownloadButtonClicked(bookmarkedVideo: BookmarkedVideo, pressedButton: MaterialButton) {
        val rect = Rect()
        requireActivity().window.decorView.getWindowVisibleDisplayFrame(rect)
        var trimmedSafeBottomPositionY = (rect.bottom * 0.6).toFloat()

        val pressedButtonLocation = intArrayOf(0, 0)
        pressedButton.getLocationInWindow(pressedButtonLocation)

        var possibleTranslationY = pressedButtonLocation[1].toFloat() - 112

        if (possibleTranslationY > trimmedSafeBottomPositionY) {
            possibleTranslationY = trimmedSafeBottomPositionY
        }
        binding.downloadPopup.translationY = possibleTranslationY
        binding.downloadPopupContent.textEditFilename.setText(bookmarkedVideo.title)
        binding.downloadPopupContent.downloadButton.setOnClickListener {
            val defaultDownloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YTCompanion")
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val downloadPath = prefs.getString("download_location", defaultDownloadsDir.absolutePath)!!
            val quality = prefs.getString("quality", "1080")!!
            viewModel.downloadBookmark(
                requireActivity(),
                activityViewModel,
                bookmarkedVideo,
                downloadPath,
                binding.downloadPopupContent.textEditFilename.text.toString(),
                binding.downloadPopupContent.textEditExtensionsDropdown.text.toString(),
                quality
            )
            morphDownloadButton(pressedButton)
        }

        /** Dropdown setup */
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val defaultExtension = prefs.getString("extension", ".mp4")
        binding.downloadPopupContent.textEditExtensionsDropdown.setText(defaultExtension)

        val extensionsArray = resources.getStringArray(R.array.download_extensions)
        val extensionsDropdownAdapter = ArrayAdapter(requireContext(), R.layout.row_dropdown_extension, extensionsArray)
        binding.downloadPopupContent.textEditExtensionsDropdown.setAdapter(extensionsDropdownAdapter)

        morphDownloadButton(pressedButton)

        /** Custom method of keeping the popup above the keyboard cause the default one is fucky in this scenario */
        keyboardListener = KeyboardVisibilityEvent.registerEventListener(requireActivity()) { keyboardVisible ->
            if (keyboardVisible) {
                requireActivity().window.decorView.getWindowVisibleDisplayFrame(rect)
                trimmedSafeBottomPositionY = (rect.bottom * 0.52).toFloat()
                if (possibleTranslationY > trimmedSafeBottomPositionY)
                    binding.downloadPopup.translationY = trimmedSafeBottomPositionY
            } else {
                if (binding.downloadPopup.translationY != possibleTranslationY)
                    binding.downloadPopup.translationY = possibleTranslationY
            }
        }
        activityViewModel.askForFabPosition()
    }

    override fun onDestroyView() {
        Data.currentSnack?.dismiss()
        super.onDestroyView()
        _binding = null
    }


}