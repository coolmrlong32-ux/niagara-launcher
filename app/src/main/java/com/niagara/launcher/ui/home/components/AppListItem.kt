package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.data.AppIconCache
import com.niagara.launcher.core.data.LocalAppIconCache
import com.niagara.launcher.core.data.LocalIconAppearance
import com.niagara.launcher.core.model.AppItem
import com.niagara.launcher.core.model.IconStyle
import com.niagara.launcher.core.theme.AccentWarmGold
import com.niagara.launcher.core.theme.IconShadowAmbient
import com.niagara.launcher.core.theme.IconShadowSpot
import com.niagara.launcher.core.theme.IconSpecularRim
import com.niagara.launcher.core.theme.IosSquircleShape
import com.niagara.launcher.core.theme.IosSubtleTileShape
import com.niagara.launcher.core.theme.LocalLauncherVisuals
import com.niagara.launcher.core.transition.LaunchBoundsRef
import com.niagara.launcher.core.transition.LaunchSourceBounds

/**
 * Resolves the Compose clipping [Shape] corresponding to a built-in [IconStyle].
 */
fun resolveIconShape(iconStyle: IconStyle): Shape {
    return when (iconStyle) {
        IconStyle.IOS_SQUIRCLE -> IosSquircleShape()
        IconStyle.CIRCLE -> CircleShape
        IconStyle.ADAPTIVE -> RectangleShape
    }
}

/**
 * Refined Niagara-style single-column application list item with crisp icon rim,
 * soft elevation, and wallpaper-adaptive text legibility.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onClickWithBounds: ((LaunchSourceBounds?) -> Unit)? = null,
    onLongClick: () -> Unit
) {
    val visuals = LocalLauncherVisuals.current
    val iconAppearance = LocalIconAppearance.current
    val iconShape = remember(iconAppearance.iconStyle) { resolveIconShape(iconAppearance.iconStyle) }
    val applyFrameChrome = iconAppearance.iconPackPackage == null && iconAppearance.iconStyle != IconStyle.ADAPTIVE
    val iconBoundsRef = remember(app.key) { LaunchBoundsRef() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 1.dp)
            .clip(IosSubtleTileShape)
            .combinedClickable(
                onClick = {
                    if (onClickWithBounds != null) {
                        onClickWithBounds(iconBoundsRef.bounds)
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = Modifier
            .size(40.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInRoot()
                iconBoundsRef.bounds = LaunchSourceBounds.fromCoordinates(
                    x = pos.x,
                    y = pos.y,
                    width = coords.size.width.toFloat(),
                    height = coords.size.height.toFloat()
                )
            }
            .then(
                if (applyFrameChrome) {
                    Modifier
                        .shadow(
                            elevation = 4.dp,
                            shape = iconShape,
                            ambientColor = IconShadowAmbient,
                            spotColor = IconShadowSpot
                        )
                        .border(
                            width = 0.5.dp,
                            color = IconSpecularRim,
                            shape = iconShape
                        )
                } else {
                    Modifier
                }
            )

        AppIcon(app = app, shape = iconShape, modifier = iconModifier)

        Spacer(modifier = Modifier.width(15.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.05.sp,
                color = visuals.textPrimary,
                shadow = Shadow(
                    color = visuals.labelShadow,
                    offset = Offset(0f, 1.2f),
                    blurRadius = 3.5f
                )
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (app.isFavorite) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = "常用应用",
                tint = AccentWarmGold.copy(alpha = 0.88f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AppIcon(
    app: AppItem,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    iconStyle: IconStyle = LocalIconAppearance.current.iconStyle,
    iconPackPackage: String? = LocalIconAppearance.current.iconPackPackage
) {
    val iconCache = LocalAppIconCache.current
    val density = LocalDensity.current
    val targetSizePx = remember(density) { with(density) { 64.dp.roundToPx() } }
    val effectiveShape = shape ?: resolveIconShape(iconStyle)
    val cacheKey = remember(app.key, iconStyle, iconPackPackage) {
        AppIconCache.buildCacheKey(
            packageName = app.packageName,
            activityName = app.activityName,
            iconStyle = iconStyle,
            iconPackPackage = iconPackPackage
        )
    }

    var bitmap by remember(app.key, iconCache) {
        mutableStateOf(iconCache?.getCached(cacheKey))
    }

    LaunchedEffect(app.key, cacheKey, iconCache, targetSizePx) {
        val cached = iconCache?.getCached(cacheKey)
        if (cached != null) {
            bitmap = cached
        } else if (iconCache != null) {
            bitmap = iconCache.loadIcon(
                packageName = app.packageName,
                activityName = app.activityName,
                targetSizePx = targetSizePx,
                iconStyle = iconStyle,
                iconPackPackage = iconPackPackage
            )
        }
    }

    Box(
        modifier = modifier
            .clip(effectiveShape)
            .background(if (bitmap == null) Color.White.copy(alpha = 0.08f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { loadedBitmap ->
            Image(
                bitmap = loadedBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Displays a third-party package's own launcher icon (used in the Icon Pack picker list).
 */
@Composable
fun PackageAppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    shape: Shape = IosSquircleShape()
) {
    val iconCache = LocalAppIconCache.current
    val density = LocalDensity.current
    val targetSizePx = remember(density) { with(density) { 48.dp.roundToPx() } }

    var bitmap by remember(packageName, iconCache) {
        mutableStateOf(iconCache?.getCachedPackageIcon(packageName))
    }

    LaunchedEffect(packageName, iconCache, targetSizePx) {
        val cached = iconCache?.getCachedPackageIcon(packageName)
        if (cached != null) {
            bitmap = cached
        } else if (iconCache != null) {
            bitmap = iconCache.loadPackageIcon(
                packageName = packageName,
                targetSizePx = targetSizePx
            )
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (bitmap == null) Color.White.copy(alpha = 0.08f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { loadedBitmap ->
            Image(
                bitmap = loadedBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
