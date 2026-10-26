package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.ui.theme.MockGpsTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

class FloatingJoystickService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    companion object {
        var onJoystickStep: ((deltaLat: Double, deltaLng: Double, bearing: Float) -> Unit)? = null

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
                val intent = Intent(context, FloatingJoystickService::class.java)
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingJoystickService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        setupOverlayWindow()
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingJoystickService)
            setViewTreeSavedStateRegistryOwner(this@FloatingJoystickService)
            setContent {
                MockGpsTheme {
                    FloatingJoystickWidget(
                        onMoveWindow = { dx, dy ->
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            windowManager?.updateViewLayout(this, params)
                        },
                        onClose = {
                            stopSelf()
                        },
                        onStep = { angleDeg, intensity ->
                            val speedStep = 0.00002 * intensity
                            val angleRad = Math.toRadians(angleDeg.toDouble())
                            val dLat = cos(angleRad) * speedStep
                            val dLng = sin(angleRad) * speedStep
                            onJoystickStep?.invoke(dLat, dLng, angleDeg)
                        }
                    )
                }
            }
        }

        floatingView = composeView
        try {
            windowManager?.addView(composeView, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // ignore
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun FloatingJoystickWidget(
    onMoveWindow: (Float, Float) -> Unit,
    onClose: () -> Unit,
    onStep: (angleDeg: Float, intensity: Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(IntOffset.Zero) }
    var bearingAngle by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(Color(0xE60F172A))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMoveWindow(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Close button top-right
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Floating Joystick",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Outer guide ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0x333B82F6))
        )

        // Joystick knob
        Box(
            modifier = Modifier
                .offset { knobOffset }
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF2563EB))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            knobOffset = IntOffset.Zero
                        },
                        onDragCancel = {
                            knobOffset = IntOffset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = knobOffset.x + dragAmount.x
                            val newY = knobOffset.y + dragAmount.y
                            val maxRadius = 40f
                            val distance = hypot(newX, newY)
                            val angleRad = atan2(newY.toDouble(), newX.toDouble())
                            val angleDeg = ((Math.toDegrees(angleRad) + 90 + 360) % 360).toFloat()
                            bearingAngle = angleDeg

                            val clampedDist = distance.coerceAtMost(maxRadius)
                            val finalX = cos(angleRad) * clampedDist
                            val finalY = sin(angleRad) * clampedDist
                            knobOffset = IntOffset(finalX.roundToInt(), finalY.roundToInt())

                            val intensity = (clampedDist / maxRadius).coerceIn(0f, 1f)
                            if (intensity > 0.05f) {
                                onStep(angleDeg, intensity)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Draggable Joystick",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(bearingAngle)
            )
        }
    }
}
