package net.bi4vmr.study

/**
 * TODO 添加描述。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

fun main() {
    // val file = File("C:\\Link\\S1-Data\\Archive\\个人资源库\\原创照片\\事件・20251105_月球观测・制品")
    val file = File("C:\\Users\\bi4vmr\\Pictures\\原创照片\\事件・20251105_月球观测・制品")
    // val file2 = File("C:\\Users\\bi4vmr\\Downloads\\1.txt")

    if (!file.isDirectory || !file.canRead()) {
        println("❌ 目录不可用")
        exitProcess(1)
    }

    file.listFiles()?.forEach { f ->
        println("处理文件：${f.absolutePath}")
        val basename = f.name
        // 使用正则提取时间部分
        val localDateTime = parseDateTimeFromFilename(basename)
        if (localDateTime == null) {
            println("⚠️ 无法从文件名解析时间: $basename")
            exitProcess(1)
        }

        try {
            val fileTime = FileTime.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())

            // 设置最后修改时间（Last Modified Time）
            Files.setLastModifiedTime(f.toPath(), fileTime)

            println("✅ 已设置修改时间: ${f.name} → $localDateTime")

            setFileCreationTimeWindows(f, localDateTime)
            println("✅ 已设置创建时间: ${f.name} → $localDateTime")
        } catch (e: Exception) {
            println("❌ 解析或设置失败: $basename - ${e.message}")
        }
    }
}

fun setFileCreationTimeWindows(file: File, dateTime: LocalDateTime) {

    val psTime = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    val escapedPath = file.absolutePath.replace("'", "''") // 防止单引号破坏 PowerShell 语法


    val command = listOf(
        "powershell", "-Command", "(Get-Item -LiteralPath '$escapedPath').CreationTime = [DateTime]'$psTime'"
    )


    val process = ProcessBuilder(command).start()

    val exitCode = process.waitFor()

    if (exitCode != 0) {

        error("PowerShell failed: ${process.errorStream.reader().readText()}")

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
