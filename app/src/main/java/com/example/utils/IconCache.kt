package com.example.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache

object IconCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // 1/8 of available memory

    private val iconCache = object : LruCache<String, Drawable>(cacheSize) {
        override fun sizeOf(key: String, value: Drawable): Int {
            // Rough estimation, since Drawable size varies
            return 1024 // 1MB estimate per icon
        }
    }

    fun getIcon(context: Context, packageName: String): Drawable? {
        val cached = iconCache.get(packageName)
        if (cached != null) return cached

        return try {
            val icon = context.packageManager.getApplicationIcon(packageName)
            iconCache.put(packageName, icon)
            icon
        } catch (e: Exception) {
            null
        }
    }
}
