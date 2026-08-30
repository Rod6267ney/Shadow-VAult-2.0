package com.example.services

import android.view.MotionEvent
import android.view.Surface

/**
 * Motor de Renderização de Interface para o ChaosSpace Hypervisor (Sem root).
 * Substitui o io.twoyi.Renderer.
 */
object ChaosSpaceRenderer {
    
    var isLoaded: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("shadow_vm")
            isLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            isLoaded = false
        }
    }

    external fun init(surface: Surface, name: String, xdpi: Float, ydpi: Float, fps: Int)
    external fun handleTouch(event: MotionEvent)
    external fun resetWindow(surface: Surface, x: Int, y: Int, width: Int, height: Int)
    external fun removeWindow(surface: Surface)
}
