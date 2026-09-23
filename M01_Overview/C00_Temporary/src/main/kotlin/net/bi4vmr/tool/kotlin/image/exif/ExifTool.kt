package net.bi4vmr.tool.kotlin.image.exif

import net.bi4vmr.tool.java.common.base.CLIUtil
import net.bi4vmr.tool.kotlin.image.exif.ExifTool.deleteTags
import net.bi4vmr.tool.kotlin.image.exif.ExifTool.readTag
import net.bi4vmr.tool.kotlin.image.exif.ExifTool.readTags
import net.bi4vmr.tool.kotlin.image.exif.ExifTool.writeTag
import net.bi4vmr.tool.kotlin.image.exif.ExifTool.writeTags
import net.bi4vmr.tool.kotlin.image.exif.constant.ExifTag
import net.bi4vmr.tool.kotlin.image.exif.util.ExifToolExecutableUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Exif 相关工具。
 *
 * [ExifTool](https://exiftool.org/) 的命令行封装，支持从图像文件解析或修改 Exif 标签。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ExifTool {

    private val logger: Logger = LoggerFactory.getLogger("ExifTool")

    /**
     * ExifTool 可执行文件。
     */
    @Volatile
    private var executableFile: File? = null

    /**
     * 是否自动侦测 ExifTool 可执行文件位置。
     */
    @Volatile
    private var detectExecutable: Boolean = true

    /**
     * 键值对分隔符。
     *
     * 用于匹配标签解析时每行的输出内容： `<属性名称>: <属性值>` 。
     */
    private val tagSplitRegex: Regex = ": ".toRegex()


    init {
        val detectPath = ExifToolExecutableUtil.detectExecutable()
        logger.debug("ExifTool init, detect executable file. Path:[{}]", detectPath)
        executableFile = detectPath
    }

    /*
     * ----- 读取标签 -----
     */

    /**
     * 读取一组自定义标签。
     *
     * 本方法用于读取工具内置枚举类 [ExifTag] 未包含的标签，对于受支持的标签建议使用 [readTags] 方法。
     *
     * @param[file] 目标文件。
     * @param[tags] 标签名称数组。默认值为空数组。若数组内容为空，则读取所有标签。
     * @param[rawValue] 是否显示原始值。默认值为 `false` 。 `true` 表示以原始格式输出； `false` 表示以人类可读格式输出，具体样式可参考
     * [ExifTag] 中各标签的注释。
     * @return Map 键为标签名称；值为标签的值，当指定的标签不存在时值为空。内部实现为 [LinkedHashMap] ，元素顺序始终与 [tags] 一致。
     * 如果文件或标签不存在，则返回空集合。
     */
    @JvmStatic
    @JvmOverloads
    fun readCustomTags(
        file: File,
        tags: Array<String> = emptyArray(),
        rawValue: Boolean = false
    ): Map<String, String?> {
        logger.debug("ReadCustomTags. Path:[{}] Tags:[{}]", file, tags.joinToString(","))

        /* 前置检查 */
        // 输入为目录时， ExifTool 将返回所有文件的标签，本方法只能返回单个文件信息，因此忽略它们。
        if (file.isDirectory) {
            logger.error("Input path is directory! Path:[{}]", file)
            return emptyMap()
        }

        if (!file.canRead()) {
            logger.error("File can not be read! Path:[{}]", file)
            return emptyMap()
        }

        val execFile = executableFile
        if (execFile == null || !execFile.canExecute()) {
            logger.error("ExifTool executable file not found or can not execute! Path:[{}]", execFile)
            return emptyMap()
        }

        val cmdBuilder = StringBuilder()
        cmdBuilder.append("${execFile.absolutePath} ")
        // 短格式（ Tag 单词之间不含空格； Tag 与分隔符之间不含空格。）
        cmdBuilder.append("-S ")
        // 是否返回原始数据
        if (rawValue) {
            cmdBuilder.append("-n ")
        }
        // 传入指定的标签
        tags.forEach { tag -> cmdBuilder.append("-\"${tag}\" ") }
        cmdBuilder.append("\"${file.absolutePath}\"")

        val initSize = if (tags.isEmpty()) 32 else tags.size
        val results: LinkedHashMap<String, String?> = LinkedHashMap(initSize, 1.0F)
        // 预填充输入标签，确保后续匹配时顺序与输入相同。
        tags.forEach { tag -> results[tag] = null }

        CLIUtil.runForLines(cmdBuilder.toString())
            ?.forEach { line ->
                val keyValue = line.split(tagSplitRegex, 2)
                if (keyValue.size != 2) {
                    logger.warn("Can not parse line [$line]!")
                    // 当前行无法拆分为两列时，结束本次循环。
                    return@forEach
                }

                val tagName = keyValue[0]
                if (tags.isEmpty()) {
                    // 如果未指定标签，则返回所有标签，无需匹配。
                    results[tagName] = keyValue[1].trim()
                } else {
                    // 如果指定了标签，则仅返回匹配的标签。
                    if (tags.contains(tagName)) {
                        results[tagName] = keyValue[1].trim()
                    }
                }
            }

        return results
    }

    /**
     * 读取一组标签。
     *
     * @param[file] 目标文件。
     * @param[tags] 标签数组。默认值为空数组。若数组内容为空，则读取所有标签。
     * @param[rawValue] 是否显示原始值。默认值为 `false` 。 `true` 表示以原始格式输出； `false` 表示以人类可读格式输出，具体样式可参考
     * [ExifTag] 中各标签的注释。
     * @return Map 键为标签；值为标签的值，当指定的标签不存在时值为空。内部实现为 [LinkedHashMap] ，元素顺序始终与 [tags] 一致。
     * 如果文件或标签不存在，则返回空集合。
     */
    @JvmStatic
    @JvmOverloads
    fun readTags(file: File, tags: Array<ExifTag>, rawValue: Boolean = false): Map<ExifTag, String?> {
        val nameArray: Array<String> = tags.map { it.shortName }.toTypedArray()
        val kvMap = readCustomTags(file, nameArray, rawValue)

        // `readCustomTags()` 返回 LinkedHashMap ，键的顺序与输入数组 `tags` 一致，此处将返回结果的值按顺序填充，无需匹配标签名称。
        val results: LinkedHashMap<ExifTag, String?> = LinkedHashMap(tags.size, 1.0F)
        kvMap.values.forEachIndexed { index, value ->
            results[tags[index]] = value
        }

        return results
    }

    /**
     * 读取自定义标签。
     *
     * 本方法用于读取工具内置枚举类 [ExifTag] 未包含的标签，对于受支持的标签建议使用 [readTag] 方法。
     *
     * @param[file] 目标文件。
     * @param[tag] 标签名称。
     * @param[rawValue] 是否显示原始值。默认值为 `false` 。 `true` 表示以原始格式输出； `false` 表示以人类可读格式输出，具体样式可参考
     * [ExifTag] 中各标签的注释。
     * @return 标签的值。如果文件或标签不存在，则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun readCustomTag(file: File, tag: String, rawValue: Boolean = false): String? =
        readCustomTags(file, arrayOf(tag), rawValue)[tag]

    /**
     * 读取标签。
     *
     * @param[file] 目标文件。
     * @param[tag] 标签。
     * @param[rawValue] 是否显示原始值。默认值为 `false` 。 `true` 表示以原始格式输出； `false` 表示以人类可读格式输出，具体样式可参考
     * [ExifTag] 中各标签的注释。
     * @return 标签的值。如果文件或标签不存在，则返回空值。
     */
    @JvmStatic
    @JvmOverloads
    fun readTag(file: File, tag: ExifTag, rawValue: Boolean = false): String? =
        readCustomTag(file, tag.shortName, rawValue)

    /**
     * 读取所有标签。
     *
     * @param[file] 目标文件。
     * @param[rawValue] 是否显示原始值。默认值为 `false` 。 `true` 表示以原始格式输出； `false` 表示以人类可读格式输出，具体样式可参考
     * [ExifTag] 中各标签的注释。
     * @return Map 键为标签；值为标签的值。
     */
    @JvmStatic
    @JvmOverloads
    fun readAllTags(file: File, rawValue: Boolean = false): Map<String, String> {
        logger.debug("ReadAllTags. Path:[{}]", file)

        /* 前置检查 */
        // 输入为目录时， ExifTool 将返回所有文件的标签，本方法只能返回单个文件信息，因此忽略它们。
        if (file.isDirectory) {
            logger.error("Input path is directory! Path:[{}]", file)
            return emptyMap()
        }

        val execFile = executableFile
        if (execFile == null || !execFile.canExecute()) {
            logger.error("ExifTool executable file not found or can not execute! Path:[{}]", execFile)
            return emptyMap()
        }

        val cmdBuilder = StringBuilder()
        cmdBuilder.append("${execFile.absolutePath} ")
        // 短格式（ Tag 单词之间不含空格； Tag 与分隔符之间不含空格。）
        cmdBuilder.append("-S ")
        // 是否返回原始数据
        if (rawValue) {
            cmdBuilder.append("-n ")
        }
        cmdBuilder.append("\"${file.absolutePath}\"")

        val results: LinkedHashMap<String, String> = LinkedHashMap(32, 1.0F)
        run {
            CLIUtil.runForLines(cmdBuilder.toString())
                ?.forEach { line ->
                    val keyValue = line.split(tagSplitRegex, 2)
                    if (keyValue.size != 2) {
                        logger.warn("Can not parse line [$line]!")
                        // 当前行无法拆分为两列时，结束本次循环。
                        return@forEach
                    }

                    val tagName = keyValue[0]
                    results[tagName] = keyValue[1].trim()
                }
        }

        return results
    }


    /*
     * ----- 写入标签 -----
     */

    /**
     * 写入一组自定义标签。
     *
     * 本方法用于写入工具内置枚举类 [ExifTag] 未包含的标签，对于受支持的标签建议使用 [writeTags] 方法。
     *
     * @param[input] 输入文件或目录。
     * @param[tags] 标签和值。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    fun writeCustomTags(input: File, tags: Map<String, String>, output: File? = null): Boolean {
        logger.debug(
            "WriteCustomTag. Input:[{}] Tags:[{}] Output:[{}]",
            input,
            tags.toList().joinToString(","),
            output
        )

        /* 前置检查 */
        if (!input.canRead()) {
            logger.error("Input file or directory can not read! Path:[{}]", input)
            return false
        }

        if (output != null) {
            if (!output.isDirectory) {
                logger.error("Output path is not directory! Path:[{}]", output)
                return false
            }
            if (!output.canWrite()) {
                logger.error("Output path can not write! Path:[{}]", output)
                return false
            }
        } else {
            // 修改原始文件，检查原始文件是否可写入。
            if (!input.canWrite()) {
                logger.error("Input file or directory can not write! Path:[{}]", input)
                return false
            }
        }

        val execFile = executableFile
        if (execFile == null || !execFile.canExecute()) {
            logger.error("ExifTool executable file not found or can not execute! Path:[{}]", execFile)
            return false
        }


        val cmdBuilder = StringBuilder()
        cmdBuilder.append("${execFile.absolutePath} ")
        if (output != null) {
            // 指定输出目录
            cmdBuilder.append("-o \"${output.absolutePath}\" ")
        } else {
            // 如果未指定输出目录且无需备份，则直接修改原始文件。
            cmdBuilder.append("-overwrite_original_in_place ")
        }
        tags.forEach { (key, value) ->
            // 通过 `<标签>=<值>` 语法修改标签
            cmdBuilder.append("-\"${key}\"=\"${value}\" ")
        }
        cmdBuilder.append("\"${input.absolutePath}\"")

        println(cmdBuilder.toString())

        val result = CLIUtil.runForStatus(cmdBuilder.toString())
        return CLIUtil.isSuccess(result)
    }

    /**
     * 写入一组标签。
     *
     * @param[input] 输入文件或目录。
     * @param[tags] 标签和值。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    fun writeTags(input: File, tags: Map<ExifTag, String>, output: File? = null): Boolean =
        writeCustomTags(input, tags.mapKeys { it.key.shortName }, output)

    /**
     * 写入自定义标签。
     *
     * 本方法用于写入工具内置枚举类 [ExifTag] 未包含的标签，对于受支持的标签建议使用 [writeTag] 方法。
     *
     * @param[input] 输入文件或目录。
     * @param[tag] 标签名称。
     * @param[value] 标签值。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    fun writeCustomTag(input: File, tag: String, value: String, output: File? = null): Boolean =
        writeCustomTags(input, mapOf(tag to value), output)

    /**
     * 写入标签。
     *
     * @param[input] 输入文件或目录。
     * @param[tag] 标签。
     * @param[value] 标签值。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    fun writeTag(input: File, tag: ExifTag, value: String, output: File? = null): Boolean =
        writeCustomTags(input, mapOf(tag.shortName to value), output)


    /*
     * ----- 移除标签 -----
     */

    /**
     * 移除一组自定义标签。
     *
     * 本方法用于修改工具内置枚举类 [ExifTag] 未包含的标签，对于内置标签建议使用 [deleteTags] 方法。
     *
     * @param[input] 输入文件或目录。
     * @param[tags] 标签数组。若数组为空，则移除所有标签。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    @JvmStatic
    @JvmOverloads
    fun deleteCustomTags(
        input: File,
        tags: Array<String> = emptyArray(),
        output: File? = null
    ): Boolean {
        logger.debug(
            "DeleteCustomTags. Input:[{}] Tags:[{}] Output:[{}]",
            input,
            tags.joinToString(","),
            output
        )

        /* 前置检查 */
        if (!input.canRead()) {
            logger.error("Input file or directory can not read! Path:[{}]", input)
            return false
        }

        if (output != null) {
            if (!output.isDirectory) {
                logger.error("Output path is not directory! Path:[{}]", output)
                return false
            }
            if (!output.canWrite()) {
                logger.error("Output path can not write! Path:[{}]", output)
                return false
            }
        } else {
            // 修改原始文件，检查原始文件是否可写入。
            if (!input.canWrite()) {
                logger.error("Input file or directory can not write! Path:[{}]", input)
                return false
            }
        }

        val execFile = executableFile
        if (execFile == null || !execFile.canExecute()) {
            logger.error("ExifTool executable file not found or can not execute! Path:[{}]", execFile)
            return false
        }


        val cmdBuilder = StringBuilder()
        cmdBuilder.append("${execFile.absolutePath} ")
        if (output != null) {
            // 指定输出目录
            cmdBuilder.append("-o \"${output.absolutePath}\" ")
        } else {
            // 如果未指定输出目录且无需备份，则直接修改原始文件。
            cmdBuilder.append("-overwrite_original_in_place ")
        }
        // 配置要删除的标签
        if (tags.isEmpty()) {
            // 删除所有标签
            cmdBuilder.append("-all= ")
        } else {
            tags.forEach { tag ->
                cmdBuilder.append("-\"${tag}\"= ")
            }
        }
        cmdBuilder.append("\"${input.absolutePath}\"")

        val result = CLIUtil.runForStatus(cmdBuilder.toString())
        return CLIUtil.isSuccess(result)
    }

    /**
     * 移除一组标签。
     *
     * @param[input] 输入文件或目录。
     * @param[tags] 标签数组。若数组为空，则移除所有标签。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    @JvmStatic
    @JvmOverloads
    fun deleteTags(
        input: File,
        tags: Array<ExifTag> = emptyArray(),
        output: File? = null
    ): Boolean {
        return deleteCustomTags(input, tags.map { it.shortName }.toTypedArray(), output)
    }

    /**
     * 移除自定义标签。
     *
     * 本方法用于修改工具内置枚举类 [ExifTag] 未包含的标签，对于内置标签建议使用 [deleteTags] 方法。
     *
     * @param[input] 输入文件或目录。
     * @param[tag] 标签。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    @JvmStatic
    @JvmOverloads
    fun deleteCustomTag(input: File, tag: String, output: File? = null): Boolean {
        return deleteCustomTags(input, arrayOf(tag), output)
    }

    /**
     * 移除标签。
     *
     * @param[input] 输入文件或目录。
     * @param[tag] 标签数组。若数组为空，则移除所有标签。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    @JvmStatic
    @JvmOverloads
    fun deleteTag(input: File, tag: ExifTag, output: File? = null): Boolean {
        return deleteCustomTag(input, tag.shortName, output)
    }

    /**
     * 清除所有标签。
     *
     * @param[input] 待处理的文件或目录。
     * @param[output] 输出目录。默认值为空。如果为空值表示直接修改原始文件。若输出目录存在同名文件将跳过它们。
     * @return 操作结果。 `true` 表示修改成功； `false` 表示修改失败。
     */
    @JvmStatic
    @JvmOverloads
    fun clearTags(input: File, output: File? = null): Boolean {
        logger.debug("ClearTag. Input:[{}] Output:[{}]", input, output)
        return deleteCustomTags(input, emptyArray(), output)
    }


    /*
     * ----- 可执行文件管理 -----
     */

    /**
     * 是否自动侦测可执行文件位置。
     *
     * @return `true` 表示自动侦测； `false` 表示使用用户指定的路径。
     */
    @JvmStatic
    fun isAutoDetectExecutable(): Boolean = detectExecutable

    /**
     * 获取当前 ExifTool 可执行文件。
     *
     * @return 可执行文件。如果当前采用自动侦测但没有找到可执行文件，则返回空值。
     */
    @JvmStatic
    fun getExecutableFile(): File? = executableFile

    /**
     * 设置 ExifTool 可执行文件路径。
     *
     * @param[path] 可执行文件路径。若为空值则启用自动侦测。
     */
    @JvmStatic
    fun setExecutableFile(path: String? = null) {
        if (path == null) {
            executableFile = ExifToolExecutableUtil.detectExecutable()
            detectExecutable = true
        } else {
            executableFile = File(path)
            detectExecutable = false
        }
    }
}
