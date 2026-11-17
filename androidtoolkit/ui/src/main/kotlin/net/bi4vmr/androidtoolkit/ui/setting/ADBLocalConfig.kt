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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.base.enums.ADBExecFileMode
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.Res
import net.bi4vmr.androidtoolkit.ui.button_save
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBExecutableHelper
import org.jetbrains.compose.resources.stringResource

/**
 * ADB 本地进程配置。
 *
 * @param[globalStateManager] 全局状态管理。
 */
@Composable
fun ADBLocalConfig(globalStateManager: GlobalStateManager) {
    val logicManager = globalStateManager.logicManager
    val adbExecMode: ADBExecFileMode by logicManager.adbExecFileModeFlow.collectAsStateWithLifecycle(
        ADBExecFileMode.INTERNAL
    )

    var adbExecModeInput: ADBExecFileMode by remember(adbExecMode) { mutableStateOf(adbExecMode) }

    // 自动侦测路径
    val autoADBPath: String by produceState("-", adbExecMode) {
        if (adbExecMode == ADBExecFileMode.AUTO) {
            value = ADBExecutableHelper.getExecutablePath() ?: "未找到可用 ADB 程序"
        }
    }
    // 用户自定义路径
    var userADBPath: String by remember {
        mutableStateOf("")
    }

    // 首次进入时从配置加载
    LaunchedEffect(Unit) {
        userADBPath = logicManager.getADBExecFileOfUser()?.absolutePath ?: ""
    }


    // 输入框显示内容由模式推导
    val adbPathInput: String = when (adbExecMode) {
        ADBExecFileMode.INTERNAL -> "-"
        ADBExecFileMode.AUTO -> autoADBPath
        ADBExecFileMode.USER -> userADBPath
    }
    val adbPathInputEnable: Boolean = (adbExecMode == ADBExecFileMode.USER)

    SettingSection("ADB 本地进程") {
        SettingColumn(label = "ADB 可执行文件路径") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (adbExecMode == ADBExecFileMode.INTERNAL),
                    onClick = {
                        logicManager.setADBExecMode(ADBExecFileMode.INTERNAL)
                    })
                Text("内置")
                RadioButton(
                    selected = (adbExecMode == ADBExecFileMode.AUTO),
                    onClick = {
                        logicManager.setADBExecMode(ADBExecFileMode.AUTO)
                    })
                Text("自动侦测")
                RadioButton(
                    selected = (adbExecMode == ADBExecFileMode.USER),
                    onClick = {
                        logicManager.setADBExecMode(ADBExecFileMode.USER)
                    })
                Text("用户定义")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("路径：")
                Spacer(modifier = Modifier.width(Dimens.Layout.Space))
                OutlinedTextField(
                    value = adbPathInput,
                    onValueChange = { userADBPath = it },
                    enabled = adbPathInputEnable,
                    modifier = Modifier.weight(1F)
                )
            }

            Row(horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    /* 如果输入框为空，将其设为默认值。 */
                    // if (nodeNameInput.isBlank()) {
                    //     nodeNameInput = ConfigDefaults.COMMON_NODE_NAME
                    // }
                    //
                    // globalStateManager.logicManager.setDarkMode(darkModeInput)
                    // globalStateManager.logicManager.setNodeName(nodeNameInput)
                }) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(Dimens.Layout.Space))
                    Text(stringResource(Res.string.button_save))
                }
            }
        }
    }
}
