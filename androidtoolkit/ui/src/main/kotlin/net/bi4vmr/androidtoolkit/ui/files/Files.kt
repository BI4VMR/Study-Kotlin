package net.bi4vmr.androidtoolkit.ui.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice


@Composable
fun FilesPage(globalStateManager: GlobalStateManager) {
    Box(contentAlignment = Alignment.Center) {
        LazyColumn {
            globalStateManager.logicManager.selectedDeviceFlow.value?.let {
                if (it is LocalDevice) {
                    it.listFiles(null).forEach { fileEntry ->
                        fileEntry.isApplicationPackage
                        item {
                            Text(text = fileEntry.fullPath)
                            Text(text = fileEntry.type.toString())
                        }
                    }
                }
            }
        }
    }
}
