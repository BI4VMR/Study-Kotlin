package net.bi4vmr.tool.kotlin.external.adb.proxy.util

import net.bi4vmr.tool.java.security.digest.MessageDigestUtil
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

    /**
     * 随机数生成器。
     */
    private val random = SecureRandom()


    /**
     * TODO 添加简述。
     *
     * TODO 添加详情。
     *
     * @param[TODO] 参数详情。
     * @return TODO 返回值详情。
     */
    fun generateNonce(): String = random.nextLong().toString()

    fun getCommit(cNonce: String): String = MessageDigestUtil.getSHA256("commit|$cNonce")

    fun generatePairCode(cfingerprint: String, sfingerprint: String, cNonce: String, sNonce: String): String {

        /*
         * 使用域前缀 `pair` 与其他元素组合生成字符串。
         *
         * 域前缀用于区分不同的业务，防止攻击者从其他使用相同算法的接口获得关键因素后攻击当前业务接口。
         */
        val text = "pair|${cfingerprint.lowercase()}_${sfingerprint.lowercase()}_${cNonce}_$sNonce"
        val hash = MessageDigestUtil.getSHA256(text)
        val value = hash.take(4).fold(0L) { sum, i ->
            // 每轮循环将当前累加数值的位左移 1 字节，空出低 8 位；然后获得当前字节的无符号值放入低 8 位。
            (sum shl 8) or (i.code.toLong() and 0xFF)
        }
        // 取余作为配对码，如果不满预定位数则在前部补零。
        return (value % 1_000_000L).toString()
            .padStart(CODE_LENGTH, '0')
    }
}
