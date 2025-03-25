package net.bi4vmr.study.mockk.callback

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File


/**
 * TODO 添加描述。
 *
 * @author BI4VMR@outlook.com
 * @since 1.0.0
 */
class FileHelperTest {

    private val fileHelper = FileHelper()

    @Test
    fun `test saveFile with valid path`() {
        // Mock the FileCallback
        val callback = mockk<FileCallback>(relaxed = true)

        // Prepare a valid path
        val validPath = "/home/bi4vmr/valid_path.txt"
        File(validPath).deleteOnExit() // Ensure the file is deleted after the test

        // Call the method under test
        fileHelper.saveFile(validPath, callback)

        // Verify that the callback was called with the correct parameters
        verify { callback.onResult(true, "OK!") }
    }

    @Test
    fun `test saveFile with invalid path`() {
        // Mock the FileCallback
        val callback = mockk<FileCallback>(relaxed = true)

        // Prepare an invalid path
        val invalidPath = "/valid_path.txt"

        // Call the method under test
        fileHelper.saveFile(invalidPath, callback)

        // Verify that the callback was called with the correct parameters
        verify { callback.onResult(false, "Error!") }
    }

    @Test
    fun `test saveFile with empty path`() {
        // 创建mock对象
        val mockCallback = mockk<FileCallback>(relaxed = true)

        // 创建服务并将mock对象设置为回调
        val invalidPath = "/valid_path.txt"
        fileHelper.saveFile(invalidPath, mockCallback)

        // 创建ArgumentCaptor
        val captor = slot<Boolean>()
        val captor2 = slot<String>()

        // 验证execute被调用且参数被捕获
        verify {
            mockCallback.onResult(capture(captor), capture(captor2))
        }

        // 获取并断言列表参数
        val captured1 = captor.captured
        val captured2 = captor2.captured
        println("captured1:[$captured1] captured2:[$captured2]")
        assertEquals(false, captured1)
    }
}
