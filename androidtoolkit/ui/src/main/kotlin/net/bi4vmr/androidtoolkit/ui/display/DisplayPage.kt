package net.bi4vmr.androidtoolkit.ui.display

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.Res
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.common.icon.Icons
import net.bi4vmr.androidtoolkit.ui.component.*
import net.bi4vmr.androidtoolkit.ui.displayselectpanel_title
import net.bi4vmr.tool.kotlin.external.adb.common.model.*
import org.jetbrains.compose.resources.stringResource

/**
 * 显示管理页面。
 */
@Composable
fun DisplayPage(globalStateManager: GlobalStateManager) {
    val viewModel: DisplayPageViewModel = viewModel { DisplayPageViewModel(globalStateManager) }
    val displays: List<DisplayInfo> by viewModel.displaysFlow.collectAsStateWithLifecycle()
    val selectedDisplay: DisplayInfo? by viewModel.selectedDisplayFlow.collectAsStateWithLifecycle()

    // 根容器
    Row(modifier = Modifier.fillMaxSize()) {
        // 左栏：设备与屏幕列表
        Column(Modifier.fillMaxHeight().weight(0.4F)) {
            // 设备选择面板
            DeviceSelectPanel(
                globalStateManager = globalStateManager,
                modifier = Modifier.weight(0.5F)
            )

            HorizontalDivider()

            // 屏幕选择面板
            DisplaySelectPanel(
                viewModel = viewModel,
                displays = displays,
                selectedDisplay = selectedDisplay,
                onSelect = { viewModel.selectDisplay(it) },
                modifier = Modifier.weight(0.5F)
            )
        }

        VerticalDivider()

        // 右栏：屏幕详情
        val currentDisplay = selectedDisplay
        if (currentDisplay == null) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                Text("请选择一个显示屏", style = MaterialTheme.typography.bodyLarge)
            }
            return
        }

        Column(Modifier.fillMaxHeight().weight(0.6F)) {
            DisplayDetailPanel(
                viewModel = viewModel,
                display = currentDisplay,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
private fun DisplaySelectPanel(
    viewModel: DisplayPageViewModel,
    displays: List<DisplayInfo>,
    selectedDisplay: DisplayInfo?,
    onSelect: (DisplayInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space),
        modifier = modifier.padding(Dimens.Layout.Padding)
    ) {
        // 标题栏
        Row {
            Text(
                text = stringResource(Res.string.displayselectpanel_title),
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(Modifier.weight(1F))

            // 刷新按钮
            IconButton(
                modifier = Modifier.size(Dimens.Icon.Normal),
                onClick = { viewModel.refreshDisplays() }
            ) {
                Icon(Icons.Refresh, null, Modifier.size(Dimens.Icon.Normal))
            }
        }


        if (displays.isEmpty()) {
            Box(Modifier.weight(1F).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("请先选择设备")
            }
            return@Column
        }

        Box(Modifier.weight(1F).fillMaxWidth()) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(displays, key = { it.id }) { display ->
                    DisplayListItem(
                        display = display,
                        selected = (display == selectedDisplay),
                        onClick = onSelect
                    )
                }
            }
        }
    }
}


/**
 * 屏幕列表表项。
 */
@Composable
private fun DisplayListItem(
    display: DisplayInfo,
    selected: Boolean,
    onClick: (display: DisplayInfo) -> Unit
) {
    val shape = CardDefaults.shape
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable { onClick(display) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimens.Layout.Space)
        ) {
            val icon = if (display.isMirrorDevice()) Icons.ScreenCast else Icons.Screen
            Icon(icon, null, modifier = Modifier.size(Dimens.Icon.Small))
            Spacer(Modifier.width(Dimens.Layout.Space))
            Text("# ${display.id}")
            Spacer(Modifier.weight(1F))
            Text("${display.width} × ${display.height}")
        }
    }
}


/**
 * 屏幕详情面板。
 */
