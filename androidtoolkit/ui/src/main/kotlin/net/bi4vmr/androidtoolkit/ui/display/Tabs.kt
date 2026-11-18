package net.bi4vmr.androidtoolkit.ui.display

import net.bi4vmr.androidtoolkit.ui.Res
import net.bi4vmr.androidtoolkit.ui.display_tab_activities
import net.bi4vmr.androidtoolkit.ui.display_tab_base
import net.bi4vmr.androidtoolkit.ui.display_tab_windows
import org.jetbrains.compose.resources.StringResource

/**
 * 页面。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class Tabs(

    /**
     * 标签文本资源。
     */
    val titleRes: StringResource
) {

    /**
     * 基本信息。
     */
    BASE(Res.string.display_tab_base),

    /**
     * Activity 信息。
     */
    ACTIVITIES(Res.string.display_tab_activities),

    /**
     * Window 信息。
     */
    WINDOWS(Res.string.display_tab_windows);
}
