package net.bi4vmr.androidtoolkit.ui.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import net.bi4vmr.androidtoolkit.ui.component.PlaceHolder
import net.bi4vmr.androidtoolkit.ui.component.PlaceHolderType
import net.bi4vmr.androidtoolkit.ui.component.ToastManager
import net.bi4vmr.androidtoolkit.ui.component.XOutlinedButton
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

        Text("快捷操作")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XOutlinedButton(
                onClick = {
                    logicManager.selectedExecutorFlow.value.openShell(device.getSN())
                }
            ) { Text("打开 Shell") }

            XOutlinedButton(
                onClick = {
                    if (device is LocalDevice) {
                        val state = device.startRemoteControl()
                        toastManager.show("启动远程调试 -> $state")
                    }
                }
            ) { Text("开启远程调试") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XOutlinedButton(
                onClick = {
                    runBlocking {
                        viewModel.getSelectedExecutor().root(device.getSN())
                    }
                }
            ) { Text("Root") }

            XOutlinedButton(
                onClick = {
                    runBlocking {
                        viewModel.getSelectedExecutor().reboot(device.getSN())
                    }
                }
            ) { Text("重启") }
        }
    }
}
