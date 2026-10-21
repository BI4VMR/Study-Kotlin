package net.bi4vmr.androidtoolkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.bi4vmr.androidtoolkit.ui.apps.AppsPage
import net.bi4vmr.androidtoolkit.ui.common.Pages
import net.bi4vmr.androidtoolkit.ui.component.ToastHost
import net.bi4vmr.androidtoolkit.ui.display.DisplayPage
import net.bi4vmr.androidtoolkit.ui.files.FilesPage
import net.bi4vmr.androidtoolkit.ui.generic.GenericPage
import net.bi4vmr.androidtoolkit.ui.setting.SettingPage

/**
 * 应用主界面。
 */
@Composable
fun App() {
    val globalStateManager: GlobalStateManager = remember { GlobalStateManager() }
    var currentPage: Pages by remember { mutableStateOf(Pages.GENERAL) }

    Box {
        /* 根容器 */
        Row {
            /* 左栏：导航栏 */
            NavigationRail {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp, 16.dp)
                ) {
                    Pages.entries.forEach { page ->
                        NavigationRailItem(
                            selected = (currentPage == page),
                            icon = { Icon(page.icon, null) },
                            label = { Text(page.titleRes) },
                            onClick = { currentPage = page }
                        )
                    }
                }
            }

            VerticalDivider()

            /* 右栏：选中的页面 */
            when (currentPage) {
                Pages.GENERAL -> GenericPage(globalStateManager)
                Pages.DISPLAY -> DisplayPage(globalStateManager)
                Pages.APP -> AppsPage(globalStateManager)
                Pages.FILE -> FilesPage(globalStateManager)
                Pages.SETTING -> SettingPage(globalStateManager)
            }
        }

        /* 全局消息气泡层 */
        ToastHost(globalStateManager.toastManager)
    }
}
