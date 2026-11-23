package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.data.LocalAppIconCache
import com.niagara.launcher.core.model.AppItem

/**
 * Standard Niagara style single-column application list item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    app: AppItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon loaded asynchronously from AppIconCache
        AppIcon(app = app, modifier = Modifier.size(42.dp))

        Spacer(modifier = Modifier.width(16.dp))

        // App title
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Favorite badge indicator
        if (app.isFavorite) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Favorite",
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AppIcon(
    app: AppItem,
    modifier: Modifier = Modifier
) {
    val iconCache = LocalAppIconCache.current
    val density = LocalDensity.current
    val targetSizePx = remember(density) { with(density) { 48.dp.roundToPx() } }

    var bitmap by remember(app.key, iconCache) {
        mutableStateOf(iconCache?.getCached(app.key))
    }

    LaunchedEffect(app.key, iconCache, targetSizePx) {
        if (bitmap == null && iconCache != null) {
            bitmap = iconCache.loadIcon(
                packageName = app.packageName,
                activityName = app.activityName,
                targetSizePx = targetSizePx
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (bitmap == null) Color.White.copy(alpha = 0.08f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { loadedBitmap ->
            Image(
                bitmap = loadedBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
