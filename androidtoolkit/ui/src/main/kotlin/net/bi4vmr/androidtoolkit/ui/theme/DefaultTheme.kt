package net.bi4vmr.androidtoolkit.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * 默认主题。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@Composable
fun DefaultTheme(
    darkTheme: Boolean = isSystemDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        // typography = ATypography,
        shapes = appShapes,
        content = content
    )
}

val appShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),  // text fields, menus
    small = RoundedCornerShape(4.dp),  // chips, snackbars
    medium = RoundedCornerShape(8.dp),  // cards
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),  // AlertDialog
)

// Surface	RectangleShape (fixed square corners)
// Surface(
// shape = MaterialTheme.shapes.extraLarge,   // the same shape AlertDialog uses
// ...
// )
