package com.niagara.launcher.ui.home.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Pure math helpers for AlphabetWaveBar positioning and Gaussian wave distortion.
 */
object AlphabetWaveMath {
    fun yToLetterIndex(touchY: Float, totalHeightPx: Float, letterCount: Int): Int? {
        if (totalHeightPx <= 0f || letterCount <= 0) return null
        val itemHeight = totalHeightPx / letterCount
        if (itemHeight <= 0f) return null
        return (touchY / itemHeight).toInt().coerceIn(0, letterCount - 1)
    }

    fun calculateGaussian(distancePx: Float, sigmaPx: Float): Float {
        if (sigmaPx <= 0f) return 0f
        return exp(-(distancePx * distancePx) / (2f * sigmaPx * sigmaPx))
    }

    fun calculateBubbleOffsetY(touchY: Float, totalHeightPx: Float, bubbleSizePx: Float): Float {
        val maxOffset = (totalHeightPx - bubbleSizePx).coerceAtLeast(0f)
        return (touchY - bubbleSizePx / 2f).coerceIn(0f, maxOffset)
    }
}

/**
 * Signature Niagara Alphabet Wave Scrollbar with unified single-stream gesture handling,
 * graphicsLayer-driven 120Hz Gaussian distortion, and accurate thumb-aligned preview bubble.
 */
@Composable
fun AlphabetWaveBar(
    modifier: Modifier = Modifier,
    isRightHanded: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    availableLetters: Set<Char> = emptySet(),
    activeLetter: Char? = null,
    onLetterSelected: (Char) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    val alphabet = remember { "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toCharArray() }

    var isTouching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var currentFocusedLetter by remember { mutableStateOf<Char?>(null) }

    // Cache Vibrator instance once per context
    val vibrator = remember(context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    // Wave displacement animated factor (0f when idle, 1f when touched)
    val waveFactor = remember { Animatable(0f) }
    val bubbleScale by animateFloatAsState(
        targetValue = if (isTouching && currentFocusedLetter != null) 1f else 0.72f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bubbleScale"
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isTouching && currentFocusedLetter != null) 1f else 0f,
        animationSpec = tween(durationMillis = 110),
        label = "bubbleAlpha"
    )

    LaunchedEffect(isTouching) {
        if (isTouching) {
            waveFactor.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        } else {
            waveFactor.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            )
            currentFocusedLetter = null
        }
    }

    fun triggerHapticTick() {
        if (!hapticFeedbackEnabled) return
        val performed = view.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        )
        if (!performed) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(10L)
                }
            } catch (_: Exception) {
                // Ignore if vibration unavailable
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .pointerInput(Unit) {
                // Single unified gesture stream: instant touch-down response + zero-slop drag
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isTouching = true
                    touchY = down.position.y

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            break
                        }
                        change.consume()
                        touchY = change.position.y
                    }
                    isTouching = false
                }
            },
        contentAlignment = if (isRightHanded) Alignment.TopEnd else Alignment.TopStart
    ) {
        val totalHeightPx = with(density) { maxHeight.toPx() }
        val itemHeightPx = if (alphabet.isNotEmpty()) totalHeightPx / alphabet.size else 0f
        val sigmaPx = itemHeightPx * 2.8f
        val maxDisplacementPx = with(density) { 36.dp.toPx() }
        val bubbleSizePx = with(density) { 52.dp.toPx() }

        val focusedIndex = if (isTouching) {
            AlphabetWaveMath.yToLetterIndex(touchY, totalHeightPx, alphabet.size)
        } else {
            null
        }

        LaunchedEffect(focusedIndex) {
            if (focusedIndex != null) {
                val newLetter = alphabet[focusedIndex]
                if (newLetter != currentFocusedLetter) {
                    currentFocusedLetter = newLetter
                    triggerHapticTick()
                    onLetterSelected(newLetter)
                }
            }
        }

        // Render 27 letters with graphicsLayer draw-phase transformations (zero recomposition while dragging)
        Box(modifier = Modifier.fillMaxHeight()) {
            alphabet.forEachIndexed { index, letter ->
                val letterCenterY = (index + 0.5f) * itemHeightPx
                val isFocused = index == focusedIndex
                val isSectionActive = !isTouching && letter == activeLetter
                val isAvailable = availableLetters.isEmpty() || availableLetters.contains(letter)
                val baseAlpha = when {
                    isSectionActive -> 0.95f
                    isAvailable -> 0.58f
                    else -> 0.25f
                }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = 0,
                                y = (letterCenterY - (itemHeightPx / 2f)).roundToInt()
                            )
                        }
                        .size(width = 32.dp, height = with(density) { itemHeightPx.toDp() })
                        .graphicsLayer {
                            val currentWave = waveFactor.value
                            val gaussian = if (currentWave > 0.001f) {
                                AlphabetWaveMath.calculateGaussian(
                                    distancePx = abs(touchY - letterCenterY),
                                    sigmaPx = sigmaPx
                                ) * currentWave
                            } else {
                                0f
                            }
                            val displacementPx = maxDisplacementPx * gaussian
                            translationX = if (isRightHanded) -displacementPx else displacementPx
                            val targetScale = if (isSectionActive) 1.15f else (1.0f + 0.85f * gaussian)
                            scaleX = targetScale
                            scaleY = targetScale
                            alpha = (baseAlpha + (1f - baseAlpha) * gaussian).coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = when {
                                isFocused || isSectionActive -> FontWeight.ExtraBold
                                isAvailable -> FontWeight.SemiBold
                                else -> FontWeight.Normal
                            },
                            color = when {
                                isFocused -> Color(0xFF64B5F6)
                                isSectionActive -> MaterialTheme.colorScheme.primary
                                else -> Color.White
                            }
                        )
                    )
                }
            }
        }

        // Top-aligned floating preview bubble aligned accurately to thumb touchY
        val displayLetter = currentFocusedLetter
        if (bubbleAlpha > 0.01f && displayLetter != null) {
            val bubbleOffsetY = AlphabetWaveMath.calculateBubbleOffsetY(
                touchY = touchY,
                totalHeightPx = totalHeightPx,
                bubbleSizePx = bubbleSizePx
            )
            val bubbleOffsetX = with(density) { if (isRightHanded) (-72).dp.toPx() else 72.dp.toPx() }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset { IntOffset(bubbleOffsetX.roundToInt(), bubbleOffsetY.roundToInt()) }
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = bubbleScale
                        scaleY = bubbleScale
                        alpha = bubbleAlpha
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayLetter.toString(),
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
