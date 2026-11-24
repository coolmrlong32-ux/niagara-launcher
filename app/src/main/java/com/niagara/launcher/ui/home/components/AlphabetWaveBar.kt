package com.niagara.launcher.ui.home.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.core.theme.WaveBubbleBorder
import com.niagara.launcher.core.theme.WaveBubbleSurface
import com.niagara.launcher.core.theme.WaveTrackActiveFill
import com.niagara.launcher.core.theme.WaveTrackBorder
import com.niagara.launcher.core.theme.WaveTrackFill
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Ergonomic vertical placement for the one-handed alphabet bar inside a container.
 */
data class ErgonomicBand(
    val topOffsetPx: Float,
    val bandHeightPx: Float
)

/**
 * Intent classification for edge-based invisible alphabet gestures so horizontal
 * paging and system edge-back gestures remain unobstructed.
 */
enum class EdgeGestureIntent {
    UNDECIDED,
    VERTICAL_ALPHABET,
    HORIZONTAL_PASS_THROUGH
}

/**
 * Pure math helpers for AlphabetWaveBar positioning, one-handed ergonomic banding,
 * Gaussian wave distortion, and left-edge gesture disambiguation.
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

    /**
     * Computes a compact middle-lower vertical band reachable comfortably with one thumb.
     */
    fun calculateErgonomicBand(
        containerHeightPx: Float,
        minBandHeightPx: Float,
        maxBandHeightPx: Float,
        heightFraction: Float = 0.56f,
        verticalBias: Float = 0.64f,
        topSafeMarginPx: Float = 0f,
        bottomSafeMarginPx: Float = 0f
    ): ErgonomicBand {
        if (containerHeightPx <= 0f) return ErgonomicBand(0f, 0f)

        val safeTop = topSafeMarginPx.coerceAtLeast(0f)
        val safeBottom = bottomSafeMarginPx.coerceAtLeast(0f)
        val availableHeight = (containerHeightPx - safeTop - safeBottom).coerceAtLeast(0f)
        if (availableHeight <= 0f) return ErgonomicBand(safeTop.coerceAtMost(containerHeightPx), 0f)

        val normalizedMin = minBandHeightPx.coerceAtLeast(0f).coerceAtMost(availableHeight)
        val normalizedMax = maxBandHeightPx.coerceAtLeast(normalizedMin).coerceAtMost(availableHeight)
        val targetHeight = (availableHeight * heightFraction.coerceIn(0.25f, 1f))
            .coerceIn(normalizedMin, normalizedMax)

        val slack = (availableHeight - targetHeight).coerceAtLeast(0f)
        val clampedBias = verticalBias.coerceIn(0f, 1f)
        val topOffset = safeTop + slack * clampedBias

        return ErgonomicBand(
            topOffsetPx = topOffset,
            bandHeightPx = targetHeight
        )
    }

    /**
     * Distinguishes vertical alphabet scrubbing from horizontal page/system-back swipes
     * on the invisible left edge hotspot.
     */
    fun classifyEdgeGesture(
        dx: Float,
        dy: Float,
        lockSlopPx: Float,
        verticalDominanceRatio: Float = 1.15f
    ): EdgeGestureIntent {
        val absDx = abs(dx)
        val absDy = abs(dy)
        val safeSlop = lockSlopPx.coerceAtLeast(1f)
        if (absDx < safeSlop && absDy < safeSlop) {
            return EdgeGestureIntent.UNDECIDED
        }
        return if (absDy >= absDx * verticalDominanceRatio && absDy >= safeSlop) {
            EdgeGestureIntent.VERTICAL_ALPHABET
        } else {
            EdgeGestureIntent.HORIZONTAL_PASS_THROUGH
        }
    }
}

private val ALPHABET_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toCharArray()

@Composable
private fun rememberAlphabetHapticTrigger(hapticFeedbackEnabled: Boolean): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current

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

    return remember(hapticFeedbackEnabled, view, vibrator) {
        {
            if (hapticFeedbackEnabled) {
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
        }
    }
}

