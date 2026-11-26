package net.bi4vmr.androidtoolkit.ui.generic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.runBlocking
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.LocalGlobalStateManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.common.icon.Icons
import net.bi4vmr.androidtoolkit.ui.component.DeviceSelectPanel
import net.bi4vmr.androidtoolkit.ui.component.InfoRow
import net.bi4vmr.androidtoolkit.ui.component.ToastManager
import net.bi4vmr.androidtoolkit.ui.component.XOutlinedButton
import net.bi4vmr.tool.kotlin.external.adb.common.enums.AndroidVersion
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice

@Composable
fun GenericPage() {
    val globalStateManager: GlobalStateManager = LocalGlobalStateManager.current
    val logicManager: LogicManager = globalStateManager.logicManager

    val viewModel: GenericPageViewModel = viewModel { GenericPageViewModel(globalStateManager) }

    val selectedDevice: ADBDevice? by logicManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize()) {
        DeviceSelectPanel(
            globalStateManager = globalStateManager,
            modifier = Modifier.fillMaxHeight().weight(0.4F)
        )

        VerticalDivider()

        DevicePanel(
            viewModel = viewModel,
            globalStateManager = globalStateManager,
            device = selectedDevice,
            modifier = Modifier.fillMaxHeight().weight(0.6F)
        )
    }
}


@Composable
private fun DevicePanel(
    viewModel: GenericPageViewModel,
    globalStateManager: GlobalStateManager,
    device: ADBDevice?, modifier: Modifier = Modifier
) {
    val logicManager: LogicManager = globalStateManager.logicManager
    val toastManager: ToastManager = globalStateManager.toastManager

    if (device == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("请在左侧选择一台设备", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val enabled = device.getState() == DeviceState.ONLINE

    // 品牌
    val brand by produceState(initialValue = "-", device) {
        value = viewModel.getSelectedExecutor().getBrand(device.getSN())
            .ifBlank { "-" }
    }
    // 型号
    val model by produceState(initialValue = "-", device) {
        value = viewModel.getSelectedExecutor().getModel(device.getSN())
            .ifBlank { "-" }
    }
    // 系统版本
    val osVersion by produceState(initialValue = AndroidVersion.UNKNOWN, device) {
        val rawValue = viewModel.getSelectedExecutor().getOSVersion(device.getSN())
        value = AndroidVersion.fromVersion(rawValue)
    }

    // 焦点信息
    val focusInfo: FocusInfo by viewModel.focusInfoFlow.collectAsStateWithLifecycle()
    // 焦点信息刷新状态
    val focusInfoRefreshState: Boolean by viewModel.focusInfoRefreshStateFlow.collectAsStateWithLifecycle()
    // 焦点信息是否自动刷新
    var focusInfoAutoRefresh: Boolean by remember { mutableStateOf(viewModel.isFocusInfoAutoRefresh()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(Dimens.Layout.Padding)
    ) {
        Text("基本信息")
        InfoRow("序列号", device.getSN())
        InfoRow("状态", device.getState().name)
        InfoRow("品牌", brand)
        InfoRow("型号", model)
        InfoRow("系统版本", osVersion.standardName)

        HorizontalDivider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("焦点信息", modifier = Modifier.weight(1F))

            Checkbox(
                checked = focusInfoAutoRefresh,
                onCheckedChange = {
                    focusInfoAutoRefresh = it
                    viewModel.setFocusInfoAutoRefresh(it)
                },
                modifier = Modifier.size(Dimens.Icon.Normal)
            )
            Text("自动刷新")

            // 手动刷新按钮或加载中动画
            if (focusInfoRefreshState) {
                CircularProgressIndicator(modifier = Modifier.size(Dimens.Icon.Normal))
            } else {
                IconButton(
                    modifier = Modifier.size(Dimens.Icon.Normal),
                    onClick = { viewModel.refreshFocusInfo() }
                ) {
                    Icon(
                        imageVector = Icons.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.Icon.Normal)
                    )
                }
            }
        }

        Text("屏幕：# ${focusInfo.displayID}")
        Text("应用：${focusInfo.activityPackage}")
        Text("Activity：${focusInfo.activityClass}")
        Text("Window：${focusInfo.windowName}")

        HorizontalDivider()

        Spacer(modifier = Modifier.weight(1F))

        Text("常用操作")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XOutlinedButton(
                onClick = {
                    logicManager.selectedExecutorFlow.value.openShell(device.getSN())
                },
                enabled = enabled
            ) { Text("打开 Shell") }

            XOutlinedButton(
                onClick = {
                    if (device is LocalDevice) {
                        val state = device.startRemoteControl()
                        toastManager.show("启动远程调试 -> $state")
                    }
                },
                enabled = enabled
            ) { Text("开启远程调试") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XOutlinedButton(
                onClick = {
                    runBlocking {
                        viewModel.getSelectedExecutor().root(device.getSN())
                    }
                },
                enabled = enabled
            ) { Text("Root") }

            XOutlinedButton(
                onClick = {
                    runBlocking {
                        viewModel.getSelectedExecutor().reboot(device.getSN())
                    }
                },
                enabled = enabled
            ) { Text("重启") }
        }
    }
}
