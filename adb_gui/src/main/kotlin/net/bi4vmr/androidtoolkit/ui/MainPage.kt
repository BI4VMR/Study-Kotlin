package net.bi4vmr.androidtoolkit.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.constant.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.ktx.ADBObserver
import net.bi4vmr.tool.kotlin.external.adb.model.ADBDevice

@Composable
fun MainPage() {
    LaunchedEffect(Unit){
        ADBController.init()

    }



    val globalManager = remember { GlobalStateManager() }
    val s: Boolean by globalManager.adbStateFlow.collectAsStateWithLifecycle()
    LaunchedEffect(s){
        ADBObserver.init()
    }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // var selected by remember { mutableStateOf<AdbDevice?>(null) }
            // val c =
            val deviceList: List<ADBDevice> by globalManager.deviceListFlow.collectAsStateWithLifecycle()
            LaunchedEffect(deviceList){
                println("deviceList: ${deviceList.size}")
            }
            Row(modifier = Modifier.fillMaxSize()) {
                DeviceList(
                    devices = deviceList,
                    // selected = selected,
                    // onSelect = { selected = it },
                    modifier = Modifier.width(280.dp).fillMaxHeight()
                )
                VerticalDivider()
                // DevicePanel(
                //     device = selected,
                //     modifier = Modifier.weight(1f).fillMaxHeight()
                // )
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<ADBDevice>,
    // selected: AdbDevice?,
    // onSelect: (AdbDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "设备列表 (${devices.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(devices, key = { it.getSN() }) { device ->
                // val isSelected = device.serial == selected?.serial
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (false) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(device.getSN(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            device.getState().name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (device.getState() == DeviceState.ONLINE) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// @Composable
// private fun DevicePanel(device: AdbDevice?, modifier: Modifier = Modifier) {
//     if (device == null) {
//         Box(modifier = modifier, contentAlignment = Alignment.Center) {
//             Text("请在左侧选择一台设备", style = MaterialTheme.typography.bodyLarge)
//         }
//         return
//     }
//
//     val enabled = device.state == DeviceState.ONLINE
//     Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
//         Text(device.model, style = MaterialTheme.typography.headlineSmall)
//         HorizontalDivider()
//         InfoRow("序列号", device.serial)
//         InfoRow("状态", device.state.label)
//         InfoRow("厂商", device.manufacturer)
//         InfoRow("Android版本", device.androidVersion)
//         InfoRow("SDK", device.sdk.toString())
//         InfoRow("CPU架构", device.abi)
//         InfoRow("Root", if (device.isRoot) "已获取" else "未获取")
//         HorizontalDivider()
//         Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//             Button(onClick = { /* TODO 重启 */ }, enabled = enabled) { Text("重启") }
//             Button(onClick = { /* TODO 截图 */ }, enabled = enabled) { Text("截图") }
//             OutlinedButton(onClick = { /* TODO Root */ }, enabled = enabled) { Text("Root") }
//         }
//     }
// }

@Composable
private fun InfoRow(name: String, value: String) {
    Row {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
