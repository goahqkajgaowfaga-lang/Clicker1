package com.zhenya.clicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ClickerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var clickX = 0f
    private var clickY = 0f

    @Volatile
    var isClicking = false
        private set

    override fun onServiceConnected() {
        instance = this
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_UP) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            if (isClicking) {
                stopClicking()
            } else {
                val m = resources.displayMetrics
                startClicking(m.widthPixels * X_RATIO, m.heightPixels * Y_RATIO)
            }
        }
        return true
    }

    fun startClicking(x: Float, y: Float) {
        if (isClicking) return
        clickX = x
        clickY = y
        isClicking = true
        job = scope.launch {
            val t0 = SystemClock.uptimeMillis()
            var n = 0L
            while (isClicking) {
                click()
                n++
                val wait = t0 + n * 1000L / CPS - SystemClock.uptimeMillis()
                if (wait > 0) delay(wait)
            }
        }
    }

    fun stopClicking() {
        isClicking = false
        job?.cancel()
        job = null
    }

    private fun click() {
        val path = Path().apply { moveTo(clickX, clickY) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 1L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopClicking()
    }

    override fun onDestroy() {
        stopClicking()
        scope.cancel()
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val CPS = 14
        private const val X_RATIO = 0.85f
        private const val Y_RATIO = 0.5f

        @Volatile
        var instance: ClickerService? = null
            private set
    }
}
