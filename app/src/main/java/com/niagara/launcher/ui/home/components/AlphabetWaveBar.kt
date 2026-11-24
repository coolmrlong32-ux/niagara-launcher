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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.niagara.launcher.core.theme.LauncherMotion
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.core.theme.WaveBubbleBorder
import com.niagara.launcher.core.theme.WaveBubbleSurface
import com.niagara.launcher.core.theme.WaveTrackActiveFill
import com.niagara.launcher.core.theme.WaveTrackBorder
import com.niagara.launcher.core.theme.WaveTrackFill
import com.niagara.launcher.ui.home.HomeListModel
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
 * Per-letter wave distortion transform (horizontal pop-out, symmetrical neighbor fan-out, scale, alpha).
 */
data class WaveLetterTransform(
    val translationX: Float,
    val translationY: Float,
    val scale: Float,
    val alpha: Float
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
 * retractable Gaussian wave distortion, and left-edge gesture disambiguation.
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

    fun calculateWaveTransform(
        letterCenterY: Float,
        touchY: Float,
        sigmaPx: Float,
        maxDisplacementX: Float,
        maxSpreadY: Float,
        waveFactor: Float,
        isRightHanded: Boolean,
        baseAlpha: Float,
        isHighlighted: Boolean
    ): WaveLetterTransform {
        val clampedWave = waveFactor.coerceIn(0f, 1f)
        val rawGaussian = if (sigmaPx > 0f) {
            calculateGaussian(abs(touchY - letterCenterY), sigmaPx)
        } else {
            0f
        }
        val activeGaussian = rawGaussian * clampedWave
        val displacementX = maxDisplacementX.coerceAtLeast(0f) * activeGaussian
        val translationX = if (isRightHanded) -displacementX else displacementX

        val diffY = letterCenterY - touchY
        val translationY = if (clampedWave > 0.001f && abs(diffY) > 0.001f && maxSpreadY > 0f) {
            val directionY = if (diffY > 0f) 1f else -1f
            val fanCurve = 4f * rawGaussian * (1f - rawGaussian) * clampedWave
            directionY * maxSpreadY * fanCurve
        } else {
            0f
        }

        val restingScale = if (isHighlighted) 1.18f else 1.0f
        val waveScale = 1.0f + 0.85f * activeGaussian
        val scale = maxOf(restingScale, waveScale)
        val alpha = (baseAlpha + (1f - baseAlpha) * activeGaussian).coerceIn(0f, 1f)

        return WaveLetterTransform(
            translationX = translationX,
            translationY = translationY,
            scale = scale,
            alpha = alpha
        )
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
 * Shared frosted glass floating letter preview bubble with optional app count caption.
 */
@Composable
private fun AlphabetPreviewBubble(
    letter: Char,
    appCount: Int?,
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
            .size(58.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .shadow(
                elevation = 16.dp,
                shape = bubbleShape,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = Color.Black.copy(alpha = 0.65f)
            )
            .clip(bubbleShape)
            .background(WaveBubbleSurface)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(WaveBubbleBorder, Color.White.copy(alpha = 0.14f))
                ),
                shape = bubbleShape
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 23.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = visuals.textPrimary,
                    shadow = Shadow(
                        color = visuals.labelShadow,
                        offset = Offset(0f, 1.5f),
                        blurRadius = 4f
                    )
                )
            )
            if (appCount != null) {
                Text(
                    text = "${appCount}个",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = visuals.accent
                    )
                )
            }
        }
    }
}

/**
 * Ergonomic one-handed right-side Niagara Alphabet Wave Scrollbar.
 * Preserves the exact golden thumb-zone vertical span while adding retractable
 * spring wave expansion, neighbor fan-out, and tap-to-lock / tap-again-to-unlock.
 */
