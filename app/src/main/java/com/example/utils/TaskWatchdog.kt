package com.example.utils

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.SessionLogEntity
import kotlinx.coroutines.*

/**
 * Watchdog mechanism for preventing freezes, deadlocks, and hung background operations.
 */
object TaskWatchdog {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Executes a task with a watchdog timeout. If the task exceeds timeoutMs,
     * it is cancelled and logged to SessionLogs.
     */
    suspend fun <T> runWithWatchdog(
        tag: String,
        timeoutMs: Long = 5000L,
        context: Context? = null,
        onTimeout: (() -> Unit)? = null,
        block: suspend CoroutineScope.() -> T
    ): T? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val result = withTimeoutOrNull(timeoutMs) {
                block()
            }
            if (result == null) {
                val elapsed = System.currentTimeMillis() - startTime
                val errorMsg = "WATCHDOG TIMEOUT: Task '$tag' exceeded $timeoutMs ms (Elapsed: $elapsed ms). Forcefully terminated."
                
                onTimeout?.invoke()

                context?.let { ctx ->
                    try {
                        val dao = AppDatabase.getDatabase(ctx).vaultDao()
                        dao.insertSessionLog(
                            SessionLogEntity(
                                timestamp = System.currentTimeMillis(),
                                eventType = "TEST_ERROR",
                                message = errorMsg
                            )
                        )
                    } catch (_: Exception) {}
                }
            }
            result
        } catch (e: Exception) {
            context?.let { ctx ->
                try {
                    val dao = AppDatabase.getDatabase(ctx).vaultDao()
                    dao.insertSessionLog(
                        SessionLogEntity(
                            timestamp = System.currentTimeMillis(),
                            eventType = "TEST_ERROR",
                            message = "WATCHDOG ERROR: Task '$tag' failed with exception: ${e.message}"
                        )
                    )
                } catch (_: Exception) {}
            }
            null
        }
    }
}
