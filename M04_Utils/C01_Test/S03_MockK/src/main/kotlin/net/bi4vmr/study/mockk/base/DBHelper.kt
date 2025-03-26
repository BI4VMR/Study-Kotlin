package net.bi4vmr.study.mockk.base

/**
 * 数据库工具类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class DBHelper {

    /**
     * 查询用户信息。
     *
     * 此处为示例代码，只是返回随意生成的数据。
     *
     * @return 用户信息Map，键为ID，值为名称。
     */
    fun queryUsers(): Map<Long, String> = mapOf(1L to "张三", 2L to "李四")

    /**
     * 查询用户数量。
     *
     * @return 用户数量。
     */
    fun queryUserCount(): Int = queryUsers().size

    /**
     * 输出日志。
     *
     * @param[info] 消息内容。
     */
    fun saveLog(info: String) {
        println(info)
    }
}
