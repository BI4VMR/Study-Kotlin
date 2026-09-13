package net.bi4vmr.study.phototool

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

@Composable
fun App() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("照片时间修改", "其他功能")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> PhotoTimeModifierScreen()
            1 -> OtherFeaturesScreen()
        }
    }
}

@Composable
fun OtherFeaturesScreen() {
    Text(
        text = "其他功能开发中...",
        modifier = Modifier.padding(16.dp)
    )
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "照片文件属性修改工具"
    ) {
        App()
    }
}
