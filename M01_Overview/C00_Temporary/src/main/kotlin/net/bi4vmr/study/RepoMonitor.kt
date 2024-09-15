package net.bi4vmr.study

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

/**
 * TODO 添加简述
 *
 * TODO 添加详情
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
fun main() {
    val URL_MAVEN =
        "http://192.168.128.1:8081/service/rest/v1/search?repository=maven-private&maven.groupId=net.bi4vmr.tool.java&maven.artifactId=io-base"
    val LOGIN_INFO = "uploader:uploader"
    val AUTHEN_INFO = "Basic ${Base64.getEncoder().encodeToString(LOGIN_INFO.toByteArray())}"

    val client = OkHttpClient()
    val request = Request.Builder()
        .url(URL_MAVEN)
        .addHeader("Authorization", AUTHEN_INFO)
        .build()
    client.newCall(request).execute().use {
        println(it.body?.string())
        // val obj: JsonObject = JsonParser.parseString(it.body?.string() ?: "{}")
        //     .asJsonObject
        // if (obj.has("items")){
        //     val arr = obj.getAsJsonArray("items")
        //     for (item in arr){
        //         val obj1 = item.asJsonObject
        //         println("${obj1.get("version")}")
        //     }
        // }
    }
}

fun compareVersions(version1: String, version2: String): Int {
    val v1 = version1.split('.').map { it.toInt() }
    val v2 = version2.split('.').map { it.toInt() }

    for (i in 0 until kotlin.math.max(v1.size, v2.size)) {
        val num1 = if (i < v1.size) v1[i] else 0
        val num2 = if (i < v2.size) v2[i] else 0

        if (num1 > num2) return 1
        if (num1 < num2) return -1
    }
    return 0
}
