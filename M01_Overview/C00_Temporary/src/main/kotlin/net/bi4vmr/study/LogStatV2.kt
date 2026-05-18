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

// 匹配 ANR 触发行中的包名："ANR in com.example.app"
private val ANR_IN_REGEX = Regex("""ANR in ([^\s(,]+)""")

// 匹配 ANR 块中 "PID: <num>" 行的消息部分
private val ANR_PID_LINE_REGEX = Regex("""^PID:\s*(\d+)""")

// 匹配 ANR 块中 "Reason: ..." 行的消息部分
private val ANR_REASON_REGEX = Regex("""^Reason:\s*(.+)""")

// 是否提取 Crash/ANR 日志片段并保存到文件
private const val EXTRACT_CRASH_ANR = true

// 提取时是否将相同的 Crash/ANR（相同异常类+相同调用栈顶帧 / 相同 ANR 原因）合并为一条记录
private const val AGGREGATE_CRASH_ANR = true

// DeepSeek API Key（优先读取 local.properties 中的 deepseek.api.key，此处留空即可）
private const val DEEPSEEK_API_KEY = ""

// DeepSeek 模型名称（优先读取 local.properties 中的 deepseek.model，此处留空即可）
private const val DEEPSEEK_MODEL = ""

/** 统计维度 */
enum class StatMode {
    /** 按包名聚合（需从日志中识别 PID→包名 映射） */
    BY_PACKAGE,

    /** 按日志 TAG 聚合 */
    BY_TAG
}

/** Crash 日志片段 */
data class CrashRecord(
    val pid: String,
    val packageName: String,
    val timestamp: String,
    val lines: List<String>
)

/** ANR 日志片段 */
data class AnrRecord(
    val pid: String,
    val packageName: String,
    val timestamp: String,
    val reason: String,
    val lines: List<String>
)

// 内部状态类（仅用于解析过程中的临时追踪，不对外暴露）
private class CrashBlockState(val timestamp: String) {
    val lines: MutableList<String> = mutableListOf()
}

private class AnrBlockState(val timestamp: String) {
    val lines: MutableList<String> = mutableListOf()
    var anrPid: String = ""
    var packageName: String = ""
    var reason: String = ""
}

