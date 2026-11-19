package net.bi4vmr.androidtoolkit.ui.display

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import net.bi4vmr.androidtoolkit.ui.component.DeviceSelectPanel
import net.bi4vmr.androidtoolkit.ui.component.InfoRow
import net.bi4vmr.androidtoolkit.ui.component.SegmentedTab
import net.bi4vmr.androidtoolkit.ui.component.SegmentedTabRow
import net.bi4vmr.androidtoolkit.ui.component.ToastManager
import net.bi4vmr.androidtoolkit.ui.component.ToastType
import net.bi4vmr.androidtoolkit.ui.component.XOutlinedButton
import net.bi4vmr.androidtoolkit.ui.displayselectpanel_title
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.WindowInfo
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
        if (selectedDisplay == null) {
            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                Text("请选择一个显示屏", style = MaterialTheme.typography.bodyLarge)
            }
            return
        }

        Column(Modifier.fillMaxHeight().weight(0.6F)) {
            DisplayDetailPanel(
                viewModel = viewModel,
                display = selectedDisplay,
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
    println(display)
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
            Icon(Icons.Screen, null, modifier = Modifier.size(Dimens.Icon.Small))
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
    display: DisplayInfo?,
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
            Tabs.BASE -> {
                BaseInfo(viewModel, display)
            }

            Tabs.ACTIVITIES -> {}
            Tabs.WINDOWS -> {}
        }
    }
}


/**
 * 基本信息面板。
 */
@Composable
private fun BaseInfo(
    viewModel: DisplayPageViewModel,
    display: DisplayInfo?,
    modifier: Modifier = Modifier
) {
    if (display == null) {
        Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Text("请选择一个显示屏", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Display ${display.id}", style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider()
        InfoRow("屏幕 ID", display.id)
        InfoRow("物理 ID", display.uniqueID.ifEmpty { "-" })
        InfoRow("分辨率", "${display.width} x ${display.height}")
        InfoRow("所属设备", display.sn.ifEmpty { "-" })
    }
}


/**
 * 分辨率与密度编辑器。
 */
@Composable
private fun DisplayMetricsEditor(
    metrics: DisplayMetrics,
    onSetSize: (width: Int, height: Int) -> Unit,
    onResetSize: () -> Unit,
    onSetDensity: (Int) -> Unit,
    onResetDensity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val size = metrics.size
    val density = metrics.density
    var widthText by remember(size) { mutableStateOf(size?.getWidth()?.toString() ?: "") }
    var heightText by remember(size) { mutableStateOf(size?.getHeight()?.toString() ?: "") }
    var densityText by remember(density) { mutableStateOf(density?.getDensity()?.toString() ?: "") }

    val width = widthText.toIntOrNull()?.takeIf { it > 0 }
    val height = heightText.toIntOrNull()?.takeIf { it > 0 }
    val densityValue = densityText.toIntOrNull()?.takeIf { it > 0 }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space)) {
        // 分辨率
        InfoRow(
            "逻辑分辨率",
            when {
                size == null -> "-"
                size.hasOverride() -> "${size.overrideWidth} x ${size.overrideHeight}" +
                        "（物理 ${size.physicalWidth} x ${size.physicalHeight}）"

                else -> "${size.physicalWidth} x ${size.physicalHeight}"
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = widthText,
                onValueChange = { widthText = it.filter(Char::isDigit) },
                label = { Text("宽度") },
                singleLine = true,
                modifier = Modifier.width(120.dp)
            )
            Text(" x ", Modifier.padding(horizontal = Dimens.Layout.SpaceSmall))
            OutlinedTextField(
                value = heightText,
                onValueChange = { heightText = it.filter(Char::isDigit) },
                label = { Text("高度") },
                singleLine = true,
                modifier = Modifier.width(120.dp)
            )
            Spacer(Modifier.width(Dimens.Layout.Space))
            Button(
                onClick = { if (width != null && height != null) onSetSize(width, height) },
                enabled = (size != null && width != null && height != null)
            ) {
                Text("修改")
            }
            Spacer(Modifier.width(Dimens.Layout.Space))
            OutlinedButton(onClick = onResetSize, enabled = (size != null)) {
                Text("还原")
            }
        }

        // 密度
        InfoRow(
            "像素密度",
            when {
                density == null -> "-"
                density.hasOverride() -> "${density.override} dpi（物理 ${density.physical} dpi）"
                else -> "${density.physical} dpi"
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = densityText,
                onValueChange = { densityText = it.filter(Char::isDigit) },
                label = { Text("密度") },
                singleLine = true,
                modifier = Modifier.width(120.dp)
            )
            Spacer(Modifier.width(Dimens.Layout.Space))
            Button(
                onClick = { if (densityValue != null) onSetDensity(densityValue) },
                enabled = (density != null && densityValue != null)
            ) {
                Text("修改")
            }
            Spacer(Modifier.width(Dimens.Layout.Space))
            OutlinedButton(onClick = onResetDensity, enabled = (density != null)) {
                Text("还原")
            }
        }
    }
}


/**
 * 界面组件面板：展示 Activity 与窗口信息。
 */
@Composable
private fun ComponentPanel(
    globalStateManager: GlobalStateManager,
    viewModel: DisplayPageViewModel,
    tab: ComponentTab,
    state: ComponentsState,
    onSelectTab: (ComponentTab) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(Dimens.Layout.Padding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentedTabRow {
                ComponentTab.entries.forEach {
                    SegmentedTab(
                        selected = (it == tab),
                        onClick = { onSelectTab(it) }
                    ) {
                        Text(it.label, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.weight(1F))

            OutlinedButton(onClick = onRefresh, enabled = !state.loading) {
                Text("刷新")
            }
        }

        Spacer(Modifier.height(Dimens.Layout.Space))

        Box(Modifier.weight(1F).fillMaxWidth()) {
            val isActivityTab = (state.tab == ComponentTab.ACTIVITIES)
            val empty = if (isActivityTab) state.activities.isEmpty() else state.windows.isEmpty()
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                empty -> Text("暂无数据", Modifier.align(Alignment.Center))

                state.tab == ComponentTab.BASE -> {
                    val metrics: DisplayMetrics by viewModel.metricsFlow.collectAsStateWithLifecycle()

                    val toastManager: ToastManager = globalStateManager.toastManager
                    val showResult: (String) -> (Boolean) -> Unit = { action ->
                        { success ->
                            if (success) {
                                toastManager.show("${action}成功", ToastType.SUCCESS)
                            } else {
                                toastManager.show("${action}失败", ToastType.ERROR)
                            }
                        }
                    }

                    DisplayMetricsEditor(
                        metrics = metrics,
                        onSetSize = { w, h -> viewModel.setDisplaySize(w, h, showResult("修改分辨率")) },
                        onResetSize = { viewModel.resetDisplaySize(showResult("还原分辨率")) },
                        onSetDensity = { viewModel.setDisplayDensity(it, showResult("修改密度")) },
                        onResetDensity = { viewModel.resetDisplayDensity(showResult("还原密度")) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)
                    )
                }

                else -> SelectionContainer {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.Layout.SpaceSmall)) {
                        if (isActivityTab) {
                            items(state.activities) { ActivityListItem(it) }
                        } else {
                            items(state.windows) { WindowListItem(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityListItem(activity: ActivityInfo) {
    ComponentCard(
        displayID = activity.displayID,
        title = activity.componentName,
        subtitle = activity.taskAffinity,
        extra = "Task ${activity.taskID}  ·  User ${activity.userID}"
    )
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
