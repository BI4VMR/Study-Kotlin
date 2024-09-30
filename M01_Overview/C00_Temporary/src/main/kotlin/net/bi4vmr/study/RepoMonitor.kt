package net.bi4vmr.study

import cn.zhxu.okhttps.HTTP
import cn.zhxu.okhttps.HttpResult
import cn.zhxu.okhttps.Process
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

/**
 * 轮询Maven仓库中的指定产物，并触发Jenkins构建。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
// 轮询间隔（秒）
const val CHECK_INTERVAL = 180

const val MAVEN_USERNAME = "thundersoft_upload"
const val MAVEN_PASSWORD = "ThunderSoft#2024"
const val MAVEN_REPO = "pateo-thundersoft"
const val MAVEN_GROUP = "com.hyundai.module"
const val MAVEN_ARTIFACT = "appstore"
const val MAVEN_URL =
    "http://10.100.0.2:8081/service/rest/v1/search?repository={repo}&maven.groupId={group}&maven.artifactId={artifact}"

const val JENKINS_URL =
    "http://10.10.96.190:8080/job/PRJ_HMTC_APP_MainInteraction"
const val JENKINS_USERNAME = "yigangzhan"
const val JENKINS_TOKEN = "1138afb0901111c8a04fac7a2c2dfd2ae0"

val client: HTTP = HTTP.builder().build()

// 上次轮循到的最大版本号
private var lastVersion: String = ""

fun main() = runBlocking {
    // 向全局变量填充初始版本号
    checkNewVersion()

    while (true) {
        try {
            // 间隔时长
            delay(CHECK_INTERVAL * 1000L)

            // 检测仓库中是否有新的版本
            val result: Boolean = checkNewVersion()
            if (result) {
                println("检测到新的版本，开始编译。")
                // 如果存在新的版本，则触发Jenkins构建。
                val buildID: String? = startBuild()
                println("任务已提交。 BuildID:[$buildID]")
                // 如果没有获取到ID，则放弃本次任务，进入下一轮循环。
                if (buildID == null) continue

                var state: BuildResult? = null
                // 轮循任务状态
                for (i in 1..15) {
                    delay(CHECK_INTERVAL * 1000L)

                    state = getBuildResult(buildID)
                    println("第 $i 次检查任务状态：$state")
                    // 结果不为空，说明任务执行完毕，退出循环。
                    if (state != null) {
                        break
                    }
                }

                // 建立结果目录
                val outPath = File("/mnt/alist/天翼云盘/Work/Hyundai8295/$buildID")
                outPath.mkdir()

                when (state) {
                    BuildResult.SUCCEESS -> {
                        downloadAPK(buildID, outPath)
                        downloadLog(buildID, outPath)
                        File(outPath, "AAR版本：$lastVersion，构建结果：成功").createNewFile()
                    }

                    BuildResult.FAILURE -> {
                        downloadLog(buildID, outPath)
                        File(outPath, "AAR版本：$lastVersion，构建结果：失败").createNewFile()
                    }

                    BuildResult.ABORTED -> {
                        File(outPath, "AAR版本：$lastVersion，构建结果：中途取消").createNewFile()
                    }
                    // 轮循超时
                    else -> {
                        File(outPath, "AAR版本：$lastVersion，构建结果：KT脚本轮循超时").createNewFile()
                    }
                }
            } else {
                println("没有新的版本。")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 检查指定的Maven组件是否存在新版本
suspend fun checkNewVersion(): Boolean {
    return suspendCoroutine {
        val result: HttpResult = client.sync(MAVEN_URL)
            .addPathPara("repo", MAVEN_REPO)
            .addPathPara("group", MAVEN_GROUP)
            .addPathPara("artifact", MAVEN_ARTIFACT)
            .basicAuth(MAVEN_USERNAME, MAVEN_PASSWORD)
            .get()

        when (result.state) {
            /* 请求成功 */
            HttpResult.State.RESPONSED -> {
                val body: String = result.body.toString()
                val obj: JsonObject = JsonParser.parseString(body).asJsonObject
                if (obj.has("items")) {
                    val versionList: MutableList<String> = mutableListOf()
                    val items: JsonArray = obj.getAsJsonArray("items")
                    for (item in items) {
                        if (item is JsonObject && item.has("version")) {
                            versionList.add(item.get("version").asString)
                        }
                    }

                    val maxVersion: String = getLatestVersion(versionList)
                    println("当前最新版本：$maxVersion，上次检查时的最新版本：$lastVersion。")
                    if (maxVersion != lastVersion) {
                        lastVersion = maxVersion
                        it.resume(true)
                    } else {
                        it.resume(false)
                    }
                } else {
                    System.err.println("Maven仓库返回的JSON无法解析！")
                    System.err.println(body)
                    it.resume(false)
                }
            }
            /* 请求失败 */
            else -> {
                System.err.println("Maven仓库不可访问，状态：${result.state}")
                it.resume(false)
            }
        }
    }
}

// 获取最新的版本号
fun getLatestVersion(versionList: List<String>): String {
    val list = versionList.sortedWith { p0, p1 -> compareVersions(p0, p1) }
        .reversed()
    return if (list.isNotEmpty()) list[0] else ""
}

