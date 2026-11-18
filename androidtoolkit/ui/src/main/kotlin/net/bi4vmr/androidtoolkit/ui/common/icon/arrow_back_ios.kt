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
public val arrow_back_ios: ImageVector
  get() {
    if (_arrow_back_ios != null) {
      return _arrow_back_ios!!
    }
    _arrow_back_ios =
      ImageVector.Builder(
          name = "arrow_back_ios",
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
            moveTo(10f, 22f)
            lineTo(0f, 12f)
            lineTo(10f, 2f)
            lineToRelative(1.78f, 1.77f)
            lineTo(3.55f, 12f)
            lineToRelative(8.23f, 8.23f)
            lineTo(10f, 22f)
            close()
          }
        }
        .build()
    return _arrow_back_ios!!
  }

private var _arrow_back_ios: ImageVector? = null
