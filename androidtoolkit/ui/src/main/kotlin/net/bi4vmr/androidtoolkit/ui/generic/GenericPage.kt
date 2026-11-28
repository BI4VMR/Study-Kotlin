package net.bi4vmr.androidtoolkit.ui.generic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import net.bi4vmr.androidtoolkit.ui.component.*
import net.bi4vmr.tool.kotlin.external.adb.common.enums.AndroidVersion
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice

/**
 * 页面：通用（主页）。
 */
@Composable
fun GenericPage() {
    val globalStateManager: GlobalStateManager = LocalGlobalStateManager.current

    val viewModel: GenericPageViewModel = viewModel { GenericPageViewModel(globalStateManager) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // 侧边栏占据整个面板的 40%，且最大不超过 350 dp。
        val panelWidth = (maxWidth * 0.4F).coerceIn(200.dp, 350.dp)

        Row(modifier = Modifier.fillMaxSize()) {
            // 左栏：设备选择器
            DeviceSelectPanel(Modifier.fillMaxHeight().width(panelWidth))

            VerticalDivider()

            // 右栏：设备信息面板
            DeviceInfoPanel(
                viewModel = viewModel,
                modifier = Modifier.fillMaxHeight().weight(1F)
            )
        }
    }
}


/**
 * 设备信息面板。
 */
@Composable
private fun DeviceInfoPanel(
    viewModel: GenericPageViewModel,
    modifier: Modifier = Modifier
) {
    val globalStateManager: GlobalStateManager = LocalGlobalStateManager.current
    val logicManager: LogicManager = globalStateManager.logicManager
    val toastManager: ToastManager = globalStateManager.toastManager

    val selectedDevice: ADBDevice? by logicManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    val device = selectedDevice
    // 占位控件：未选中设备
    if (device == null) {
        PlaceHolder(
            type = PlaceHolderType.Info,
            info = "请选择设备",
            modifier = modifier
        )
        return
    }

    // 占位控件：设备未就绪
    if (device.getState() != DeviceState.ONLINE) {
        PlaceHolder(
            type = PlaceHolderType.Warning,
            info = "设备未就绪",
            detail = "当前状态：${device.getState().name}",
            modifier = modifier
        )
        return
    }


    Column(
        modifier = modifier.padding(Dimens.Layout.Padding)
    ) {
        Column(
            modifier = Modifier
                .weight(1F)
                .verticalScroll(rememberScrollState())
        ) {
            BaseInfoPanel(viewModel)

            HorizontalDivider()

            FocusInfoPanel(
                viewModel = viewModel,
                modifier = Modifier.padding(vertical = Dimens.Layout.Space)
            )
        }

        HorizontalDivider()

        ControlPanel(viewModel)
    }
}


/**
 * 基本信息面板。
 */
@Composable
private fun BaseInfoPanel(
    viewModel: GenericPageViewModel,
    modifier: Modifier = Modifier
) {
    val globalStateManager: GlobalStateManager = LocalGlobalStateManager.current
    val logicManager: LogicManager = globalStateManager.logicManager
    val selectedDevice: ADBDevice? by logicManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    val device = selectedDevice ?: return

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

    Column {
        Text("基本信息")
        InfoRow("序号：", device.getSN())
        InfoRow("品牌：", brand)
        InfoRow("型号：", model)
        InfoRow("系统：", osVersion.standardName)
    }
}


/**
 * 焦点信息面板。
 */
@Composable
private fun FocusInfoPanel(
    viewModel: GenericPageViewModel,
    modifier: Modifier = Modifier
) {
    // 焦点信息
    val focusInfo: FocusInfo by viewModel.focusInfoFlow.collectAsStateWithLifecycle()
    // 焦点信息刷新状态
    val focusInfoRefreshState: Boolean by viewModel.focusInfoRefreshStateFlow.collectAsStateWithLifecycle()
    // 焦点信息是否自动刷新
    var focusInfoAutoRefresh: Boolean by remember { mutableStateOf(viewModel.isFocusInfoAutoRefresh()) }

    Column(modifier = modifier) {
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
            Spacer(Modifier.size(Dimens.Layout.SpaceSmall))
            Text("自动刷新")

            Spacer(Modifier.size(Dimens.Layout.Space))

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

        Text("屏幕 ID：# ${focusInfo.displayID}")
        MultiLineInfoRow("前台包名：", focusInfo.activityPackage)
        val activityFullName = if (focusInfo.activityClass.startsWith(".")) {
            "${focusInfo.activityPackage}${focusInfo.activityClass}"
        } else {
            focusInfo.activityPackage
        }
        MultiLineInfoRow("Activity：", activityFullName)
        MultiLineInfoRow("Window：", focusInfo.windowName)
    }
}

/**
 * 快捷控制面板。
 */
@Composable
private fun ControlPanel(
    viewModel: GenericPageViewModel,
    modifier: Modifier = Modifier
) {
    val globalStateManager: GlobalStateManager = LocalGlobalStateManager.current
    val logicManager: LogicManager = globalStateManager.logicManager
    val selectedDevice: ADBDevice? by logicManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    val device = selectedDevice ?: return

    Text("快捷操作")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        XOutlinedButton(
            onClick = {
                // TODO
            }
        ) { Text("远程控制") }

        XOutlinedButton(
            onClick = {
                // TODO
            }
        ) { Text("截屏") }

        XOutlinedButton(
            onClick = {
                logicManager.selectedExecutorFlow.value.openShell(device.getSN())
            }
        ) { Text("打开 Shell") }

        XOutlinedButton(
            onClick = {
                if (device is LocalDevice) {
                    val state = device.startRemoteControl()
                    globalStateManager.toastManager.show("启动远程调试 -> $state")
                }
            }
        ) { Text("开启远程调试") }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        XOutlinedButton(
            onClick = {
                runBlocking {
                    val r = viewModel.getSelectedExecutor().root(device.getSN())
                    globalStateManager.toastManager.show("Root -> $r")
                }
            }
        ) { Text("Root") }

        XOutlinedButton(
            onClick = {
                runBlocking {
                    val r = viewModel.getSelectedExecutor().remount(device.getSN())
                    globalStateManager.toastManager.show("Remount -> $r")
                }
            }
        ) { Text("Remount") }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        XOutlinedButton(
            onClick = {
                runBlocking {
                    viewModel.getSelectedExecutor().reboot(device.getSN())
                }
            }
        ) { Text("重启") }

        XOutlinedButton(
            onClick = {
                runBlocking {
                    viewModel.getSelectedExecutor().reboot(device.getSN(), "recovery")
                }
            }
        ) { Text("重启至 Recovery") }

        XOutlinedButton(
            onClick = {
                runBlocking {
                    viewModel.getSelectedExecutor().reboot(device.getSN(), "bootloader")
                }
            }
        ) { Text("重启至 Bootloader") }
    }
}
