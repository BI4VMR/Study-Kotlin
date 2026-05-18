package net.bi4vmr.study

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LogStat Tool",
        state = rememberWindowState(width = 960.dp, height = 720.dp)
    ) {
        MaterialTheme {
            LogStatApp(window = window)
        }
    }
}

@Composable
fun LogStatApp(window: ComposeWindow) {
    var path by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(StatMode.BY_PACKAGE) }
    var aggregateByPackage by remember { mutableStateOf(true) }
    var extractCrashAnr by remember { mutableStateOf(false) }
    var aggregateCrashAnr by remember { mutableStateOf(true) }
    var deepSeekApiKey by remember { mutableStateOf(loadDeepSeekApiKey()) }
    var deepSeekModel by remember { mutableStateOf(loadDeepSeekModel()) }
    var output by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var isDragOver by remember { mutableStateOf(false) }
    // 记住文件浏览器上次打开的目录，程序运行期间持续有效
    var lastBrowseDir by remember { mutableStateOf<File?>(null) }

    val scope = rememberCoroutineScope()

    // 在 AWT 窗口上注册拖拽监听器，支持整窗口拖入文件/目录
    DisposableEffect(window) {
        val dropTarget = DropTarget(window, object : DropTargetAdapter() {
            override fun dragOver(dtde: DropTargetDragEvent) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    isDragOver = true
                    dtde.acceptDrag(DnDConstants.ACTION_COPY)
                }
            }

            override fun dragExit(dte: DropTargetEvent) {
                isDragOver = false
            }

            override fun drop(event: DropTargetDropEvent) {
                isDragOver = false
                event.acceptDrop(DnDConstants.ACTION_COPY)
                runCatching {
                    val files = event.transferable
                        .getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                    (files.firstOrNull() as? File)?.let { path = it.absolutePath }
                    event.dropComplete(true)
                }.onFailure { event.dropComplete(false) }
            }
        })
        onDispose { dropTarget.isActive = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDragOver) Color(0xFFE8F0FE) else MaterialTheme.colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 路径输入行
        PathInputRow(
            path = path,
            isDragOver = isDragOver,
            lastBrowseDir = lastBrowseDir,
            onPathChange = { path = it },
            onBrowseDirChange = { lastBrowseDir = it }
        )

        // 模式选择 + 操作按钮行
        ModeAndActionRow(
            mode = mode,
            aggregateByPackage = aggregateByPackage,
            extractCrashAnr = extractCrashAnr,
            aggregateCrashAnr = aggregateCrashAnr,
            deepSeekApiKey = deepSeekApiKey,
            deepSeekModel = deepSeekModel,
            isRunning = isRunning,
            onModeChange = { mode = it },
            onAggregateChange = { aggregateByPackage = it },
            onExtractCrashAnrChange = { extractCrashAnr = it },
            onAggregateCrashAnrChange = { aggregateCrashAnr = it },
            onDeepSeekApiKeyChange = { deepSeekApiKey = it },
            onDeepSeekModelChange = { deepSeekModel = it },
            onAnalyze = {
                if (path.isBlank()) return@ModeAndActionRow
                scope.launch {
                    isRunning = true
                    output = "Analyzing...\n"
                    val result = withContext(Dispatchers.IO) {
                        captureStdout {
                            runLogStat(
                                path,
                                mode,
                                aggregateByPackage,
                                extractCrashAnr,
                                aggregateCrashAnr,
                                deepSeekApiKey.trim(),
                                deepSeekModel.trim()
                            )
                        }
                    }
                    output = result
                    isRunning = false
                }
            },
            onClear = { output = "" }
        )

        // 结果输出区域
        OutputPanel(output = output, isRunning = isRunning)
    }
}

// -----------------------------------------------------------------------------
// 子组件
// -----------------------------------------------------------------------------

@Composable
private fun PathInputRow(
    path: String,
    isDragOver: Boolean,
    lastBrowseDir: File?,
    onPathChange: (String) -> Unit,
    onBrowseDirChange: (File?) -> Unit
) {
    val borderColor = if (isDragOver) Color(0xFF4285F4) else Color.LightGray

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Path:", style = MaterialTheme.typography.body1)
            OutlinedTextField(
                value = path,
                onValueChange = onPathChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("输入路径，或将文件/目录拖入窗口...", color = Color.Gray) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = borderColor,
                    unfocusedBorderColor = borderColor
                )
            )
            Button(onClick = {
                // 优先使用上次记住的目录；否则尝试从当前路径推导；最终回退到用户主目录
                val initDir: File? = lastBrowseDir
                    ?: File(path).takeIf { it.exists() }?.let { if (it.isDirectory) it else it.parentFile }

                val chooser = javax.swing.JFileChooser(initDir).apply {
                    fileSelectionMode = javax.swing.JFileChooser.FILES_AND_DIRECTORIES
                    dialogTitle = "选择日志文件或目录"
                }
                if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                    val selected = chooser.selectedFile
                    onPathChange(selected.absolutePath)
                    // 记住本次浏览的目录，供下次打开时使用
                    onBrowseDirChange(if (selected.isDirectory) selected else selected.parentFile)
                }
            }) {
                Text("Browse...")
            }
        }
        // 拖拽提示
        if (isDragOver) {
            Text(
                text = "  ↑ 松开鼠标完成拖入",
                color = Color(0xFF4285F4),
                style = MaterialTheme.typography.caption
            )
        } else {
            Text(
                text = "  支持拖拽文件或目录到窗口任意位置",
                color = Color.Gray,
                style = MaterialTheme.typography.caption
            )
        }
    }
}

