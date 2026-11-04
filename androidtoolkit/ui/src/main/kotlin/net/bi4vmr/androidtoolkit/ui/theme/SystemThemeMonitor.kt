package net.bi4vmr.androidtoolkit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.bi4vmr.tool.java.common.base.system.OSType
import net.bi4vmr.tool.java.common.base.system.SystemUtil

/**
 * 系统深色模式监听器。
 *
 * Compose Desktop 的 [isSystemInDarkTheme] 在 Linux 平台始终返回 `false` ，此处通过 freedesktop 门户
 * （`org.freedesktop.appearance color-scheme`）读取系统配色，并监听 `SettingChanged` 信号实时更新。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object SystemThemeMonitor {

    private const val PORTAL_DEST = "org.freedesktop.portal.Desktop"
    private const val PORTAL_PATH = "/org/freedesktop/portal/desktop"
    private const val NAMESPACE = "org.freedesktop.appearance"
    private const val KEY = "color-scheme"

    private val colorSchemeRegex = Regex("""uint32 (\d)""")

    // 系统是否为深色模式；为 `null` 时表示无法获取。
    private val darkFlow = MutableStateFlow<Boolean?>(null)

    val isDark: StateFlow<Boolean?> = darkFlow.asStateFlow()

    init {
        if (SystemUtil.getOSType() == OSType.LINUX) {
            darkFlow.value = readLinux()
            startLinuxMonitor()
        }
    }

    private fun readLinux(): Boolean? = runCatching {
        val process = ProcessBuilder(
            "gdbus", "call", "--session",
            "--dest", PORTAL_DEST,
            "--object-path", PORTAL_PATH,
            "--method", "org.freedesktop.portal.Settings.Read",
            NAMESPACE, KEY
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        parseColorScheme(output)
    }.getOrNull()

    private fun startLinuxMonitor() {
        val process = runCatching {
            ProcessBuilder(
                "gdbus", "monitor", "--session",
                "--dest", PORTAL_DEST,
                "--object-path", PORTAL_PATH
            ).redirectErrorStream(true).start()
        }.getOrNull() ?: return

        Runtime.getRuntime().addShutdownHook(Thread { process.destroy() })

        Thread {
            runCatching {
                process.inputStream.bufferedReader().forEachLine { line ->
                    // 示例：... SettingChanged ('org.freedesktop.appearance', 'color-scheme', <uint32 1>)
                    if (line.contains("SettingChanged") && line.contains("'$NAMESPACE'") && line.contains("'$KEY'")) {
                        darkFlow.value = parseColorScheme(line)
                    }
                }
            }
        }.apply {
            name = "SystemThemeMonitor"
            isDaemon = true
            start()
        }
    }

    // 取值：0 = 无偏好，1 = 深色，2 = 浅色。
    private fun parseColorScheme(text: String): Boolean? =
        when (colorSchemeRegex.find(text)?.groupValues?.get(1)) {
            "1" -> true
            "2" -> false
            else -> null
        }
}

/**
 * 获取系统是否为深色模式，系统配色变化时自动触发重组。
 */
@Composable
fun isSystemDarkTheme(): Boolean {
    val fallback = isSystemInDarkTheme()
    val dark by SystemThemeMonitor.isDark.collectAsState()
    return dark ?: fallback
}
