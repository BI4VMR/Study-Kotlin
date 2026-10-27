package net.bi4vmr.tool.kotlin.external.adb.proxy.util

import java.security.SecureRandom

/**
 * 配对工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object PairUtil {

    /**
     * 配对码长度。
     */
    const val CODE_LENGTH: Int = 6

    private val random = SecureRandom()
}