@Composable
private fun ModeAndActionRow(
    mode: StatMode,
    aggregateByPackage: Boolean,
    extractCrashAnr: Boolean,
    aggregateCrashAnr: Boolean,
    deepSeekApiKey: String,
    deepSeekModel: String,
    isRunning: Boolean,
    onModeChange: (StatMode) -> Unit,
    onAggregateChange: (Boolean) -> Unit,
    onExtractCrashAnrChange: (Boolean) -> Unit,
    onAggregateCrashAnrChange: (Boolean) -> Unit,
    onDeepSeekApiKeyChange: (String) -> Unit,
    onDeepSeekModelChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mode:", style = MaterialTheme.typography.body1)

            // 两个单选项
            listOf(
                StatMode.BY_PACKAGE to "By Package（按包名）",
                StatMode.BY_TAG to "By TAG（按标签）"
            ).forEach { (statMode, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(enabled = !isRunning) { onModeChange(statMode) }
                        .padding(end = 8.dp)
                ) {
                    RadioButton(
                        selected = mode == statMode,
                        onClick = { onModeChange(statMode) },
                        enabled = !isRunning
                    )
                    Text(label)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(onClick = onClear, enabled = !isRunning) {
                Text("Clear")
            }
            Button(onClick = onAnalyze, enabled = !isRunning) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Running...")
                } else {
                    Text("Analyze")
                }
            }
        }

        // BY_PACKAGE 模式下显示聚合选项
        if (mode == StatMode.BY_PACKAGE) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(enabled = !isRunning) { onAggregateChange(!aggregateByPackage) }
                    .padding(start = 4.dp)
            ) {
                Checkbox(
                    checked = aggregateByPackage,
                    onCheckedChange = { onAggregateChange(it) },
                    enabled = !isRunning
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "按包名聚合（合并同包名的所有 PID，不显示 PID 明细）",
                    style = MaterialTheme.typography.body2
                )
            }
        }

        // 提取 Crash/ANR 片段选项（始终可见）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(enabled = !isRunning) { onExtractCrashAnrChange(!extractCrashAnr) }
                .padding(start = 4.dp)
        ) {
            Checkbox(
                checked = extractCrashAnr,
                onCheckedChange = { onExtractCrashAnrChange(it) },
                enabled = !isRunning
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "提取 Crash/ANR 日志片段（保存到 <输入路径>_crashanr/ 目录，并统计每 PID 发生次数）",
                style = MaterialTheme.typography.body2
            )
        }

        // 聚合选项（仅在提取开启时显示）
        if (extractCrashAnr) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(enabled = !isRunning) { onAggregateCrashAnrChange(!aggregateCrashAnr) }
                    .padding(start = 28.dp)  // 缩进，视觉上归属于上一项
            ) {
                Checkbox(
                    checked = aggregateCrashAnr,
                    onCheckedChange = { onAggregateCrashAnrChange(it) },
                    enabled = !isRunning
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "聚合相同问题（相同异常类+调用栈顶帧 / 相同 ANR 原因合并为一个文件，统计按问题分组）",
                    style = MaterialTheme.typography.body2
                )
            }

            // DeepSeek API Key 输入框
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 28.dp).fillMaxWidth()
            ) {
                Text(
                    text = "DeepSeek API Key:",
                    style = MaterialTheme.typography.body2,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = deepSeekApiKey,
                    onValueChange = onDeepSeekApiKeyChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isRunning,
                    placeholder = {
                        Text(
                            "留空跳过 AI 分析，填入后对每个片段文件生成 .analysis.md",
                            color = Color.Gray,
                            style = MaterialTheme.typography.caption
                        )
                    },
                    textStyle = MaterialTheme.typography.body2.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 28.dp).fillMaxWidth()
            ) {
                Text(
                    text = "DeepSeek Model:",
                    style = MaterialTheme.typography.body2,
                    color = Color.DarkGray
                )
                OutlinedTextField(
                    value = deepSeekModel,
                    onValueChange = onDeepSeekModelChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isRunning,
                    placeholder = {
                        Text(
                            "模型名称，例如 DeepSeek-V32",
                            color = Color.Gray,
                            style = MaterialTheme.typography.caption
                        )
                    },
                    textStyle = MaterialTheme.typography.body2.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )
            }
        }
    }
}

@Composable
private fun OutputPanel(output: String, isRunning: Boolean) {
    val scrollState = rememberScrollState()

    // 仅在内容追加时自动滚到底部（分析结束后 output 变化触发一次）
    LaunchedEffect(output) {
        if (output.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = MaterialTheme.shapes.small
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // verticalScroll 必须在 SelectionContainer 外层：
            // SelectionContainer 会拦截指针事件用于文字选中，若 scroll 在其内部则无法滚动。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 12.dp) // 为右侧滚动条留出空间
            ) {
                SelectionContainer(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = output.ifEmpty {
                            if (isRunning) "" else "分析结果将显示在此处..."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (output.isEmpty() && !isRunning) Color.Gray else Color.Unspecified
                    )
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

// -----------------------------------------------------------------------------
// 工具函数
// -----------------------------------------------------------------------------

/**
 * 执行 [block]，捕获其 stdout 输出并返回为字符串。
 * 用于将 runLogStat 的 println 输出收集后展示到 Compose UI 中。
 */
private fun captureStdout(block: () -> Unit): String {
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos, true, Charsets.UTF_8)
    val orig = System.out
    System.setOut(ps)
    try {
        block()
    } finally {
        ps.flush()
        System.setOut(orig)
    }
    return baos.toString(Charsets.UTF_8)
}
