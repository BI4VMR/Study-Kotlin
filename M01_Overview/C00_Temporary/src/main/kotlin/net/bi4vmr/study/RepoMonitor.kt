package net.bi4vmr.study

import okhttp3.*

fun main() {
    val URL_MAVEN =
        "http://192.168.128.1:8081/service/rest/v1/search?repository=maven-private&maven.groupId=net.bi4vmr.tool.java"

    val client = OkHttpClient()
//     (object : Authenticator {
//
//            override fun authenticate(route: Route?, response: Response): Request? {
//                if (response.request.header("Authorization") != null) {
//                    return null // Give up, we've already attempted to authenticate.
//                }
//
//                println("Authenticating for response: $response")
//                println("Challenges: ${response.challenges()}")
//                val credential = Credentials.basic("upload", "upload")
//                return response.request.newBuilder()
//                    .header("Authorization", credential)
//                    .build()
//            }
//        })
    val request = Request.Builder()
        .url(URL_MAVEN)

        .build()
    client.newCall(request).execute().use {
        println(it.body)
    }
}
