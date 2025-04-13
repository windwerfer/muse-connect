package com.windwerfer.museconnect.data

import android.content.Context
import androidx.core.content.edit

class ConfigManager(context: Context) {
    private val prefs = context.getSharedPreferences("MuseConfig", Context.MODE_PRIVATE)

    fun getEegEnabled(): Boolean = prefs.getBoolean("eegEnabled", false)
    fun setEegEnabled(enabled: Boolean) = prefs.edit { putBoolean("eegEnabled", enabled) }

    fun getAccEnabled(): Boolean = prefs.getBoolean("accEnabled", false)
    fun setAccEnabled(enabled: Boolean) = prefs.edit { putBoolean("accEnabled", enabled) }

    fun getPpgEnabled(): Boolean = prefs.getBoolean("ppgEnabled", false)
    fun setPpgEnabled(enabled: Boolean) = prefs.edit { putBoolean("ppgEnabled", enabled) }

    fun getDebugEnabled(): Boolean = prefs.getBoolean("debugEnabled", false)
    fun setDebugEnabled(enabled: Boolean) = prefs.edit { putBoolean("debugEnabled", enabled) }

    fun getLastConnectedDevice(): String? = prefs.getString("lastConnectedDevice", null)
    fun setLastConnectedDevice(mac: String) = prefs.edit { putString("lastConnectedDevice", mac) }
}