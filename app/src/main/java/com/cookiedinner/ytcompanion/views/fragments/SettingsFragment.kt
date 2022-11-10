package com.cookiedinner.ytcompanion.views.fragments

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.anggrayudi.storage.file.FileFullPath
import com.anggrayudi.storage.file.getAbsolutePath
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.FragmentSettingsBinding
import com.cookiedinner.ytcompanion.utilities.Data
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import java.io.File
import java.util.prefs.PreferenceChangeListener

class SettingsFragment : PreferenceFragmentCompat() {

    private val activityViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupThemePreference()
        setupQualityPreference()
        setupExtensionPreference()
        setupDownloadLocationPreference()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val themeListPreference = preferenceManager.findPreference<ListPreference>("theme")
        themeListPreference?.setOnPreferenceChangeListener { preference, newValue ->
            updateListPreference(preference, newValue)
            when (newValue.toString()) {
                "On" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "Off" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "System" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            activity?.finish()
            startActivity(activity?.intent)
            true
        }

        val qualityListPreference = preferenceManager.findPreference<ListPreference>("quality")
        qualityListPreference?.setOnPreferenceChangeListener { preference, newValue ->
            updateListPreferenceQualities(preference, newValue)
        }

        val extensionListPreference = preferenceManager.findPreference<ListPreference>("extension")
        extensionListPreference?.setOnPreferenceChangeListener { preference, newValue ->
            updateListPreferenceExtensions(preference, newValue)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun updateListPreference(preference: Preference, newValue: Any): Boolean {
        preference.summary = newValue.toString()
        return true
    }

    private fun updateListPreferenceExtensions(preference: Preference, newValue: Any): Boolean {
        when (newValue) {
            ".mp3" -> preference.summary = "Audio (.mp3)"
            ".mp4" -> preference.summary = "Video (.mp4)"
            ".webm" -> preference.summary = "Video (.webm)"
        }
        return true
    }

    private fun updateListPreferenceQualities(preference: Preference, newValue: Any): Boolean {
        when (newValue) {
            "144"  -> preference.summary = "144p"
            "240"  -> preference.summary = "240p"
            "360"  -> preference.summary = "360p"
            "480"  -> preference.summary = "480p"
            "720"  -> preference.summary = "720p"
            "1080" -> preference.summary = "1080p"
        }
        return true
    }

    private fun setupThemePreference() {
        when (preferenceManager.sharedPreferences?.getString("theme", "System")) {
            "On"     -> findPreference<ListPreference>("theme")?.summary = "On"
            "Off"    -> findPreference<ListPreference>("theme")?.summary = "Off"
            "System" -> findPreference<ListPreference>("theme")?.summary = "System"
        }
    }

    private fun setupQualityPreference() {
        when (preferenceManager.sharedPreferences?.getString("quality", "1080")) {
            "144"  -> findPreference<ListPreference>("quality")?.summary = "144p"
            "240"  -> findPreference<ListPreference>("quality")?.summary = "240p"
            "360"  -> findPreference<ListPreference>("quality")?.summary = "360p"
            "480"  -> findPreference<ListPreference>("quality")?.summary = "480p"
            "720"  -> findPreference<ListPreference>("quality")?.summary = "720p"
            "1080" -> findPreference<ListPreference>("quality")?.summary = "1080p"
        }
    }

    private fun setupExtensionPreference() {
        when (preferenceManager.sharedPreferences?.getString("extension", ".mp3")) {
            ".mp3" -> findPreference<ListPreference>("extension")?.summary = "Audio (.mp3)"
            ".mp4" -> findPreference<ListPreference>("extension")?.summary = "Video (.mp4)"
            ".webm" -> findPreference<ListPreference>("extension")?.summary = "Video (.webm)"
        }
    }

    private fun setupDownloadLocationPreference() {
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YTCompanion")
        val downloadLocationPref = preferenceManager.findPreference<Preference>("download_location")
        downloadLocationPref?.setDefaultValue(downloadsDir.absolutePath)

        val currentLocation = preferenceManager.sharedPreferences?.getString("download_location", downloadsDir.absolutePath)!!
        downloadLocationPref?.summary = currentLocation.substring(1)

        downloadLocationPref?.setOnPreferenceClickListener {
            Data.storageHelper?.onFolderSelected = { requestCode, folder ->
                val newLocation = folder.getAbsolutePath(requireContext())
                preferenceManager.sharedPreferences?.edit()?.putString("download_location", newLocation)?.apply()
                it.summary = newLocation.substring(1)
            }
            Data.storageHelper?.openFolderPicker()
            true
        }
    }
}