/**
 * Shared frosted glass floating letter preview bubble.
 */
@Composable
private fun AlphabetPreviewBubble(
    letter: Char,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    alpha: Float
) {
    val visuals = LocalLauncherVisuals.current
    val bubbleShape = CircleShape

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(
                elevation = 14.dp,
                shape = bubbleShape,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.65f)
            )
            .clip(bubbleShape)
            .background(WaveBubbleSurface)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        Color.White.copy(alpha = 0.04f)
                    )
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(WaveBubbleBorder, Color.White.copy(alpha = 0.12f))
                ),
                shape = bubbleShape
            )
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = visuals.textPrimary,
                shadow = Shadow(
                    color = visuals.labelShadow,
                    offset = Offset(0f, 1.5f),
                    blurRadius = 4f
                )
            )
        )
    }
}

/**
 * Ergonomic one-handed right-side Niagara Alphabet Wave Scrollbar.
 * Constrained to the middle-lower golden thumb zone with refined frosted track,
 * graphicsLayer-driven 120Hz Gaussian distortion, and thumb-aligned floating bubble.
 */
@Composable
fun AlphabetWaveBar(
    modifier: Modifier = Modifier,
    isRightHanded: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    availableLetters: Set<Char> = emptySet(),
    activeLetter: Char? = null,
    externalFocusedLetter: Char? = null,
    onLetterSelected: (Char) -> Unit
) {
    val density = LocalDensity.current
    val visuals = LocalLauncherVisuals.current
    val triggerHapticTick = rememberAlphabetHapticTrigger(hapticFeedbackEnabled)

    var isTouching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var currentFocusedLetter by remember { mutableStateOf<Char?>(null) }

    val waveFactor = remember { Animatable(0f) }
    val bubbleScale by animateFloatAsState(
        targetValue = if (isTouching && currentFocusedLetter != null) 1f else 0.72f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rightBubbleScale"
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isTouching && currentFocusedLetter != null) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "rightBubbleAlpha"
    )
    val trackColor by animateColorAsState(
        targetValue = if (isTouching) WaveTrackActiveFill else WaveTrackFill,
        animationSpec = tween(durationMillis = 140),
        label = "waveTrackColor"
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(42.dp),
        contentAlignment = if (isRightHanded) Alignment.TopEnd else Alignment.TopStart
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val band = remember(containerHeightPx, density) {
            AlphabetWaveMath.calculateErgonomicBand(
                containerHeightPx = containerHeightPx,
                minBandHeightPx = with(density) { 300.dp.toPx() },
                maxBandHeightPx = with(density) { 424.dp.toPx() },
                heightFraction = 0.56f,
                verticalBias = 0.64f,
                topSafeMarginPx = with(density) { 72.dp.toPx() },
                bottomSafeMarginPx = with(density) { 54.dp.toPx() }
            )
        }
        val bandHeightDp = with(density) { band.bandHeightPx.toDp() }
        val itemHeightPx = if (ALPHABET_CHARS.isNotEmpty()) band.bandHeightPx / ALPHABET_CHARS.size else 0f
        val sigmaPx = (itemHeightPx * 2.9f).coerceAtLeast(18f)
        val maxDisplacementPx = with(density) { 32.dp.toPx() }
        val bubbleSizePx = with(density) { 54.dp.toPx() }
        val letterFontSp = if (itemHeightPx < with(density) { 12.5.dp.toPx() }) 9.sp else 9.5.sp

        val focusedIndex = if (isTouching) {
            AlphabetWaveMath.yToLetterIndex(touchY, band.bandHeightPx, ALPHABET_CHARS.size)
        } else {
            null
        }

        LaunchedEffect(focusedIndex) {
            if (focusedIndex != null) {
                val newLetter = ALPHABET_CHARS[focusedIndex]
                if (newLetter != currentFocusedLetter) {
                    currentFocusedLetter = newLetter
                    triggerHapticTick()
                    onLetterSelected(newLetter)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, band.topOffsetPx.roundToInt()) }
                .height(bandHeightDp)
                .width(42.dp)
                .pointerInput(band.bandHeightPx) {
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
            // Subtle frosted hairline pill track behind the compact alphabet list
            Box(
                modifier = Modifier
                    .align(if (isRightHanded) Alignment.CenterEnd else Alignment.CenterStart)
                    .offset(x = if (isRightHanded) (-5).dp else 5.dp)
                    .width(20.dp)
                    .height(bandHeightDp + 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(trackColor)
                    .border(0.5.dp, WaveTrackBorder, RoundedCornerShape(50))
            )

            // 27 compact letters with draw-phase Gaussian distortion
            Box(modifier = Modifier.fillMaxHeight()) {
                ALPHABET_CHARS.forEachIndexed { index, letter ->
                    val letterCenterY = (index + 0.5f) * itemHeightPx
                    val isFocused = index == focusedIndex
                    val isExternallyFocused = !isTouching && letter == externalFocusedLetter
                    val isSectionActive = !isTouching && !isExternallyFocused && letter == activeLetter
                    val isAvailable = availableLetters.isEmpty() || availableLetters.contains(letter)
                    val baseAlpha = when {
                        isExternallyFocused -> 0.98f
                        isSectionActive -> 0.94f
                        isAvailable -> 0.66f
                        else -> 0.24f
                    }

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = if (isRightHanded) -with(density) { 4.dp.roundToPx() } else with(density) { 4.dp.roundToPx() },
                                    y = (letterCenterY - (itemHeightPx / 2f)).roundToInt()
                                )
                            }
                            .size(width = 22.dp, height = with(density) { itemHeightPx.toDp() })
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
                                val targetScale = when {
                                    isExternallyFocused -> 1.28f
                                    isSectionActive -> 1.14f
                                    else -> 1.0f + 0.78f * gaussian
                                }
                                scaleX = targetScale
                                scaleY = targetScale
                                alpha = (baseAlpha + (1f - baseAlpha) * gaussian).coerceIn(0f, 1f)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = letterFontSp,
                                lineHeight = letterFontSp,
                                fontWeight = when {
                                    isFocused || isExternallyFocused || isSectionActive -> FontWeight.ExtraBold
                                    isAvailable -> FontWeight.SemiBold
                                    else -> FontWeight.Normal
                                },
                                color = when {
                                    isFocused || isExternallyFocused -> visuals.accent
                                    isSectionActive -> visuals.textPrimary
                                    else -> Color.White
                                },
                                shadow = Shadow(
                                    color = Color(0x8A000000),
                                    offset = Offset(0f, 0.8f),
                                    blurRadius = 2f
                                )
                            )
                        )
                    }
                }
            }

            // Floating frosted preview bubble aligned to thumb Y
            val displayLetter = currentFocusedLetter
            if (bubbleAlpha > 0.01f && displayLetter != null) {
                val bubbleOffsetY = AlphabetWaveMath.calculateBubbleOffsetY(
                    touchY = touchY,
                    totalHeightPx = band.bandHeightPx,
                    bubbleSizePx = bubbleSizePx
                )
                val bubbleOffsetX = with(density) { if (isRightHanded) (-76).dp.toPx() else 66.dp.toPx() }
                AlphabetPreviewBubble(
                    letter = displayLetter,
                    offsetX = bubbleOffsetX,
                    offsetY = bubbleOffsetY,
                    scale = bubbleScale,
                    alpha = bubbleAlpha
                )
            }
        }
    }
}

