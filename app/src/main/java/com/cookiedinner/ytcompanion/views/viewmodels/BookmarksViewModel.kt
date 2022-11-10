package com.cookiedinner.ytcompanion.views.viewmodels

import android.app.Activity
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.anggrayudi.storage.file.DocumentFileCompat
import com.cookiedinner.ytcompanion.utilities.database.BookmarkedVideo
import com.cookiedinner.ytcompanion.utilities.hideKeyboard
import com.cookiedinner.ytcompanion.utilities.showSnackbar
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import java.io.File


class BookmarksViewModel : ViewModel() {

    /** Workaround for the hard crash caused by MaterialContainerTransform failing to find the target view (download button on bookmarks) when the keyboard is covering it... christ */
    fun closePopup(activity: Activity, callback: (() -> Unit)) {
        viewModelScope.launch {
            activity.hideKeyboard()
            delay(10)
            if (KeyboardVisibilityEvent.isKeyboardVisible(activity)) {
                closePopup(activity, callback)
                return@launch
            }
            callback.invoke()
        }
    }

    fun downloadBookmark(activity: Activity, activityViewModel: MainActivityViewModel, bookmarkedVideo: BookmarkedVideo, filepath: String, filename: String, extension: String, quality: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val request = YoutubeDLRequest(bookmarkedVideo.videoUrl)
            when (extension) {
                ".mp3" -> {
                    request.addOption("--extract-audio")
                    request.addOption("--audio-format", "mp3")
                }
                ".mp4" -> {
                    request.addOption("-f", "(mp4)[height<=?$quality]")

                }
                ".webm" -> {
                    request.addOption("-f", "webm")

                }
            }
            request.addOption("-o", "${filepath}/${filename}_%(height)s${extension}")


            val processId = "YtDlpProcess${bookmarkedVideo.id}"
            try {
                YoutubeDL.getInstance()
                    .execute(request, processId) { progress, etaInSeconds, line ->
                        Log.d("Tests", "Progress: $progress\t ETA: $etaInSeconds\t Line(?): $line")
                        activityViewModel.updateProgress(bookmarkedVideo, progress.toInt())
                    }
            } catch (ex: YoutubeDLException) {
                //ex.localizedMessage?.let { Log.d("Tests", it) }
                val snackMessage = ex.message!!.substringAfterLast(":").substringBefore(".")
                activity.showSnackbar(snackMessage)
            }
            YoutubeDL.getInstance().destroyProcessById(processId)
        }
    }

    private fun getDownloadLocation(): File {
        val downloadsDir: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val youtubeDLDir = File(downloadsDir, "youtubedl-android")
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir()
        return youtubeDLDir
    }
}