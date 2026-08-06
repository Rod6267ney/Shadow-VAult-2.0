package com.example.utils

import android.util.Log
import com.example.BuildConfig

object Logger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, throwable)
        }
        // Em produção, salvar no banco de dados interno de forma criptografada (implementação pendente)
    }
}
