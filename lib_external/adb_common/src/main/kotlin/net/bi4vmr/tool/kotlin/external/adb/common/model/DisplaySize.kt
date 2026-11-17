package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * 屏幕分辨率信息。
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
     * 用户覆写宽度。
     *
     * 若用户未设置则为 `-1`。
     */
    @get:JvmName("getOverrideWidth")
    val overrideWidth: Int = -1,

    /**
     * 用户覆写高度。
     *
     * 若用户未设置则为 `-1`。
     */
    @get:JvmName("getOverrideHeight")
    val overrideHeight: Int = -1
) {

    /**
     * 用户是否设置了覆写尺寸。
     *
     * @return `true` 表示存在用户覆写尺寸，`false` 表示不存在用户设置，以物理尺寸为准。
     */
    fun hasOverride(): Boolean = overrideWidth > 0 && overrideHeight > 0

    /**
     * 获取当前生效的宽度。
     *
     * @return 宽度。
     */
    fun getWidth(): Int = if (hasOverride()) overrideWidth else physicalWidth

    /**
     * 获取当前生效的高度。
     *
     * @return 高度。
     */
    fun getHeight(): Int = if (hasOverride()) overrideHeight else physicalHeight
}
