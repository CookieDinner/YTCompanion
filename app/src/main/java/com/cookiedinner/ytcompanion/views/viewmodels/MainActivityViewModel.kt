package com.cookiedinner.ytcompanion.views.viewmodels

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch

class MainActivityViewModel: ViewModel() {

    private var onBackPressedBlocked = false

    private var _mutableLiveDataHideBottomNav = MutableLiveData<Boolean>()
    val liveDataHideBottomNav: LiveData<Boolean> get() = _mutableLiveDataHideBottomNav

    private var _mutableLiveDataBackPressed = MutableLiveData<Boolean>()
    val liveDataBackPressed: LiveData<Boolean> get() = _mutableLiveDataBackPressed

    private var _mutableLiveDataFab = MutableLiveData<FabData>()
    val liveDataFab: LiveData<FabData> get() = _mutableLiveDataFab

    private var _mutableLiveDataResetPopup = MutableLiveData<Boolean>()
    val liveDataResetPopup: LiveData<Boolean> get() = _mutableLiveDataResetPopup

//    private val _mutableLiveDataYoutubeVideoMetadata = MutableLiveData<Result<YoutubeVideoMetadata>>()
//    val liveDataYoutubeVideoMetadata: LiveData<Result<YoutubeVideoMetadata>> get() = _mutableLiveDataYoutubeVideoMetadata

    val liveDataYoutubeVideoMetadata: MutableLiveData<Event<Result<YoutubeVideoMetadata>>> by lazy {
        MutableLiveData<Event<Result<YoutubeVideoMetadata>>>()
    }



    fun hideBottomNavigationView(shouldHide: Boolean) {
        _mutableLiveDataHideBottomNav.value = shouldHide
    }

    fun handleOnBackPressed(): Boolean {
        return if (onBackPressedBlocked) {
            _mutableLiveDataBackPressed.value = true
            onBackPressedBlocked = false
            false
        } else true
    }

    fun notifyPopupOpened(isPopupOpened: Boolean) {
        onBackPressedBlocked = isPopupOpened
    }

    fun notifyFab(isExpanded: Boolean? = null, isVisible: Boolean? = null) {
        _mutableLiveDataFab.value = FabData(
            isExpanded = isExpanded,
            isVisible = isVisible,
        )
    }

    fun resetPopup() {
        _mutableLiveDataResetPopup.value = true
    }

    fun checkIfValidYoutubeURL(stringToCheck: String) : String? {
        val ytRegex =
            Regex("^(?:http://|https://|)(?:youtube\\.com/watch\\?v=|(?:http://|https://|)youtu\\.be/)([a-zA-z0-9-_]+)(?:&\\S*$|$)")
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
//        val videoURL = "https://youtu.be/$videoID"
//        YoutubeRequests
//            .getVideoMetadata(videoURL)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(
//                { response ->
//                    if (response.isSuccessful) {
//                        callback?.invoke(response.body())
//                    } else {
//                        callback?.invoke(null)
//                    }
//                },
//                { errorResponse ->
//                    errorCallback?.invoke(errorResponse)
//                }
//            )
//            .apply {}
    }
}