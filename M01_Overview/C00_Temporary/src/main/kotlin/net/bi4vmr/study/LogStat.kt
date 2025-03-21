package net.bi4vmr.study

import java.io.File

fun main() {
    val packagePidMap =
        extractPackagePidMap("/home/bi4vmr/Downloads/A")
//        extractPackagePidMap("/home/bi4vmr/Downloads/Logs/logcat-PLAIN-0142-0001-00010.545286-19700101_080615-.log")

    // 打印包名与PID列表的映射关系
//    println("Package Name -> PIDs:")
//    packagePidMap.forEach { (packageName, pids) ->
//        println("$packageName -> ${pids.joinToString(", ")}")
//    }

    val inputFile = File("/home/bi4vmr/Downloads/A")
//    val inputFile = File("/home/bi4vmr/Downloads/Logs/logcat-PLAIN-0142-0001-00010.545286-19700101_080615-.log")
    if (!inputFile.exists()) {
        println("Log file not found")
        return
    }

    val lines = inputFile.readLines()

    // 匹配PID的正则表达式（第三个字段）
    var totalLines = 0
    val pidStats = mutableMapOf<Int, Int>()

    lines.forEach { line ->
        // 忽略"beginning of main"等分隔符号
        if (line.contains("beginning of ")) {
            return@forEach
        }

        totalLines++

        // 识别PID所属
        val logEntity: List<String> = line.split(Regex(" +"), 7)
        val pid: Int = logEntity.getOrElse(2) { "-1" }.toInt()
        pidStats[pid] = pidStats.getOrDefault(pid, 0) + 1
    }

    if (totalLines == 0) {
        println("The log file is empty.")
        return
    }

    println("PID Statistics Result:")
    println("Total Log Lines: $totalLines\n")

    pidStats.entries
        .sortedByDescending { it.value }
        .forEach { (pid, count) ->
            var pkgName = "Unknown"
            run {
                packagePidMap.entries.forEach {
                    it.value.forEach { pid2 ->
                        if (pid2.toInt() == pid) {
                            pkgName = it.key
                            return@run
                        }
                    }
                }
            }

            val percentage = "%.2f".format((count / totalLines.toFloat()) * 100.0)
            println("PID $pid PKG $pkgName : $count lines ($percentage%)")
        }
}

//fun go(path:)

/**
 * 从日志中提取包名与PID列表的映射关系，包括Crash日志
 *
 * @param logFilePath 日志文件路径
 * @return 返回一个Map，其中键是包名，值是对应的PID列表
 */
fun extractPackagePidMap(logFilePath: String): Map<String, List<String>> {
    val packagePidMap = mutableMapOf<String, MutableList<String>>()
    val logFile = File(logFilePath)

    // 正则表达式匹配以下格式的日志：
    // 1. Start proc <PID>:<包名>
    // 2. Process: <包名>, PID: <PID> (Crash日志)
    val pidPackageRegex = Regex("(Start proc (\\d+):([^/]+)|Process: ([^,]+), PID: (\\d+))")

    logFile.forEachLine { line ->
        val matchResult = pidPackageRegex.find(line)
        if (matchResult != null) {
            val pid = matchResult.groupValues[2].ifEmpty { matchResult.groupValues[5] }
            val packageName = matchResult.groupValues[3].ifEmpty { matchResult.groupValues[4] }
            if (pid.isNotEmpty() && packageName.isNotEmpty()) {
                // 如果包名已存在，将PID添加到列表中；否则创建新列表
                packagePidMap.getOrPut(packageName) { mutableListOf() }.add(pid)
            }
        }
    }

    return packagePidMap
}
