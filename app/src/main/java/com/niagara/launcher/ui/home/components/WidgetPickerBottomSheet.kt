package com.niagara.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niagara.launcher.core.model.BuiltInWidgetKind
import com.niagara.launcher.core.model.HomeWidgetCodec
import com.niagara.launcher.core.model.HomeWidgetItem
import com.niagara.launcher.core.theme.IosAccentBlue
import com.niagara.launcher.core.theme.IosIconBadgeBlue
import com.niagara.launcher.core.theme.IosIconBadgeGreen
import com.niagara.launcher.core.theme.IosIconBadgeOrange
import com.niagara.launcher.core.theme.IosIconBadgePurple
import com.niagara.launcher.core.theme.IosPillShape
import com.niagara.launcher.core.theme.IosSettingsCardElevated
import com.niagara.launcher.core.theme.IosSquircleShape
import com.niagara.launcher.core.widget.SystemWidgetProviderEntry

/**
 * Dual-tab modal sheet allowing the user to add either built-in launcher cards
 * or third-party/system Android AppWidgets to Page 0.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerBottomSheet(
    currentWidgets: List<HomeWidgetItem>,
    systemWidgets: List<SystemWidgetProviderEntry>,
    isLoadingSystemWidgets: Boolean,
    onAddBuiltInWidget: (BuiltInWidgetKind) -> Unit,
    onRemoveWidget: (String) -> Unit,
    onSelectSystemWidget: (SystemWidgetProviderEntry) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val addedBuiltInKinds = remember(currentWidgets) {
        currentWidgets.filterIsInstance<HomeWidgetItem.BuiltIn>().map { it.kind }.toSet()
    }

    val filteredSystemWidgets = remember(systemWidgets, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) systemWidgets
        else systemWidgets.filter {
            it.appLabel.lowercase().contains(q) ||
                it.widgetLabel.lowercase().contains(q) ||
                it.providerPackage.lowercase().contains(q)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color(0xF7191A20),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "添加桌面插件",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "已放置 ${currentWidgets.size} / ${HomeWidgetCodec.MAX_HOME_WIDGETS} 个",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.58f),
                            fontSize = 12.sp
                        )
                    )
                }

                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "完成",
                        color = IosAccentBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            IosSegmentedControl(
                options = listOf("内置插件", "系统小部件"),
                selectedIndex = selectedTab,
                onOptionSelected = { selectedTab = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(items = BuiltInWidgetKind.entries, key = { it.storageKey }) { kind ->
                        val isAdded = addedBuiltInKinds.contains(kind)
                        val widgetId = "builtin_${kind.storageKey}"
                        val (icon, bgColor) = resolveBuiltInMeta(kind)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(0.6.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .clickable {
                                    if (isAdded) {
                                        onRemoveWidget(widgetId)
                                    } else {
                                        onAddBuiltInWidget(kind)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            IosSettingsIconBadge(
                                icon = icon,
                                backgroundColor = bgColor,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = kind.title,
                                    color = Color.White,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${kind.subtitle} · 默认 ${kind.defaultHeightDp}dp",
                                    color = Color.White.copy(alpha = 0.58f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(IosPillShape)
                                    .background(if (isAdded) IosAccentBlue.copy(alpha = 0.22f) else IosAccentBlue)
                                    .border(
                                        0.6.dp,
                                        if (isAdded) IosAccentBlue.copy(alpha = 0.7f) else Color.Transparent,
                                        IosPillShape
                                    )
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAdded) Icons.Rounded.Check else Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isAdded) "已添加" else "添加",
                                        color = Color.White,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "搜索系统或应用小部件…",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "清空",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = IosAccentBlue,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                if (isLoadingSystemWidgets) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = IosAccentBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "正在加载已安装的桌面小部件…",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.5.sp
                        )
                    }
                } else if (filteredSystemWidgets.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Text(
                            text = if (systemWidgets.isEmpty()) {
                                "未检测到可用的系统桌面小部件"
                            } else {
                                "未找到匹配「$searchQuery」的小部件"
                            },
                            color = Color.White.copy(alpha = 0.58f),
                            fontSize = 13.5.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                    ) {
                        items(
                            items = filteredSystemWidgets,
                            key = { "${it.providerPackage}/${it.providerClass}" }
                        ) { entry ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onDismissRequest()
                                        onSelectSystemWidget(entry)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 10.dp)
                            ) {
                                PackageAppIcon(
                                    packageName = entry.providerPackage,
                                    shape = IosSquircleShape(),
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.widgetLabel,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = entry.appLabel,
                                        color = Color.White.copy(alpha = 0.58f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(IosPillShape)
                                        .background(IosSettingsCardElevated)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.14f), IosPillShape)
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = entry.cellSpanLabel,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

private fun resolveBuiltInMeta(kind: BuiltInWidgetKind): Pair<ImageVector, Color> {
    return when (kind) {
        BuiltInWidgetKind.WEATHER -> Icons.Rounded.WbCloudy to IosIconBadgeBlue
        BuiltInWidgetKind.DEVICE_STATUS -> Icons.Rounded.Memory to IosIconBadgeGreen
        BuiltInWidgetKind.QUICK_SEARCH -> Icons.Rounded.Search to IosIconBadgeOrange
        BuiltInWidgetKind.DIGITAL_CLOCK -> Icons.Rounded.AccessTime to IosIconBadgePurple
    }
}
