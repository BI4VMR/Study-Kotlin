package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.ui.*
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.tool.kotlin.external.adb.ADBController
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.cancellation.CancellationException

/**
 * 窗口：添加远程 ADB 设备。
 */
@Composable
fun DeviceConnectDialog(
    globalStateManager: GlobalStateManager,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = {
            onDismiss()
        }
    ) {
        val logicManager: LogicManager = globalStateManager.logicManager

        var hostInput: String by remember { mutableStateOf("") }
        var hostInvalid: Boolean by remember { mutableStateOf(false) }
        var portInput: String by remember { mutableStateOf(ADBController.PORT_DEVICE.toString()) }
        var portInvalid: Boolean by remember { mutableStateOf(false) }
        var save: Boolean by remember { mutableStateOf(false) }

        var loading: Boolean by remember { mutableStateOf(false) }
        var errorRes: StringResource? by remember { mutableStateOf(null) }
        var errorText: String by remember { mutableStateOf("") }

        /*
         * 设置错误信息。
         *
         * Compose 资源无法在按钮回调中同步获取，因此需要通过该方法设置资源 ID ，获取时优先采用资源文本，若为空值再取字符文本。
         */
        fun setErrorHint(res: StringResource? = null, text: String = "") {
            errorRes = res
            errorText = text
        }

        Surface(
            shape = RoundedCornerShape(Dimens.Layout.DialogCorner)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space),
                modifier = Modifier.padding(Dimens.Layout.DialogPadding)
            ) {
                Text("添加远程设备")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("地址：")
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        singleLine = true,
                        enabled = !loading,
                        isError = hostInvalid,
                        modifier = Modifier.weight(0.75F)
                    )

                    Spacer(Modifier.width(Dimens.Layout.Space))

                    Text("端口：")
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        singleLine = true,
                        enabled = !loading,
                        isError = portInvalid,
                        modifier = Modifier.weight(0.25F)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = save,
                        onCheckedChange = { save = it },
                        enabled = !loading
                    )
                    Text("记住此设备")
                }

                // 错误提示
                val errorHint = errorRes?.let { stringResource(it) } ?: errorText
                if (errorHint.isNotBlank()) {
                    Text(
                        text = errorHint,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        enabled = !loading,
                        onClick = {
                            val hostText = hostInput.trim()
                            if (hostText.isBlank()) {
                                hostInvalid = true
                                setErrorHint(res = Res.string.error_host_blank)
                                return@Button
                            }

                            /* 如果端口号为空，将其设为默认值。 */
                            if (portInput.isBlank()) {
                                portInput = ADBController.PORT_DEVICE.toString()
                            }

                            /* 检查端口号是否符合规则 */
                            val portNum = portInput.toIntOrNull()
                            if (portNum == null) {
                                portInvalid = true
                                setErrorHint(res = Res.string.error_port_not_a_number)
                                return@Button
                            } else if (portNum !in 1..65535) {
                                portInvalid = true
                                setErrorHint(res = Res.string.error_port_out_of_range)
                                return@Button
                            }

                            /* 未因错误提前返回，则重置错误状态。 */
                            errorRes = null
                            errorText = ""
                            hostInvalid = false
                            portInvalid = false

                            scope.launch {
                                loading = true

                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        logicManager.addRemoteDevice(hostText, portNum, save)
                                    }
                                }
                                    .onSuccess {
                                        // 全局设备列表将会自动刷新，此处只需要关闭窗口即可。
                                        onDismiss()
                                        // TODO 记住设备
                                        globalStateManager.toastManager.show("已连接远程设备：$hostText")
                                    }
                                    .onFailure {
                                        if (it is CancellationException) throw it
                                        setErrorHint(text = "${it.message}")
                                    }

                                loading = false
                            }
                        }
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(Dimens.Layout.Space))
                        }
                        Text(stringResource(Res.string.button_connect))
                    }
                }
            }
        }
    }
}
