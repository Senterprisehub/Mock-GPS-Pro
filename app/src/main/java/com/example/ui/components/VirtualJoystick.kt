package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlueDark
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun VirtualJoystick(
    onDragStep: (deltaX: Float, deltaY: Float, maxRadius: Float) -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Int = 130
) {
    val coroutineScope = rememberCoroutineScope()
    val animatedOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var bearingAngle by remember { mutableFloatStateOf(0f) }
    val maxRadiusPx = (sizeDp * 1.3f)

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PureWhite.copy(alpha = 0.96f),
                        Color(0xFFF1F5F9).copy(alpha = 0.96f)
                    )
                )
            )
            .border(2.dp, Slate300, CircleShape)
            .testTag("joystick_container"),
        contentAlignment = Alignment.Center
    ) {
        // Concentric inner radar ring
        Box(
            modifier = Modifier
                .size((sizeDp * 0.65f).dp)
                .clip(CircleShape)
                .border(1.dp, BluePrimary.copy(alpha = 0.25f), CircleShape)
        )

        // Direction labels
        Text(
            text = "N",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Text(
            text = "S",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Text(
            text = "W",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400
            ),
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = "E",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // Center Draggable Knob
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        animatedOffset.value.x.roundToInt(),
                        animatedOffset.value.y.roundToInt()
                    )
                }
                .size((sizeDp * 0.40f).dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B82F6),
                            BluePrimary
                        )
                    )
                )
                .border(2.dp, PureWhite, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                animatedOffset.animateTo(
                                    Offset.Zero,
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                                )
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                animatedOffset.animateTo(
                                    Offset.Zero,
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                                )
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val current = animatedOffset.value
                            val newX = current.x + dragAmount.x
                            val newY = current.y + dragAmount.y
                            val dist = hypot(newX, newY)
                            val angleRad = atan2(newY.toDouble(), newX.toDouble())
                            val angleDeg = ((Math.toDegrees(angleRad) + 90 + 360) % 360).toFloat()
                            bearingAngle = angleDeg

                            val clampedDist = dist.coerceAtMost(maxRadiusPx)
                            val finalX = (cos(angleRad) * clampedDist).toFloat()
                            val finalY = (sin(angleRad) * clampedDist).toFloat()

                            coroutineScope.launch {
                                animatedOffset.snapTo(Offset(finalX, finalY))
                            }

                            onDragStep(finalX, finalY, maxRadiusPx)
                        }
                    )
                }
                .testTag("joystick_knob"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Joystick Pointer",
                tint = PureWhite,
                modifier = Modifier
                    .size((sizeDp * 0.18f).dp)
                    .rotate(bearingAngle)
            )
        }
    }
}