/**
 * Completely invisible left-edge alphabet scrubbing hotspot aligned to the same one-handed
 * ergonomic vertical band. Renders no letter column or background, and yields immediately
 * to horizontal swipes so HorizontalPager and system edge-back gestures never conflict.
 */
@Composable
fun InvisibleLeftAlphabetHotspot(
    modifier: Modifier = Modifier,
    hapticFeedbackEnabled: Boolean = true,
    onLetterSelected: (Char) -> Unit,
    onFocusedLetterChanged: (Char?) -> Unit = {}
) {
    val density = LocalDensity.current
    val triggerHapticTick = rememberAlphabetHapticTrigger(hapticFeedbackEnabled)

    var isScrubbing by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var currentFocusedLetter by remember { mutableStateOf<Char?>(null) }

    val bubbleScale by animateFloatAsState(
        targetValue = if (isScrubbing && currentFocusedLetter != null) 1f else 0.72f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "leftBubbleScale"
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isScrubbing && currentFocusedLetter != null) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "leftBubbleAlpha"
    )

    LaunchedEffect(isScrubbing) {
        if (!isScrubbing) {
            currentFocusedLetter = null
            onFocusedLetterChanged(null)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(34.dp),
        contentAlignment = Alignment.TopStart
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val band = remember(containerHeightPx, density) {
            AlphabetWaveMath.calculateErgonomicBand(
                containerHeightPx = containerHeightPx,
                minBandHeightPx = with(density) { 300.dp.toPx() },
                maxBandHeightPx = with(density) { 424.dp.toPx() },
                heightFraction = 0.56f,
                verticalBias = 0.64f,
                topSafeMarginPx = with(density) { 72.dp.toPx() },
                bottomSafeMarginPx = with(density) { 54.dp.toPx() }
            )
        }
        val bandHeightDp = with(density) { band.bandHeightPx.toDp() }
        val bubbleSizePx = with(density) { 54.dp.toPx() }
        val lockSlopPx = with(density) { 7.dp.toPx() }

        val focusedIndex = if (isScrubbing) {
            AlphabetWaveMath.yToLetterIndex(touchY, band.bandHeightPx, ALPHABET_CHARS.size)
        } else {
            null
        }

        LaunchedEffect(focusedIndex) {
            if (focusedIndex != null) {
                val newLetter = ALPHABET_CHARS[focusedIndex]
                if (newLetter != currentFocusedLetter) {
                    currentFocusedLetter = newLetter
                    onFocusedLetterChanged(newLetter)
                    triggerHapticTick()
                    onLetterSelected(newLetter)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, band.topOffsetPx.roundToInt()) }
                .height(bandHeightDp)
                .width(34.dp)
                .pointerInput(band.bandHeightPx, lockSlopPx) {
                    awaitEachGesture {
                        // Do NOT consume down immediately so taps on items and horizontal swipes work smoothly
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        var intent = EdgeGestureIntent.UNDECIDED

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                break
                            }

                            if (intent == EdgeGestureIntent.UNDECIDED) {
                                val dx = change.position.x - startPos.x
                                val dy = change.position.y - startPos.y
                                intent = AlphabetWaveMath.classifyEdgeGesture(
                                    dx = dx,
                                    dy = dy,
                                    lockSlopPx = lockSlopPx
                                )
                                when (intent) {
                                    EdgeGestureIntent.HORIZONTAL_PASS_THROUGH -> break
                                    EdgeGestureIntent.VERTICAL_ALPHABET -> {
                                        change.consume()
                                        isScrubbing = true
                                        touchY = change.position.y
                                    }
                                    EdgeGestureIntent.UNDECIDED -> Unit
                                }
                            } else if (intent == EdgeGestureIntent.VERTICAL_ALPHABET) {
                                change.consume()
                                touchY = change.position.y
                            }
                        }

                        if (isScrubbing) {
                            isScrubbing = false
                        }
                    }
                },
            contentAlignment = Alignment.TopStart
        ) {
            // Visually transparent edge strip; only the floating preview bubble appears while scrubbing
            val displayLetter = currentFocusedLetter
            if (bubbleAlpha > 0.01f && displayLetter != null) {
                val bubbleOffsetY = AlphabetWaveMath.calculateBubbleOffsetY(
                    touchY = touchY,
                    totalHeightPx = band.bandHeightPx,
                    bubbleSizePx = bubbleSizePx
                )
                val bubbleOffsetX = with(density) { 58.dp.toPx() }
                AlphabetPreviewBubble(
                    letter = displayLetter,
                    offsetX = bubbleOffsetX,
                    offsetY = bubbleOffsetY,
                    scale = bubbleScale,
                    alpha = bubbleAlpha
                )
            }
        }
    }
}
