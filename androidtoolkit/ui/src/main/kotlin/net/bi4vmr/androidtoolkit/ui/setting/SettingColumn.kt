package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.bi4vmr.androidtoolkit.ui.common.Dimens


/**
 * 设置项。
 *
 * @param[label] 标签。显示于设置项顶部。
 * @param[description] 描述。显示于设置项底部，若配置为空值则不显示。
 * @param[content] 内容。根布局为 Column 。
 */
@Composable
fun SettingColumn(
    label: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space),
        modifier = modifier
    ) {
        Text(text = label, fontSize = Dimens.Text.Normal)

        content()

        if (description != null) {
            Text(text = description, fontSize = Dimens.Text.Small, color = Color.Gray)
        }
    }
}
