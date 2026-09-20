package net.bi4vmr.study

import net.bi4vmr.tool.kotlin.image.exif.ExifTool
import java.io.File

fun main() {
    // val i = Toolkit.getDefaultToolkit().screenResolution
    // println(i)
    val f = File("C:\\Users\\bi4vmr\\Download\\IMG_20260609_131435.jpg")
    // val map: Map<Tag, String> = ExifTool().getImageMeta(f, Tag.FOCAL_LENGTH)
    // map.forEach { tag, string ->
    //     println("tag -> $tag")
    //     println("value -> $string")
    // }

    // EXIFTool.readTag(f, EXIFTag.ISO)
    //     ?.let {
    //         println(it)
    //     }

    // val i = ExifTool.writeCustomTag(f,"aaa","田所浩二")
    // println("write : $i")


    // ExifTool.readAllTags(f)
    //     .forEach { k, v ->
    //         println("$k -> $v")
    //     }
    // ExifTool.clearTag(f)
    ExifTool.clearTag(f, null,false)

    // val adbName = getADBExecName()
    //
    // // 尝试获取 `ANDROID_HOME` 环境变量
    // val androidHome = System.getenv("ANDROID_HOME")
    // if (TextUtil.isNotBlank(androidHome)) {
    //     val test = File("$androidHome${File.separator}platform-tools", adbName)
    //     if (test.exists()) {
    //         println("good ${test}")
    //     }
    // }

    // 尝试从命令反推文件位置
    // val cmd = if (SystemUtil.isWindows()) arrayOf("where", adbFileName)
    // else arrayOf("sh", "-c", "command -v $adbFileName")

    // 尝试从常见路径查找
    // isWindows -> {
    //
    //     val localAppData = System.getenv("LOCALAPPDATA") ?: "$home/AppData/Local"
    //
    //     paths += listOf(
    //
    //         "$localAppData/Android/Sdk/platform-tools/$adbFileName",
    //
    //         "C:/Android/Sdk/platform-tools/$adbFileName",
    //
    //         "$home/Android/Sdk/platform-tools/$adbFileName",
    //
    //         )
    //
    // }
    //
    // System.getProperty("os.name").lowercase().contains("mac") -> {
    //
    //     paths += listOf(
    //
    //         "$home/Library/Android/sdk/platform-tools/$adbFileName",
    //
    //         "/usr/local/share/android-commandlinetools/platform-tools/$adbFileName",
    //
    //         "/opt/homebrew/bin/$adbFileName",
    //
    //         "/usr/local/bin/$adbFileName",
    //
    //         )
    //
    // }
    //
    // else -> { // Linux
    //
    //     paths += listOf(
    //
    //         "$home/Android/Sdk/platform-tools/$adbFileName",
    //
    //         "/usr/bin/$adbFileName",
    //
    //         "/usr/local/bin/$adbFileName",
    //
    //         "/opt/android-sdk/platform-tools/$adbFileName",
    //
    //         "$home/.local/share/android-commandlinetools/platform-tools/$adbFileName",
    //
    //         "/snap/android-platform-tools/current/platform-tools/$adbFileName",
    //
    //         )
    //
    // }
}
