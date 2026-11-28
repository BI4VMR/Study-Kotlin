@file:OptIn(ExperimentalComposeUiApi::class)

package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.common.icon.Icons
import java.awt.datatransfer.StringSelection

/**
 * 信息展示行。
 *
 * 左侧为标签，右侧为内容文本，右侧末端为复制按钮。
 */
@Composable
fun InfoRow(
    label: String,
    content: String,
    allowCopy: Boolean = true
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )

        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1F)
        )

        IconButton(
            modifier = Modifier.size(Dimens.Icon.Small),
            onClick = {
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(StringSelection(content)))
                }
            }
        ) {
            Icon(
                imageVector = Icons.Copy,
                contentDescription = null,
                modifier = Modifier.size(Dimens.Icon.Small)
            )
        }
    }
}


/**
 * 信息展示行（多行）。
 *
 * 首行左侧为标签，右侧为复制按钮，后续行为内容，允许多行显示。
 */
@Composable
fun MultiLineInfoRow(
    label: String,
    content: String,
    allowCopy: Boolean = true
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Column {
        Row {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.weight(1F)
            )

            if (allowCopy) {
                Spacer(modifier = Modifier.size(Dimens.Layout.Space))

                IconButton(
                    modifier = Modifier.size(Dimens.Icon.Normal),
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(StringSelection(content)))
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Copy,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.Icon.Small)
                    )
                }
            }
        }

        if (allowCopy) {
            SelectionContainer {
                Text(content)
            }
        } else {
            Text(content)
        }
    }
}
