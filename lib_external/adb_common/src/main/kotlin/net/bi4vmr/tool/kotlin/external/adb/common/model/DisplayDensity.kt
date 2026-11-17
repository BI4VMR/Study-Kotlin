package net.bi4vmr.tool.kotlin.external.adb.common.model

/**
 * 屏幕像素密度信息。
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
     * 用户覆写密度。
     *
     * 若用户未设置则为 `-1`。
     */
    @get:JvmName("getOverride")
    val override: Int = -1
) {

    /**
     * 用户是否设置了覆写像素密度。
     *
     * @return `true` 表示存在用户覆写像素密度，`false` 表示不存在用户设置，以物理像素密度为准。
     */
    fun hasOverride(): Boolean = override > 0

    /**
     * 获取当前生效的像素密度。
     *
     * @return 像素密度。
     */
    fun getDensity(): Int = if (hasOverride()) override else physical
}
