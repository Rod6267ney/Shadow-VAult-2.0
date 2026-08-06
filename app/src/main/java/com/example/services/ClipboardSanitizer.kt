package com.example.services

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.example.data.AppDatabase
import com.example.data.ClipboardEntity
import com.example.utils.ClipboardSettings
import kotlinx.coroutines.*

object ClipboardSanitizer {
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false
    private var lastKnownClip: String? = null

    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        isMonitoring = true

        val clipboard = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return

        try {
            clipboard.addPrimaryClipChangedListener {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString() ?: ""
                    if (text.isNotBlank() && text != lastKnownClip) {
                        lastKnownClip = text
                        onNewTextCopied(context.applicationContext, text)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onNewTextCopied(context: Context, text: String, workspaceId: String? = null, sourceApp: String = "Chaos OS") {
        scope.launch {
            try {
                // Auto-save to SQLCipher encrypted database with workspace isolation context if enabled
                val isIsolated = ClipboardSettings.isIsolationEnabled(context)
                val targetWorkspaceId = if (isIsolated) workspaceId else null
                
                val dao = AppDatabase.getDatabase(context).vaultDao()
                dao.insertClipboardItem(
                    ClipboardEntity(
                        copiedText = text,
                        sourceApp = sourceApp,
                        workspaceId = targetWorkspaceId
                    )
                )

                // Schedule auto-clear timeout
                val timeoutSec = ClipboardSettings.getAutoClearTimeoutSeconds(context)
                if (timeoutSec > 0) {
                    scheduleAutoClear(context, timeoutSec)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun scheduleAutoClear(context: Context, timeoutSec: Int) {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(timeoutSec * 1000L)
            withContext(Dispatchers.Main) {
                sanitizeClipboard(context, notifyUser = true, reason = "Timeout ($timeoutSec s)")
            }
        }
    }

    fun onAppBackgrounded(context: Context) {
        if (ClipboardSettings.isClearOnBackgroundEnabled(context)) {
            scope.launch(Dispatchers.Main) {
                sanitizeClipboard(context, notifyUser = false, reason = "App em segundo plano")
            }
        }
    }

    fun sanitizeClipboard(context: Context, notifyUser: Boolean = true, reason: String = "Higienização") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard?.clearPrimaryClip()
            } else {
                clipboard?.setPrimaryClip(ClipData.newPlainText("", ""))
            }
            lastKnownClip = null

            if (notifyUser) {
                Toast.makeText(
                    context,
                    "🛡️ Área de transferência higienizada ($reason)!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sanitizeAndWipeForWorkspaceSwitch(
        context: Context,
        oldWorkspaceId: String?,
        newWorkspaceId: String?,
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch {
            try {
                // Clear OS system clipboard
                withContext(Dispatchers.Main) {
                    sanitizeClipboard(context, notifyUser = false)
                }

                val isIsolated = ClipboardSettings.isIsolationEnabled(context)
                val dao = AppDatabase.getDatabase(context).vaultDao()

                if (isIsolated) {
                    // Wipe cached history or keep workspace strictly separated
                    if (oldWorkspaceId != null) {
                        // Keep database isolated per workspaceId query
                    }
                }

                withContext(Dispatchers.Main) {
                    val targetName = newWorkspaceId ?: "Todas as Instâncias"
                    Toast.makeText(
                        context,
                        "🛡️ Isolação Ativa: Clipboard sanitizado para workspace [$targetName]",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
