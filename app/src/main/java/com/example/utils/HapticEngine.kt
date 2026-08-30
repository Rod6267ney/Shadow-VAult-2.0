package com.example.utils

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticEngine {

    fun vibrateClick(context: Context) {
        performVibration(context, longArrayOf(0, 15), intArrayOf(0, 70))
    }

    fun vibrateSuccess(context: Context) {
        // Double soft burst
        performVibration(context, longArrayOf(0, 25, 60, 35), intArrayOf(0, 100, 0, 180))
    }

    fun vibrateError(context: Context) {
        // Triple harsh vibration
        performVibration(context, longArrayOf(0, 50, 50, 50, 50, 70), intArrayOf(0, 220, 0, 220, 0, 255))
    }

    fun vibrateFreeze(context: Context) {
        // Quick subtle crisp buzz
        performVibration(context, longArrayOf(0, 40, 30, 40), intArrayOf(0, 90, 0, 140))
    }

    fun vibrateLock(context: Context) {
        // Deep thud
        performVibration(context, longArrayOf(0, 80), intArrayOf(0, 200))
    }

    private fun performVibration(context: Context, timings: LongArray, amplitudes: IntArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(timings[1])
            }
        } catch (_: Exception) {
            // Silently ignore if device doesn't have vibration hardware or permission
        }
    }
}
