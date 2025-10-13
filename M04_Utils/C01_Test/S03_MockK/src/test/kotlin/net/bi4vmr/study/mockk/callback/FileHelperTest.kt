package net.bi4vmr.study.mockk.callback

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

/**
 * FileHelper的测试类。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class FileHelperTest {

    @Test
    fun `test saveFile`() {
        val fileHelper = FileHelper()
        // 创建Callback的Mock对象
        val mockCallback: FileCallback = mockk(relaxed = true)

        // 调用待测方法，传入Callback的Mock对象
        val invalidPath = "/invalid_path.txt"
        fileHelper.saveFile(invalidPath, mockCallback)

        // 定义捕获器， `slot()` 方法的泛型即参数类型。
        val captorResult = slot<String>()

        // 验证回调方法已触发，并使用 `capture()` 方法捕获第二个参数。
        verify {
            mockCallback.onResult(any(), capture(captorResult))
        }

        // 查看捕获到的参数值
        val capturedValue: String = captorResult.captured
        println("捕获到的参数值:[$capturedValue]")
        // 进一步验证该参数值
        assertFalse(capturedValue.startsWith("OK"))
    }

    @Test
    fun `test saveFile2`() {
        val capturedValues = mutableListOf<String>()

        val fileHelper = FileHelper()
        // 创建Callback的Mock对象
        val mockCallback: FileCallback = mockk(relaxed = true)

        // 多次调用待测方法，传入Callback的Mock对象
        val invalidPath = "/invalid_path.txt"
        fileHelper.saveFile(invalidPath, mockCallback)
        fileHelper.saveFile(invalidPath, mockCallback)
        val validPath = "/tmp/valid_path.txt"
        fileHelper.saveFile(validPath, mockCallback)
        File(validPath).deleteOnExit()

        // 验证回调方法已触发，并使用 `capture()` 方法捕获第二个参数。
        verify(exactly = 3) {
            mockCallback.onResult(any(), capture(capturedValues))
        }

        // 查看捕获到的参数值
        capturedValues.forEachIndexed { index, s ->
            println("Index:[$index] Value:[$s]")
        }
    }
}
