package com.niagara.launcher.ui.home.components

import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.niagara.launcher.core.theme.IosPillShape
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Immutable
data class ClockDisplaySnapshot(
    val hourText: String,
    val minuteText: String,
    val secondText: String,
    val dateText: String,
    val weekdayText: String,
    val periodLabel: String
)

/**
 * Pure Kotlin formatter producing consistent tabular clock & localized date strings.
 */
object ClockFormatHelper {
    fun format(
        epochMillis: Long,
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): ClockDisplaySnapshot {
        val date = Date(epochMillis)
        val hourFmt = SimpleDateFormat("HH", locale).apply { this.timeZone = timeZone }
        val minFmt = SimpleDateFormat("mm", locale).apply { this.timeZone = timeZone }
        val secFmt = SimpleDateFormat("ss", locale).apply { this.timeZone = timeZone }
        val datePattern = if (locale.language.equals("zh", ignoreCase = true)) "M月d日" else "MMM d"
        val dateFmt = SimpleDateFormat(datePattern, locale).apply { this.timeZone = timeZone }
        val weekdayFmt = SimpleDateFormat("EEEE", locale).apply { this.timeZone = timeZone }
        val periodFmt = SimpleDateFormat("a", locale).apply { this.timeZone = timeZone }

        return ClockDisplaySnapshot(
            hourText = hourFmt.format(date),
            minuteText = minFmt.format(date),
            secondText = secFmt.format(date),
            dateText = dateFmt.format(date),
            weekdayText = weekdayFmt.format(date),
            periodLabel = periodFmt.format(date)
        )
    }
}

enum class ClockVariant {
    HERO_HOME,
    COMPACT_DRAWER,
    WIDGET_CARD
}

@Composable
fun rememberClockSnapshot(): ClockDisplaySnapshot {
    val lifecycleOwner = LocalLifecycleOwner.current
    val locale = Locale.getDefault()
    var snapshot by remember(locale) {
        mutableStateOf(ClockFormatHelper.format(System.currentTimeMillis(), locale))
    }

    LaunchedEffect(lifecycleOwner, locale) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                snapshot = ClockFormatHelper.format(now, locale)
                val nextDelay = (1000L - (now % 1000L)).coerceIn(100L, 1000L)
                delay(nextDelay)
            }
        }
    }

    return snapshot
}

/**
 * Unified editorial clock component shared across Page 0, Page 1 ("All Apps"),
 * and the built-in Digital Clock widget card.
 */
@Composable
fun LauncherClockDisplay(
    variant: ClockVariant,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onTimeClick: (() -> Unit)? = null,
    onDateClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val visuals = LocalLauncherVisuals.current
    val clock = rememberClockSnapshot()

    val timeSizeSp = when (variant) {
        ClockVariant.HERO_HOME -> 52.sp
        ClockVariant.COMPACT_DRAWER -> 38.sp
        ClockVariant.WIDGET_CARD -> 44.sp
    }
    val dateSizeSp = when (variant) {
        ClockVariant.HERO_HOME -> 13.5.sp
        ClockVariant.COMPACT_DRAWER -> 12.5.sp
        ClockVariant.WIDGET_CARD -> 13.sp
    }
    val colonSizeSp = when (variant) {
        ClockVariant.HERO_HOME -> 42.sp
        ClockVariant.COMPACT_DRAWER -> 31.sp
        ClockVariant.WIDGET_CARD -> 36.sp
    }
    val letterSpacing = when (variant) {
        ClockVariant.HERO_HOME -> (-1.6).sp
        ClockVariant.COMPACT_DRAWER -> (-1.1).sp
        ClockVariant.WIDGET_CARD -> (-1.3).sp
    }

    val defaultTimeClick = remember(context) {
        {
            try {
                context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {
                // Ignore if alarm app unavailable
            }
        }
    }

    val defaultDateClick = remember(context) {
        {
            try {
                val calendarUri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()
                context.startActivity(Intent(Intent.ACTION_VIEW, calendarUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {
                // Ignore if calendar app unavailable
            }
        }
    }

    val timeShadow = Shadow(
        color = visuals.labelShadow,
        offset = Offset(0f, 2f),
        blurRadius = 7f
    )
    val dateShadow = Shadow(
        color = visuals.labelShadow,
        offset = Offset(0f, 1.2f),
        blurRadius = 4f
    )

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTimeClick ?: defaultTimeClick
            )
        ) {
            Text(
                text = clock.hourText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = timeSizeSp,
                    lineHeight = timeSizeSp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = letterSpacing,
                    fontFeatureSettings = "tnum",
                    color = visuals.textPrimary,
                    shadow = timeShadow
                )
            )
            Text(
                text = ":",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = colonSizeSp,
                    lineHeight = timeSizeSp,
                    fontWeight = FontWeight.Light,
                    color = visuals.clockColonTint,
                    shadow = timeShadow
                ),
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
            )
            Text(
                text = clock.minuteText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = timeSizeSp,
                    lineHeight = timeSizeSp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = letterSpacing,
                    fontFeatureSettings = "tnum",
                    color = visuals.textPrimary.copy(alpha = 0.92f),
                    shadow = timeShadow
                )
            )

            if (variant == ClockVariant.WIDGET_CARD) {
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(IosPillShape)
                            .background(visuals.pillTint)
                            .border(0.5.dp, visuals.pillBorder, IosPillShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = clock.secondText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum",
                                color = visuals.accent
                            )
                        )
                    }
                    Text(
                        text = clock.periodLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = visuals.textTertiary
                        ),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (variant == ClockVariant.HERO_HOME) 4.dp else 3.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDateClick ?: defaultDateClick
            )
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(visuals.accent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = clock.dateText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = dateSizeSp,
                    fontWeight = FontWeight.Medium,
                    color = visuals.textPrimary.copy(alpha = 0.90f),
                    shadow = dateShadow
                ),
                maxLines = 1
            )
            Text(
                text = " · ${clock.weekdayText}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = dateSizeSp,
                    fontWeight = FontWeight.Normal,
                    color = visuals.clockDateSecondary,
                    shadow = dateShadow
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = " · $subtitle",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (dateSizeSp.value - 0.5f).sp,
                        fontWeight = FontWeight.Normal,
                        color = visuals.textTertiary,
                        shadow = dateShadow
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
