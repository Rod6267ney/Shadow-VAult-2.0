package com.example.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

data class CloneLogEntry(
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val tag: String,
    val message: String,
    val level: LogLevel
)

object CloneLogManager {

    private val _logs = MutableStateFlow<List<CloneLogEntry>>(emptyList())
    val logs: StateFlow<List<CloneLogEntry>> = _logs.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private const val MAX_LOGS = 200

    fun startOperation() {
        _logs.value = emptyList()
        _isRunning.value = true
    }

    fun finishOperation() {
        _isRunning.value = false
    }

    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val entry = CloneLogEntry(tag = tag, message = message, level = level)
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > MAX_LOGS) current.removeAt(0)
        _logs.value = current
        when (level) {
            LogLevel.ERROR   -> android.util.Log.e("CloneLog", "[] ")
            LogLevel.WARNING -> android.util.Log.w("CloneLog", "[] ")
            else             -> android.util.Log.d("CloneLog", "[] ")
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
