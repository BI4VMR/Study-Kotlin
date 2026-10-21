package net.bi4vmr.androidtoolkit.ui.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.component.DeviceSidebar
import net.bi4vmr.androidtoolkit.ui.component.InfoRow
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice

@Composable
fun GenericPage(globalStateManager: GlobalStateManager) {
    val selectedDevice: ADBDevice? by globalStateManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    Row(modifier = Modifier.fillMaxSize()) {
        DeviceSidebar(
            globalStateManager = globalStateManager,
            modifier = Modifier.width(280.dp).fillMaxHeight()
        )

        VerticalDivider()
        DevicePanel(
            device = selectedDevice,
            modifier = Modifier.weight(1F).fillMaxHeight()
        )
    }
}


@Composable
private fun DevicePanel(device: ADBDevice?, modifier: Modifier = Modifier) {
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
            Button(onClick = { /* TODO 重启 */ }, enabled = enabled) { Text("重启") }
            Button(onClick = { /* TODO 截图 */ }, enabled = enabled) { Text("截图") }
            OutlinedButton(onClick = { /* TODO Root */ }, enabled = enabled) { Text("Root") }
        }
    }
}