@Composable
private fun DisplayDetailPanel(
    viewModel: DisplayPageViewModel,
    display: DisplayInfo,
    modifier: Modifier = Modifier
) {
    var tabs: Tabs by remember { mutableStateOf(Tabs.BASE) }

    Column(modifier.padding(Dimens.Layout.Space)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedTabRow {
                Tabs.entries.forEach { tab ->
                    SegmentedTab(
                        selected = (tab == tabs),
                        onClick = { tabs = tab }
                    ) {
                        Text(text = stringResource(tab.titleRes), maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.weight(1F))

            XOutlinedButton(onClick = { viewModel.startCast() }) {
                Text("远程控制")
            }
        }

        Spacer(Modifier.width(Dimens.Layout.Space))

        when (tabs) {
            Tabs.BASE -> BaseInfo(viewModel, display)

            Tabs.ACTIVITIES -> ActivityInfo(viewModel)

            Tabs.WINDOWS -> WindowInfo(viewModel)
        }
    }
}


/**
 * 基本信息面板。
 */
@Composable
private fun BaseInfo(
    viewModel: DisplayPageViewModel,
    display: DisplayInfo,
    modifier: Modifier = Modifier
) {
    val toastManager: ToastManager = viewModel.globalStateManager.toastManager

    val size: DisplaySize? by viewModel.sizeInfoFlow.collectAsStateWithLifecycle()
    val density: DisplayDensity? by viewModel.densityInfoFlow.collectAsStateWithLifecycle()

    var widthInput by remember(size) { mutableStateOf(size?.getWidth()?.toString() ?: "") }
    var widthInvalid: Boolean by remember(size) { mutableStateOf(false) }
    var heightInput by remember(size) { mutableStateOf(size?.getHeight()?.toString() ?: "") }
    var heightInvalid: Boolean by remember(size) { mutableStateOf(false) }
    var densityInput by remember(density) { mutableStateOf(density?.getDensity()?.toString() ?: "") }
    var densityInvalid: Boolean by remember(density) { mutableStateOf(false) }

    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoRow("屏幕 ID", display.id)
        InfoRow("物理 ID", display.uniqueID.ifEmpty { "-" })
        InfoRow("分辨率", "${display.width} x ${display.height}")
        InfoRow("所属设备", display.sn.ifEmpty { "-" })

        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space)) {
            /* 分辨率 */
            val currentSize = size
            if (currentSize == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("无法获取分辨率信息", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Row {
                    Text("分辨率")
                    Spacer(Modifier.weight(1F))
                    Text("${currentSize.getWidth()} x ${currentSize.getHeight()} | ")
                    Text(if (currentSize.hasOverride()) "用户设置" else "系统默认")
                }
                Row {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { widthInput = it.filter(Char::isDigit) },
                        label = { Text("宽度") },
                        singleLine = true,
                        isError = widthInvalid,
                        modifier = Modifier.width(120.dp)
                    )
                    Text(" x ", Modifier.padding(horizontal = Dimens.Layout.SpaceSmall))
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it.filter(Char::isDigit) },
                        label = { Text("高度") },
                        singleLine = true,
                        isError = heightInvalid,
                        modifier = Modifier.width(120.dp)
                    )

                    XOutlinedButton(
                        onClick = {
                            val widthNumber = widthInput.toIntOrNull()
                            if (widthNumber == null) {
                                widthInvalid = true
                                toastManager.show("尺寸必须为整数！", ToastType.ERROR)
                                return@XOutlinedButton
                            }

                            val heightNumber = heightInput.toIntOrNull()
                            if (heightNumber == null) {
                                heightInvalid = true
                                toastManager.show("尺寸必须为整数！", ToastType.ERROR)
                                return@XOutlinedButton
                            }

                            viewModel.setDisplaySize(widthNumber, heightNumber) { error ->
                                if (error != null) {
                                    toastManager.show("修改失败！\n${error.message}", ToastType.ERROR)
                                } else {
                                    toastManager.show("修改成功！", ToastType.SUCCESS)
                                }
                            }
                        },
                        enabled = widthInput != currentSize.getWidth().toString() ||
                                heightInput != currentSize.getHeight().toString()
                    ) {
                        Text("修改")
                    }
                    Spacer(Modifier.width(Dimens.Layout.Space))
                    XOutlinedButton(
                        onClick = {
                            viewModel.resetDisplaySize { error ->
                                if (error != null) {
                                    toastManager.show("还原失败！\n${error.message}", ToastType.ERROR)
                                } else {
                                    toastManager.show("还原成功！", ToastType.SUCCESS)
                                }
                            }
                        },
                        enabled = currentSize.hasOverride(),
                    ) {
                        Text("还原")
                    }
                }
            }

            /* 像素密度 */
            val currentDensity = density
            if (currentDensity == null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("无法获取分辨率信息", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Row {
                    Text("像素密度")
                    Spacer(Modifier.weight(1F))
                    Text("${currentDensity.getDensity()} | ")
                    Text(if (currentDensity.hasOverride()) "用户设置" else "系统默认")
                }

                Row {
                    OutlinedTextField(
                        value = densityInput,
                        onValueChange = { densityInput = it.filter(Char::isDigit) },
                        label = { Text("密度") },
                        singleLine = true,
                        isError = densityInvalid,
                        modifier = Modifier.width(120.dp)
                    )

                    XOutlinedButton(
                        onClick = {
                            val densityNumber = densityInput.toIntOrNull()
                            if (densityNumber == null) {
                                densityInvalid = true
                                toastManager.show("密度必须为整数！", ToastType.ERROR)
                                return@XOutlinedButton
                            }

                            viewModel.setDisplayDensity(densityNumber) { error ->
                                if (error != null) {
                                    toastManager.show("修改失败！\n${error.message}", ToastType.ERROR)
                                } else {
                                    toastManager.show("修改成功！", ToastType.SUCCESS)
                                }
                            }
                        },
                        enabled = densityInput != currentDensity.getDensity().toString()
                    ) {
                        Text("修改")
                    }
                    Spacer(Modifier.width(Dimens.Layout.Space))
                    XOutlinedButton(
                        onClick = {
                            viewModel.resetDisplayDensity { error ->
                                if (error != null) {
                                    toastManager.show("还原失败！\n${error.message}", ToastType.ERROR)
                                } else {
                                    toastManager.show("还原成功！", ToastType.SUCCESS)
                                }
                            }
                        },
                        enabled = currentDensity.hasOverride(),
                    ) {
                        Text("还原")
                    }
                }
            }
        }
    }
}

