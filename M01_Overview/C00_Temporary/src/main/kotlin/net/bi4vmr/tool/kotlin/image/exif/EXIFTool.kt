package net.bi4vmr.tool.kotlin.image.exif

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.kotlin.image.exif.EXIFTool.readTag
import net.bi4vmr.tool.kotlin.image.exif.EXIFTool.readTags
import net.bi4vmr.tool.kotlin.image.exif.EXIFTool.writeTag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * EXIF 相关工具。
 *
 * [EXIFTool](https://exiftool.org/) 的命令行封装，支持从图像文件解析或修改 EXIF 标签。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object EXIFTool {

    private val logger: Logger = LoggerFactory.getLogger("EXIFTool")

    /**
     * 键值对分隔符。
     *
     * 用于匹配工具的每行输出内容： `<属性名称>: <属性值>` 。
     */
    private val kvSplitRegex: Regex = ": ".toRegex()


    /*
     * ----- 读取标签 -----
     */

    /**
     * 读取自定义标签。
     *
     * 本方法用于读取本工具内置枚举类 [EXIFTag] 未包含的标签，对于内置标签建议使用 [readTag] 方法。
     *
     * @param[file] 目标文件。
     * @param[tag] 标签名称。
     * @param[rawValue] `false` 表示人类可读格式； `true` 表示原始格式。默认值为 `false` 。
     * @return 标签的值。如果文件或标签不存在，则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun readCustomTag(file: File, tag: String, rawValue: Boolean = false): String? {
        if (!file.canRead() || tag.isEmpty()) {
            logger.error("File is not readable or tag is empty! Path:[{}] Tag:[{}]", file, tag)
            return null
        }

        val cmdBuilder = StringBuilder()
        cmdBuilder.append("exiftool ")
        // 短格式（ Tag 单词之间不含空格； Tag 与分隔符之间不含空格。）
        cmdBuilder.append("-S ")
        // 是否返回原始数据
        if (rawValue) {
            cmdBuilder.append("-n ")
        }
        // 传入指定的标签
        cmdBuilder.append("-\"${tag}\" ")
        cmdBuilder.append("\"${file.absolutePath}\"")

        var result: String? = null
        run {
            CLIUtil.runForLines(cmdBuilder.toString())
                ?.forEach { line ->
                    val keyValue = line.split(kvSplitRegex, 2)
                    if (keyValue.size != 2) {
                        logger.warn("Can not parse line [$line]!")
                        // 当前行无法拆分为两列时，结束本次循环。
                        return@forEach
                    }

                    if (keyValue[0].equals(tag, ignoreCase = true)) {
                        result = keyValue[1].trim()
                        // 找到匹配的键后，结束所有循环。
                        return@run
                    }
                }
        }

        return result
    }

    /**
     * 读取指定的标签。
     *
     * @param[file] 目标文件。
     * @param[tag] 标签。
     * @param[rawValue] `false` 表示人类可读格式； `true` 表示原始格式。默认值为 `false` 。
     * @return 标签的值。如果文件或标签不存在，则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun readTag(file: File, tag: EXIFTag, rawValue: Boolean = false): String? =
        readCustomTag(file, tag.title, rawValue)

    /**
     * 读取一组自定义标签。
     *
     * 本方法用于读取本工具内置枚举类 [EXIFTag] 未包含的标签，对于内置标签建议使用 [readTags] 方法。
     *
     * @param[file] 目标文件。
     * @param[tags] 标签名称数组。
     * @param[rawValue] `false` 表示人类可读格式； `true` 表示原始格式。默认值为 `false` 。
     * @return Map 键为标签， Map 值为标签的值。 Map 的键顺序始终与 [tags] 一致。如果文件或标签不存在，则返回空集合。
     */
    @JvmStatic
    @JvmOverloads
    fun readCustomTags(file: File, tags: Array<String>, rawValue: Boolean = false): Map<String, String?> {
        if (!file.canRead() || tags.isEmpty()) {
            logger.error("File is not readable or tags is empty! Path:[{}] TagCount:[{}]", file, tags.size)
            return emptyMap()
        }

        val cmdBuilder = StringBuilder()
        cmdBuilder.append("exiftool ")
        // 短格式（ Tag 单词之间不含空格； Tag 与分隔符之间不含空格。）
        cmdBuilder.append("-S ")
        // 是否返回原始数据
        if (rawValue) {
            cmdBuilder.append("-n ")
        }
        // 传入指定的标签
        tags.forEach { tag -> cmdBuilder.append("-\"${tag}\" ") }
        cmdBuilder.append("\"${file.absolutePath}\"")

        val results: LinkedHashMap<String, String?> = LinkedHashMap(tags.size, 1.0F)
        // 填充输入的标签
        tags.forEach { results[it] = null }

        run {
            CLIUtil.runForLines(cmdBuilder.toString())
                ?.forEach { line ->
                    val keyValue = line.split(kvSplitRegex, 2)
                    if (keyValue.size != 2) {
                        logger.warn("Can not parse line [$line]!")
                        // 当前行无法拆分为两列时，结束本次循环。
                        return@forEach
                    }

                    val tagName = keyValue[0]
                    if (results.containsKey(tagName)) {
                        results[tagName] = keyValue[1].trim()
                    }
                }
        }

        return results
    }

    /**
     * 读取一组指定标签。
     *
     * @param[file] 目标文件。
     * @param[tags] 标签数组。
     * @param[rawValue] `true` 表示人类可读格式； `false` 表示原始格式。默认值为 `false` 。
     * @return Map 键为标签， Map 值为标签的值。 Map 的键顺序始终与 [tags] 一致。如果文件或标签不存在，则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun readTags(file: File, tags: Array<EXIFTag>, rawValue: Boolean = false): Map<EXIFTag, String?> {
        val nameArray: Array<String> = tags.map { it.title }.toTypedArray()
        val kvMap = readCustomTags(file, nameArray, rawValue)

        // `readCustomTags()` 返回 LinkedHashMap ，键的顺序与输入数组 `tags` 一致，此处将返回结果的值按顺序填充，无需匹配标签名称。
        val results: LinkedHashMap<EXIFTag, String?> = LinkedHashMap(tags.size, 1.0F)
        kvMap.values.forEachIndexed { index, value ->
            results[tags[index]] = value
        }
        return results
    }


    /*
     * ----- 写入标签 -----
     */

    /**
     * 修改自定义标签。
     *
     * 本方法用于修改本工具内置枚举类 [EXIFTag] 未包含的标签，对于内置标签建议使用 [writeTag] 方法。
     *
     * @param[file] 目标文件。
     * @param[tag] 标签名称。
     * @param[value] 标签值。
     * @return `true` 表示操作成功； `false` 表示操作失败，例如：指定的标签不可写入。
     */
    fun writeCustomTag(file: File, tag: String, value: String): Boolean {
        if (!file.canWrite() || tag.isEmpty()) {
            logger.error("File is not writeable or tag is empty! Path:[{}] Tag:[{}]", file, tag)
            return false
        }

        val cmdBuilder = StringBuilder()
        cmdBuilder.append("exiftool ")
        // 通过 `<标签>=<值>` 语法修改标签
        cmdBuilder.append("-\"${tag}\"=\"${value}\" ")
        cmdBuilder.append("\"${file.absolutePath}\"")

        val result = CLIUtil.runForStatus(cmdBuilder.toString())
        return CLIUtil.isSuccess(result)
    }

    /**
     * 修改指定的标签。
     *
     * @param[file] 目标文件。
     * @param[tag] 标签名称。
     * @param[value] 标签值。
     * @return `true` 表示命令执行成功； `false` 表示命令执行失败，例如：指定的标签不可写入。
     */
    fun writeTag(file: File, tag: EXIFTag, value: String): Boolean =
        writeCustomTag(file, tag.title, value)
}
