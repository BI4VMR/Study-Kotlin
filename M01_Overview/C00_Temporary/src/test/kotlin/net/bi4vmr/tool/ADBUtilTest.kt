package net.bi4vmr.tool

import net.bi4vmr.tool.kotlin.external.adb.ADBUtil
import net.bi4vmr.tool.kotlin.external.adb.ADBUtil.buildCommand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [ADBUtil] 功能测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ADBUtilTest {

    @Test
    fun test_BuildCommand() {
        // 裸命令，应当插入序号前缀。
        val cmd1 = buildCommand("SN", "wm size")
        println("wm size -> $cmd1")
        assertEquals("adb -s SN exec-out wm size", cmd1)

        // ADB Shell 命令，应当插入序号前缀。
        val cmd2 = buildCommand("SN", "adb shell wm size")
        println("adb shell wm size -> $cmd2")
        assertEquals("adb -s SN shell wm size", cmd2)

        // ADB Exec-Out 命令，应当插入序号前缀。
        val cmd3 = buildCommand("SN", "adb exec-out wm size")
        println("adb exec-out wm size -> $cmd3")
        assertEquals("adb -s SN exec-out wm size", cmd3)

        // 带有其他序号的 ADB Shell 命令，应当改为当前序号。
        val cmd4 = buildCommand("SN", "adb -s SN1 shell wm size")
        println("adb shell wm size -> $cmd4")
        assertEquals("adb -s SN shell wm size", cmd4)

        // 空命令，应当转为Shell的空语句，防止卡住调用线程。
        val cmd5 = buildCommand("SN", "adb shell \"\"")
        println("adb shell -> $cmd5")
        assertEquals("adb -s SN shell :", cmd5)
    }
}