fun main() {
    // 留空时自动从 local.properties 读取对应配置项
    val apiKey = DEEPSEEK_API_KEY.ifBlank { loadDeepSeekApiKey() }
    val model = DEEPSEEK_MODEL.ifBlank { loadDeepSeekModel() }
    runLogStat(
        LOG_FILE_PATH, STAT_MODE,
        aggregateByPackage = true,
        extractCrashAnr = EXTRACT_CRASH_ANR,
        aggregateCrashAnr = AGGREGATE_CRASH_ANR,
        deepSeekApiKey = apiKey,
        deepSeekModel = model
    )
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
 * @param extractCrashAnr    true = 提取 Crash/ANR 日志片段保存到文件，并输出按 PID 统计的计数表。
 * @param aggregateCrashAnr  仅在 extractCrashAnr=true 时有效：将相同异常/原因的 Crash/ANR 合并，
 *                           每种问题只保留一个代表性片段文件，文件头注明聚合数量与涉及 PID。
 * @param deepSeekApiKey     非空时对每个保存的片段文件调用 DeepSeek API 进行分析，
 *                           结果保存为同名 `.analysis.md`；已存在则跳过。
 * @param deepSeekModel      调用 DeepSeek API 时使用的模型名称；
 *                           留空时从 local.properties 读取，再空则使用默认值。
 */
fun runLogStat(
    path: String,
    mode: StatMode,
    aggregateByPackage: Boolean = true,
    extractCrashAnr: Boolean = false,
    aggregateCrashAnr: Boolean = false,
    deepSeekApiKey: String = "",
    deepSeekModel: String = ""
) {
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
    val allCrashRecords = if (extractCrashAnr) mutableListOf<CrashRecord>() else null
    val allAnrRecords = if (extractCrashAnr) mutableListOf<AnrRecord>() else null
    var totalLines = 0L

    // 逐文件解析，数据累加到同一份 Map 中
    logFiles.forEachIndexed { index, file ->
        println("  [${index + 1}/${logFiles.size}] Parsing: ${file.name} (${file.length() / 1024} KB)")
        totalLines += parseLogFile(file, pidPackageMap, pidLineCount, tagLineCount, allCrashRecords, allAnrRecords)
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

    if (extractCrashAnr && allCrashRecords != null && allAnrRecords != null) {
        // 输出目录与输入路径同级，避免下次扫描时被当作日志文件读入
        val outputDir = if (input.isDirectory)
            File(input.parentFile ?: input, "${input.name}_crashanr")
        else
            File(input.parentFile ?: File("."), "${input.nameWithoutExtension}_crashanr")
        val effectiveModel = deepSeekModel.ifBlank { loadDeepSeekModel() }
        saveCrashAnrRecords(outputDir, allCrashRecords, allAnrRecords, aggregateCrashAnr, deepSeekApiKey, effectiveModel)
        printCrashAnrStatistics(allCrashRecords, allAnrRecords, aggregateCrashAnr)
    }
}

/**
 * 解析单个日志文件，将结果累加到传入的 Map 中。
 * 若传入 [crashRecords] 或 [anrRecords]，则同时提取对应的日志片段。
 *
 * @param file           待解析的日志文件
 * @param pidPackageMap  pid -> packageName 映射（累加写入）
 * @param pidLineCount   pid -> 行数 映射（累加写入）
 * @param tagLineCount   tag -> 行数 映射（累加写入）
 * @param crashRecords   非 null 时收集 Crash 片段（累加写入）
 * @param anrRecords     非 null 时收集 ANR 片段（累加写入）
 * @return 本文件中解析到的有效日志行数
 */
fun parseLogFile(
    file: File,
    pidPackageMap: MutableMap<String, String>,
    pidLineCount: MutableMap<String, Long>,
    tagLineCount: MutableMap<String, Long>,
    crashRecords: MutableList<CrashRecord>? = null,
    anrRecords: MutableList<AnrRecord>? = null
): Long {
    var lines = 0L
    // per-PID crash 块累积器（key = 崩溃进程 PID）
    val activeCrash = mutableMapOf<String, CrashBlockState>()
    // per-AM-PID ANR 块累积器（key = ActivityManager 进程的 PID）
    val activeAnr = mutableMapOf<String, AnrBlockState>()

    fun finalizeCrash(pid: String) {
        val state = activeCrash.remove(pid) ?: return
        if (state.lines.isEmpty()) return
        val pkg = pidPackageMap[pid] ?: "(Unknown)"
        crashRecords?.add(CrashRecord(pid, pkg, state.timestamp, state.lines.toList()))
    }

    fun finalizeAnr(amPid: String) {
        val state = activeAnr.remove(amPid) ?: return
        if (state.lines.isEmpty()) return
        val effectivePid = state.anrPid.ifEmpty { amPid }
        val pkg = state.packageName.ifEmpty { pidPackageMap[effectivePid] ?: "(Unknown)" }
        anrRecords?.add(AnrRecord(effectivePid, pkg, state.timestamp, state.reason, state.lines.toList()))
    }

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

        val timestamp = "${parts.getOrNull(0) ?: ""} ${parts.getOrNull(1) ?: ""}"
        val message = parts.getOrNull(6) ?: ""

        // 尝试从当前行提取 PID -> 包名 信息
        START_PROC_REGEX.find(line)?.let { match ->
            pidPackageMap[match.groupValues[1]] = match.groupValues[2].trim()
        }
        CRASH_PROC_REGEX.find(line)?.let { match ->
            pidPackageMap[match.groupValues[2]] = match.groupValues[1].trim()
        }

        // ── Crash 块追踪（tag: AndroidRuntime）─────────────────────────────────
        if (crashRecords != null) {
            when {
                tag == "AndroidRuntime" && message.contains("FATAL EXCEPTION") -> {
                    finalizeCrash(pid)  // 若该 PID 已有未完成的块，先保存
                    activeCrash[pid] = CrashBlockState(timestamp).also { it.lines.add(line) }
                }
                tag == "AndroidRuntime" && activeCrash.containsKey(pid) -> {
                    activeCrash[pid]!!.lines.add(line)
                }
                activeCrash.containsKey(pid) -> {
                    // 该 PID 出现了非 AndroidRuntime 行，视为 crash 块结束
                    finalizeCrash(pid)
                }
            }
        }

        // ── ANR 块追踪（tag: ActivityManager / am_anr 事件日志）────────────────
        if (anrRecords != null) {
            when {
                tag == "ActivityManager" && ANR_IN_REGEX.containsMatchIn(message) -> {
                    finalizeAnr(pid)
                    val pkg = ANR_IN_REGEX.find(message)?.groupValues?.get(1) ?: ""
                    activeAnr[pid] = AnrBlockState(timestamp).also {
                        it.lines.add(line)
                        it.packageName = pkg
                    }
                }
                tag == "ActivityManager" && activeAnr.containsKey(pid) -> {
                    val state = activeAnr[pid]!!
                    state.lines.add(line)
                    if (state.anrPid.isEmpty()) {
                        ANR_PID_LINE_REGEX.find(message)?.let { state.anrPid = it.groupValues[1] }
                    }
                    if (state.reason.isEmpty()) {
                        ANR_REASON_REGEX.find(message)?.let { state.reason = it.groupValues[1].trim() }
                    }
                }
                activeAnr.containsKey(pid) -> {
                    // 该 PID 出现了非 ActivityManager 行，视为 ANR 块结束
                    finalizeAnr(pid)
                }
            }

            // am_anr 事件日志（来自 events buffer）：格式 "[userId,anrPid,pkg,...]"
            if (tag == "am_anr") {
                val m = Regex("""\[(\d+),(\d+),([^,\]]+)""").find(message)
                if (m != null) {
                    val anrPid = m.groupValues[2]
                    val anrPkg = m.groupValues[3]
                    pidPackageMap[anrPid] = anrPkg
                    anrRecords.add(AnrRecord(anrPid, anrPkg, timestamp, "(event log)", listOf(line)))
                }
            }
        }
    }

    // 文件末尾：将所有未关闭的块强制终止并保存
    activeCrash.keys.toList().forEach { finalizeCrash(it) }
    activeAnr.keys.toList().forEach { finalizeAnr(it) }

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

/**
 * 将提取到的 Crash / ANR 片段写入文件。
 * 目录结构：[outputDir]/crash/ 和 [outputDir]/anr/
 *
 * 非聚合模式：每条记录保存为独立文件，文件名含包名、PID 和序号。
 * 聚合模式  ：相同问题（相同异常类+顶帧 / 相同 ANR 原因）合并为一个文件，
 *             文件头注明出现次数、涉及 PID 列表、首末时间戳，正文为代表性片段。
 *
 * 若 [deepSeekApiKey] 非空，在保存每个文件后调用 DeepSeek API 进行 AI 分析，
 * 结果保存为同名 `.analysis.md`（已存在则跳过）。
 */
fun saveCrashAnrRecords(
    outputDir: File,
    crashRecords: List<CrashRecord>,
    anrRecords: List<AnrRecord>,
    aggregate: Boolean = false,
    deepSeekApiKey: String = "",
    deepSeekModel: String = ""
) {
    if (crashRecords.isEmpty() && anrRecords.isEmpty()) {
        println("\nNo Crash or ANR events found.")
        return
    }

    val doAi = deepSeekApiKey.isNotBlank()
    var aiCallCount = 0

    fun sanitize(name: String) = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    fun maybeAnalyzeCrash(logFile: File, record: CrashRecord) {
        if (!doAi) return
        analyzeAndSaveResult(logFile, deepSeekApiKey, deepSeekModel, { key ->
            analyzeCrashWithDeepSeek(record, key, deepSeekModel)
        }, needSleep = aiCallCount++ > 0)
    }

    fun maybeAnalyzeAnr(logFile: File, record: AnrRecord) {
        if (!doAi) return
        analyzeAndSaveResult(logFile, deepSeekApiKey, deepSeekModel, { key ->
            analyzeAnrWithDeepSeek(record, key, deepSeekModel)
        }, needSleep = aiCallCount++ > 0)
    }

    if (crashRecords.isNotEmpty()) {
        val crashDir = File(outputDir, "crash").also { it.mkdirs() }
        if (aggregate) {
            crashRecords.groupBy { crashSignature(it) }.entries
                .sortedByDescending { it.value.size }
                .forEachIndexed { idx, (sig, group) ->
                    val rep = group.first()
                    val exShort = sig.split("|").getOrNull(1)
                        ?.substringAfterLast('.')?.take(40) ?: "crash"
                    val name = "crash_${sanitize(rep.packageName)}_${sanitize(exShort)}" +
                        "_${group.size}x_${"${idx + 1}".padStart(3, '0')}.txt"
                    val header = buildAggregatedHeader(
                        kind = "Crash",
                        sig = sig,
                        group = group.map { Triple(it.pid, it.packageName, it.timestamp) }
                    )
                    val logFile = File(crashDir, name)
                    logFile.writeText(header + rep.lines.joinToString("\n"))
                    maybeAnalyzeCrash(logFile, rep)
                }
        } else {
            crashRecords.groupBy { it.pid }.forEach { (pid, records) ->
                records.forEachIndexed { idx, record ->
                    val name = "crash_${sanitize(record.packageName)}_pid${pid}" +
                        "_${"${idx + 1}".padStart(3, '0')}.txt"
                    val logFile = File(crashDir, name)
                    logFile.writeText(record.lines.joinToString("\n"))
                    maybeAnalyzeCrash(logFile, record)
                }
            }
        }
        val label = if (aggregate) "${crashRecords.groupBy { crashSignature(it) }.size} group(s)" else "${crashRecords.size} record(s)"
        println("\nCrash files → ${File(outputDir, "crash").absolutePath}  ($label)")
    }

    if (anrRecords.isNotEmpty()) {
        val anrDir = File(outputDir, "anr").also { it.mkdirs() }
        if (aggregate) {
            anrRecords.groupBy { anrSignature(it) }.entries
                .sortedByDescending { it.value.size }
                .forEachIndexed { idx, (sig, group) ->
                    val rep = group.first()
                    val reasonShort = rep.reason.take(40).replace(Regex("[^a-zA-Z0-9 _-]"), "").trimEnd()
                    val name = "anr_${sanitize(rep.packageName)}_${sanitize(reasonShort)}" +
                        "_${group.size}x_${"${idx + 1}".padStart(3, '0')}.txt"
                    val header = buildAggregatedHeader(
                        kind = "ANR",
                        sig = sig,
                        group = group.map { Triple(it.pid, it.packageName, it.timestamp) }
                    )
                    val logFile = File(anrDir, name)
                    logFile.writeText(header + rep.lines.joinToString("\n"))
                    maybeAnalyzeAnr(logFile, rep)
                }
        } else {
            anrRecords.groupBy { it.pid }.forEach { (pid, records) ->
                records.forEachIndexed { idx, record ->
                    val name = "anr_${sanitize(record.packageName)}_pid${pid}" +
                        "_${"${idx + 1}".padStart(3, '0')}.txt"
                    val logFile = File(anrDir, name)
                    logFile.writeText(record.lines.joinToString("\n"))
                    maybeAnalyzeAnr(logFile, record)
                }
            }
        }
        val label = if (aggregate) "${anrRecords.groupBy { anrSignature(it) }.size} group(s)" else "${anrRecords.size} record(s)"
        println("ANR files   → ${File(outputDir, "anr").absolutePath}  ($label)")
    }

    if (doAi) println("DeepSeek analysis: $aiCallCount file(s) processed.")
}

/** 构建聚合文件顶部的注释头块。 */
private fun buildAggregatedHeader(
    kind: String,
    sig: String,
    group: List<Triple<String, String, String>>   // pid, packageName, timestamp
): String {
    val sep = "#" + "─".repeat(64)
    val pids = group.map { it.first }.distinct().joinToString(", ")
    val sigParts = sig.split("|")
    return buildString {
        appendLine(sep)
        appendLine("# [$kind] Occurrences : ${group.size}")
        appendLine("# Package      : ${group.first().second}")
        if (sigParts.size >= 2) appendLine("# Identifier   : ${sigParts.drop(1).joinToString(" | ")}")
        appendLine("# PIDs         : $pids")
        appendLine("# First seen   : ${group.first().third}")
        appendLine("# Last seen    : ${group.last().third}")
        appendLine("$sep\n")
    }
}

/**
 * 打印 Crash / ANR 统计表。
 *
 * 非聚合模式：按 PID 聚合计数。
 * 聚合模式  ：按问题特征（异常类+顶帧 / ANR 原因）聚合，展示每种问题的出现次数与涉及 PID。
 */
fun printCrashAnrStatistics(
    crashRecords: List<CrashRecord>,
    anrRecords: List<AnrRecord>,
    aggregate: Boolean = false
) {
    println("\n===== Crash / ANR Statistics =====")

    if (crashRecords.isNotEmpty()) {
        println("\n----- Crash Summary (Total: ${crashRecords.size}) -----")
        if (aggregate) {
            val groups = crashRecords.groupBy { crashSignature(it) }
                .entries.sortedByDescending { it.value.size }
            val colId = maxOf(groups.maxOf { it.key.length }, 10).coerceAtMost(60)
            val hdr = "%5s  %-${colId}s  %s".format("Count", "Identifier (pkg|exception|topFrame)", "PIDs")
            println(hdr)
            println("-".repeat(hdr.length))
            groups.forEach { (sig, recs) ->
                val pids = recs.map { it.pid }.distinct().joinToString(",")
                println("%5d  %-${colId}s  %s".format(recs.size, sig.take(colId), pids))
            }
        } else {
            val byPid = crashRecords.groupBy { it.pid }
            val colPkg = maxOf(crashRecords.maxOf { it.packageName.length }, 11)
            val hdr = "%8s  %-${colPkg}s  %5s".format("PID", "PackageName", "Count")
            println(hdr)
            println("-".repeat(hdr.length))
            byPid.entries.sortedByDescending { it.value.size }.forEach { (pid, recs) ->
                println("%8s  %-${colPkg}s  %5d".format(pid, recs.first().packageName, recs.size))
            }
        }
    } else {
        println("No Crash events found.")
    }

    if (anrRecords.isNotEmpty()) {
        println("\n----- ANR Summary (Total: ${anrRecords.size}) -----")
        if (aggregate) {
            val groups = anrRecords.groupBy { anrSignature(it) }
                .entries.sortedByDescending { it.value.size }
            val colId = maxOf(groups.maxOf { it.key.length }, 10).coerceAtMost(60)
            val hdr = "%5s  %-${colId}s  %s".format("Count", "Identifier (pkg|reason)", "PIDs")
            println(hdr)
            println("-".repeat(hdr.length))
            groups.forEach { (sig, recs) ->
                val pids = recs.map { it.pid }.distinct().joinToString(",")
                println("%5d  %-${colId}s  %s".format(recs.size, sig.take(colId), pids))
            }
        } else {
            val byPid = anrRecords.groupBy { it.pid }
            val colPkg = maxOf(anrRecords.maxOf { it.packageName.length }, 11)
            val hdr = "%8s  %-${colPkg}s  %5s  %s".format("PID", "PackageName", "Count", "LastReason")
            println(hdr)
            println("-".repeat(hdr.length))
            byPid.entries.sortedByDescending { it.value.size }.forEach { (pid, recs) ->
                println("%8s  %-${colPkg}s  %5d  %s".format(pid, recs.first().packageName, recs.size, recs.last().reason.take(60)))
            }
        }
    } else {
        println("No ANR events found.")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 聚合辅助函数
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 提取 Crash 记录的聚合签名：`<package>|<exceptionClass>|<topFrame>`
 * exceptionClass：第一个包含 Exception/Error 且不是 "at" 行的消息
 * topFrame      ：第一个 "at" 行的方法全限定名（不含括号内的文件信息）
 */
private fun crashSignature(record: CrashRecord): String {
    var exClass = ""
    var topFrame = ""
    for (line in record.lines) {
        val msg = line.split(Regex(" +"), 7).getOrNull(6) ?: continue
        val trimmed = msg.trimStart()
        if (exClass.isEmpty()
            && !trimmed.startsWith("at ")
            && !trimmed.startsWith("FATAL")
            && !trimmed.startsWith("Process:")
            && (trimmed.contains("Exception") || trimmed.contains("Error"))
        ) {
            exClass = trimmed.substringBefore(':').trim()
        }
        if (topFrame.isEmpty() && trimmed.startsWith("at ")) {
            topFrame = trimmed.removePrefix("at ").substringBefore('(')
        }
        if (exClass.isNotEmpty() && topFrame.isNotEmpty()) break
    }
    return "${record.packageName}|${exClass.ifEmpty { "(unknown)" }}|${topFrame}"
}

/**
 * 提取 ANR 记录的聚合签名：`<package>|<normalizedReason>`
 * 将原因中的纯数字替换为 `#`，避免因不同 PID / 超时毫秒数导致误判为不同问题。
 */
private fun anrSignature(record: AnrRecord): String {
    val normalized = record.reason
        .replace(Regex("""0x[0-9a-fA-F]+"""), "0x#")
        .replace(Regex("""\b\d+\b"""), "#")
        .trim()
    return "${record.packageName}|${normalized.ifEmpty { "(unknown)" }}"
}
