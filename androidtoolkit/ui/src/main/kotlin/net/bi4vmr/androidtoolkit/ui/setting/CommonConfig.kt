package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.data.config.Configs
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.Res
import net.bi4vmr.androidtoolkit.ui.button_save
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.setting_common
import net.bi4vmr.androidtoolkit.ui.setting_common_node_name
import net.bi4vmr.androidtoolkit.ui.setting_common_node_name_info
import org.jetbrains.compose.resources.stringResource

/**
 * 通用配置。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun CommonConfig(globalStateManager: GlobalStateManager) {
    val logicManager = globalStateManager.logicManager
    val nodeName: String by logicManager.nodeNameFlow.collectAsStateWithLifecycle("")

    var nodeNameInput: String by remember(nodeName) { mutableStateOf(nodeName) }

    SettingSection(stringResource(Res.string.setting_common)) {
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
                    nodeNameInput = Configs.Value.COMMON_NODE_NAME
                }

                globalStateManager.logicManager.setNodeName(nodeNameInput)
            }) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(Dimens.Layout.Space))
                Text(stringResource(Res.string.button_save))
            }
        }
    }
}
