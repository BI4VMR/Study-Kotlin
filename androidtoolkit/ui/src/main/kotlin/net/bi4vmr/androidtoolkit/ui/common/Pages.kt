package net.bi4vmr.androidtoolkit.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.ui.graphics.vector.ImageVector

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
    val titleRes: String,

    /**
     * 图标。
     */
    val icon: ImageVector
) {

    /**
     * 主页。
     */
    GENERAL("主页", Icons.Filled.Home),

    /**
     * 屏幕管理。
     */
    DISPLAY("显示", Icons.Filled.SmartDisplay),

    /**
     * 应用管理。
     */
    APP("应用", Icons.Filled.Apps),

    /**
     * 文件管理。
     */
    FILE("文件", Icons.Filled.Folder),

    /**
     * 设置。
     */
    SETTING("设置", Icons.Filled.Settings);
}
