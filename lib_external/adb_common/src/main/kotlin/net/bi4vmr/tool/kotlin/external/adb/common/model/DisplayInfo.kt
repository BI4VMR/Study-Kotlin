package net.bi4vmr.tool.kotlin.external.adb.common.model

import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBOutputParser

/**
 * 设备屏幕信息。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class DisplayInfo(

    /**
     * 屏幕 ID 。
     *
     * 应用层的屏幕 ID ，即我们在 `startActivity()`  、 `am start` 等位置使用的屏幕标识符。
     */
    @get:JvmName("getID")
    val id: String,

    /**
     * 物理 ID 。
     *
     * 屏幕的物理标识符，通常为 `local:0` 、 `overlay:1` 等格式，冒号前的部分表示屏幕类型，冒号后的部分为编号。若解析失败则为空字符串。
     *
     * 在 Android 9 及更低版本中，通常为 `local:0` 等形式，编号即应用层的屏幕 ID ，插拔屏幕可能会改变。
     *
     * 在 Android 10 及更高版本中，通常为 `local:129` 、 `local:4630946370515662722` 等形式，编号根据屏幕参数生成，即使插拔屏幕也不
     * 会改变。
     */
    @get:JvmName("getUniqueID")
    val uniqueID: String = "",

    /**
     * 屏幕宽度。
     *
     * 若解析失败则为 `-1` 。
     */
    @get:JvmName("getWidth")
    val width: Int = -1,

    /**
     * 屏幕高度。
     *
     * 若解析失败则为 `-1` 。
     */
    @get:JvmName("getHeight")
    val height: Int = -1,

    /**
     * 所属设备的序列号。
     */
    @get:JvmName("getSN")
    val sn: String = ""
) {

    /**
     * 获取物理 ID 索引编号。
     *
     * 获取屏幕物理 ID 冒号后的索引编号，类似 `local:0` 中的 `0` 。如果格式不符合预期，则返回 [uniqueID] 原值。
     *
     * @return 索引编号。
     */
    fun getUniqueIndex(): String = ADBOutputParser.parseUniqueIndex(uniqueID)
}
