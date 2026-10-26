package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant

/**
 * ADB 执行器与设备列表。
 *
 * 选中状态由 [GlobalStateManager] 管理，全局可复用。
 */
@Composable
fun DeviceSelector(
    globalStateManager: GlobalStateManager,
    modifier: Modifier = Modifier
) {
    val logicManager: LogicManager = globalStateManager.logicManager
    val executors: List<ADBExecutor> by logicManager.executorsFlow.collectAsStateWithLifecycle()
    val selectedExecutor: ADBExecutor by logicManager.selectedExecutorFlow.collectAsStateWithLifecycle()
    val devices: List<ADBDevice> by logicManager.devicesFlow.collectAsStateWithLifecycle()
    val selectedDevice: ADBDevice? by logicManager.selectedDeviceFlow.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        Text(
            text = "设备列表",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        // 执行器列表
        SegmentedTabRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            executors.forEach { executor ->
                SegmentedTab(
                    selected = executor == selectedExecutor,
                    onClick = { logicManager.selectExecutor(executor) }
                ) {
                    Text(text = executor.getDisplayName(), maxLines = 1)
                }
            }
        }

        // 设备列表
        DeviceList(
            globalStateManager = globalStateManager,
            devices = devices,
            selected = selectedDevice,
            onSelect = { logicManager.selectDevice(it) },
            modifier = Modifier.weight(1F)
        )
    }
}

@Composable
private fun DeviceList(
    globalStateManager: GlobalStateManager,
    devices: List<ADBDevice>,
    selected: ADBDevice?,
    onSelect: (ADBDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1F),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(devices, key = { it.getSN() }) { device ->
                val isSelected = (device.getSN() == selected?.getSN())
                DeviceListItem(
                    device = device,
                    selected = isSelected,
                    onClick = { onSelect(device) }
                )
            }
        }

        var connectDeviceDialogState: Boolean by remember { mutableStateOf(false) }
        var connectProxyDialogState: Boolean by remember { mutableStateOf(false) }

        Row {
            OutlinedButton(
                onClick = { connectDeviceDialogState = true }
            ) {
                Icon(Icons.Filled.Add, null)
                Text("添加设备")
            }

            OutlinedButton(
                onClick = { connectProxyDialogState = true }
            ) {
                Icon(Icons.Filled.Add, null)
                Text("添加转发器")
            }
        }

        if (connectDeviceDialogState) {
            Dialog(
                onDismissRequest = { connectDeviceDialogState = false },
            ) {
                var host: String by remember { mutableStateOf("") }
                var errorInfo: String? by remember { mutableStateOf(null) }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column {
                        Text("连接 Android 设备")

                        Text("主机名或 IP 地址：")
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it }
                        )

                        if (errorInfo != null) {
                            Text(errorInfo ?: "-")
                        }

                        Button(
                            onClick = {
                                val result = ADBController.connectRemoteDevice(host)
                                if (result) {
                                    connectDeviceDialogState = false
                                } else {
                                    errorInfo = "连接失败，请检查主机名或 IP 地址是否正确"
                                }
                            }
                        ) {
                            Text("连接")
                        }
                    }
                }
            }
        }

        if (connectProxyDialogState) {
            ConnectProxyDialog(
                globalStateManager,
                onDismiss = { connectProxyDialogState = false }
            )
        }
    }
}


@Composable
private fun DeviceListItem(
    device: ADBDevice,
    selected: Boolean,
    onClick: (device: ADBDevice) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(device) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Dimens.Layout.Padding)
        ) {
            Icon(Icons.Filled.Devices, null)
            Spacer(Modifier.width(Dimens.Layout.Space))
            Text(device.getSN(), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1F))
            Text(device.getState().name, style = MaterialTheme.typography.labelSmall)
        }
    }
}


@Composable
fun ConnectProxyDialog(
    globalStateManager: GlobalStateManager,
    onDismiss: () -> Unit,
) {
    val logicManager: LogicManager = globalStateManager.logicManager

    // TODO TEST
    var host: String by remember { mutableStateOf("127.0.0.1") }
    var port: Int by remember { mutableIntStateOf(Constant.PORT_SERVER_DEFAULT) }
    var loading: Boolean by remember { mutableStateOf(false) }
    var pairingCode: String? by remember { mutableStateOf(null) }
    var errorInfo: String? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { onDismiss() },
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(Dimens.Layout.Padding)
        ) {
            Column {
                Text("连接 ADB 转发器")

                Text("主机名或 IP 地址：")
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    enabled = !loading
                )

                pairingCode?.let {
                    Text("配对码：$it", style = MaterialTheme.typography.headlineMedium)
                    Text("请与对方设备上显示的配对码核对，一致时在对方设备上批准配对。")
                }

                if (errorInfo != null) {
                    Text(errorInfo ?: "-", color = MaterialTheme.colorScheme.error)
                }

                Button(
                    enabled = !loading,
                    onClick = {
                        loading = true
                        errorInfo = null
                        scope.launch {
                            try {
                                logicManager.addRemoteExecutor(host) { pairingCode = it }
                                onDismiss()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                errorInfo = e.message ?: "连接失败，请检查主机名或 IP 地址是否正确"
                            } finally {
                                loading = false
                                pairingCode = null
                            }
                        }
                    }
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("连接中…")
                    } else {
                        Text("连接")
                    }
                }
            }
        }
    }
}
