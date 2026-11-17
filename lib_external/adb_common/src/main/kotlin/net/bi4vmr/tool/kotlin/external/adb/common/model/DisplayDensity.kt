package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * 显示屏像素密度信息（ `wm density` 命令）。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class DisplayDensity(

    /**
     * 物理密度。
     */
    @get:JvmName("getPhysical")
    val physical: Int,

    /**
     * 覆盖密度。未设置覆盖值时为 `-1` 。
     */
    @get:JvmName("getOverride")
    val override: Int = -1
) {

    /**
     * 是否设置了覆盖值。
     */
    fun isOverridden(): Boolean = override > 0

    /**
     * 获取当前生效的密度。
     */
    fun getDensity(): Int = if (isOverridden()) override else physical
}
