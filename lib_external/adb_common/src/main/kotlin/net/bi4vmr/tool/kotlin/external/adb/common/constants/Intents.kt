package net.bi4vmr.tool.kotlin.external.adb.common.constants

/**
 * Intent 配置常量。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object Intents {

    object Action {

        /**
         * 媒体文件扫描。
         *
         * 创建文件后需要通过广播通知系统扫描文件，否则新文件不会出现在媒体库中。
         */
        const val MEDIA_SCAN = "android.intent.action.MEDIA_SCANNER_SCAN_FILE"
    }
}
