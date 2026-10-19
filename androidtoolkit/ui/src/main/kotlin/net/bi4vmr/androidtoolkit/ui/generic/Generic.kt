package net.bi4vmr.androidtoolkit.ui.generic

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.core.IDevice
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.component.ExecutorTabs

/**
 * 主页：上方为已连接的服务端选项卡，选中后左侧展示其设备列表。
 */
@Composable
fun GenericPage(globalStateManager: GlobalStateManager) {
    val scope = rememberCoroutineScope()
    val executors by globalStateManager.executorsFlow.collectAsState()
    val selectedExecutor by globalStateManager.selectedExecutorFlow.collectAsState()
    val selectedDevice by globalStateManager.selectedDeviceFlow.collectAsState()
    val devices by selectedExecutor.getDevicesFlow().collectAsState()
    val available by selectedExecutor.getAvailableFlow().collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            ExecutorTabs(
                executors = executors,
                selectedIndex = executors.indexOf(selectedExecutor),
                onSelect = { globalStateManager.selectExecutor(executors[it]) },
                modifier = Modifier.weight(1F, fill = false)
            )
            OutlinedButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, null)
                Text("添加服务端")
            }
        }

        Row(modifier = Modifier.weight(1F).fillMaxWidth()) {
            DeviceList(
                devices = devices,
                available = available,
                selected = selectedDevice,
                onReconnect = { selectedExecutor.reconnect() },
                onSelect = { globalStateManager.selectDevice(it) },
                modifier = Modifier.width(280.dp).fillMaxHeight()
            )
            VerticalDivider()
            Box(modifier = Modifier.weight(1F).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(
                    text = selectedDevice?.getSN() ?: "请在左侧选择一台设备",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    if (showAddDialog) {
        AddServerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { host -> globalStateManager.addExecutor(host) }
        )
    }
}

@Composable
private fun DeviceList(
    devices: List<IDevice>,
    available: Boolean,
    selected: IDevice?,
    onReconnect: () -> Unit,
    onSelect: (IDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "设备列表",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (!available) {
            Column(
                modifier = Modifier.weight(1F).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("连接已断开", color = MaterialTheme.colorScheme.error)
                Button(onClick = onReconnect) { Text("重新连接") }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1F),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(devices, key = { it.getSN() }) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(device) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (device.getSN() == selected?.getSN()) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(device.getSN(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            device.getStateName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (device.isOnline()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddServerDialog(onDismiss: () -> Unit, onConfirm: suspend (String) -> Boolean) {
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("") }
    var connecting by remember { mutableStateOf(false) }
    var errorInfo: String? by remember { mutableStateOf(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("连接 ADB 服务端", style = MaterialTheme.typography.titleMedium)
                Text("主机名或 IP 地址：")
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    singleLine = true,
                    enabled = !connecting
                )
                errorInfo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = !connecting && host.isNotBlank(),
                    onClick = {
                        connecting = true
                        errorInfo = null
                        scope.launch {
                            val result = onConfirm(host.trim())
                            connecting = false
                            if (result) onDismiss() else errorInfo = "连接失败，请检查地址是否正确或服务端是否已存在"
                        }
                    }
                ) {
                    Text(if (connecting) "连接中…" else "连接")
                }
            }
        }
    }
}
