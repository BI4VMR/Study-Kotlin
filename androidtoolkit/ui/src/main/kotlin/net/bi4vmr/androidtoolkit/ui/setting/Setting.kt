package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable


@Composable
fun SettingPage() {
    Column {
        Row {
            Checkbox(checked = true, onCheckedChange = {})
            Text("启用代理服务器")
        }
    }
}
