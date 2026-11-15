package net.bi4vmr.androidtoolkit.ui.common.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
public val screenshot_region: ImageVector
  get() {
    if (_screenshot_region != null) {
      return _screenshot_region!!
    }
    _screenshot_region =
      ImageVector.Builder(
          name = "screenshot_region",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(17f, 22f)
            verticalLineTo(19f)
            horizontalLineTo(14f)
            verticalLineTo(17f)
            horizontalLineToRelative(3f)
            verticalLineTo(14f)
            horizontalLineToRelative(2f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(2f)
            horizontalLineTo(19f)
            verticalLineToRelative(3f)
            horizontalLineTo(17f)
            close()
            moveTo(5f, 19f)
            verticalLineTo(14f)
            horizontalLineTo(7f)
            verticalLineToRelative(3f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(2f)
            horizontalLineTo(5f)
            close()
            moveTo(5f, 10f)
            verticalLineTo(5f)
            horizontalLineToRelative(5f)
            verticalLineTo(7f)
            horizontalLineTo(7f)
            verticalLineToRelative(3f)
            horizontalLineTo(5f)
            close()
            moveToRelative(12f, 0f)
            verticalLineTo(7f)
            horizontalLineTo(14f)
            verticalLineTo(5f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(5f)
            horizontalLineTo(17f)
            close()
          }
        }
        .build()
    return _screenshot_region!!
  }

private var _screenshot_region: ImageVector? = null
