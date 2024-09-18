package net.bi4vmr.study

import cn.zhxu.okhttps.HTTP
import cn.zhxu.okhttps.HttpResult
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

/**
 * 轮询Maven仓库中的指定产物，并触发Jenkins构建。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
// 轮询间隔（秒）
const val CHECK_INTERVAL = 120

const val MAVEN_USERNAME = "thundersoft_upload"
const val MAVEN_PASSWORD = "ThunderSoft#2024"
const val MAVEN_REPO = "pateo-thundersoft"
const val MAVEN_GROUP = "com.hyundai.module"
const val MAVEN_ARTIFACT = "appstore"
const val MAVEN_URL =
    "http://10.100.0.2:8081/service/rest/v1/search?repository={repo}&maven.groupId={group}&maven.artifactId={artifact}"

const val JENKINS_URL =
    "http://10.10.96.190:8080/job/PRJ_HMTC_APP_MainInteraction/buildWithParameters?TARGET_MODULE=PateoLauncher"
const val JENKINS_USERNAME = "yigangzhan"
const val JENKINS_PASSWORD = "1138afb0901111c8a04fac7a2c2dfd2ae0"

// 上次轮循到的最大版本号
private var lastVersion: String = ""

fun main() = runBlocking {
    // 获取初始版本号
    checkNewVersion()
    exitProcess(0)
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
        }

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
        println("$i   -> ${list1[i]}  -  ${list2[i]}")
        if (list1[i] > list2[i]) return 1
        if (list1[i] < list2[i]) return -1
    }
    return 0
}

// 触发构建
fun startBuild() {
    // curl -u yigangzhan:1138afb0901111c8a04fac7a2c2dfd2ae0 -X 'POST' "http://10.10.96.190:8080/job/PRJ_HMTC_APP_MainInteraction/buildWithParameters?TARGET_MODULE=PateoLauncher"
    val jenkinsClient: HTTP = HTTP.builder()
        .build()

    val result: HttpResult = jenkinsClient.sync(JENKINS_URL)
        .basicAuth(JENKINS_USERNAME, MAVEN_PASSWORD)
        .get()

    return when (result.state) {
        /* 请求成功 */
        HttpResult.State.RESPONSED -> {
            println("Jenkins start build.")
        }
        /* 请求失败 */
        else -> {
            println("Jenkins服务器不可访问，状态：${result.state}")
        }
    }
}
