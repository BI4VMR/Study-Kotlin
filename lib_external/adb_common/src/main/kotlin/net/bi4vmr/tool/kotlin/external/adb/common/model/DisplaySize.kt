package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * 显示屏分辨率信息（ `wm size` 命令）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class DisplaySize(

    /**
     * 物理宽度。
     */
    @get:JvmName("getPhysicalWidth")
    val physicalWidth: Int,

    /**
     * 物理高度。
     */
    @get:JvmName("getPhysicalHeight")
    val physicalHeight: Int,

    /**
     * 覆盖宽度。未设置覆盖值时为 `-1` 。
     */
    @get:JvmName("getOverrideWidth")
    val overrideWidth: Int = -1,

    /**
     * 覆盖高度。未设置覆盖值时为 `-1` 。
     */
    @get:JvmName("getOverrideHeight")
    val overrideHeight: Int = -1
) {

    /**
     * 是否设置了覆盖值。
     */
    fun isOverridden(): Boolean = overrideWidth > 0 && overrideHeight > 0

    /**
     * 获取当前生效的宽度。
     */
    fun getWidth(): Int = if (isOverridden()) overrideWidth else physicalWidth

    /**
     * 获取当前生效的高度。
     */
    fun getHeight(): Int = if (isOverridden()) overrideHeight else physicalHeight
}