/**
 * Activity 信息面板。
 */
@Composable
private fun ActivityInfo(
    viewModel: DisplayPageViewModel
) {
    val list = viewModel.activitiesFlow.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(list.value, key = { it.componentName }) { activity ->
            ActivityListItem(activity)
        }
    }
}


@Composable
private fun ActivityListItem(activity: ActivityInfo) {
    ComponentCard(
        displayID = activity.displayID,
        title = activity.componentName,
        subtitle = activity.taskAffinity,
        extra = "Task ${activity.taskID}  ·  User ${activity.userID}  ·  ${if (activity.resumed) "Resumed" else ""}"
    )
}

/**
 * Window 信息面板。
 */
@Composable
private fun WindowInfo(
    viewModel: DisplayPageViewModel
) {
    val list = viewModel.windowsFlow.collectAsStateWithLifecycle()
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        items(list.value, key = { it.hash }) { window ->
            WindowListItem(window)
        }
    }
}

@Composable
private fun WindowListItem(window: WindowInfo) {
    ComponentCard(
        displayID = window.displayID,
        title = window.title,
        subtitle = window.packageName.ifEmpty { "-" },
        extra = "${if (window.visible) "可见" else "不可见"}  ·  User ${window.userID}  ·  ${window.hash}"
    )
}

@Composable
private fun ComponentCard(displayID: String, title: String, subtitle: String, extra: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimens.Layout.Padding)
        ) {
            Text(
                "#${displayID.ifEmpty { "?" }}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(40.dp)
            )
            Column(Modifier.weight(1F)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(Dimens.Layout.Space))
            Text(
                extra,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
