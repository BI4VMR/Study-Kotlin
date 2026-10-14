package net.bi4vmr.study

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(CIO, port = 8080) {
        routing {
            get("/") {
                call.respondText("Hello Ktor")
            }
        }
    }.start(wait = true)

    // 键值对
    // // 创建并从文件加载数据
    // Properties().load(FileReader(""))
    // // 将数据保存到流
    // Properties().store(FileWriter(""),"111")
    //
    // // 读程序内置的 src/main/resources/1.properties
    // // Properties().load(xxx.class.getClassLoader.getResourceAsStream("1.properties"))
    //
    // // 默认编码  ISO-8859-1 ，不支持非 ASCII 字符，中文会乱码，若要支持中文需在stream中指定utf-8编码
    // // 读写
    // Properties().getProperty("key")
    // Properties().getProperty("key","def")
    // Properties().setProperty("key","val")
    // // 所有属性集合
    // Properties().stringPropertyNames()
}
