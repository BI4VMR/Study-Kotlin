package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.ADBController

/**
 * 窗口：添加远程 ADB 设备。
 */
@Composable
fun DeviceConnectDialog(
    globalStateManager: GlobalStateManager,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { onDismiss() },
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
                            // 全局设备列表将会自动刷新，此处只需要关闭窗口即可。
                            onDismiss()
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
