package net.bi4vmr.study

import java.io.File

// 修改此路径以指向目标日志文件或目录
// private const val LOG_FILE_PATH = "/home/bi4vmr/Downloads/logcat.log"
private const val LOG_FILE_PATH = "/home/bi4vmr/Downloads/logcat"

// 统计维度：BY_PACKAGE 按包名统计；BY_TAG 按日志TAG统计
private val STAT_MODE = StatMode.BY_PACKAGE

// 统计目录时，只处理匹配该扩展名的文件（忽略大小写）
private val LOG_FILE_EXTENSIONS = setOf("log", "txt")

// 匹配 "Start proc <PID>:<packageName>/..." 格式
private val START_PROC_REGEX = Regex("""Start proc (\d+):([^/\s]+)""")

// 匹配 "Process: <packageName>, PID: <PID>" 格式（Crash日志）
private val CRASH_PROC_REGEX = Regex("""Process:\s+([^,]+),\s+PID:\s+(\d+)""")

/** 统计维度 */
enum class StatMode {
    /** 按包名聚合（需从日志中识别 PID→包名 映射） */
    BY_PACKAGE,

    /** 按日志 TAG 聚合 */
    BY_TAG
}

fun main() {
    runLogStat(LOG_FILE_PATH, STAT_MODE, aggregateByPackage = true)
}

/**
 * 核心入口：解析指定路径（文件或目录）并按指定维度输出统计结果。
 * 结果通过 println 输出，可在命令行或 GUI 中重定向 stdout 使用。
 *
 * @param path               日志文件路径或目录路径
 * @param mode               统计维度
 * @param aggregateByPackage 仅对 BY_PACKAGE 模式有效：
 *                           true  = 按包名聚合，合并同包名所有 PID 的行数，不输出 PID 明细；
 *                           false = 展示 PID 明细（含各 PID 对应包名）。
 */
fun runLogStat(path: String, mode: StatMode, aggregateByPackage: Boolean = true) {
    val input = File(path)
    if (!input.exists()) {
        println("Path not found: $path")
        return
    }

    // 收集待处理的文件列表
    val logFiles: List<File> = when {
        input.isFile -> {
            println("Input: file -> ${input.name}")
            listOf(input)
        }
        input.isDirectory -> {
            val files = input.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in LOG_FILE_EXTENSIONS }
                .sortedBy { it.name }
                .toList()
            println("Input: directory -> ${input.absolutePath}")
            println("Found ${files.size} log file(s): ${files.joinToString { it.name }}")
            files
        }
        else -> {
            println("Unsupported path type: $path")
            return
        }
    }

    if (logFiles.isEmpty()) {
        println("No log files found.")
        return
    }

    // 跨文件累计的统计数据
    val pidPackageMap = mutableMapOf<String, String>()  // pid -> packageName
    val pidLineCount = mutableMapOf<String, Long>()     // pid -> lineCount
    val tagLineCount = mutableMapOf<String, Long>()     // tag -> lineCount
    var totalLines = 0L

    // 逐文件解析，数据累加到同一份 Map 中
    logFiles.forEachIndexed { index, file ->
        println("  [${index + 1}/${logFiles.size}] Parsing: ${file.name} (${file.length() / 1024} KB)")
        totalLines += parseLogFile(file, pidPackageMap, pidLineCount, tagLineCount)
    }

    if (totalLines == 0L) {
        println("No valid log lines found.")
        return
    }

    when (mode) {
        StatMode.BY_PACKAGE -> {
            val packageLineCount = mutableMapOf<String, Long>()
            var unknownLines = 0L
            for ((pid, count) in pidLineCount) {
                val packageName = pidPackageMap[pid]
                if (packageName != null) {
                    packageLineCount[packageName] = packageLineCount.getOrDefault(packageName, 0L) + count
                } else {
                    unknownLines += count
                }
            }
            if (unknownLines > 0) packageLineCount["(Unknown)"] = unknownLines
            printPackageStatistics(totalLines, pidLineCount, pidPackageMap, packageLineCount, aggregateByPackage)
        }
        StatMode.BY_TAG -> {
            printTagStatistics(totalLines, tagLineCount)
        }
    }
}

/**
 * 解析单个日志文件，将结果累加到传入的 Map 中。
 *
 * @param file           待解析的日志文件
 * @param pidPackageMap  pid -> packageName 映射（累加写入）
 * @param pidLineCount   pid -> 行数 映射（累加写入）
 * @param tagLineCount   tag -> 行数 映射（累加写入）
 * @return 本文件中解析到的有效日志行数
 */
