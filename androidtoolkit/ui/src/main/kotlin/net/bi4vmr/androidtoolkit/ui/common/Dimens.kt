package net.bi4vmr.androidtoolkit.ui.common

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {

    object Layout {

        // 通用布局边距
        val Padding = 12.dp

        // 图标与文字之间的间距
        val Space = 8.dp

        // 图标与文字之间的间距（较小）
        val SpaceSmall = 4.dp

        // 弹窗内容边距
        val DialogPadding = 14.dp

        // 弹窗圆角
        val DialogCorner = 8.dp

        // 表项边距
        val ListItemPadding = 4.dp
    }

    object Text {

        // 通用标题
        val Title = 18.sp

        val Normal = 16.sp

        val Small = 14.sp

        val Button = 16.sp

        // 行高系列，桌面平台需要显式指定比字体大2号的行高才能实现垂直居中，原因不明。
        val Normal_LH = 18.sp

        // 窗口标题
        val DialogTitle = 18.sp
    }

    object Icon {

        // 较小图标
        val Small = 20.dp

        // 标准图标
        val Normal = 24.dp

        // 较大图标
        val Large = 32.dp
    }
}
