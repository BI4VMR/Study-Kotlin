package net.bi4vmr.tool.kotlin.external.adb.common.constants

/**
 * 设备属性。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Propertys {

    object Key {

        /**
         * 制造商。
         */
        const val MANUFACTURER = "ro.product.manufacturer"

        /**
         * 品牌。
         */
        const val BRAND = "ro.product.brand"

        /**
         * 型号。
         */
        const val MODEL = "ro.product.model"

        /**
         * Android OS 版本。
         */
        const val OS_VERSION = "ro.build.version.release"

        /**
         * Android API Level 。
         */
        const val API_LEVEL = "ro.build.version.sdk"
    }
}
