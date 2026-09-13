package net.bi4vmr.study.phototool

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Composable
fun PhotoTimeModifierScreen() {
    var selectedDirectory by remember { mutableStateOf("") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // 用户输入时间
    var userDateTimeInput by remember { mutableStateOf("") }
    
    // 时间格式化器
    val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 目录选择区域
        Text("选择照片目录", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedDirectory,
                onValueChange = { selectedDirectory = it },
                label = { Text("目录路径") },
                modifier = Modifier.weight(1f),
                readOnly = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    // 这里应该使用文件选择器，但为了简化，我们先使用文本输入
                    // 实际实现中可以使用 JFileChooser 或其他文件选择对话框
                    if (selectedDirectory.isNotBlank()) {
                        val dir = File(selectedDirectory)
                        if (dir.isDirectory && dir.canRead()) {
                            files = dir.listFiles()?.filter { file ->
                                file.isFile && isImageFile(file.name)
                            } ?: emptyList()
                            statusMessage = "找到 ${files.size} 个图片文件"
                        } else {
                            statusMessage = "目录不可用或无法读取"
                        }
                    }
                },
                enabled = selectedDirectory.isNotBlank()
            ) {
                Text("扫描目录")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 时间来源选择
        Text("选择时间来源", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))
        
        var timeSource by remember { mutableStateOf("exif") }
        
        Row {
            RadioButton(
                selected = timeSource == "exif",
                onClick = { timeSource = "exif" }
            )
            Text("从EXIF读取拍摄时间")
            
            Spacer(modifier = Modifier.width(16.dp))
            
            RadioButton(
                selected = timeSource == "user",
                onClick = { timeSource = "user" }
            )
            Text("用户输入时间")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 用户输入时间（仅当选择用户输入时显示）
        if (timeSource == "user") {
            OutlinedTextField(
                value = userDateTimeInput,
                onValueChange = { userDateTimeInput = it },
                label = { Text("输入时间 (格式: yyyy-MM-dd HH:mm:ss)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 执行按钮
        Button(
            onClick = {
                isLoading = true
                statusMessage = "处理中..."
                
                try {
                    val targetDateTime = when (timeSource) {
                        "exif" -> null // 从EXIF读取
                        "user" -> {
                            try {
                                LocalDateTime.parse(userDateTimeInput, dateTimeFormatter)
                            } catch (e: DateTimeParseException) {
                                statusMessage = "时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式"
                                isLoading = false
                                return@Button
                            }
                        }
                        else -> null
                    }
                    
                    var successCount = 0
                    var failCount = 0
                    
                    files.forEach { file ->
                        try {
                            val dateTime = when (timeSource) {
                                "exif" -> readExifDateTime(file)
                                "user" -> targetDateTime
                                else -> null
                            }
                            
                            if (dateTime != null) {
                                setFileModifyTime(file, dateTime)
                                successCount++
                            } else {
                                failCount++
                            }
                        } catch (e: Exception) {
                            failCount++
                        }
                    }
                    
                    statusMessage = "处理完成: 成功 $successCount 个, 失败 $failCount 个"
                } catch (e: Exception) {
                    statusMessage = "处理出错: ${e.message}"
                } finally {
                    isLoading = false
                }
            },
            enabled = files.isNotEmpty() && !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("修改文件时间")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 状态信息
        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 文件列表
        if (files.isNotEmpty()) {
            Text("文件列表 (${files.size} 个文件)", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            
            files.forEach { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.body1
                        )
                        Text(
                            text = "路径: ${file.absolutePath}",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                        
                        // 显示EXIF时间（如果可用）
                        val exifTime = readExifDateTime(file)
                        if (exifTime != null) {
                            Text(
                                text = "EXIF时间: ${exifTime.format(dateTimeFormatter)}",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 检查文件是否为图片文件
 */
private fun isImageFile(filename: String): Boolean {
    val imageExtensions = listOf(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "webp"
    )
    return imageExtensions.any { extension ->
        filename.lowercase().endsWith(".$extension")
    }
}

/**
 * 从文件EXIF数据中读取拍摄时间
 */
private fun readExifDateTime(file: File): LocalDateTime? {
    return try {
        val metadata = ImageMetadataReader.readMetadata(file)
        val directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        
        directory?.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)?.let { date ->
            // 将java.util.Date转换为LocalDateTime
            date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 设置文件的修改时间
 */
private fun setFileModifyTime(file: File, dateTime: LocalDateTime) {
    val zoneId = ZoneId.systemDefault()
    val zonedDateTime = dateTime.atZone(zoneId)
    val millis = zonedDateTime.toInstant().toEpochMilli()
    
    file.setLastModified(millis)
}
