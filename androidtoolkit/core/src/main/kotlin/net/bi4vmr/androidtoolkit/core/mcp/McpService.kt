package net.bi4vmr.androidtoolkit.core.mcp

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.respondText
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.common.ExecutorType
import net.bi4vmr.androidtoolkit.core.model.ADBExecutor
import java.security.MessageDigest

/**
 * MCP 服务，向外部 AI 工具提供各执行器下 ADB 设备的查询与命令执行能力。
 *
 * 仅监听回环地址，使用 Streamable HTTP 传输，端点为 `http://127.0.0.1:<port>/mcp` 。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.1
 */
object McpService {

    const val PORT_DEFAULT = 18765

    private const val ID_LOCAL = "local"

    /**
     * 访问令牌，客户端需在请求头中携带 `Authorization: Bearer <token>` 。
     *
     * 当前为测试用的硬编码值，后续应改为随机生成或由用户配置。
     */
    const val TOKEN = "3f9c2b6e-7a41-4d58-9b0e-5c1d8a2f6e73"

    private var engine: EmbeddedServer<*, *>? = null

    @Synchronized
    fun start(port: Int = PORT_DEFAULT) {
        if (engine != null) return

        engine = embeddedServer(CIO, port = port, host = "127.0.0.1") {
            intercept(ApplicationCallPipeline.Plugins) {
                if (!isAuthorized(call.request.headers[HttpHeaders.Authorization])) {
                    call.response.headers.append(HttpHeaders.WWWAuthenticate, "Bearer")
                    call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                    finish()
                }
            }
            mcpStreamableHttp { createServer() }
        }.also { it.start(wait = false) }
        println("McpService: started at http://127.0.0.1:$port/mcp (Bearer token required)")
    }

    @Synchronized
    fun stop() {
        engine?.stop(500, 1000)
        engine = null
    }

    private fun isAuthorized(header: String?): Boolean {
        val prefix = "Bearer "
        if (header == null || !header.startsWith(prefix)) return false
        // 恒定时间比较，避免时序攻击。
        return MessageDigest.isEqual(
            header.substring(prefix.length).trim().toByteArray(),
            TOKEN.toByteArray()
        )
    }

    private fun createServer(): Server {
        val server = Server(
            Implementation(name = "androidtoolkit", version = "1.0.1"),
            ServerOptions(ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = false)))
        )

        server.addTool(
            name = "list_executors",
            description = "列出所有执行器。执行器是设备所在的主机：LOCAL 表示本软件所在的本机，" +
                    "可直接在终端使用 `adb` 命令操作其设备，无需经过本软件；REMOTE 表示通过 ADB 代理连接的其他主机，" +
                    "本机无法直接访问其设备，必须通过本软件的 adb_shell 工具并指定 executor 才能操作。"
        ) {
            text(
                LogicManager.executorsFlow.value.joinToString("\n") {
                    "id=${executorId(it)}\ttype=${it.getType()}\tstate=${it.getStateFlow().value}\t${describe(it)}"
                }
            )
        }

        server.addTool(
            name = "list_devices",
            description = "列出指定执行器下已连接的设备及状态。LOCAL 执行器的设备也可直接使用 `adb devices` 查看。",
            inputSchema = ToolSchema(
                properties = buildJsonObject { executorProperty() }
            )
        ) { request ->
            val executor = resolveExecutor(request.arguments)
                ?: return@addTool text(executorNotFound(), isError = true)
            val devices = executor.getDevicesFlow().value
            text(
                if (devices.isEmpty()) "没有已连接的设备"
                else devices.joinToString("\n") { "${it.getSN()}\t${it.getState()}" }
            )
        }

        server.addTool(
            name = "adb_shell",
            description = "在指定执行器下的指定设备上执行 shell 命令并返回输出（stdout 与 stderr 合并），" +
                    "等同于 `adb -s <sn> shell <command>` ，命令不做限制。" +
                    "对 LOCAL 执行器，建议直接在终端使用 adb 命令，不必经过本工具；对 REMOTE 执行器必须使用本工具。",
            inputSchema = ToolSchema(
                properties = buildJsonObject {
                    executorProperty()
                    putJsonObject("sn") {
                        put("type", "string")
                        put("description", "设备序列号，可通过 list_devices 获取。")
                    }
                    putJsonObject("command") {
                        put("type", "string")
                        put("description", "要执行的 shell 命令，例如 `getprop ro.build.version.release` 。")
                    }
                },
                required = listOf("sn", "command")
            )
        ) { request ->
            val args: JsonObject = request.arguments ?: JsonObject(emptyMap())
            val sn = args["sn"]?.jsonPrimitive?.contentOrNull
            val command = args["command"]?.jsonPrimitive?.contentOrNull
            if (sn.isNullOrBlank() || command.isNullOrBlank()) {
                return@addTool text("参数 sn 与 command 不能为空", isError = true)
            }
            val executor = resolveExecutor(args)
                ?: return@addTool text(executorNotFound(), isError = true)
            if (executor.getDevicesFlow().value.none { it.getSN() == sn }) {
                return@addTool text("执行器 ${executorId(executor)} 下没有序列号为 $sn 的设备", isError = true)
            }

            val output = executor.runForText(sn, command)
            if (output == null) text("命令执行失败", isError = true) else text(output)
        }

        return server
    }

    private fun JsonObjectBuilder.executorProperty() {
        putJsonObject("executor") {
            put("type", "string")
            put("description", "执行器 ID，可通过 list_executors 获取；省略时使用本机执行器 `local` 。")
        }
    }

    private fun executorId(executor: ADBExecutor): String =
        if (executor.getType() == ExecutorType.LOCAL) ID_LOCAL else executor.getDisplayName()

    private fun describe(executor: ADBExecutor): String = when (executor.getType()) {
        ExecutorType.LOCAL -> "本机 ADB，客户端可直接在终端使用 adb 命令操作，无需经过本软件。"
        ExecutorType.REMOTE -> "远程主机 ${executor.getDisplayName()} 上的 ADB，仅能通过 adb_shell 工具操作。"
    }

    private fun resolveExecutor(args: JsonObject?): ADBExecutor? {
        val id = args?.get("executor")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: ID_LOCAL
        return LogicManager.executorsFlow.value.firstOrNull { executorId(it) == id }
    }

    private fun executorNotFound(): String =
        "执行器不存在，可用 ID：" + LogicManager.executorsFlow.value.joinToString { executorId(it) }

    private fun text(content: String, isError: Boolean = false) =
        CallToolResult(content = listOf(TextContent(content)), isError = isError)
}
