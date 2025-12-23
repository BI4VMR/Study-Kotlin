package net.bi4vmr.study.behavior_define

/**
 * 数据库工具类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class DBHelper {

    // 根据用户ID查询姓名
    fun queryUserName(id: Int): String = "Real Name"

    // 根据身份证号查询姓名
    fun queryUserName(cardID: String): String = "Real Name"

    // 查询所有年龄和性别符合要求的用户ID
    fun queryUserNames(age: Int, male: Boolean): List<String> = listOf("Real Name")
}
