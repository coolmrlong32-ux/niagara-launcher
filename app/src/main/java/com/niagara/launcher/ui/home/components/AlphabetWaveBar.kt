package com.niagara.launcher.ui.home.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Signature Niagara Alphabet Wave Scrollbar with Gaussian physical distortion
 * and haptic tick feedback on letter boundary transitions.
 */
@Composable
fun AlphabetWaveBar(
    modifier: Modifier = Modifier,
    isRightHanded: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    onLetterSelected: (Char) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val alphabet = remember { "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toCharArray() }

    var isTouching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var currentFocusedLetter by remember { mutableStateOf<Char?>(null) }

    // Wave displacement animated factor (0f when idle, 1f when touched)
    val waveFactor = remember { Animatable(0f) }

    LaunchedEffect(isTouching) {
        if (isTouching) {
            waveFactor.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
        } else {
            waveFactor.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            currentFocusedLetter = null
        }
    }

    // Trigger haptic tick feedback
    fun triggerHapticFeedback() {
        if (!hapticFeedbackEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(12)
                }
            }
        } catch (_: Exception) {
            // Safe fallback if permission or hardware unavailable
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(52.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isTouching = true
                        touchY = offset.y
                        tryAwaitRelease()
                        isTouching = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isTouching = true
                        touchY = offset.y
                    },
                    onDrag = { change, _ ->
                        touchY = change.position.y
                    },
                    onDragEnd = {
                        isTouching = false
                    },
                    onDragCancel = {
                        isTouching = false
                    }
                )
            },
        contentAlignment = if (isRightHanded) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val itemHeightPx = totalHeightPx / alphabet.size
        val sigmaPx = itemHeightPx * 2.8f
        val maxDisplacementPx = with(density) { 36.dp.toPx() }

        // Find currently focused letter index
        val focusedIndex = if (isTouching && itemHeightPx > 0) {
            (touchY / itemHeightPx).toInt().coerceIn(0, alphabet.lastIndex)
        } else {
            null
        }

        LaunchedEffect(focusedIndex) {
            if (focusedIndex != null) {
                val newLetter = alphabet[focusedIndex]
                if (newLetter != currentFocusedLetter) {
                    currentFocusedLetter = newLetter
                    triggerHapticFeedback()
                    onLetterSelected(newLetter)
                }
            }
        }

        // Draw individual letters with Gaussian offset & scaling
        Box(modifier = Modifier.fillMaxHeight()) {
            alphabet.forEachIndexed { index, letter ->
                val letterCenterY = (index + 0.5f) * itemHeightPx

                // Calculate Gaussian wave influence
                val distance = if (isTouching) kotlin.math.abs(touchY - letterCenterY) else 1000f
                val gaussian = exp(- (distance * distance) / (2f * sigmaPx * sigmaPx)) * waveFactor.value

                // Horizontal translation: moves toward center of the screen
                val displacementPx = maxDisplacementPx * gaussian
                val offsetX = if (isRightHanded) -displacementPx else displacementPx

                // Scale up dynamically
                val scale = 1.0f + 0.85f * gaussian
                val alpha = (0.45f + 0.55f * gaussian).coerceIn(0f, 1f)
                val isFocused = index == focusedIndex

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = offsetX.roundToInt(),
                                y = (letterCenterY - (itemHeightPx / 2f)).roundToInt()
                            )
                        }
                        .size(width = 32.dp, height = with(density) { itemHeightPx.toDp() })
                        .scale(scale)
                        .alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isFocused) Color(0xFF64B5F6) else Color.White
                        )
                    )
                }
            }
        }

        // Floating magnified letter bubble indicator next to thumb
        if (isTouching && currentFocusedLetter != null) {
            val bubbleOffsetY = (touchY - with(density) { 26.dp.toPx() }).coerceIn(
                0f,
                totalHeightPx - with(density) { 52.dp.toPx() }
            )
            val bubbleOffsetX = with(density) { if (isRightHanded) (-72).dp.toPx() else 72.dp.toPx() }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset { IntOffset(bubbleOffsetX.roundToInt(), bubbleOffsetY.roundToInt()) }
                    .size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = currentFocusedLetter.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }
    }
}
