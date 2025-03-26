package net.bi4vmr.study.mockk.base

/**
 * 用户管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class UserManager {

    // 依赖项：数据库
    private val mDBHelper: DBHelper = DBHelper()

    // 获取所有用户名称
    fun getUserNames(): List<String> {
        return mDBHelper.queryUsers()
            .values
            .toList()
    }

    // 获取所有用户名称（新增了日志记录的步骤）
    fun getUserNames2(): List<String> {
        mDBHelper.saveLog("GetUserNames")
        return mDBHelper.queryUsers()
            .values
            .toList()
    }

    // 获取用户数量
    fun getUserCount(): Int {
        return mDBHelper.queryUserCount()
    }
}
