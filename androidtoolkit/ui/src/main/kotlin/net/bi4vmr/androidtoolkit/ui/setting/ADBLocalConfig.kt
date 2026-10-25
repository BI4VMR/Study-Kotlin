package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.data.config.enums.ADBExecFileMode
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager

/**
 * ADB 本地进程配置。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun ADBLocalConfig(globalStateManager: GlobalStateManager) {
    val adbExecMode: ADBExecFileMode by globalStateManager.logicManager.adbExecFileModeFlow.collectAsStateWithLifecycle(ADBExecFileMode.INTERNAL)
    var adbPath: String by remember { mutableStateOf("") }

    SettingSection("ADB 本地进程") {
        SettingColumn(label = "ADB 可执行文件路径") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (adbExecMode == ADBExecFileMode.INTERNAL),
                    onClick = {
                        globalStateManager.logicManager.setADBExecMode(ADBExecFileMode.INTERNAL)
                    })
                Text("内置")
                RadioButton(
                    selected = (adbExecMode == ADBExecFileMode.AUTO),
                    onClick = {
                        globalStateManager.logicManager.setADBExecMode(ADBExecFileMode.AUTO)
                    })
                Text("自动侦测")
                RadioButton(
                    selected = (adbExecMode == ADBExecFileMode.USER),
                    onClick = {
                        globalStateManager.logicManager.setADBExecMode(ADBExecFileMode.USER)
                    })
                Text("用户定义")
            }

            OutlinedTextField(
                value = adbPath,
                onValueChange = { newValue ->
                    adbPath = newValue
                }
            )
        }
    }
}
