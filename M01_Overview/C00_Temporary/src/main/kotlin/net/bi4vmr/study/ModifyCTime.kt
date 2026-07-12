package net.bi4vmr.study

/**
 * TODO 添加描述。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
import net.bi4vmr.tool.java.common.base.FileUtil
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

fun main() {
    // val file = File("C:\\Link\\S1-Data\\Archive\\个人资源库\\原创照片\\日志_2024_原始")
    // val file = File("C:\\Users\\bi4vmr\\Pictures\\原创照片\\地点_南京_长江北岸_原始")
    val file = File("C:\\Users\\bi4vmr\\Download\\T")

    if (!file.isDirectory || !file.canRead()) {
        println("❌ 目录不可用")
        exitProcess(1)
    }

    file.listFiles()?.forEach { f ->
        println("处理文件：${f.absolutePath}")
        val basename = f.name
        // 使用正则提取时间部分
        val dateTime = parseZonedDateTimeFromFilename(basename)
        if (dateTime == null) {
            println("⚠️ 无法从文件名解析时间: $basename")
            return@forEach
        }

        try {
            FileUtil.setModifyTime(f, dateTime)
            println("✅ 已设置修改时间: ${f.name} → $dateTime")

            FileUtil.setCreateTime(f, dateTime)
            println("✅ 已设置创建时间: ${f.name} → $dateTime")
        } catch (e: Exception) {
            println("❌ 解析或设置失败: $basename - ${e.message}")
        }
    }
}

fun parseZonedDateTimeFromFilename(filename: String): ZonedDateTime? {

    // 去掉扩展名
    val nameWithoutExt = filename.substringBeforeLast('.')

    // 检查是否以 8位数字 + _ + 6位数字 开头
    if (nameWithoutExt.length < 15) return null

    val prefix = nameWithoutExt.substring(0, 15) // "20251129_130931"

    if (!prefix.matches(Regex("""\d{8}_\d{6}"""))) return null

    return try {
        // 默认认为文件时间是以系统时区为准的
        ZonedDateTime.parse(
            prefix, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.systemDefault())
        )
    } catch (e: Exception) {
        null
    }
}

fun parseDateTimeFromFilename(filename: String): LocalDateTime? {

    // 去掉扩展名
    val nameWithoutExt = filename.substringBeforeLast('.')

    // 检查是否以 8位数字 + _ + 6位数字 开头
    if (nameWithoutExt.length < 15) return null

    val prefix = nameWithoutExt.substring(0, 15) // "20251129_130931"

    if (!prefix.matches(Regex("""\d{8}_\d{6}"""))) return null

    return try {
        LocalDateTime.parse(prefix, DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    } catch (e: Exception) {
        null
    }
}
