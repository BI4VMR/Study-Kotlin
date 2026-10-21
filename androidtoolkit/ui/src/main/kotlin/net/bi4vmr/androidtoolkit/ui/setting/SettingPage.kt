package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
        ADBLocalConfig(globalStateManager)
        ADBProxyConfig(globalStateManager)
    }
}
