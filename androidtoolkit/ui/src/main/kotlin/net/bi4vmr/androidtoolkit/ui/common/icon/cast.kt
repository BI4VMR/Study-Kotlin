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
public val cast: ImageVector
  get() {
    if (_cast != null) {
      return _cast!!
    }
    _cast =
      ImageVector.Builder(
          name = "cast",
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
            moveTo(12f, 12f)
            close()
            moveToRelative(8f, 8f)
            horizontalLineTo(15f)
            quadToRelative(0f, -0.5f, -0.04f, -1f)
            reflectiveQuadTo(14.85f, 18f)
            horizontalLineTo(20f)
            verticalLineTo(6f)
            horizontalLineTo(4f)
            verticalLineTo(7.15f)
            quadTo(3.5f, 7.07f, 3f, 7.04f)
            reflectiveQuadTo(2f, 7f)
            verticalLineTo(6f)
            quadTo(2f, 5.18f, 2.59f, 4.59f)
            reflectiveQuadTo(4f, 4f)
            horizontalLineTo(20f)
            quadToRelative(0.83f, 0f, 1.41f, 0.59f)
            quadTo(22f, 5.18f, 22f, 6f)
            verticalLineTo(18f)
            quadToRelative(0f, 0.82f, -0.59f, 1.41f)
            reflectiveQuadTo(20f, 20f)
            close()
            moveTo(2f, 20f)
            verticalLineTo(17f)
            quadToRelative(1.25f, 0f, 2.13f, 0.88f)
            reflectiveQuadTo(5f, 20f)
            horizontalLineTo(2f)
            close()
            moveToRelative(5f, 0f)
            quadTo(7f, 17.93f, 5.54f, 16.46f)
            reflectiveQuadTo(2f, 15f)
            verticalLineTo(13f)
            quadToRelative(2.93f, 0f, 4.96f, 2.04f)
            reflectiveQuadTo(9f, 20f)
            horizontalLineTo(7f)
            close()
            moveToRelative(4f, 0f)
            quadToRelative(0f, -1.88f, -0.71f, -3.51f)
            reflectiveQuadTo(8.36f, 13.64f)
            reflectiveQuadTo(5.51f, 11.71f)
            reflectiveQuadTo(2f, 11f)
            verticalLineTo(9f)
            quadTo(4.28f, 9f, 6.28f, 9.86f)
            quadToRelative(2f, 0.86f, 3.5f, 2.36f)
            reflectiveQuadToRelative(2.36f, 3.5f)
            reflectiveQuadTo(13f, 20f)
            horizontalLineTo(11f)
            close()
          }
        }
        .build()
    return _cast!!
  }

private var _cast: ImageVector? = null
