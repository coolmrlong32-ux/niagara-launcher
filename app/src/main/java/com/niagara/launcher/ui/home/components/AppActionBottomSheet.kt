package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.model.AppItem

/**
 * Modal Bottom Sheet appearing on app item long-press.
 * Offers options to Favorite, Reorder Favorites, Hide/Unhide, view App Info, or Uninstall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionBottomSheet(
    app: AppItem,
    canMoveFavoriteUp: Boolean = false,
    canMoveFavoriteDown: Boolean = false,
    onDismissRequest: () -> Unit,
    onToggleFavorite: (AppItem) -> Unit,
    onMoveFavoriteUp: (AppItem) -> Unit = {},
    onMoveFavoriteDown: (AppItem) -> Unit = {},
    onToggleHidden: (AppItem) -> Unit,
    onOpenDetails: (AppItem) -> Unit,
    onUninstall: (AppItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF1E1F24),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // App header: icon + name + package
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                AppIcon(app = app, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action: Favorite / Unfavorite
            ActionRowItem(
                icon = if (app.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                title = if (app.isFavorite) "取消收藏" else "设为收藏",
                tint = if (app.isFavorite) Color(0xFFFFD54F) else Color.White,
                onClick = {
                    onToggleFavorite(app)
                    onDismissRequest()
                }
            )

            // Action: Reorder within Favorites
            if (app.isFavorite && !app.isHidden && (canMoveFavoriteUp || canMoveFavoriteDown)) {
                if (canMoveFavoriteUp) {
                    ActionRowItem(
                        icon = Icons.Outlined.KeyboardArrowUp,
                        title = "在常用中上移",
                        onClick = {
                            onMoveFavoriteUp(app)
                            onDismissRequest()
                        }
                    )
                }
                if (canMoveFavoriteDown) {
                    ActionRowItem(
                        icon = Icons.Outlined.KeyboardArrowDown,
                        title = "在常用中下移",
                        onClick = {
                            onMoveFavoriteDown(app)
                            onDismissRequest()
                        }
                    )
                }
            }

            // Action: Hide / Unhide App
            ActionRowItem(
                icon = if (app.isHidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                title = if (app.isHidden) "取消隐藏" else "隐藏应用",
                onClick = {
                    onToggleHidden(app)
                    onDismissRequest()
                }
            )

            // Action: App Info (Settings)
            ActionRowItem(
                icon = Icons.Outlined.Info,
                title = "应用详情",
                onClick = {
                    onOpenDetails(app)
                    onDismissRequest()
                }
            )

            // Action: Uninstall
            ActionRowItem(
                icon = Icons.Outlined.Delete,
                title = "卸载应用",
                tint = Color(0xFFFF6B6B),
                onClick = {
                    onUninstall(app)
                    onDismissRequest()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionRowItem(
    icon: ImageVector,
    title: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = tint,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
