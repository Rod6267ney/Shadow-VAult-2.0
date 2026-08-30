package io.twoyi

import android.view.MotionEvent
import android.view.Surface

/**
 * Ponte JNI nativa para o motor de renderização TwoYi (Rust/OpenGL).
 * Responsável por conectar o SurfaceView do Compose ao hipervisor do Android virtual.
 */
object Renderer {
    var isLoaded = false

    init {
        try {
            System.loadLibrary("OpenglRender")
            isLoaded = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    private external fun nativeInit(surface: Surface, loader: String, xdpi: Float, ydpi: Float, fps: Int)
    
    @JvmStatic
    private external fun nativeResetWindow(surface: Surface, top: Int, left: Int, width: Int, height: Int)
    
    @JvmStatic
    private external fun nativeRemoveWindow(surface: Surface)
    
    @JvmStatic
    private external fun nativeHandleTouch(event: MotionEvent)
    
    @JvmStatic
    private external fun nativeSendKeycode(keycode: Int)

    fun init(surface: Surface, loader: String, xdpi: Float, ydpi: Float, fps: Int) {
        if (!isLoaded) return
        try { nativeInit(surface, loader, xdpi, ydpi, fps) } catch (e: Throwable) { e.printStackTrace() }
    }

    fun resetWindow(surface: Surface, top: Int, left: Int, width: Int, height: Int) {
        if (!isLoaded) return
        try { nativeResetWindow(surface, top, left, width, height) } catch (e: Throwable) { e.printStackTrace() }
    }

    fun removeWindow(surface: Surface) {
        if (!isLoaded) return
        try { nativeRemoveWindow(surface) } catch (e: Throwable) { e.printStackTrace() }
    }

    fun handleTouch(event: MotionEvent) {
        if (!isLoaded) return
        try { nativeHandleTouch(event) } catch (e: Throwable) { e.printStackTrace() }
    }

    fun sendKeycode(keycode: Int) {
        if (!isLoaded) return
        try { nativeSendKeycode(keycode) } catch (e: Throwable) { e.printStackTrace() }
    }
}
