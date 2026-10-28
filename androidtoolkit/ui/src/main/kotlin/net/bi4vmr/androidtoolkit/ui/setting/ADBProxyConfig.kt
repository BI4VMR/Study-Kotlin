package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.data.config.Configs
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.enums.ServerState
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.security.PairedClient

/**
 * ADB 转发器配置。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun ADBProxyConfig(globalStateManager: GlobalStateManager) {
    val serverState: ServerState by globalStateManager.logicManager
        .adbProxyServer.stateFlow.collectAsStateWithLifecycle(ServerState.INIT)
    val proxyStartOnInit: Boolean by globalStateManager.logicManager
        .proxyStartOnInitFlow.collectAsStateWithLifecycle(false)
    val proxyInterface: String by globalStateManager.logicManager
        .proxyInterfaceFlow.collectAsStateWithLifecycle("")
    val proxyPort: Int by globalStateManager.logicManager
        .proxyPortFlow.collectAsStateWithLifecycle(0)

    val proxyEncrypt: Boolean by globalStateManager.logicManager
        .proxyEncryptFlow.collectAsStateWithLifecycle(true)
    val serverError: Exception? by globalStateManager.logicManager
        .adbProxyServer.errorFlow.collectAsStateWithLifecycle(null)
    val proxyTrustAll: Boolean by globalStateManager.logicManager
        .proxyTrustAllFlow.collectAsStateWithLifecycle(false)
    val fingerprint: String? = "TODO"
    val pairedClients: List<PairedClient> by globalStateManager.logicManager
        .pairedClientsFlow.collectAsStateWithLifecycle(emptyList())

    var interfaceText: String by remember(proxyInterface) { mutableStateOf(proxyInterface) }
    var portText: String by remember(proxyPort) { mutableStateOf(proxyPort.toString()) }
    var portInvalid: Boolean by remember { mutableStateOf(false) }
    var errorInfo: String by remember { mutableStateOf("") }

    SettingSection("ADB 转发器") {
        SettingColumn(label = "服务控制") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("当前状态")
                Text("$serverState")
                OutlinedButton(
                    enabled = (serverState == ServerState.INIT),
                    onClick = {
                        globalStateManager.logicManager.startProxyServer()
                    }) {
                    Text("启动")
                }
                OutlinedButton(
                    enabled = (serverState == ServerState.RUNNING),
                    onClick = {
                        globalStateManager.logicManager.stopProxyServer()
                    }) {
                    Text("停止")
                }

                Checkbox(
                    checked = proxyStartOnInit,
                    onCheckedChange = { new ->
                        globalStateManager.logicManager.setProxyStartOnInit(new)
                    })
                Text("自动启动转发器")
            }
        }

        SettingColumn(label = "安全") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = proxyEncrypt,
                    onCheckedChange = { new ->
                        globalStateManager.logicManager.setProxyEncrypt(new)
                    })
                Text("启用加密与配对（需重启转发器生效）")
            }
            if (!proxyEncrypt) {
                Text("加密已关闭：通信为明文，且任何人都可以操作本机设备。", color = Color.Red)
            }
            serverError?.let { Text("转发器启动失败：${it.message}", color = Color.Red) }
            Text("证书指纹（SHA-256）：${fingerprint ?: "服务启动后生成"}")

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = proxyTrustAll,
                    onCheckedChange = { new ->
                        globalStateManager.logicManager.setProxyTrustAll(new)
                    })
                Text("信任所有连接（仅用于调试，将跳过配对与认证）", color = if (proxyTrustAll) Color.Red else Color.Unspecified)
            }

            Text("已配对的客户端")
            if (pairedClients.isEmpty()) {
                Text("无")
            }
            Column {
                pairedClients.forEach { client ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Text("${client.name}（${client.id}）", modifier = Modifier.weight(1F))
                        TextButton(onClick = {
                            // globalStateManager.logicManager.revokePairedClient(client.id)
                        }) {
                            Text("撤销")
                        }
                    }
                }
            }
        }

        SettingColumn(label = "网络配置") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("监听地址")
                OutlinedTextField(
                    value = interfaceText,
                    onValueChange = { new ->
                        interfaceText = new
                    },
                    singleLine = true
                )

                Text("监听端口")
                OutlinedTextField(
                    value = portText,
                    onValueChange = { new ->
                        portInvalid = false
                        portText = new
                    },
                    singleLine = true,
                    isError = portInvalid
                )
            }


            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.Layout.Space),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 消息
                Text(
                    errorInfo,
                    color = Color.Red,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1F)
                )

                Button(onClick = {
                    /* 如果输入框为空，将其设为默认值。 */
                    if (interfaceText.isBlank()) {
                        interfaceText = Configs.Value.PROXY_INTERFACE_DEFAULT
                    }
                    if (portText.isBlank()) {
                        portText = Configs.Value.PROXY_PORT_DEFAULT.toString()
                    }

                    /* 检查端口号是否符合规则 */
                    val portInput = portText
                    val isNum = portInput.all { it.isDigit() }
                    if (!isNum) {
                        portInvalid = true

                        errorInfo = "端口号必须为数字"
                        return@Button
                    }

                    val portNumber = portInput.toInt()
                    if (portNumber !in 1..65535) {
                        portInvalid = true
                        errorInfo = "端口号必须在 0~65535 范围内"
                        return@Button
                    }

                    val interfaceInput = interfaceText
                    globalStateManager.logicManager.setProxyInterface(interfaceInput)
                    globalStateManager.logicManager.setProxyPort(portNumber)
                }) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(Dimens.Layout.Space))
                    Text("保存")
                }
            }
        }
    }
}
