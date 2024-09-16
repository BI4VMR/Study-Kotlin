package net.bi4vmr.study

import cn.zhxu.okhttps.HTTP
import cn.zhxu.okhttps.HttpResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.offbytwo.jenkins.JenkinsServer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.URI
import kotlin.system.exitProcess

/**
 * 轮询Maven仓库中的指定产物，并触发Jenkins构建。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
// 轮询间隔（秒）
const val CHECK_INTERVAL = 120

const val MAVEN_USERNAME = "uploader"
const val MAVEN_PASSWORD = "uploader"
const val MAVEN_REPO = "maven-private"
const val MAVEN_GROUP = "net.bi4vmr.tool.java"
const val MAVEN_ARTIFACT = "io-base"
const val MAVEN_URL =
    "http://192.168.128.1:8081/service/rest/v1/search?repository={repo}&maven.groupId={group}&maven.artifactId={artifact}"

private var lastVersion: String = ""

// curl -u admin:1105e3f897e39e7b29a5c25c3c07a4d3da -X 'POST' "http://192.168.128.1:8082/job/BaseLib/job/BaseLib-Java/build"
fun main() = runBlocking {
    // 获取初始版本号
    checkNewVersion()

    // 轮询
    while (true) {
        // 检测REPO中是否有新的版本
        val result: Boolean = checkNewVersion()
        if (result) {
            println("存在新的版本。")
            // TODO 如果存在新的版本，则触发Jenkins构建。

            // TODO 将构建结果通过飞书发送

        } else {
            println("没有新的版本。")
            val server = JenkinsServer(URI("http://192.168.128.1:8082/"), "admin", "Emczyg300498.")
            server.getJob("BaseLib-Java")
                .build()
        }
        exitProcess(0)
        // 间隔时长
        delay(CHECK_INTERVAL * 1000L)
    }
}

// 检查指定的组件是否存在新的版本
fun checkNewVersion(): Boolean {
    val repoClient: HTTP = HTTP.builder()
        .build()

    val result: HttpResult = repoClient.sync(MAVEN_URL)
        .addPathPara("repo", MAVEN_REPO)
        .addPathPara("group", MAVEN_GROUP)
        .addPathPara("artifact", MAVEN_ARTIFACT)
        .basicAuth(MAVEN_USERNAME, MAVEN_PASSWORD)
        .get()

    return when (result.state) {
        /* 请求成功 */
        HttpResult.State.RESPONSED -> {
            val obj: JsonObject = JsonParser.parseString(result.body.toString())
                .asJsonObject
            if (obj.has("items")) {
                val versionList: MutableList<String> = mutableListOf()
                val items: JsonArray = obj.getAsJsonArray("items")
                for (item in items) {
                    if (item is JsonObject && item.has("version")) {
                        versionList.add(item.get("version").asString)
                    }
                }

                val maxVersion: String = getLatestVersion(versionList)
                println("Current max version is [$maxVersion], last version is [$lastVersion]")
                return if (maxVersion != lastVersion) {
                    lastVersion = maxVersion
                    true
                } else {
                    false
                }
            } else {
                println("Repo返回的消息格式错误！")
                return false
            }
        }
        /* 请求失败 */
        else -> {
            println("Maven仓库不可访问，状态：${result.state}")
            false
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
    val list1 = version1.split('.').map { it.toInt() }
    val list2 = version2.split('.').map { it.toInt() }

    if (list1.size < 3 || list2.size < 3) {
        println("版本号格式不正确！")
        return 0
    }

    for (i in 0 until 3) {
        if (list1[i] > list2[i]) return 1
        if (list1[i] < list2[i]) return -1
    }
    return 0
}
