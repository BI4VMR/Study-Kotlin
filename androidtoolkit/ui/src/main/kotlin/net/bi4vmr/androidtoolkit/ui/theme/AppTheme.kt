package net.bi4vmr.androidtoolkit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.bi4vmr.androidtoolkit.data.config.enums.DarkMode
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens

/**
 * 应用程序主题。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Composable
fun AppTheme(
    globalStateManager: GlobalStateManager,
    content: @Composable () -> Unit
) {
    val darkModeS: DarkMode by globalStateManager.logicManager.darkModeFlow.collectAsStateWithLifecycle(DarkMode.AUTO)
    val dark: Boolean = when (darkModeS) {
        DarkMode.AUTO -> isSystemDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
        typography = appTypography,
        shapes = appShapes,
        content = content
    )
}


val appTypography = Typography(
    titleMedium = Typography().bodyLarge.copy(
        fontSize = Dimens.Text.Title,
        fontWeight = FontWeight.Bold
    ),
    // 默认文本样式
    bodyLarge = Typography().bodyLarge.copy(
        fontSize = Dimens.Text.Normal,
    ),
    bodyMedium = Typography().bodyLarge.copy(
        fontSize = Dimens.Text.Normal,
    ),
    bodySmall = Typography().bodyLarge.copy(
        fontSize = Dimens.Text.Small
    )
)

val appShapes = Shapes(
    // 影响 TextField、Menu 等组件
    extraSmall = RoundedCornerShape(4.dp),
    // 影响 Chip、Snackbar 等组件
    small = RoundedCornerShape(4.dp),
    // 影响 Card 等组件
    medium = RoundedCornerShape(8.dp),
    // 影响 FAB 等组件
    large = RoundedCornerShape(8.dp),
    // 影响 AlertDialog 等组件
    extraLarge = RoundedCornerShape(8.dp)
)
