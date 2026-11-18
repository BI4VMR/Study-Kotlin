package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.base.constants.ConfigDefaults
import net.bi4vmr.androidtoolkit.base.enums.DarkMode
import net.bi4vmr.androidtoolkit.ui.*
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.common.icon.Icons
import org.jetbrains.compose.resources.stringResource

/**
 * 通用配置。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun CommonConfig(globalStateManager: GlobalStateManager) {
    val logicManager = globalStateManager.logicManager
    val darkMode: DarkMode by logicManager.darkModeFlow.collectAsStateWithLifecycle(DarkMode.AUTO)
    val nodeName: String by logicManager.nodeNameFlow.collectAsStateWithLifecycle("")

    var darkModeInput: DarkMode by remember(darkMode) { mutableStateOf(darkMode) }
    var nodeNameInput: String by remember(nodeName) { mutableStateOf(nodeName) }

    SettingSection(stringResource(Res.string.setting_common)) {
        SettingColumn(label = stringResource(Res.string.setting_common_dark_mode)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (darkModeInput == DarkMode.AUTO),
                    onClick = { darkModeInput = DarkMode.AUTO })
                Text(stringResource(Res.string.setting_common_dark_mode_auto))
                RadioButton(
                    selected = (darkModeInput == DarkMode.LIGHT),
                    onClick = { darkModeInput = DarkMode.LIGHT })
                Text(stringResource(Res.string.setting_common_dark_mode_light))
                RadioButton(
                    selected = (darkModeInput == DarkMode.DARK),
                    onClick = { darkModeInput = DarkMode.DARK })
                Text(stringResource(Res.string.setting_common_dark_mode_dark))
            }
        }

        SettingColumn(
            label = stringResource(Res.string.setting_common_node_name),
            description = stringResource(Res.string.setting_common_node_name_info)
        ) {
            OutlinedTextField(
                value = nodeNameInput,
                onValueChange = { newValue ->
                    nodeNameInput = newValue
                }
            )
        }

        Row(horizontalArrangement = Arrangement.End) {
            Button(onClick = {
                /* 如果输入框为空，将其设为默认值。 */
                if (nodeNameInput.isBlank()) {
                    nodeNameInput = ConfigDefaults.COMMON_NODE_NAME
                }

                globalStateManager.logicManager.setDarkMode(darkModeInput)
                globalStateManager.logicManager.setNodeName(nodeNameInput)
            }) {
                Icon(Icons.Save, null)
                Spacer(Modifier.width(Dimens.Layout.Space))
                Text(stringResource(Res.string.button_save))
            }
        }
    }
}
