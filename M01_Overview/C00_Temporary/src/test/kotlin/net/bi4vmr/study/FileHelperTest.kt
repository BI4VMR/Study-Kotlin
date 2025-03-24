package net.bi4vmr.study

import io.mockk.mockk
import io.mockk.verify
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
}