fun parseLogFile(
    file: File,
    pidPackageMap: MutableMap<String, String>,
    pidLineCount: MutableMap<String, Long>,
    tagLineCount: MutableMap<String, Long>
): Long {
    var lines = 0L

    file.forEachLine { line ->
        // 忽略 "beginning of ..." 等分隔行
        if (line.contains("beginning of ")) return@forEachLine

        // 解析 Logcat 标准格式：MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG: message
        val parts = line.split(Regex(" +"), 7)
        val pid = parts.getOrNull(2) ?: return@forEachLine

        // 仅统计 PID 为纯数字的行（过滤无效行）
        if (!pid.all { it.isDigit() }) return@forEachLine

        lines++
        pidLineCount[pid] = pidLineCount.getOrDefault(pid, 0L) + 1L

        // 提取 TAG（index=5，去除末尾冒号）
        val tag = parts.getOrNull(5)?.trimEnd(':')?.trim() ?: "(NoTag)"
        tagLineCount[tag] = tagLineCount.getOrDefault(tag, 0L) + 1L

        // 尝试从当前行提取 PID -> 包名 信息
        START_PROC_REGEX.find(line)?.let { match ->
            pidPackageMap[match.groupValues[1]] = match.groupValues[2].trim()
        }
        CRASH_PROC_REGEX.find(line)?.let { match ->
            pidPackageMap[match.groupValues[2]] = match.groupValues[1].trim()
        }
    }

    return lines
}

/**
 * 按包名维度格式化并打印统计结果。
 *
 * @param aggregateByPackage true = 仅输出包名聚合表（不含 PID 明细）；false = 同时输出 PID 明细。
 */
fun printPackageStatistics(
    totalLines: Long,
    pidLineCount: Map<String, Long>,
    pidPackageMap: Map<String, String>,
    packageLineCount: Map<String, Long>,
    aggregateByPackage: Boolean = true
) {
    println("\n===== Log Statistics (By Package) =====")
    println("Total Valid Lines : $totalLines")
    println("Unique PIDs       : ${pidLineCount.size}")
    println("Identified Pkgs   : ${pidPackageMap.values.toSet().size}")
    println()

    val sortedPkgEntries = packageLineCount.entries.sortedByDescending { it.value }
    val colPkg = maxOf(sortedPkgEntries.maxOf { it.key.length }, 11)

    printTable(
        labelHeader = "PackageName",
        colWidth = colPkg,
        totalLines = totalLines,
        entries = sortedPkgEntries
    )

    // aggregateByPackage = false 时额外输出各 PID 明细
    if (!aggregateByPackage) {
        println("\n===== PID Detail =====")
        val pidHeader = "%8s  %-${colPkg}s  %8s  %8s".format("PID", "PackageName", "Lines", "Percent")
        println(pidHeader)
        println("-".repeat(pidHeader.length))
        pidLineCount.entries
            .sortedByDescending { it.value }
            .forEach { (pid, lines) ->
                val pkg = pidPackageMap[pid] ?: "(Unknown)"
                val percent = "%.2f%%".format(lines.toDouble() / totalLines * 100.0)
                println("%8s  %-${colPkg}s  %8d  %8s".format(pid, pkg, lines, percent))
            }
    }
}

/**
 * 按 TAG 维度格式化并打印统计结果。
 */
fun printTagStatistics(
    totalLines: Long,
    tagLineCount: Map<String, Long>
) {
    println("\n===== Log Statistics (By TAG) =====")
    println("Total Valid Lines : $totalLines")
    println("Unique TAGs       : ${tagLineCount.size}")
    println()

    val sortedTagEntries = tagLineCount.entries.sortedByDescending { it.value }
    val colTag = maxOf(sortedTagEntries.maxOf { it.key.length }, 3)

    printTable(
        labelHeader = "TAG",
        colWidth = colTag,
        totalLines = totalLines,
        entries = sortedTagEntries
    )
}

/** 通用表格打印：标签列 + Lines 列 + Percent 列，末行打印合计。 */
private fun printTable(
    labelHeader: String,
    colWidth: Int,
    totalLines: Long,
    entries: List<Map.Entry<String, Long>>
) {
    val header = "%-${colWidth}s  %8s  %8s".format(labelHeader, "Lines", "Percent")
    val divider = "-".repeat(header.length)
    println(header)
    println(divider)
    entries.forEach { (label, lines) ->
        val percent = "%.2f%%".format(lines.toDouble() / totalLines * 100.0)
        println("%-${colWidth}s  %8d  %8s".format(label, lines, percent))
    }
    println(divider)
    println("%-${colWidth}s  %8d  %8s".format("TOTAL", totalLines, "100.00%"))
}
