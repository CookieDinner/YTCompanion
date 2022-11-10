package com.cookiedinner.ytcompanion.utilities

import android.app.Activity
import android.content.Context
import android.content.res.Resources.Theme
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.cookiedinner.ytcompanion.R
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun <T> LiveData<T>.observeFresh(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    removeObservers(lifecycleOwner)
    observe(lifecycleOwner, observer)
}

fun <T> LiveData<T>.observeOnceOnMainThread(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    Handler(Looper.getMainLooper()).post(Runnable {
        observe(lifecycleOwner, object : Observer<T> {
            override fun onChanged(t: T?) {
                observer.onChanged(t)
                removeObserver(this)
            }
        })
    })
}

/**
 * A class used to wrap Livedata as a way to prevent the pointless observer calls on their creation when navigating fragments
 */
class Event<T>(content: T?) {
    private val mContent: T
    private var hasBeenHandled = false

    init {
        requireNotNull(content) { "null values in Event are not allowed." }
        mContent = content
    }

    val contentIfNotHandled: T?
        get() = if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            mContent
        }

    fun hasBeenHandled(): Boolean {
        return hasBeenHandled
    }
}

fun Button.disable() {
    setTextColor(Color.GRAY)
    isClickable = false
}

fun Button.enable(activity: Context) {
    val typedValue = TypedValue()
    activity.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)
    setTextColor(typedValue.data)
    isClickable = true
}

fun Drawable.toBase64(): String? {
    return try {
        val byteStream = ByteArrayOutputStream()
        val bitmap = this.toBitmap()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteStream)
        val byteArray = byteStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }
}

fun Activity.showSnackbar(message: String, actionText: String = "OK", duration: Int = Snackbar.LENGTH_SHORT, callback: (() -> Unit)? = null) {
    if (Data.currentSnack?.isShown == true)
        Data.currentSnack?.dismiss()
    Data.currentSnack = Snackbar.make(this.findViewById(R.id.coordinator), message, duration)
    Data.currentSnack?.setAction(actionText) {
        if (callback == null)
            Data.currentSnack?.dismiss()
        else {
            callback.invoke()
            Data.currentSnack?.dismiss()
        }
    }
    Data.currentSnack?.setActionTextColor(getThemeColorId(androidx.appcompat.R.attr.colorPrimary))
    Data.currentSnack?.show()
}

fun Activity.getThemeColorId(resId: Int): Int {
    val colorTypedValue = TypedValue()
    theme.resolveAttribute(resId, colorTypedValue, true)
    return colorTypedValue.data
}

fun getCurrentDate(): String {
    val currentTime = LocalDateTime.now()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    return currentTime.format(dateFormatter)
}