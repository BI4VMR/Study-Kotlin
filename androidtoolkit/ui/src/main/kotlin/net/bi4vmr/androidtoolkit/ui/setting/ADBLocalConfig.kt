package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager

/**
 * ADB 本地进程配置。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun ADBLocalConfig(globalStateManager: GlobalStateManager) {
    var adbPath: String by remember { mutableStateOf("") }

    SettingSection("ADB 本地进程") {
        SettingRow(label = "ADB 可执行文件路径") {
            RadioButton(selected = true, onClick = {

            })
            Text("自动")
            RadioButton(selected = true, onClick = {

            })
            Text("内置")
            RadioButton(selected = true, onClick = {

            })
            Text("自定义")
            OutlinedTextField(
                value = adbPath,
                onValueChange = { newValue ->
                    adbPath = newValue
                }
            )
        }
    }
}
