package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager

/**
 * TODO 添加名称。
 *
 * TODO 添加描述。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun ADBLocalConfig(globalStateManager: GlobalStateManager) {
    var adbPath: String by remember { mutableStateOf("") }

    SettingRow(label = "ADB 路径") {
        OutlinedTextField(
            value = adbPath,
            onValueChange = { newValue ->
                adbPath = newValue
            }
        )
    }
}
