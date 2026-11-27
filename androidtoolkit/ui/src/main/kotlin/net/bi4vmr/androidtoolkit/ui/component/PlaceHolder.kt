package net.bi4vmr.androidtoolkit.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.common.icon.Icons

/**
 * 组件：通用占位符。
 */
@Composable
fun PlaceHolder(
    type: PlaceHolderType,
    info: String,
    detail: String? = null,
    modifier: Modifier = Modifier,
    extras: @Composable BoxScope.() -> Unit = {}
) {
    val icon = when (type) {
        PlaceHolderType.Info -> Icons.Info
        PlaceHolderType.Warning -> Icons.Warning
        PlaceHolderType.Error -> Icons.Error
    }
    val color = when (type) {
        PlaceHolderType.Info -> MaterialTheme.colorScheme.onSurfaceVariant
        PlaceHolderType.Warning -> Color.Yellow
        PlaceHolderType.Error -> MaterialTheme.colorScheme.onError
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.Layout.Space, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize().padding(Dimens.Layout.Padding)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(Dimens.Icon.Large)
        )

        Text(
            text = info,
            color = color,
            style = MaterialTheme.typography.titleMedium
        )

        if (detail != null) {
            Text(text = detail)
        }

        Box(content = extras)
    }
}


enum class PlaceHolderType {
    Info, Warning, Error;
}