// 比较版本号
fun compareVersions(version1: String, version2: String): Int {
    // 去掉数字序号之后的内容
    fun removeSuffix(v: String): String {
        val i = v.indexOf('_')
        return if (i != -1) {
            v.substring(0, i)
        } else {
            v
        }
    }

    val v1 = removeSuffix(version1)
    val v2 = removeSuffix(version2)
    val list1 = v1.split('.').map { it.toInt() }
    val list2 = v2.split('.').map { it.toInt() }

    if (list1.size < 3 || list2.size < 3) {
        System.err.println("版本号格式不正确！")
        return 0
    }

    for (i in 0 until 3) {
        if (list1[i] > list2[i]) return 1
        if (list1[i] < list2[i]) return -1
    }
    return 0
}

/**
 * 触发构建。
 *
 * @return BuildID。
 */
suspend fun startBuild(): String? {
    return suspendCoroutine {
        val result: HttpResult = client.sync("$JENKINS_URL/buildWithParameters?TARGET_MODULE=PateoLauncher")
            .basicAuth(JENKINS_USERNAME, JENKINS_TOKEN)
            .post()

        when (result.state) {
            /* 请求成功 */
            HttpResult.State.RESPONSED -> {
                // 等待10秒，静默期结束后再检查任务状态。
                runBlocking { delay(10 * 1000L) }

                var buildID: String?
                var times = 1
                while (true) {
                    // 获取JSON格式的任务详情
                    val requestURI = "${result.getHeader("Location")}api/json"
                    buildID = runBlocking { getBuildID(requestURI) }
                    if (buildID != "-1") {
                        break
                    } else {
                        println("Jenkins节点暂无可用的执行器，稍后将进行第 $times 次重试。")
                        times++
                        runBlocking { delay(CHECK_INTERVAL * 1000L) }
                    }
                }
                it.resume(buildID)
            }
            /* 请求失败 */
            else -> {
                System.err.println("Jenkins服务器不可访问，状态：${result.state}")
                it.resume(null)
            }
        }
    }
}

// 获取BuildID
suspend fun getBuildID(url: String): String? {
    return suspendCoroutine {
        println("开始获取BuildID...")
        val result: HttpResult = client.sync(url)
            .basicAuth(JENKINS_USERNAME, JENKINS_TOKEN)
            .get()

        when (result.state) {
            /* 请求成功 */
            HttpResult.State.RESPONSED -> {
                val body: String = result.body.toString()
                val obj: JsonObject = JsonParser.parseString(body).asJsonObject

                // 等待可用的执行器
                if (obj.has("why") && obj.get("why").asString.startsWith("Waiting for next available executor")) {
                    it.resume("-1")
                    return@suspendCoroutine
                }

                if (obj.has("executable")) {
                    val item: JsonObject = obj.getAsJsonObject("executable")
                    val id: String = item.get("number").asString
                    it.resume(id)
                } else {
                    System.err.println("Jenkins返回的消息格式与预期不符，无法解析！")
                    System.err.println(body)
                    it.resume(null)
                }
            }
            /* 请求失败 */
            else -> {
                System.err.println("Jenkins服务器不可访问，状态：${result.state}")
                it.resume(null)
            }
        }
    }
}

enum class BuildResult {
    SUCCEESS,
    FAILURE,
    ABORTED;
}

// 获取构建状态
suspend fun getBuildResult(buildID: String): BuildResult? {
    return suspendCoroutine {
        val result: HttpResult = client.sync("$JENKINS_URL/$buildID/api/json")
            .basicAuth(JENKINS_USERNAME, JENKINS_TOKEN)
            .get()

        when (result.state) {
            /* 请求成功 */
            HttpResult.State.RESPONSED -> {
                val obj: JsonObject = JsonParser.parseString(result.body.toString())
                    .asJsonObject
                if (obj.has("result")) {
                    // 构建未完成，结果字段为NULL。
                    if (obj.get("result").isJsonNull) {
                        it.resume(null)
                    } else {
                        val state: String = obj.get("result").asString
                        val enum: BuildResult = when (state) {
                            "SUCCESS" -> BuildResult.SUCCEESS
                            "FAILURE" -> BuildResult.FAILURE
                            "ABORTED" -> BuildResult.ABORTED
                            else -> BuildResult.FAILURE
                        }
                        it.resume(enum)
                    }
                } else {
                    it.resume(null)
                }
            }
            /* 请求失败 */
            else -> {
                System.err.println("Jenkins服务器不可访问，状态：${result.state}")
                it.resume(null)
            }
        }
    }
}

// 下载产物到本地
fun downloadAPK(buildID: String, dstPath: File) {
    println("开始下载产物...")
    client.sync("$JENKINS_URL/$buildID/artifact/OUT_APP/app/HyundaiCarLauncher.apk")
        .basicAuth(JENKINS_USERNAME, JENKINS_TOKEN)
        .get()
        .body
        .stepRate(0.2)
        .setOnProcess { p: Process -> println("产物下载中... ${(p.rate * 100).roundToInt()} %") }
        .toFile("$dstPath${File.separator}HyundaiCarLauncher.apk")
        .setOnSuccess { file: File -> println("产物下载完成！ ${file.absolutePath}") }
        .setOnFailure {
            System.err.println("产物下载失败！")
            it.exception.printStackTrace()
        }
        .start()
}

// 下载日志到本地
fun downloadLog(buildID: String, dstPath: File) {
    println("开始下载日志...")
    client.sync("$JENKINS_URL/$buildID/consoleText")
        .basicAuth(JENKINS_USERNAME, JENKINS_TOKEN)
        .get()
        .body
        .toFile("$dstPath${File.separator}构建日志.txt")
        .setOnSuccess { file: File -> println("日志下载完成！ ${file.absolutePath}") }
        .setOnFailure {
            System.err.println("日志下载失败！")
            it.exception.printStackTrace()
        }
        .start()
}
