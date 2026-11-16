package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.base.enums.ADBExecFileMode
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.proxy.server.enums.ServerState

/**
 * MCP 服务端配置。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun MCPConfig(globalStateManager: GlobalStateManager) {
    val adbExecMode: ADBExecFileMode by globalStateManager.logicManager.adbExecFileModeFlow.collectAsStateWithLifecycle(
        ADBExecFileMode.INTERNAL
    )
    var adbPath: String by remember { mutableStateOf("") }

    SettingSection("MCP 服务端配置") {
        SettingColumn(label = "服务控制") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("当前状态")
                // Text("$serverState")
                OutlinedButton(
                    // enabled = (serverState == ServerState.INIT),
                    onClick = {
                        globalStateManager.logicManager.startProxyServer()
                    }) {
                    Text("启动")
                }
                OutlinedButton(
                    // enabled = (serverState == ServerState.RUNNING),
                    onClick = {
                        globalStateManager.logicManager.stopProxyServer()
                    }) {
                    Text("停止")
                }

                Checkbox(
                    checked = true,
                    onCheckedChange = { new ->
                        globalStateManager.logicManager.setProxyStartOnInit(new)
                    })
                Text("自动启动")
            }

            // serverError?.let { Text("转发器启动失败：${it.message}", color = Color.Red) }
        }
    }
}
