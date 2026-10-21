package net.bi4vmr.androidtoolkit.ui.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import net.bi4vmr.androidtoolkit.ui.common.Dimens

/**
 * 设置片段。
 *
 * 显示为卡片形式，用于包裹一组设置项。
 *
 * @param[title] 标题。显示于设置片段顶部。
 * @param[content] 内容。根布局为 Column 部。
 */
@Composable
fun SettingSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space),
            modifier = Modifier.padding(Dimens.Layout.Padding)
        ) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = Dimens.Text.Normal)
            HorizontalDivider()
            content()
        }
    }
}
