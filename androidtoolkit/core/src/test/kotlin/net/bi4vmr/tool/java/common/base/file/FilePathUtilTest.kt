package net.bi4vmr.tool.java.common.base.file

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * [FilePathUtil] 功能测试。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class FilePathUtilTest {

    @Test
    fun test_GetType() {
        assertEquals(FilePathType.UNIX, FilePathUtil.getType("/"))
        assertEquals(FilePathType.UNIX, FilePathUtil.getType("/data/local/tmp/test.txt"))
        assertEquals(FilePathType.UNIX, FilePathUtil.getType("/data/"))
        assertEquals(FilePathType.WINDOWS, FilePathUtil.getType("C:\\"))
        assertEquals(FilePathType.WINDOWS, FilePathUtil.getType("D:\\Data\\test.txt"))
        assertEquals(FilePathType.UNC, FilePathUtil.getType("\\\\PC_Home\\Share\\test.txt"))
        assertEquals(FilePathType.RELATIVE, FilePathUtil.getType("test.txt"))
        assertEquals(FilePathType.RELATIVE, FilePathUtil.getType(""))
        assertEquals(FilePathType.RELATIVE, FilePathUtil.getType(null))
    }

    @Test
    fun test_IsRoot() {
        assertTrue(FilePathUtil.isRoot("/"))
        assertTrue(FilePathUtil.isRoot("C:\\"))
        assertTrue(FilePathUtil.isRoot("\\\\PC_Home"))

        assertFalse(FilePathUtil.isRoot("/data/local/tmp/"))
        assertFalse(FilePathUtil.isRoot("D:\\Data\\test.txt"))
        assertFalse(FilePathUtil.isRoot("\\\\PC_Home\\Share\\test.txt"))
        assertFalse(FilePathUtil.isRoot(""))
        assertFalse(FilePathUtil.isRoot(null))
    }

    @Test
    fun test_IsDirectory() {
        assertTrue(FilePathUtil.isDirectory("/"))
        assertTrue(FilePathUtil.isDirectory("/data/local/tmp/"))
        assertTrue(FilePathUtil.isDirectory("D:\\Data\\"))
        assertTrue(FilePathUtil.isDirectory("\\\\PC_Home\\Share\\"))
        assertTrue(FilePathUtil.isDirectory(""))
        assertTrue(FilePathUtil.isDirectory(null))

        assertFalse(FilePathUtil.isDirectory("/data/local/tmp"))
        assertFalse(FilePathUtil.isDirectory("D:\\Data\\test.txt"))
        assertFalse(FilePathUtil.isDirectory("\\\\PC_Home\\Share\\test.txt"))
    }

    @Test
    fun test_GetParent() {
        // Unix 路径
        assertEquals("/data/local/tmp", FilePathUtil.getParent("/data/local/tmp/test.txt"))
        assertEquals("/data/local", FilePathUtil.getParent("/data/local/tmp/"))
        assertEquals("/", FilePathUtil.getParent("/test.txt"))
        assertEquals("/", FilePathUtil.getParent("/data/"))
        assertEquals(null, FilePathUtil.getParent("/"))

        // Windows 路径
        assertEquals("D:\\Data", FilePathUtil.getParent("D:\\Data\\test.txt"))
        assertEquals("D:\\", FilePathUtil.getParent("D:\\Data\\"))
        assertEquals("D:\\", FilePathUtil.getParent("D:\\Data"))
        assertEquals("D:\\", FilePathUtil.getParent("D:\\file.txt"))
        assertEquals(null, FilePathUtil.getParent("D:\\"))

        // UNC 路径
        assertEquals("\\\\PC_Home\\Share", FilePathUtil.getParent("\\\\PC_Home\\Share\\test.txt"))
        assertEquals("\\\\PC_Home\\", FilePathUtil.getParent("\\\\PC_Home\\Share\\"))
        assertEquals(null, FilePathUtil.getParent("\\\\PC_Home"))

        // 相对路径
        assertEquals("a/b/", FilePathUtil.getParent("a/b/c.txt"))
        assertEquals(".", FilePathUtil.getParent("a/"))
        assertEquals(".", FilePathUtil.getParent("test.txt"))
        assertEquals(".", FilePathUtil.getParent(""))
        assertEquals(".", FilePathUtil.getParent(null))
    }

    @Test
    fun test_GetFileName() {
        // Unix 路径
        assertEquals("test.txt", FilePathUtil.getFileName("/data/local/tmp/test.txt"))
        assertEquals("tmp", FilePathUtil.getFileName("/data/local/tmp/"))
        assertEquals("", FilePathUtil.getFileName("/"))

        // 相对路径
        assertEquals("test.txt", FilePathUtil.getFileName("test.txt"))
        assertEquals("b", FilePathUtil.getFileName("a/b/"))

        // 空路径与根路径均返回空字符串。
        assertEquals("", FilePathUtil.getFileName(""))
        assertEquals("", FilePathUtil.getFileName(null))

        // Windows 路径
        assertEquals("test.txt", FilePathUtil.getFileName("D:\\Data\\test.txt"))
        assertEquals("Data", FilePathUtil.getFileName("D:\\Data\\"))
        assertEquals("", FilePathUtil.getFileName("D:\\"))
        assertEquals("file.txt", FilePathUtil.getFileName("D:file.txt"))

        // UNC 路径
        assertEquals("test.txt", FilePathUtil.getFileName("\\\\PC_Home\\Share\\test.txt"))
        assertEquals("Share", FilePathUtil.getFileName("\\\\PC_Home\\Share\\"))
    }

    @Test
    fun test_GetExtension() {
        assertEquals("txt", FilePathUtil.getExtension("/data/local/tmp/test.txt"))
        assertEquals("gz", FilePathUtil.getExtension("/data/local/tmp/test.tar.gz"))
        assertEquals("txt", FilePathUtil.getExtension("D:\\Data\\test.txt"))
        assertEquals("txt", FilePathUtil.getExtension("test.txt"))

        // 目录没有扩展名。
        assertEquals("", FilePathUtil.getExtension("/data/local/tmp/"))
        assertEquals("", FilePathUtil.getExtension("D:\\Data\\"))

        // 无点、点在末尾、点在首位（隐藏文件）均视为无扩展名。
        assertEquals("", FilePathUtil.getExtension("/data/local/tmp/README"))
        assertEquals("", FilePathUtil.getExtension("/data/local/tmp/test."))
        assertEquals("", FilePathUtil.getExtension("/data/local/tmp/.gitignore"))
        assertEquals("", FilePathUtil.getExtension(".gitignore"))

        // 点位于上级目录时，不影响文件本身的扩展名判定。
        assertEquals("", FilePathUtil.getExtension("/data/local.tmp/test"))

        assertEquals("", FilePathUtil.getExtension(""))
        assertEquals("", FilePathUtil.getExtension(null))
    }
}
