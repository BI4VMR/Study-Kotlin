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

        /**
         * Android API Level（包含次版本号，例如 `36.1`）。
         */
        const val API_LEVEL_FULL = "ro.build.version.sdk_full"

        /**
         * Android 版本代号。
         *
         * 正式版为 `REL`，预览版为对应的开发代号。
         */
        const val CODENAME = "ro.build.version.codename"
    }
}
