package com.example.services

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object AppLockManager : DefaultLifecycleObserver {
    
    private const val AUTO_LOCK_TIMEOUT_MS = 60_000L // 1 minuto de inatividade

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    private var lockJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun init(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // O app foi para background
        startLockTimer()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // O app voltou para foreground
        cancelLockTimer()
    }

    private fun startLockTimer() {
        lockJob?.cancel()
        lockJob = scope.launch {
            delay(AUTO_LOCK_TIMEOUT_MS)
            lockApp()
        }
    }

    private fun cancelLockTimer() {
        lockJob?.cancel()
    }

    fun lockApp() {
        _isLocked.value = true
    }

    fun unlockApp() {
        _isLocked.value = false
    }
}
