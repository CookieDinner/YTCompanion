package com.cookiedinner.ytcompanion.views

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View

import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.transition.TransitionManager
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.ActivityMainBinding
import com.cookiedinner.ytcompanion.databinding.AddBookmarkSheetBinding
import com.cookiedinner.ytcompanion.utilities.*
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform

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
            viewModel.hidePopup()
        }

        binding.floatingActionButton.setOnClickListener {
            morphFloatingActionButton()
        }

        viewModel.liveDataAskForFabLocation.observeFresh(this) {
            if (it.contentIfNotHandled != null) {
                val fabLocation = intArrayOf(0, 0)
                binding.navView.getLocationInWindow(fabLocation)
                viewModel.setFabPosition(fabLocation[1])
            }
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
                    viewModel.hidePopup()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun morphFloatingActionButton() {
        val views = listOf(binding.floatingActionButton, binding.fabSheet).sortedBy { !it.isVisible }
        val transform = MaterialContainerTransform().apply {
            startView = views.first()
            endView = views.last()
            addTarget(views.last())
            scrimColor = Color.TRANSPARENT
            isElevationShadowEnabled = false
            setPathMotion(MaterialArcMotion())
        }

        TransitionManager.beginDelayedTransition(binding.root, transform)
        views.first().isVisible = false
        views.last().isVisible = true
        if (!binding.floatingActionButton.isVisible) {
            bottomNavView.visibility = View.GONE
            viewModel.notifyPopupOpened(true) {
                hidePopup()
            }
            window.navigationBarColor = getThemeColorId(androidx.appcompat.R.attr.colorBackgroundFloating)
            binding.dimLayout.visibility = View.VISIBLE
        }
        else {
            bottomNavView.visibility = View.VISIBLE
            viewModel.notifyPopupOpened(false)
            window.navigationBarColor = getThemeColorId(com.google.android.material.R.attr.colorOnPrimary)
            binding.dimLayout.visibility = View.GONE
        }
    }

    private fun setupBookmarksSheet() {
        binding.fabSheet.removeAllViewsInLayout()
        val sheetBinding = AddBookmarkSheetBinding.inflate(layoutInflater)
        binding.fabSheet.addView(sheetBinding.root)
        var currentBase64Image = ""

        viewModel.liveDataResetFabSheet.observeFresh(this) {
            sheetBinding.textEditURL.text = null
            sheetBinding.textViewURL.error = null
            clearBookmarksSheet(sheetBinding)
        }

        sheetBinding.dialogAddButton.setOnClickListener {
            val possibleVideoID = viewModel.checkIfValidYoutubeURL(sheetBinding.textEditURL.text.toString())
            if (possibleVideoID != null) {
                currentMetadata?.video_link = sheetBinding.textEditURL.text.toString()
                currentMetadata?.thumbnailBase64 = currentBase64Image
                viewModel.insertIntoBookmarks(this,
                    BookmarkedVideo(
                        0,
                        currentMetadata!!.title,
                        currentMetadata!!.author_name,
                        currentMetadata!!.video_link!!,
                        currentMetadata!!.thumbnailBase64!!,
                        getCurrentDate()
                    )
                )
                viewModel.hidePopup()
                showSnackbar("Added")
            }
        }

        viewModel.liveDataYoutubeVideoMetadata.observeFresh(this) {
            sheetBinding.metadataProgressBar.visibility = View.GONE
            val result = it.contentIfNotHandled
            if (result != null) {
                if (result.isSuccess) {
                    currentMetadata = result.getOrNull()!!
                    sheetBinding.videoTitle.text = currentMetadata!!.title
                    sheetBinding.videoChannel.text = currentMetadata!!.author_name
                    sheetBinding.placeholder.visibility = View.INVISIBLE
                    viewModel.loadImageFromURLInto(
                        baseContext,
                        currentMetadata!!.thumbnail_url,
                        sheetBinding.thumbnailImageView
                    ) { imageResult, base64Image ->
                        if (imageResult) {
                            sheetBinding.activeThumbnailBorder.visibility = View.VISIBLE
                            sheetBinding.dialogAddButton.enable(this)
                            if (base64Image != null)
                                currentBase64Image = base64Image
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
        morphFloatingActionButton()
        viewModel.resetFabSheet()
    }

    private fun clearBookmarksSheet(sheetBinding: AddBookmarkSheetBinding) {
        sheetBinding.activeThumbnailBorder.visibility = View.INVISIBLE
        sheetBinding.thumbnailImageView.setImageResource(0)
        sheetBinding.videoTitle.text = ""
        sheetBinding.videoChannel.text = ""
        sheetBinding.placeholder.visibility = View.VISIBLE
    }
}