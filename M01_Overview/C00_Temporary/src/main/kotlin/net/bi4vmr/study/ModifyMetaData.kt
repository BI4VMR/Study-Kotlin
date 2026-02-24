package net.bi4vmr.study

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants
import java.io.File
import java.io.FileOutputStream

val mutex = Mutex()
var counter = 0

fun main() = runBlocking {
    // val jobs = List(10) { // 启动10个协程
    //     launch(Dispatchers.Default) {
    //         repeat(10) { // 每个协程递增计数器1000次
    //             mutex.withLock { // 自动加锁/解锁
    //                 println("th: ${Thread.currentThread().name} mutex isLocked:${mutex.isLocked}")
    //                 delay(10L)
    //                 counter++
    //             }
    //         }
    //     }
    // }
    // jobs.forEach { it.join() }
    // println("Final counter: $counter") // 正确输出10000

    val filePath = "C:\\Users\\bi4vmr\\Pictures\\原创照片\\事件・20251105_月球观测・制品\\20251106_205206.jpg"

    try {
        val imageFile = File(filePath)
        // val metadata = ImageMetadataReader.readMetadata(imageFile)
        //
        // // 读取焦距
        // val exifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        // exifSubIFDDirectory?.let {
        //     if (it.containsTag(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)) {
        //         println("原始焦距: ${it.getDescription(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)}")
        //     }
        // }
        //
        // // 修改焦距（注意：drewnoakes-metadata-extractor 不支持直接修改文件）
        // println("焦距无法直接修改，需使用其他工具生成新文件。")

        val m = Imaging.getMetadata(imageFile)
        val jm = m as JpegImageMetadata
        jm.exif.items.forEach {
            println(it)
        }
        val oSet = jm.exif.outputSet
        val oD = oSet.orCreateExifDirectory
        oD.removeField(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH_IN_35MM_FORMAT)
        oD.add(ExifTagConstants.EXIF_TAG_FOCAL_LENGTH_IN_35MM_FORMAT, 900)
        val newFilePath =
            "C:\\Users\\bi4vmr\\Pictures\\原创照片\\事件・20251105_月球观测・制品\\20251106_205206_modify.jpg"
        val fos = FileOutputStream(File(newFilePath))
        ExifRewriter().updateExifMetadataLossless(imageFile, fos, oSet)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 示例：retrun
 */
fun example01() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    array.forEach {
        if (it == 2) {
            // 返回foreach之外
            return
        }

        println("Loop $it")
    }

    println("Some statement after loop...")
}

fun example02() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    array.forEach {
        if (it == 2) {
            // 相当于"for"循环中的"continue"
            return@forEach
        }

        println("Loop $it")
    }

    println("Some statement after loop...")
}

/**
 * 示例：类似break
 */
fun example03() {
    val array: Array<Int> = arrayOf(1, 2, 3, 4, 5)
    // 使用"forEach()"方法遍历数组
    run loop@{
        array.forEach {
            if (it == 3) {
                // 相当于"for"循环中的"break"
                return@loop
            }

            println("Loop $it")
        }
    }

    println("Some statement after loop...")
}
