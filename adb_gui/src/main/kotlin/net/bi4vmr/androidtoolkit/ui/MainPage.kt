package net.bi4vmr.androidtoolkit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 设备连接状态。 */
enum class DeviceState(val label: String) {
    ONLINE("在线"),
    OFFLINE("离线"),
    UNAUTHORIZED("未授权")
}

/** 设备信息（仅用于UI展示）。 */
data class AdbDevice(
    val serial: String,
    val model: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdk: Int,
    val abi: String,
    val state: DeviceState,
    val isRoot: Boolean = false
)

private val mockDevices = listOf(
    AdbDevice("emulator-5554", "Pixel 7", "Google", "14", 34, "arm64-v8a", DeviceState.ONLINE),
    AdbDevice("1A2B3C4D5E", "Mi 13", "Xiaomi", "13", 33, "arm64-v8a", DeviceState.ONLINE, true),
    AdbDevice("192.168.1.20:5555", "TV Box", "Generic", "11", 30, "armeabi-v7a", DeviceState.OFFLINE),
    AdbDevice("9F8E7D6C5B", "Unknown", "-", "-", 0, "-", DeviceState.UNAUTHORIZED)
)

@Composable
fun MainPage() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var selected by remember { mutableStateOf<AdbDevice?>(null) }
            Row(modifier = Modifier.fillMaxSize()) {
                DeviceList(
                    devices = mockDevices,
                    selected = selected,
                    onSelect = { selected = it },
                    modifier = Modifier.width(280.dp).fillMaxHeight()
                )
                VerticalDivider()
                DevicePanel(
                    device = selected,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<AdbDevice>,
    selected: AdbDevice?,
    onSelect: (AdbDevice) -> Unit,
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
            items(devices, key = { it.serial }) { device ->
                val isSelected = device.serial == selected?.serial
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(device) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(device.model, style = MaterialTheme.typography.titleSmall)
                        Text(device.serial, style = MaterialTheme.typography.bodySmall)
                        Text(
                            device.state.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (device.state == DeviceState.ONLINE) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevicePanel(device: AdbDevice?, modifier: Modifier = Modifier) {
    if (device == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("请在左侧选择一台设备", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val enabled = device.state == DeviceState.ONLINE
    Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(device.model, style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider()
        InfoRow("序列号", device.serial)
        InfoRow("状态", device.state.label)
        InfoRow("厂商", device.manufacturer)
        InfoRow("Android版本", device.androidVersion)
        InfoRow("SDK", device.sdk.toString())
        InfoRow("CPU架构", device.abi)
        InfoRow("Root", if (device.isRoot) "已获取" else "未获取")
        HorizontalDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { /* TODO 重启 */ }, enabled = enabled) { Text("重启") }
            Button(onClick = { /* TODO 截图 */ }, enabled = enabled) { Text("截图") }
            OutlinedButton(onClick = { /* TODO Root */ }, enabled = enabled) { Text("Root") }
        }
    }
}

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
