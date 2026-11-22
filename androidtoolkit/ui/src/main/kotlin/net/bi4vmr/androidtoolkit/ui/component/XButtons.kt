package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * 按钮（描边）。
 *
 * 使用 [Box] 自行绘制容器，只为内容与 [contentPadding] 分配尺寸，
 * 不会像 Material3 的 `OutlinedButton` 那样附带 40dp 最小高度和 48dp 触控留白，
 * 因此可以完全贴合父容器或内容。
 *
 * @param onClick 点击回调。
 * @param modifier 外部修改器。
 * @param enabled 是否启用；禁用时不响应输入，并使用 Material3 的禁用配色。
 * @param contentPadding 容器与内容之间的留白。
 * @param content 按钮内容，处于 [RowScope] 中。
 */
@Composable
fun XOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(6.dp)
    // 复用 Material3 的默认配色与描边，保持与系统按钮一致（含禁用状态）。
    val colors = ButtonDefaults.outlinedButtonColors()
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val border = ButtonDefaults.outlinedButtonBorder(enabled)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(border, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
