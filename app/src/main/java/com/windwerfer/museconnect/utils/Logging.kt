package com.windwerfer.museconnect.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object Logging {
    private val _debugMessages = MutableStateFlow<List<String>>(emptyList())
    val debugMessages = _debugMessages.asStateFlow()

    fun appendData(message: String) {
        Log.d("MuseConnect", message)
        _debugMessages.update { it + message }
    }

    fun clearMessages() {
        _debugMessages.value = emptyList()
    }
}