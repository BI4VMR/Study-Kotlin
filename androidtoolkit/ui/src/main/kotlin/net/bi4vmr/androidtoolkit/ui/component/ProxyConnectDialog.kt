package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.tool.kotlin.external.adb.proxy.common.constants.Constants

/**
 * 窗口：添加远程 ADB 转发器。
 */
@Composable
fun ProxyConnectDialog(
    globalStateManager: GlobalStateManager,
    onDismiss: () -> Unit,
) {
    val logicManager: LogicManager = globalStateManager.logicManager

    // TODO TEST
    var host: String by remember { mutableStateOf("127.0.0.1") }
    var port: Int by remember { mutableIntStateOf(Constants.PORT_SERVER_DEFAULT) }
    var loading: Boolean by remember { mutableStateOf(false) }
    var secure: Boolean by remember { mutableStateOf(true) }
    var pairingCode: String? by remember { mutableStateOf(null) }
    var errorInfo: String? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { onDismiss() },
    ) {
        Surface(
            shape = RoundedCornerShape(Dimens.Layout.DialogCorner)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space),
                modifier = Modifier.padding(Dimens.Layout.DialogPadding)
            ) {
                Text(
                    text = "添加 ADB 转发器",
                    fontSize = Dimens.Text.DialogTitle
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("地址：")
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        enabled = !loading,
                        modifier = Modifier.weight(0.75F)
                    )

                    Text("端口：")
                    OutlinedTextField(
                        value = port.toString(),
                        onValueChange = { port = it.toIntOrNull() ?: Constants.PORT_SERVER_DEFAULT },
                        enabled = !loading,
                        modifier = Modifier.weight(0.25F)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = secure,
                        onCheckedChange = { secure = it },
                        enabled = !loading
                    )
                    Text("启用 TLS")
                }

                pairingCode?.let {
                    Text("配对码：$it", style = MaterialTheme.typography.headlineMedium)
                    Text("请与对方设备上显示的配对码核对，一致时在对方设备上批准配对。")
                }

                if (errorInfo != null) {
                    Text(errorInfo ?: "-", color = MaterialTheme.colorScheme.error)
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("取消配对")
                        }
                    }

                    Button(
                        enabled = !loading,
                        onClick = {
                            loading = true
                            errorInfo = null
                            scope.launch {
                                try {
                                    logicManager.addRemoteExecutor(host, secure = secure) { pairingCode = it }
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
}
