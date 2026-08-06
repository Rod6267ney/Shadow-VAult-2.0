package com.example.utils

import android.content.Context
import android.content.pm.PackageManager
import com.example.models.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManager {
    suspend fun getInstalledApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            
            packages.mapNotNull { appInfo ->
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    InstalledApp(
                        name = pm.getApplicationLabel(appInfo).toString(),
                        packageName = appInfo.packageName
                    )
                } else {
                    null
                }
            }.sortedBy { it.name.lowercase() }
        } catch (t: Throwable) {
            emptyList()
        }
    }
}
