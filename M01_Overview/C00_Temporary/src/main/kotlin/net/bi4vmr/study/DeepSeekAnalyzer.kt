package net.bi4vmr.study

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties

private const val DEEPSEEK_API_PATH = "/v1/chat/completions"
private const val DEEPSEEK_MODEL_DEFAULT = "DeepSeek-V32"

// 单次请求的日志行最大字符数（约 2000 token）
private const val MAX_LOG_CHARS = 8000

// 两次 API 调用之间的间隔（毫秒），防止触发速率限制
private const val API_CALL_INTERVAL_MS = 800L

/**
 * 从项目根目录的 local.properties 读取 DeepSeek 配置。
 * local.properties 已在 .gitignore 中，密钥不会提交到版本库。
 *
 * 支持的 key：
 *   deepseek.api.url   — API 域名（含协议，不含路径）
 *   deepseek.api.key   — Bearer Token
 *   deepseek.model     — 模型名称（默认 DeepSeek-V32）
 */
private val localConfig: Properties by lazy {
    Properties().also { props ->
        // 依次在工作目录和项目根目录查找 local.properties
        val candidates = listOf(
            File(System.getProperty("user.dir"), "local.properties"),
            File(System.getProperty("user.dir")).parentFile?.let { File(it, "local.properties") },
            // Gradle 多模块项目：向上最多 3 级查找
            generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
                .take(4).map { File(it, "local.properties") }.firstOrNull { it.exists() }
        )
        candidates.filterNotNull().firstOrNull { it.exists() }
            ?.inputStream()?.use { props.load(it) }
    }
}

/** 从 local.properties 读取 API base URL，未配置时返回 null。 */
fun loadDeepSeekApiUrl(): String? = localConfig.getProperty("deepseek.api.url")?.trimEnd('/')

/** 从 local.properties 读取 API Key，未配置时返回空字符串。 */
fun loadDeepSeekApiKey(): String = localConfig.getProperty("deepseek.api.key")?.trim() ?: ""

/** 从 local.properties 读取模型名称，未配置时返回默认值。 */
fun loadDeepSeekModel(): String =
    localConfig.getProperty("deepseek.model")?.trim()?.ifBlank { null } ?: DEEPSEEK_MODEL_DEFAULT

private val gson = Gson()

/**
 * 使用 DeepSeek API 分析 Crash 日志片段，返回 Markdown 格式的分析报告。
 * 失败时返回包含错误信息的字符串，不抛出异常。
 */
fun analyzeCrashWithDeepSeek(record: CrashRecord, apiKey: String, model: String): String {
    val logSnippet = record.lines.joinToString("\n").take(MAX_LOG_CHARS)
    val prompt = """
你是资深 Android 开发工程师，请分析以下 Crash 日志，用**中文**给出结构化报告：

## 1. 崩溃根本原因
（精准定位到具体异常类型和代码位置）

## 2. 涉及的模块 / 组件

## 3. 修复建议
（可附伪代码或关键修改点）

---
**应用包名**：${record.packageName}
**PID**：${record.pid}
**首次出现**：${record.timestamp}

**日志片段**：
```
$logSnippet
```
""".trimIndent()
    return callDeepSeek(prompt, apiKey, model)
}

/**
 * 使用 DeepSeek API 分析 ANR 日志片段，返回 Markdown 格式的分析报告。
 * 失败时返回包含错误信息的字符串，不抛出异常。
 */
fun analyzeAnrWithDeepSeek(record: AnrRecord, apiKey: String, model: String): String {
    val logSnippet = record.lines.joinToString("\n").take(MAX_LOG_CHARS)
    val prompt = """
你是资深 Android 开发工程师，请分析以下 ANR 日志，用**中文**给出结构化报告：

## 1. ANR 根本原因

## 2. 主线程被阻塞的具体原因

## 3. 修复建议
（可附伪代码或关键修改点）

---
**应用包名**：${record.packageName}
**PID**：${record.pid}
**ANR 原因**：${record.reason}
**首次出现**：${record.timestamp}

**日志片段**：
```
$logSnippet
```
""".trimIndent()
    return callDeepSeek(prompt, apiKey, model)
}

// ─────────────────────────────────────────────────────────────────────────────

private fun callDeepSeek(prompt: String, apiKey: String, model: String): String {
    val baseUrl = loadDeepSeekApiUrl() ?: return "Config error: deepseek.api.url not set in local.properties"
    val payload = gson.toJson(
        mapOf(
            "model" to model,
            "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
            "temperature" to 0.3
        )
    )
    return try {
        val conn = URL("$baseUrl$DEEPSEEK_API_PATH").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.connectTimeout = 30_000
        conn.readTimeout = 120_000
        conn.doOutput = true
        conn.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }

        val code = conn.responseCode
        val body = if (code in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } else {
            val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: "(no body)"
            return "API Error $code: $err"
        }
        gson.fromJson(body, JsonObject::class.java)
            .getAsJsonArray("choices")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("message")
            ?.get("content")?.asString
            ?: "Parse error: unexpected response format\n\nRaw response:\n$body"
    } catch (e: Exception) {
        "Request failed: ${e.message}"
    }
}

/**
 * 调用 DeepSeek API 分析日志并将结果保存到与 [logFile] 同目录的 `.analysis.md` 文件。
 * 若分析文件已存在则跳过（避免重复计费）。
 *
 * @param logFile    已保存的日志 txt 文件
 * @param apiKey     DeepSeek API Key（优先使用；为空时回退到 local.properties）
 * @param analyze    实际调用 API 的 lambda（接收 apiKey，返回分析文本）
 * @param needSleep  是否在调用前等待 [API_CALL_INTERVAL_MS]（首次调用传 false）
 */
fun analyzeAndSaveResult(
    logFile: File,
    apiKey: String,
    model: String,
    analyze: (String) -> String,
    needSleep: Boolean = true
) {
    val effectiveKey = apiKey.ifBlank { loadDeepSeekApiKey() }
    if (effectiveKey.isBlank()) return

    val analysisFile = File(logFile.parent, "${logFile.nameWithoutExtension}.analysis.md")
    if (analysisFile.exists()) {
        println("    [DeepSeek] Skip (already exists): ${analysisFile.name}")
        return
    }
    if (needSleep) Thread.sleep(API_CALL_INTERVAL_MS)
    print("    [DeepSeek] Analyzing ${logFile.name} ...")
    System.out.flush()
    val result = analyze(effectiveKey)
    analysisFile.writeText(
        "# DeepSeek Analysis\n\n" +
                "> Generated by DeepSeek · Model: $model\n\n" +
                result
    )
    println(" ✓  →  ${analysisFile.name}")
}
