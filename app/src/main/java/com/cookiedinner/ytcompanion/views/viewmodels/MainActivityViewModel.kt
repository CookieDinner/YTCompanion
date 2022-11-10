package com.cookiedinner.ytcompanion.views.viewmodels

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.anggrayudi.storage.SimpleStorageHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cookiedinner.ytcompanion.models.FabData
import com.cookiedinner.ytcompanion.utilities.Event
import com.cookiedinner.ytcompanion.utilities.YoutubeRequests
import com.cookiedinner.ytcompanion.utilities.YoutubeVideoMetadata
import com.cookiedinner.ytcompanion.utilities.database.AppDatabase
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.utilities.toBase64
import com.cookiedinner.ytcompanion.views.MainActivity
import com.cookiedinner.ytcompanion.views.fragments.adapters.BookmarkedVideosAdapter
import com.google.android.material.snackbar.Snackbar
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File

class MainActivityViewModel: ViewModel() {

    private var onBackPressedBlocked = false
    private var fabPositionY = 0

//    private var _mutableLiveDataHideBottomNav = MutableLiveData<Boolean>()
//    val liveDataHideBottomNav: LiveData<Boolean> get() = _mutableLiveDataHideBottomNav
//
    val liveDataHideBottomNav: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

//    private var _mutableLiveDataBackPressed = MutableLiveData<Boolean>()
//    val liveDataBackPressed: LiveData<Boolean> get() = _mutableLiveDataBackPressed

    val liveDataBackPressed: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }


//    private var _mutableLiveDataFab = MutableLiveData<FabData>()
//    val liveDataFab: LiveData<FabData> get() = _mutableLiveDataFab
    val liveDataFab: MutableLiveData<Event<FabData>> by lazy {
        MutableLiveData<Event<FabData>>()
}

//    private var _mutableLiveDataResetPopup = MutableLiveData<Boolean>()
//    val liveDataResetPopup: LiveData<Boolean> get() = _mutableLiveDataResetPopup

    val liveDataResetFabSheet: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

