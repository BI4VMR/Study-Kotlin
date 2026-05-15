package net.bi4vmr.study.base

import java.sql.Timestamp

/**
 * 示例类：学生。
 *
 * Kotlin中推荐使用data class定义POJO，编译器会自动生成
 * equals()、hashCode()、toString()、copy()等方法。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class Student(
    val id: String = "",
    var name: String = "",
    var birthDay: Timestamp? = null
)
