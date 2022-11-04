package com.cookiedinner.ytcompanion.views

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.ActivityMainBinding
import com.cookiedinner.ytcompanion.databinding.AddBookmarkSheetBinding
import com.cookiedinner.ytcompanion.utilities.*
import com.cookiedinner.ytcompanion.utilities.database.AppDatabase
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var navOptions: NavOptions
    private lateinit var bottomNavView: BottomNavigationView
    private var currentMetadata: YoutubeVideoMetadata? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupTheme()
        setupBottomNavigationView()
        setupUI()
        setupBackPressed()
    }

    private fun setupUI() {
        binding.dimLayout.setOnClickListener {
            hidePopup()
        }
        binding.floatingActionButton.setOnClickListener {
            bottomNavView.visibility = View.GONE
            binding.dimLayout.visibility = View.VISIBLE
            viewModel.notifyPopupOpened(true)
            binding.floatingActionButton.isExpanded = true
        }
        viewModel.liveDataFab.observe(this) {
            val event = it.contentIfNotHandled
            if (event != null) {
                if (event.isVisible != null) {
                    if (event.isVisible)
                        binding.floatingActionButton.show()
                    else
                        binding.floatingActionButton.hide()
                }
            }
        }
        setupBookmarksSheet()
    }

    private fun setupBottomNavigationView() {
        bottomNavView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        navOptions = NavOptions.Builder()
            .setEnterAnim(androidx.transition.R.anim.abc_grow_fade_in_from_bottom)
            .setExitAnim(androidx.appcompat.R.anim.abc_shrink_fade_out_from_bottom)
            .build()

        bottomNavView.setOnItemSelectedListener { selectedItem ->
            when (selectedItem.itemId) {
                R.id.navigation_bookmarks -> {
                    binding.floatingActionButton.hide()
                    binding.floatingActionButton.setImageResource(R.drawable.ic_baseline_add_24)
                    binding.floatingActionButton.show()

                    navController.navigate(R.id.navigation_bookmarks,null, navOptions)
                    setupBookmarksSheet()
                }
                R.id.navigation_downloads -> {
                    binding.floatingActionButton.hide()
                    binding.floatingActionButton.setImageResource(R.drawable.ic_baseline_download_24)
                    binding.floatingActionButton.show()

                    navController.navigate(R.id.navigation_downloads,null, navOptions)
                    setupBookmarksSheet()
                }
                R.id.navigation_settings -> {
                    binding.floatingActionButton.hide()
                    navController.navigate(R.id.navigation_settings,null, navOptions)
                }
            }
            return@setOnItemSelectedListener true
        }
        supportActionBar?.hide()
        viewModel.liveDataHideBottomNav.observe(this) { shouldHideBottomNav ->
            if (shouldHideBottomNav)
                bottomNavView.visibility = View.GONE
            else
                bottomNavView.visibility = View.VISIBLE
        }
    }

    private fun setupTheme() {
        when (PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "System")) {
            "On" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "Off" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "System" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun setupBackPressed() {
        val onBackPressedCallback = object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val noPopupVisible = viewModel.handleOnBackPressed()
                if (noPopupVisible) {
                    if (bottomNavView.selectedItemId != R.id.navigation_bookmarks)
                        bottomNavView.selectedItemId = R.id.navigation_bookmarks
                    else
                        finish()
                } else {
                    hidePopup()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun setupBookmarksSheet() {
        binding.fabSheet.removeAllViewsInLayout()
        val sheetBinding = AddBookmarkSheetBinding.inflate(layoutInflater)
        binding.fabSheet.addView(sheetBinding.root)

        viewModel.liveDataResetPopup.observeFresh(this) {
            sheetBinding.textEditURL.text = null
            sheetBinding.textViewURL.error = null
            clearBookmarksSheet(sheetBinding)
        }

        sheetBinding.dialogAddButton.setOnClickListener {
            val possibleVideoID = viewModel.checkIfValidYoutubeURL(sheetBinding.textEditURL.text.toString())
            if (possibleVideoID != null) {
                currentMetadata?.video_link = sheetBinding.textEditURL.text.toString()
                viewModel.insertIntoBookmarks(this, currentMetadata)
                hidePopup()
            }
        }

        viewModel.liveDataYoutubeVideoMetadata.observeFresh(this) {
            sheetBinding.metadataProgressBar.visibility = View.GONE
            val result = it.contentIfNotHandled
            if (result != null) {
                if (result.isSuccess) {
                    sheetBinding.dialogAddButton.enable(this)
                    currentMetadata = result.getOrNull()!!
                    sheetBinding.videoTitle.text = currentMetadata!!.title
                    sheetBinding.videoChannel.text = currentMetadata!!.author_name
                    sheetBinding.placeholder.visibility = View.INVISIBLE
                    viewModel.loadImageFromURLInto(
                        baseContext,
                        currentMetadata!!.thumbnail_url,
                        sheetBinding.thumbnailImageView
                    ) { imageResult ->
                        if (imageResult) {
                            sheetBinding.activeThumbnailBorder.visibility = View.VISIBLE
                        } else {
                            sheetBinding.activeThumbnailBorder.visibility = View.INVISIBLE
                        }
                        sheetBinding.thumbnailProgressBar.visibility = View.GONE
                        sheetBinding.placeholder.visibility = View.VISIBLE
                    }
                } else {
                    val exception = result.exceptionOrNull()!!
                    sheetBinding.textViewURL.error = exception.message
                    clearBookmarksSheet(sheetBinding)
                    sheetBinding.thumbnailProgressBar.visibility = View.GONE
                    sheetBinding.placeholder.visibility = View.VISIBLE
                }
            }
        }

        val handler = Handler(Looper.getMainLooper())
        var workRunnable: Runnable? = null
        sheetBinding.textEditURL.addTextChangedListener {
            sheetBinding.dialogAddButton.disable()
            clearBookmarksSheet(sheetBinding)
            if (workRunnable != null)
                handler.removeCallbacks(workRunnable!!)
            workRunnable = Runnable {
                if (it.toString().isNotEmpty()) {
                    val possibleVideoID = viewModel.checkIfValidYoutubeURL(it.toString())
                    if (possibleVideoID != null) {
                        sheetBinding.textViewURL.error = null
                        sheetBinding.metadataProgressBar.visibility = View.VISIBLE
                        sheetBinding.thumbnailProgressBar.visibility = View.VISIBLE
                        sheetBinding.placeholder.visibility = View.GONE
                        viewModel.getYoutubeVideoMetadata(possibleVideoID)
                    } else {
                        sheetBinding.textViewURL.error = "Invalid URL"
                        clearBookmarksSheet(sheetBinding)
                    }
                } else {
                    sheetBinding.textViewURL.error = null
                    clearBookmarksSheet(sheetBinding)
                }
            }
            handler.postDelayed(workRunnable!!, 500)
        }

        sheetBinding.textEditURL.text = null
    }

    private fun hidePopup() {
        hideKeyboard()
        binding.dimLayout.visibility = View.GONE
        bottomNavView.visibility = View.VISIBLE
        binding.floatingActionButton.isExpanded = false
        viewModel.resetPopup()
    }

    private fun clearBookmarksSheet(sheetBinding: AddBookmarkSheetBinding) {
        sheetBinding.activeThumbnailBorder.visibility = View.INVISIBLE
        sheetBinding.thumbnailImageView.setImageResource(0)
        sheetBinding.videoTitle.text = ""
        sheetBinding.videoChannel.text = ""
        sheetBinding.placeholder.visibility = View.VISIBLE
    }
}