//    private val _mutableLiveDataYoutubeVideoMetadata = MutableLiveData<Result<YoutubeVideoMetadata>>()
//    val liveDataYoutubeVideoMetadata: LiveData<Result<YoutubeVideoMetadata>> get() = _mutableLiveDataYoutubeVideoMetadata

    val liveDataYoutubeVideoMetadata: MutableLiveData<Event<Result<YoutubeVideoMetadata>>> by lazy {
        MutableLiveData<Event<Result<YoutubeVideoMetadata>>>()
    }

    val liveDataBookmarkInserted: MutableLiveData<Event<BookmarkedVideo>> by lazy {
        MutableLiveData<Event<BookmarkedVideo>>()
    }

    val liveDataBookmarkDeleted: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData<Event<Int>>()
    }

    val liveDataAskForFabLocation: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val liveDataGiveFabLocation: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData<Event<Int>>()
    }

    val liveDataUpdateProgressBar: MutableLiveData<Pair<BookmarkedVideo, Int>> by lazy {
        MutableLiveData<Pair<BookmarkedVideo, Int>>()
    }

    fun setFabPosition(positionY: Int) {
        liveDataGiveFabLocation.value = Event(positionY)
    }

    fun askForFabPosition() {
        liveDataAskForFabLocation.value = Event(true)
    }

    fun hideBottomNavigationView(shouldHide: Boolean) {
        liveDataHideBottomNav.value = shouldHide
    }

    fun handleOnBackPressed(): Boolean {
        return if (onBackPressedBlocked) {
            liveDataBackPressed.value = true
            onBackPressedBlocked = false
            false
        } else true

    }

    private var hidePopupCallback: (() -> Unit)? = null

    fun notifyPopupOpened(isPopupOpened: Boolean, callback: (() -> Unit)? = null) {
        onBackPressedBlocked = isPopupOpened
        hidePopupCallback = callback
    }

    fun hidePopup() {
        hidePopupCallback?.invoke()
    }

    fun notifyFab(isExpanded: Boolean? = null, isVisible: Boolean? = null) {
        liveDataFab.value = Event(
            FabData(
                isExpanded = isExpanded,
                isVisible = isVisible,
            )
        )
    }

    fun resetFabSheet() {
        liveDataResetFabSheet.value = true
    }

    fun checkIfValidYoutubeURL(stringToCheck: String) : String? {
        val ytRegex =
            Regex("^(?:http://|https://|)(?:(?:|www\\.)youtube\\.com/watch\\?v=|(?:http://|https://|)youtu\\.be/)([a-zA-z0-9-_]+)(?:&\\S*$|$)")
        val result = ytRegex.find(stringToCheck)
        if (result != null) {
            val groupResults = result.groupValues
            return groupResults[1]
        }
        return null
    }

    fun loadImageFromURLInto(context: Context, thumbnailURL: String, imageView: ImageView, callback: ((result: Boolean, base64Image: String?) -> Unit)? = null) {
        Glide.with(context).load(thumbnailURL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    imageView.setImageResource(0)
                    callback?.invoke(false, null)
                    return false
                }
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    try {
                        val base64String = resource?.toBase64()
                        callback?.invoke(true, base64String)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        callback?.invoke(false, null)
                    }

                    return false
                }
            })
            .into(imageView)
    }

    fun getYoutubeVideoMetadata(videoID: String) {
        viewModelScope.launch {
            val videoURL = "https://youtu.be/$videoID"
            try {
                val response = YoutubeRequests.getVideoMetadata(videoURL)
                if (response.isSuccessful && response.code() == 200 && response.body() != null) {
                    liveDataYoutubeVideoMetadata.value = Event(Result.success(response.body()!!))
                } else {
                    val exception = YoutubeRequests.exceptionFromMessage(response.errorBody()?.string())
                    liveDataYoutubeVideoMetadata.value = Event(Result.failure(exception))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                val exception = YoutubeRequests.exceptionFromMessage(ex.message)
                liveDataYoutubeVideoMetadata.value = Event(Result.failure(exception))
            }
        }
    }

    fun insertIntoBookmarks(activity: Activity, bookmarkedVideo: BookmarkedVideo) {
        viewModelScope.launch {
            try {
                val db = AppDatabase.getInstance(activity)
                val bookmarksDao = AppDatabase.getInstance(activity).bookmarkedVideosDao()
                val newId = bookmarksDao.insertAll(
                    bookmarkedVideo
                )
                val newBookmarkedVideo = bookmarksDao.findById(newId.first())
                liveDataBookmarkInserted.value = Event(newBookmarkedVideo)
                //liveDataSnackbar.value = Event("Added \"${newBookmarkedVideo.title}\" at Id=${newId.first()}")
                db.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
//                Toast.makeText(activity, ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun deleteFromBookmarks(activity: Activity, id: Int) {
        viewModelScope.launch {
            try {
                val db = AppDatabase.getInstance(activity)
                val bookmarksDao = db.bookmarkedVideosDao()
                val deletedRowsNumber = bookmarksDao.deleteById(id)
                if (deletedRowsNumber > 0) {
                    liveDataBookmarkDeleted.value = Event(id)
                    //liveDataSnackbar.value = Event("Deleted bookmark")
                }
                db.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
//                Toast.makeText(activity, ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getAllBookmarks(activity: Activity): LiveData<List<BookmarkedVideo>>{
        val result = MutableLiveData<List<BookmarkedVideo>>()
        viewModelScope.launch {
            val db = AppDatabase.getInstance(activity)
            val bookmarksDao = db.bookmarkedVideosDao()
            val list = bookmarksDao.getAll()
            result.value = list
            db.close()
        }
        return result
    }

    fun populateBookmarksList(activity: Activity): LiveData<BookmarkedVideo> {
        val result = MutableLiveData<BookmarkedVideo>()
        viewModelScope.launch {
            val db = AppDatabase.getInstance(activity)
            val bookmarksDao = db.bookmarkedVideosDao()
            val list = bookmarksDao.getAll()
            list.forEach {
                delay(30)
                result.value = it
            }
            db.close()
        }
        return result
    }

    fun updateProgress(item: BookmarkedVideo, progress: Int) {
        liveDataUpdateProgressBar.postValue(Pair(item, progress))
    }
}