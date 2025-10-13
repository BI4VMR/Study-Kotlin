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
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 轮询Maven仓库中的指定产物，并触发Jenkins构建。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
// 轮询间隔（秒）
const val CHECK_INTERVAL = 180

const val MAVEN_USERNAME = "-"
const val MAVEN_PASSWORD = "-"
const val MAVEN_REPO = "-"
const val MAVEN_GROUP = "-"
const val MAVEN_ARTIFACT = "-"
const val MAVEN_URL =
    "http://127.0.0.1:8080/service/rest/v1/search?repository={repo}&maven.groupId={group}&maven.artifactId={artifact}&sort=version"

const val JENKINS_URL_AAR =
    "http://127.0.0.1:8080/job/AAR/"
const val JENKINS_URL_APK =
    "http://127.0.0.1:8080/job/APK/"
const val JENKINS_USERNAME = "-"
const val JENKINS_TOKEN = "-"

const val PATH_TEMP = "/var/tmp/build/Hyundai8295/"
const val PATH_ALIST = "/mnt/alist-local/天翼云盘/Work/"

val client: HTTP = HTTP.builder().build()

// 上次轮循到的最大版本号
private var lastVersion: String = ""

fun main() = runBlocking {
    // For Debug
    // downloadAPKSync("463", File("/home/bi4vmr/Download"))
    // println("end :${Thread.currentThread().id}")
    // delay(3000L)
    // exitProcess(0)

    // 向全局变量填充初始版本号
    checkNewVersion()

    while (true) {
        try {
            // 间隔时长
            delay(CHECK_INTERVAL * 1000L)

            // 检测仓库中是否有新的版本
            val result: Boolean = checkNewVersion()
            if (result) {
                println("检测到新的版本，开始构建。")
                // 建立结果目录
                val outPath = File(PATH_TEMP, lastVersion)
                if (outPath.exists()) {
                    outPath.deleteRecursively()
                }
                outPath.mkdir()

                // 如果存在新的版本，则触发Jenkins构建。
                val aarBuildID: String? = startBuildAAR()
                println("AAR构建任务已提交。 BuildID:[$aarBuildID]")
                // 如果没有获取到ID，则放弃本次任务，进入下一轮循环。
                if (aarBuildID == null) {
                    File(outPath, "中间件构建结果：失败（获取第一阶段BuildID超时）").createNewFile()
                    copyFiles(outPath.toPath(), Paths.get(PATH_ALIST, lastVersion))
                    continue
                }

                var aarJobState: BuildResult? = null
                // 轮循任务状态
                for (i in 1..15) {
                    delay(CHECK_INTERVAL * 1000L)

                    aarJobState = getBuildResult(JENKINS_URL_AAR, aarBuildID)
                    println("第 $i 次检查AAR构建任务状态：$aarJobState")
                    // 结果不为空，说明任务执行完毕，退出循环。
                    if (aarJobState != null) {
                        break
                    }
                }

                when (aarJobState) {
                    BuildResult.SUCCEESS -> {
                        downloadLog(JENKINS_URL_AAR, aarBuildID, outPath, "中间件")
                        File(outPath, "中间件构建结果：成功").createNewFile()
                    }

                    BuildResult.FAILURE -> {
                        downloadLog(JENKINS_URL_AAR, aarBuildID, outPath, "中间件")
                        File(outPath, "中间件构建结果：失败").createNewFile()
                        copyFiles(outPath.toPath(), Paths.get(PATH_ALIST, lastVersion))
                        continue
                    }

                    BuildResult.ABORTED -> {
                        File(outPath, "中间件构建结果：中途取消").createNewFile()
                        copyFiles(outPath.toPath(), Paths.get(PATH_ALIST, lastVersion))
                        continue
                    }
                    // 轮循超时
                    else -> {
                        File(outPath, "中间件构建结果：KT脚本轮循超时").createNewFile()
                        copyFiles(outPath.toPath(), Paths.get(PATH_ALIST, lastVersion))
                        continue
                    }
                }

                delay(5000L)
                println("AAR构建完成，开始构建APK。")

                val buildID: String? = startBuild()
                println("APK构建任务已提交。 BuildID:[$buildID]")
                // 如果没有获取到ID，则放弃本次任务，进入下一轮循环。
                if (buildID == null) {
                    File(outPath, "最终产物构建结果：失败（获取第二阶段BuildID超时）").createNewFile()
                    copyFiles(outPath.toPath(), Paths.get(PATH_ALIST, lastVersion))
                    continue
                }

                var state: BuildResult? = null
                // 轮循任务状态
                for (i in 1..15) {
                    delay(CHECK_INTERVAL * 1000L)

                    state = getBuildResult(JENKINS_URL_APK, buildID)
                    println("第 $i 次检查任务状态：$state")
                    // 结果不为空，说明任务执行完毕，退出循环。
                    if (state != null) {
                        break
                    }
                }

                delay(5000L)

                when (state) {
                    BuildResult.SUCCEESS -> {
                        downloadAPKSync(JENKINS_URL_APK, buildID, outPath)
                        delay(1000L)
                        downloadLog(JENKINS_URL_APK, buildID, outPath, "最终产物")
                        File(outPath, "最终产物构建结果：成功").createNewFile()
                    }

                    BuildResult.FAILURE -> {
                        downloadLog(JENKINS_URL_APK, buildID, outPath, "最终产物")
                        File(outPath, "最终产物构建结果：失败").createNewFile()
                    }

                    BuildResult.ABORTED -> {
                        File(outPath, "最终产物构建结果：中途取消").createNewFile()
                    }
                    // 轮循超时
                    else -> {
                        File(outPath, "最终产物构建结果：KT脚本轮循超时").createNewFile()
                    }
                }

                // 复制文件到AList目录
                copyFiles(outPath.toPath(), Paths.get(PATH_ALIST, lastVersion))
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

                    val currentMaxVersion: String = getLatestVersion(versionList)
                    println("当前最新版本：$currentMaxVersion，上次检查时的最新版本：$lastVersion。")
                    if (compareVersions(currentMaxVersion, lastVersion) > 0) {
                        lastVersion = currentMaxVersion
                        it.resume(true)
                    } else {
                        lastVersion = currentMaxVersion
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
    // 处理第一次变量未初始化的情况
    if (version1 == "" || version2 == "") {
        return 1
    }

    // 去掉数字序号之后的内容
    fun removeSuffix(v: String): String {
        val i = v.indexOf('_')
        return if (i != -1) {
            v.substring(0, i)
        } else {
            v
        }
    }

    fun getSuffix(v: String): String {
        val i = v.indexOf('_')
        return if (i != -1) {
            v.substring(i + 1, v.length)
        } else {
            ""
        }
    }

    val v1 = removeSuffix(version1)
    val v2 = removeSuffix(version2)
    val list1 = v1.split('.').map { it.toLong() }.toMutableList()
    val list2 = v2.split('.').map { it.toLong() }.toMutableList()

    // 追加版本号之后的数字
    try {
        val v1Suffixs = getSuffix(version1).split('_').map { it.toLong() }
        val v2Suffixs = getSuffix(version2).split('_').map { it.toLong() }
        list1.addAll(v1Suffixs)
        list2.addAll(v2Suffixs)
    } catch (e: Exception) {
        // System.err.println("版本号包含非数字部分，已忽略！ A:[$version1] B:[$version2]")
        // e.printStackTrace()
    }

    if (list1.size < 3 || list2.size < 3) {
        System.err.println("版本号格式不正确！")
        return 0
    }

    for (i in 0 until (min(list1.size, list2.size))) {
        if (list1[i] > list2[i]) return 1
        if (list1[i] < list2[i]) return -1
    }
    return 0
}

/**
 * 触发构建AAR。
 *
 * @return BuildID。
 */
suspend fun startBuildAAR(): String? {
    return suspendCoroutine {
        val result: HttpResult =
            client.sync("$JENKINS_URL_AAR/buildWithParameters?TARGET_MODULE=packages/apps/Common%20publish")
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
                    when (buildID) {
                        /* 等待可用的执行器 */
                        "-1" -> {
                            println("Jenkins节点暂无可用的执行器，稍后将进行第 $times 次重试。")
                            times++
                            runBlocking { delay(CHECK_INTERVAL * 1000L) }
                        }
                        /* 等待前一个任务执行完毕 */
                        "-2" -> {
                            println("Jenkins等待前一个任务执行完毕，稍后将进行第 $times 次重试。")
                            times++
                            runBlocking { delay(CHECK_INTERVAL * 1000L) }
                        }
                        /* "executable"节点内容为空 */
                        "-3" -> {
                            println("`executable`节点内容为空，稍后将进行第 $times 次重试。")
                            times++
                            runBlocking { delay(CHECK_INTERVAL * 1000L) }
                        }
                        /* 已取到ID，跳出轮循。 */
                        else -> {
                            break
                        }
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

/**
 * 触发构建。
 *
 * @return BuildID。
 */
suspend fun startBuild(): String? {
    return suspendCoroutine {
        val result: HttpResult = client.sync("$JENKINS_URL_APK/buildWithParameters?TARGET_MODULE=Launcher")
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
                    when (buildID) {
                        /* 等待可用的执行器 */
                        "-1" -> {
                            println("Jenkins节点暂无可用的执行器，稍后将进行第 $times 次重试。")
                            times++
                            runBlocking { delay(CHECK_INTERVAL * 1000L) }
                        }
                        /* 等待前一个任务执行完毕 */
                        "-2" -> {
                            println("Jenkins等待前一个任务执行完毕，稍后将进行第 $times 次重试。")
                            times++
                            runBlocking { delay(CHECK_INTERVAL * 1000L) }
                        }
                        /* "executable"节点内容为空 */
                        "-3" -> {
                            println("`executable`节点内容为空，稍后将进行第 $times 次重试。")
                            times++
                            runBlocking { delay(CHECK_INTERVAL * 1000L) }
                        }
                        /* 已取到ID，跳出轮循。 */
                        else -> {
                            break
                        }
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
                println("----- BuildID JSON Start -----")
                println(body)
                println("----- BuildID JSON End -----")
                val obj: JsonObject = JsonParser.parseString(body).asJsonObject

                // 等待可用的执行器
                if (obj.has("why") &&
                    !obj.get("why").isJsonNull &&
                    obj.get("why").asString.startsWith("Waiting for next available executor")
                ) {
                    it.resume("-1")
                    return@suspendCoroutine
                }

                // 等待前一个任务执行完毕
                if (obj.has("blocked") && obj.get("blocked").asBoolean) {
                    it.resume("-2")
                    return@suspendCoroutine
                }

                if (obj.has("executable")) {
                    // "executable"节点内容为空
                    if (obj.get("executable").isJsonNull) {
                        it.resume("-3")
                        return@suspendCoroutine
                    }

                    val item: JsonObject = obj.getAsJsonObject("executable")
                    val id: String = item.get("number").asString
                    it.resume(id)
                } else {
                    System.err.println("Jenkins返回的消息格式与预期不符，无法解析！")
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
suspend fun getBuildResult(jobURL: String, buildID: String): BuildResult? {
    return suspendCoroutine {
        val result: HttpResult = client.sync("$jobURL/$buildID/api/json")
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
                            "SUCCESS", "UNSTABLE" -> BuildResult.SUCCEESS
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
    client.sync("$JENKINS_URL_APK/$buildID/artifact/OUT_APP/app/HyundaiCarLauncher.apk")
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

suspend fun downloadAPKSync(jobURL: String, buildID: String, dstPath: File) {
    return suspendCoroutine { sc ->
        val url = "$jobURL/$buildID/artifact/OUT_APP/app/HyundaiCarLauncher.apk"
        println("开始下载产物... URL:[$url]")
        client.sync(url)
            .basicAuth(JENKINS_USERNAME, JENKINS_TOKEN)
            .get()
            .body
            .stepRate(0.2)
            .setOnProcess { p: Process -> println("产物下载中... ${(p.rate * 100).roundToInt()} %") }
            .toFile("$dstPath${File.separator}HyundaiCarLauncher.apk")
            .setOnSuccess { file: File ->
                println("产物下载完成！ ${file.absolutePath}")
                sc.resume(Unit)
            }
            .setOnFailure {
                System.err.println("产物下载失败！")
                sc.resumeWithException(it.exception)
            }
            .start()
    }
}

// 下载日志到本地
fun downloadLog(jobURL: String, buildID: String, dstPath: File, fileSuffix: String) {
    println("开始下载日志...")
    client.sync("$jobURL/$buildID/consoleText")
        .basicAuth(JENKINS_USERNAME, JENKINS_TOKEN)
        .get()
        .body
        .toFile("$dstPath${File.separator}构建日志_${fileSuffix}.txt")
        .setOnSuccess { file: File -> println("日志下载完成！ ${file.absolutePath}") }
        .setOnFailure {
            System.err.println("日志下载失败！")
            it.exception.printStackTrace()
        }
        .start()
}

@Throws(IOException::class)
fun copyFiles(from: Path, to: Path) {
    if (Files.notExists(from)) {
        println("源文件夹不存在")
    }
    if (Files.notExists(to)) {
        Files.createDirectories(to)
    }

    Files.walkFileTree(from, object : SimpleFileVisitor<Path>() {
        @Throws(IOException::class)
        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            val to1 = to.resolve(from.relativize(file))
            // 如果说父路径不存在，则创建
            if (Files.notExists(to1.parent)) {
                Files.createDirectories(to1.parent)
            }
            Files.copy(file, to1)
            return FileVisitResult.CONTINUE
        }
    })
}
