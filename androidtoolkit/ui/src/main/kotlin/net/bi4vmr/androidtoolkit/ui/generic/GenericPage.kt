package net.bi4vmr.androidtoolkit.ui.generic

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.component.DeviceSelector
import net.bi4vmr.androidtoolkit.ui.component.InfoRow
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.RemoteDevice

@Composable
fun GenericPage(globalStateManager: GlobalStateManager) {
    val logicManager: LogicManager = globalStateManager.logicManager
    val selectedDevice: ADBDevice? by logicManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize()) {
        DeviceSelector(
            globalStateManager = globalStateManager,
            modifier = Modifier.width(280.dp).fillMaxHeight()
        )

        VerticalDivider()
        DevicePanel(
            globalStateManager = globalStateManager,
            device = selectedDevice,
            modifier = Modifier.weight(1F).fillMaxHeight()
        )
    }
}


@Composable
private fun DevicePanel(
    globalStateManager: GlobalStateManager,
    device: ADBDevice?, modifier: Modifier = Modifier
) {
    if (device == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("请在左侧选择一台设备", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val enabled = device.getState() == DeviceState.ONLINE
    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider()
        InfoRow("序列号", device.getSN())
        InfoRow("状态", device.getState().name)
        InfoRow("Android版本", "-")
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                if (device is RemoteDevice) {
                    globalStateManager.logicManager.selectedExecutorFlow.value.openShell(device.getSN())
                } else if (device is LocalDevice) {
                    device.openShell(newWindow = false)
                }
            }, enabled = enabled) { Text("打开 Shell") }


            // Button(onClick = { /* TODO 重启 */ }, enabled = enabled) { Text("重启") }
            // Button(onClick = { /* TODO 截图 */ }, enabled = enabled) { Text("截图") }
            // OutlinedButton(onClick = { /* TODO Root */ }, enabled = enabled) { Text("Root") }
        }
    }
}
