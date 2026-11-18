package net.bi4vmr.androidtoolkit.ui.common

import androidx.compose.ui.graphics.vector.ImageVector
import net.bi4vmr.androidtoolkit.ui.*
import net.bi4vmr.androidtoolkit.ui.common.icon.Icons
import org.jetbrains.compose.resources.StringResource

/**
 * 页面。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class Pages(

    /**
     * 标签文本资源。
     */
    val titleRes: StringResource,

    /**
     * 图标。
     */
    val icon: ImageVector
) {

    /**
     * 主页。
     */
    GENERAL(Res.string.common_home, Icons.Home),

    /**
     * 屏幕管理。
     */
    DISPLAY(Res.string.common_display, Icons.Screen),

    /**
     * 应用管理。
     */
    APP(Res.string.common_apps, Icons.Apps),

    /**
     * 文件管理。
     */
    FILE(Res.string.common_files, Icons.File),

    /**
     * 设置。
     */
    SETTING(Res.string.common_setting, Icons.Settings);
}
