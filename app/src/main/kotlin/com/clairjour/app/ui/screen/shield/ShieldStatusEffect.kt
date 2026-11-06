package com.clairjour.app.ui.screen.shield

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Time Android needs to bind the service after it is switched on in the settings. */
private const val BIND_SETTLE_MILLIS = 2_000L

/**
 * Calls [onRefresh] when the screen resumes and whenever accessibility settings change.
 * A change fires before Android binds the service, so the state is read again once it settled:
 * otherwise a freshly enabled shield would look "stopped by the phone".
 */
@Composable
fun ShieldStatusEffect(onRefresh: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        onRefresh()
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        val listener = AccessibilityManager.AccessibilityStateChangeListener {
            onRefresh()
            scope.launch {
                delay(BIND_SETTLE_MILLIS)
                onRefresh()
            }
        }
        accessibilityManager?.addAccessibilityStateChangeListener(listener)
        onPauseOrDispose { accessibilityManager?.removeAccessibilityStateChangeListener(listener) }
    }
}
