package com.example

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.AppNavigation
import com.example.ui.theme.AmbientBackground
import com.example.ui.theme.ShadowVaultTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.example.data.AppDatabase
import com.example.data.SessionLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.services.AppLockManager

// Removed dummy object AppLockManager { ... }

object GlobalErrorHandler {
    var hasCrashed by mutableStateOf(false)
    var lastError by mutableStateOf("")
}

object AppState {
    var targetWorkspaceShortcut by mutableStateOf<String?>(null)
}

class CustomExceptionHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val context: android.content.Context
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        android.util.Log.e("ShadowVault", "FATAL CRASH INTERCEPTED", exception)
        
        CoroutineScope(Dispatchers.Main).launch {
            GlobalErrorHandler.lastError = exception.localizedMessage ?: exception.toString()
            GlobalErrorHandler.hasCrashed = true
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.example.data.AppDatabase.getDatabase(context).vaultDao()
                    .insertSessionLog(com.example.data.SessionLogEntity(eventType = "FATAL", message = "Crash intercepted: ${exception.message}"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class MainActivity : FragmentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("ShadowVaultSettings", android.content.Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "en") ?: "en"
        super.attachBaseContext(com.example.utils.ContextUtils.updateLocale(newBase, lang))
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock disabled onStop for emulator environment stability
    }
    
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        // [SECURITY] Clipboard sanitizer removed per user request
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // [SECURITY] Flip-to-lock REMOVED per user request
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("shortcut_workspace_id")?.let {
            AppState.targetWorkspaceShortcut = it
        }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // [SECURITY] Screenshot restriction removed per user request
    
    sensorManager = getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    intent?.getStringExtra("shortcut_workspace_id")?.let {
        AppState.targetWorkspaceShortcut = it
    }
    
    val launchUserId = intent?.getStringExtra("LAUNCH_CLONE_USER_ID")
    val launchPackage = intent?.getStringExtra("LAUNCH_CLONE_PACKAGE")
    if (launchUserId != null && launchPackage != null) {
        lifecycleScope.launch {
            com.example.utils.ShizukuUtils.launchApp(this@MainActivity, launchUserId, launchPackage)
        }
    }

    // Global Exception Handler
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    if (defaultHandler !is CustomExceptionHandler) {
        Thread.setDefaultUncaughtExceptionHandler(CustomExceptionHandler(defaultHandler, this))
    }

    try {
        com.example.utils.ShizukuUtils.initialize(this)
        AppLockManager.init(application)

        lifecycleScope.launch(Dispatchers.IO) {
            com.example.services.AntiDetectionEngine.initialize(applicationContext)
            
            val dao = AppDatabase.getDatabase(applicationContext).vaultDao()
            com.example.utils.ShizukuUtils.onLogEvent = { eventType, message ->
                lifecycleScope.launch(Dispatchers.IO) {
                    dao.insertSessionLog(SessionLogEntity(eventType = eventType, message = message))
                }
            }
        }
    } catch (t: Throwable) {
        android.util.Log.e("ShadowVault", "Shizuku initialization failed", t)
    }

    enableEdgeToEdge()
    setContent {
      val context = androidx.compose.ui.platform.LocalContext.current
      val vaultManager = androidx.compose.runtime.remember { com.example.data.VaultManager(context) }
      val themeMode = vaultManager.getThemeMode()
      val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
      val isDark = when (themeMode) {
          "LIGHT" -> false
          "DARK" -> true
          else -> isSystemDark
      }
      
      @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
      val windowSizeClass = calculateWindowSizeClass(this)

      ShadowVaultTheme(darkTheme = isDark) {
        AmbientBackground {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .systemBarsPadding()
          ) {
              if (GlobalErrorHandler.hasCrashed) {
                  // Recovery UI
                  Column(
                      modifier = Modifier.fillMaxSize().padding(16.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center
                  ) {
                      Text("SYSTEM FAILURE", color = com.example.ui.theme.DangerRed, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                      Text(GlobalErrorHandler.lastError, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(vertical = 16.dp))
                      Button(
                          onClick = { GlobalErrorHandler.hasCrashed = false },
                          colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.NeonCyan)
                      ) {
                          Text("Attempt Recovery", color = Color.Black)
                      }
                  }
              } else {
                  AppNavigation(windowSizeClass = windowSizeClass)
              }
          }
        }
      }
    }
  }
}
