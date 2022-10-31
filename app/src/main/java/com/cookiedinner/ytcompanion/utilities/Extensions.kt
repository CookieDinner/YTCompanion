package com.cookiedinner.ytcompanion.utilities

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

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