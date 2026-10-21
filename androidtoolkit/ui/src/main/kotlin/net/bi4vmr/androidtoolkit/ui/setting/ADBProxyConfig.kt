package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager

/**
 * TODO 添加名称。
 *
 * TODO 添加描述。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun ADBProxyConfig(globalStateManager: GlobalStateManager) {
    SettingRow(label = "启用 ADB 代理") {
        Row {
            Checkbox(checked = true, onCheckedChange = {})
            Text("启用代理服务器")
        }
    }
}