@Composable
fun AlphabetWaveBar(
    modifier: Modifier = Modifier,
    isRightHanded: Boolean = true,
    hapticFeedbackEnabled: Boolean = true,
    availableLetters: Set<Char> = emptySet(),
    activeLetter: Char? = null,
    externalFocusedLetter: Char? = null,
    lockedLetter: Char? = null,
    letterCounts: Map<Char, Int> = emptyMap(),
    onLetterSelected: (Char) -> Unit,
    onLetterLocked: ((Char?) -> Unit)? = null,
    onScrubStateChanged: ((Boolean) -> Unit)? = null
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

    val latestAvailableLetters by rememberUpdatedState(availableLetters)
    val latestLockedLetter by rememberUpdatedState(lockedLetter)
    val latestOnLetterSelected by rememberUpdatedState(onLetterSelected)
    val latestOnLetterLocked by rememberUpdatedState(onLetterLocked)
    val latestOnScrubStateChanged by rememberUpdatedState(onScrubStateChanged)
    val latestTriggerHaptic by rememberUpdatedState(triggerHapticTick)

    LaunchedEffect(isTouching) {
        latestOnScrubStateChanged?.invoke(isTouching)
        if (isTouching) {
            waveFactor.animateTo(1f, LauncherMotion.WaveExpandSpring)
        } else {
            waveFactor.animateTo(0f, LauncherMotion.WaveReboundSpring)
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
        val sigmaPx = (itemHeightPx * 3.2f).coerceAtLeast(22f)
        val maxDisplacementPx = with(density) { 44.dp.toPx() }
        val maxSpreadYPx = with(density) { 10.dp.toPx() }
        val bubbleSizePx = with(density) { 58.dp.toPx() }
        val letterFontSp = if (itemHeightPx < with(density) { 12.5.dp.toPx() }) 9.sp else 9.5.sp

        val focusedIndex = if (isTouching) {
            AlphabetWaveMath.yToLetterIndex(touchY, band.bandHeightPx, ALPHABET_CHARS.size)
        } else {
            null
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

                        val initialLocked = latestLockedLetter
                        val downIdx = AlphabetWaveMath.yToLetterIndex(
                            down.position.y,
                            band.bandHeightPx,
                            ALPHABET_CHARS.size
                        )
                        val downRaw = downIdx?.let { ALPHABET_CHARS[it] }
                        val downResolved = downRaw?.let { raw ->
                            HomeListModel.resolveTargetLetter(raw, latestAvailableLetters) ?: raw
                        }

                        var didMoveAcrossLetters = false
                        var lastRaw = downRaw
                        var lastResolved = downResolved

                        isTouching = true
                        touchY = down.position.y
                        currentFocusedLetter = downRaw

                        if (downRaw != null && !(downResolved == initialLocked && initialLocked != null)) {
                            latestTriggerHaptic()
                            latestOnLetterSelected(downRaw)
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                                ?: event.changes.firstOrNull()
                            if (change == null || !change.pressed) {
                                break
                            }
                            change.consume()
                            touchY = change.position.y
                            val nextIdx = AlphabetWaveMath.yToLetterIndex(
                                touchY,
                                band.bandHeightPx,
                                ALPHABET_CHARS.size
                            )
                            val nextRaw = nextIdx?.let { ALPHABET_CHARS[it] }
                            if (nextRaw != null && nextRaw != lastRaw) {
                                didMoveAcrossLetters = true
                                lastRaw = nextRaw
                                lastResolved = HomeListModel.resolveTargetLetter(nextRaw, latestAvailableLetters)
                                    ?: nextRaw
                                currentFocusedLetter = nextRaw
                                latestTriggerHaptic()
                                latestOnLetterSelected(nextRaw)
                            }
                        }

                        isTouching = false

                        val shouldToggleUnlock = !didMoveAcrossLetters &&
                            downResolved != null &&
                            downResolved == initialLocked
                        if (shouldToggleUnlock) {
                            latestTriggerHaptic()
                            latestOnLetterLocked?.invoke(null)
                        } else if (lastResolved != null) {
                            latestOnLetterLocked?.invoke(lastResolved)
                        }
                    }
                },
            contentAlignment = if (isRightHanded) Alignment.TopEnd else Alignment.TopStart
        ) {
            // Subtle slim frosted hairline pill track behind the compact alphabet list
            Box(
                modifier = Modifier
                    .align(if (isRightHanded) Alignment.CenterEnd else Alignment.CenterStart)
                    .offset(x = if (isRightHanded) (-5).dp else 5.dp)
                    .width(18.dp)
                    .height(bandHeightDp + 10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(trackColor)
                    .border(0.5.dp, WaveTrackBorder, RoundedCornerShape(50))
            )

            // 27 compact letters with draw-phase retractable Gaussian distortion & neighbor fan-out
            Box(modifier = Modifier.fillMaxHeight()) {
                ALPHABET_CHARS.forEachIndexed { index, letter ->
                    val letterCenterY = (index + 0.5f) * itemHeightPx
                    val isFocused = index == focusedIndex
                    val isLocked = letter == lockedLetter
                    val isExternallyFocused = !isTouching && letter == externalFocusedLetter
                    val isSectionActive = !isTouching && !isExternallyFocused && !isLocked && letter == activeLetter
                    val isAvailable = availableLetters.isEmpty() || availableLetters.contains(letter)
                    val isHighlighted = isExternallyFocused || isLocked || isSectionActive

                    val baseAlpha = when {
                        isExternallyFocused || isLocked -> 1f
                        isSectionActive -> 0.94f
                        isAvailable -> 0.68f
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
                            .size(width = 20.dp, height = with(density) { itemHeightPx.toDp() })
                            .graphicsLayer {
                                val transform = AlphabetWaveMath.calculateWaveTransform(
                                    letterCenterY = letterCenterY,
                                    touchY = touchY,
                                    sigmaPx = sigmaPx,
                                    maxDisplacementX = maxDisplacementPx,
                                    maxSpreadY = maxSpreadYPx,
                                    waveFactor = waveFactor.value,
                                    isRightHanded = isRightHanded,
                                    baseAlpha = baseAlpha,
                                    isHighlighted = isHighlighted
                                )
                                translationX = transform.translationX
                                translationY = transform.translationY
                                scaleX = transform.scale
                                scaleY = transform.scale
                                alpha = transform.alpha
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLocked && !isTouching) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(visuals.accent.copy(alpha = 0.28f))
                                    .border(0.6.dp, visuals.accent.copy(alpha = 0.78f), CircleShape)
                            )
                        }
                        Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = letterFontSp,
                                lineHeight = letterFontSp,
                                fontWeight = when {
                                    isFocused || isLocked || isExternallyFocused || isSectionActive -> FontWeight.ExtraBold
                                    isAvailable -> FontWeight.SemiBold
                                    else -> FontWeight.Normal
                                },
                                color = when {
                                    isFocused || isLocked || isExternallyFocused -> visuals.accent
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
                val resolvedBubbleLetter = HomeListModel.resolveTargetLetter(displayLetter, availableLetters)
                    ?: displayLetter
                val bubbleOffsetY = AlphabetWaveMath.calculateBubbleOffsetY(
                    touchY = touchY,
                    totalHeightPx = band.bandHeightPx,
                    bubbleSizePx = bubbleSizePx
                )
                val bubbleOffsetX = with(density) { if (isRightHanded) (-96).dp.toPx() else 72.dp.toPx() }
                AlphabetPreviewBubble(
                    letter = resolvedBubbleLetter,
                    appCount = if (letterCounts.isNotEmpty()) letterCounts[resolvedBubbleLetter] ?: 0 else null,
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
    availableLetters: Set<Char> = emptySet(),
    letterCounts: Map<Char, Int> = emptyMap(),
    onLetterSelected: (Char) -> Unit,
    onLetterLocked: ((Char?) -> Unit)? = null,
    onFocusedLetterChanged: (Char?) -> Unit = {},
    onScrubStateChanged: ((Boolean) -> Unit)? = null
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

    val latestAvailableLetters by rememberUpdatedState(availableLetters)
    val latestOnLetterSelected by rememberUpdatedState(onLetterSelected)
    val latestOnLetterLocked by rememberUpdatedState(onLetterLocked)
    val latestOnFocusedLetterChanged by rememberUpdatedState(onFocusedLetterChanged)
    val latestOnScrubStateChanged by rememberUpdatedState(onScrubStateChanged)
    val latestTriggerHaptic by rememberUpdatedState(triggerHapticTick)

    LaunchedEffect(isScrubbing) {
        latestOnScrubStateChanged?.invoke(isScrubbing)
        if (!isScrubbing) {
            currentFocusedLetter = null
            latestOnFocusedLetterChanged(null)
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
        val bubbleSizePx = with(density) { 58.dp.toPx() }
        val lockSlopPx = with(density) { 7.dp.toPx() }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, band.topOffsetPx.roundToInt()) }
                .height(bandHeightDp)
                .width(34.dp)
                .pointerInput(band.bandHeightPx, lockSlopPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        var intent = EdgeGestureIntent.UNDECIDED
                        var lastRaw: Char? = null
                        var lastResolved: Char? = null

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
                                        val idx = AlphabetWaveMath.yToLetterIndex(
                                            touchY,
                                            band.bandHeightPx,
                                            ALPHABET_CHARS.size
                                        )
                                        val raw = idx?.let { ALPHABET_CHARS[it] }
                                        if (raw != null) {
                                            lastRaw = raw
                                            lastResolved = HomeListModel.resolveTargetLetter(raw, latestAvailableLetters)
                                                ?: raw
                                            currentFocusedLetter = raw
                                            latestOnFocusedLetterChanged(raw)
                                            latestTriggerHaptic()
                                            latestOnLetterSelected(raw)
                                        }
                                    }
                                    EdgeGestureIntent.UNDECIDED -> Unit
                                }
                            } else if (intent == EdgeGestureIntent.VERTICAL_ALPHABET) {
                                change.consume()
                                touchY = change.position.y
                                val idx = AlphabetWaveMath.yToLetterIndex(
                                    touchY,
                                    band.bandHeightPx,
                                    ALPHABET_CHARS.size
                                )
                                val raw = idx?.let { ALPHABET_CHARS[it] }
                                if (raw != null && raw != lastRaw) {
                                    lastRaw = raw
                                    lastResolved = HomeListModel.resolveTargetLetter(raw, latestAvailableLetters)
                                        ?: raw
                                    currentFocusedLetter = raw
                                    latestOnFocusedLetterChanged(raw)
                                    latestTriggerHaptic()
                                    latestOnLetterSelected(raw)
                                }
                            }
                        }

                        if (isScrubbing) {
                            isScrubbing = false
                            if (lastResolved != null) {
                                latestOnLetterLocked?.invoke(lastResolved)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.TopStart
        ) {
            // Visually transparent edge strip; only the floating preview bubble appears while scrubbing
            val displayLetter = currentFocusedLetter
            if (bubbleAlpha > 0.01f && displayLetter != null) {
                val resolvedBubbleLetter = HomeListModel.resolveTargetLetter(displayLetter, availableLetters)
                    ?: displayLetter
                val bubbleOffsetY = AlphabetWaveMath.calculateBubbleOffsetY(
                    touchY = touchY,
                    totalHeightPx = band.bandHeightPx,
                    bubbleSizePx = bubbleSizePx
                )
                val bubbleOffsetX = with(density) { 58.dp.toPx() }
                AlphabetPreviewBubble(
                    letter = resolvedBubbleLetter,
                    appCount = if (letterCounts.isNotEmpty()) letterCounts[resolvedBubbleLetter] ?: 0 else null,
                    offsetX = bubbleOffsetX,
                    offsetY = bubbleOffsetY,
                    scale = bubbleScale,
                    alpha = bubbleAlpha
                )
            }
        }
    }
}
