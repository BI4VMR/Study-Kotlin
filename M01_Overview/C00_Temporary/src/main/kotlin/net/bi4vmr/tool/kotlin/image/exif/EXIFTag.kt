package net.bi4vmr.tool.kotlin.image.exif

/**
 * EXIF 常用标签。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class EXIFTag(val title: String) {

    /**
     * 光圈 F 值。
     *
     * 示例： `6.3` 、 `8` 。
     */
    APERTURE("FNumber"),

    /**
     * 快门速度。
     *
     * 示例： `1/250 ` （原始数值为 `0.004` ）、 `1/800 ` （原始数值为 `0.00125` ）。
     */
    SHUTTER_SPEED("ShutterSpeed"),

    /**
     * 感光度。
     *
     * 示例： `100` 、 `400` 。
     */
    ISO("ISO"),

    /**
     * 镜头焦距。
     *
     * 示例： `22.5 mm` （原始数值为 `22.5` ）、 `120 mm` （原始数值为 `120` ）。
     */
    FOCAL_LENGTH("FocalLength"),

    /**
     * 镜头等效焦距。
     *
     * 示例： `33 mm` （原始数值为 `22.5` ）、 `50 mm` （原始数值为 `50` ）。
     */
    FOCAL_LENGTH_35MM("FocalLengthIn35mmFormat"),

    /**
     * 对焦模式。
     *
     * 示例： `AF-A` 、 `MF` 。
     */
    FOCUS_MODE("FocusMode"),

    EXPOSURE_PROGRAM("ExposureProgram"),

    /**
     * 设备名称。
     *
     * 示例： `NIKON Z 30`
     */
    DEVICE("Model"),

    /**
     * 镜头名称。
     *
     * 示例： `NIKKOR Z DX 16-50mm f/3.5-6.3 VR`
     */
    LENS("LensID"),

    /**
     * 创建时间。
     *
     * 示例： `2025:01:24 14:12:40` 。
     */
    CREATE_DATE("CreateDate"),

    /**
     * 修改时间。
     *
     * 示例： `2025:01:24 14:12:40` 。
     */
    MODIFY_DATE("ModifyDate"),

    /**
     * 时间偏移量。
     *
     * 示例： `+08:00` 。
     */
    OFFSET_TIME("OffsetTime"),

    /**
     * 艺术家。
     */
    ARTIST("Artist"),

    /**
     * 备注。
     */
    COMMENT("UserComment");
}
