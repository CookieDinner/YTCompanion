package com.cookiedinner.ytcompanion.views.fragments

import android.os.Bundle
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
import com.cookiedinner.ytcompanion.R
import com.cookiedinner.ytcompanion.databinding.FragmentSettingsBinding
import com.cookiedinner.ytcompanion.views.viewmodels.MainActivityViewModel
import java.util.prefs.PreferenceChangeListener

class SettingsFragment : PreferenceFragmentCompat() {

    private val activityViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupThemePreference()
        setupQualityPreference()
        setupExtensionPreference()
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
            updateListPreference(preference, newValue)
        }

        val extensionListPreference = preferenceManager.findPreference<ListPreference>("extension")
        extensionListPreference?.setOnPreferenceChangeListener { preference, newValue ->
            updateListPreference(preference, newValue)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun updateListPreference(preference: Preference, newValue: Any): Boolean{
        preference.summary = newValue.toString()
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
        when (preferenceManager.sharedPreferences?.getString("quality", "1080p")) {
            "144p"  -> findPreference<ListPreference>("quality")?.summary = "144p"
            "240p"  -> findPreference<ListPreference>("quality")?.summary = "240p"
            "360p"  -> findPreference<ListPreference>("quality")?.summary = "360p"
            "480p"  -> findPreference<ListPreference>("quality")?.summary = "480p"
            "720p"  -> findPreference<ListPreference>("quality")?.summary = "720p"
            "1080p" -> findPreference<ListPreference>("quality")?.summary = "1080p"
        }
    }

    private fun setupExtensionPreference() {
        when (preferenceManager.sharedPreferences?.getString("extension", "Video")) {
            "Video" -> findPreference<ListPreference>("extension")?.summary = "Video"
            "Audio" -> findPreference<ListPreference>("extension")?.summary = "Audio"
        }
    }
}