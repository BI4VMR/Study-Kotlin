package net.bi4vmr.androidtoolkit.ui.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.common.enums.PackageType


@Composable
fun AppsPage(globalStateManager: GlobalStateManager) {
    Box(contentAlignment = Alignment.Center) {
        val e = globalStateManager.logicManager.getSelectedExecutor()
        val d = globalStateManager.logicManager.getSelectedDevice()
        d?.let { device ->
            var packageList: List<String> by remember { mutableStateOf(emptyList()) }
            val scope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                packageList = e.listPackages(device.getSN(), type = PackageType.ALL)
            }

            LazyColumn {
                items(packageList.size) { index ->
                    val packageName = packageList[index]
                    Text(packageName)
                }
            }
        }
    }
}
