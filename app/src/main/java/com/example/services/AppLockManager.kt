package com.example.services

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object AppLockManager : DefaultLifecycleObserver {
    
    // [SECURITY] AUTO-LOCK and timers have been REMOVED per user request.
    // The app will no longer lock itself in the background.

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    fun init(application: Application) {
        // Disabled
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // No-op
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // No-op
    }

    fun lockApp() {
        // No-op - we don't want the app to ever aggressively lock
    }

    fun unlockApp() {
        _isLocked.value = false
    }
}
