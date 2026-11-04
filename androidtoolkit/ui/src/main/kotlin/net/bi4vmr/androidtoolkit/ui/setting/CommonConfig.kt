package net.bi4vmr.androidtoolkit.ui.setting

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
import net.bi4vmr.androidtoolkit.ui.common.Dimens

/**
 * 通用配置。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun CommonConfig(globalStateManager: GlobalStateManager) {
    val logicManager = globalStateManager.logicManager
    val nodeName: String by logicManager.nodeNameFlow.collectAsStateWithLifecycle("")

    var nodeNameInput: String by remember { mutableStateOf(nodeName) }

    SettingSection("通用") {
        SettingColumn(
            label = "节点名称",
            description = "用于标识当前设备的名称，与远程转发器配对时将会显示给对端。",
        ) {
            OutlinedTextField(
                value = nodeNameInput,
                onValueChange = { newValue ->
                    nodeNameInput = newValue
                }
            )
        }

        Button(onClick = {
            /* 如果输入框为空，将其设为默认值。 */
            if (nodeNameInput.isBlank()) {
                nodeNameInput = Configs.Value.COMMON_NODE_NAME
            }

            globalStateManager.logicManager.setNodeName(nodeNameInput)
        }) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(Dimens.Layout.Space))
            Text("保存")
        }
    }
}
