package net.bi4vmr.androidtoolkit.ui.common

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {

    object Layout {
        // 布局边距
        val Padding = 12.dp

        val DialogPadding = 28.dp

        // 图标与文字之间的间距
        val Space = 8.dp

        // 卡片圆角
        val CardCorner = 12.dp
    }

    object Text {
        val Title = 18.sp

        val DialogTitle = 20.sp

        val Normal = 16.sp

        val Small = 14.sp

        val Button = 16.sp

        // 行高系列，桌面平台需要显式指定比字体大2号的行高才能实现垂直居中，原因不明。
        val Normal_LH = 18.sp
    }

    object Icon {
        // 触摸区域
        val Touch = 32.dp

        // 标准图标
        val Normal = 24.dp

        // 卡片图标
        val Subtitle = 22.dp

        // 按钮等图标
        val Small = 20.dp
    }
}
