package com.cookiedinner.ytcompanion.views.viewmodels

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
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
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivityViewModel: ViewModel() {

    private var onBackPressedBlocked = false

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

    val liveDataResetPopup: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

//    private val _mutableLiveDataYoutubeVideoMetadata = MutableLiveData<Result<YoutubeVideoMetadata>>()
//    val liveDataYoutubeVideoMetadata: LiveData<Result<YoutubeVideoMetadata>> get() = _mutableLiveDataYoutubeVideoMetadata

    val liveDataYoutubeVideoMetadata: MutableLiveData<Event<Result<YoutubeVideoMetadata>>> by lazy {
        MutableLiveData<Event<Result<YoutubeVideoMetadata>>>()
    }

    val liveDataSnackbar: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    val liveDataBookmarkInserted: MutableLiveData<Event<BookmarkedVideo>> by lazy {
        MutableLiveData<Event<BookmarkedVideo>>()
    }

    val liveDataBookmarkDeleted: MutableLiveData<Event<Int>> by lazy {
        MutableLiveData<Event<Int>>()
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

    fun notifyPopupOpened(isPopupOpened: Boolean) {
        onBackPressedBlocked = isPopupOpened
    }

    fun notifyFab(isExpanded: Boolean? = null, isVisible: Boolean? = null) {
        liveDataFab.value = Event(
            FabData(
                isExpanded = isExpanded,
                isVisible = isVisible,
            )
        )
    }

    fun resetPopup() {
        liveDataResetPopup.value = true
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

    fun loadImageFromURLInto(context: Context, thumbnailURL: String, imageView: ImageView, callback: ((result: Boolean) -> Unit)? = null) {
        Glide.with(context).load(thumbnailURL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    imageView.setImageResource(0)
                    callback?.invoke(false)
                    return false
                }
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback?.invoke(true)
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

    fun insertIntoBookmarks(activity: Activity, metadata: YoutubeVideoMetadata?) {
        viewModelScope.launch {
            try {
                val bookmarksDao = AppDatabase.getInstance(activity).bookmarkedVideosDao()
                val newId = bookmarksDao.insertAll(
                    BookmarkedVideo(
                        0,
                        metadata!!.title,
                        metadata.author_name,
                        metadata.video_link!!,
                        null
                    )
                )
                val newBookmarkedVideo = bookmarksDao.findById(newId.first())
                liveDataBookmarkInserted.value = Event(newBookmarkedVideo)
                //liveDataSnackbar.value = Event("Added \"${newBookmarkedVideo.title}\" at Id=${newId.first()}")
            } catch (ex: Exception) {
                ex.printStackTrace()
//                Toast.makeText(activity, ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun deleteFromBookmarks(activity: Activity, id: Int) {
        viewModelScope.launch {
            try {
                val bookmarksDao = AppDatabase.getInstance(activity).bookmarkedVideosDao()
                val deletedRowsNumber = bookmarksDao.deleteById(id)
                if (deletedRowsNumber > 0) {
                    Log.d("Tests", "$deletedRowsNumber: $id")
                    liveDataBookmarkDeleted.value = Event(id)
                    //liveDataSnackbar.value = Event("Deleted bookmark")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
//                Toast.makeText(activity, ex.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getAllBookmarks(activity: Activity): LiveData<List<BookmarkedVideo>>{
        val result = MutableLiveData<List<BookmarkedVideo>>()
        viewModelScope.launch {
            val bookmarksDao = AppDatabase.getInstance(activity).bookmarkedVideosDao()
            val list = bookmarksDao.getAll()
            result.value = list
        }
        return result
    }
}