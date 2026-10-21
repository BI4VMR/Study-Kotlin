package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens

/**
 * 设置主页。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun SettingPage(globalStateManager: GlobalStateManager) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space),
        modifier = Modifier.padding(Dimens.Layout.Padding)
    ) {
        SettingSection(title = "ADB 本地进程") {
            SettingRow(label = "启用 ADB 代理") {
                Row {
                    Checkbox(checked = true, onCheckedChange = {})
                    Text("启用代理服务器")
                }
            }
        }

        SettingSection(title = "ADB 转发器") {
            SettingRow(label = "启用 ADB 代理") {
                Row {
                    Checkbox(checked = true, onCheckedChange = {})
                    Text("启用代理服务器")
                }
            }
        }
    }
}
