package net.bi4vmr.androidtoolkit.ui.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.bi4vmr.androidtoolkit.core.LogicManager
import net.bi4vmr.androidtoolkit.core.model.DeviceFile
import net.bi4vmr.androidtoolkit.ui.GlobalStateManager
import net.bi4vmr.androidtoolkit.ui.common.Dimens
import net.bi4vmr.androidtoolkit.ui.common.icon.Icons
import net.bi4vmr.androidtoolkit.ui.component.DeviceSelectPanel
import net.bi4vmr.androidtoolkit.ui.component.XOutlinedButton
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFileTransferListener

/**
 * 文件管理页面。
 *
 * 交互方式参考 Dolphin ：顶部为路径导航栏（面包屑 / 可编辑文本框），下方为当前目录的文件列表。
 */
@Composable
fun FilesPage(globalStateManager: GlobalStateManager) {
    val logicManager: LogicManager = globalStateManager.logicManager
    val viewModel: FilesPageViewModel = viewModel { FilesPageViewModel(logicManager) }

    val currentPath: String by viewModel.currentPathFlow.collectAsStateWithLifecycle()
    val listState: FilesPageViewModel.ListState by viewModel.listStateFlow.collectAsStateWithLifecycle()
    val canGoBack: Boolean by viewModel.canGoBackFlow.collectAsStateWithLifecycle()
    val canGoForward: Boolean by viewModel.canGoForwardFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Row(modifier = Modifier.fillMaxSize()) {
        DeviceSelectPanel(
            globalStateManager = globalStateManager,
            modifier = Modifier.width(280.dp).fillMaxHeight()
        )

        VerticalDivider()

        Column(modifier = Modifier.weight(1F)) {
            NavigationBar(
                path = currentPath,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onBack = viewModel::goBack,
                onForward = viewModel::goForward,
                onUp = viewModel::goUp,
                onRefresh = viewModel::refresh,
                onNavigate = viewModel::navigateTo
            )

            HorizontalDivider()

            FileList(
                state = listState,
                onOpen = { file -> if (file.isDirectory) viewModel.navigateTo(file.path) },
                modifier = Modifier.weight(1F).fillMaxWidth()
            )

            XOutlinedButton(
                onClick = {
                    globalStateManager.logicManager.getSelectedExecutor().let { executor ->
                        val d = globalStateManager.logicManager.getSelectedDevice()!!
                        scope.launch {
                            executor.pushFile(
                                sn = d.getSN(),
                                local = "/home/bi4vmr/Downloads/jadx-1.5.2.zip",
                                remote = "/data/local/tmp/",
                                refreshMediaDB = true,
                                listener = object : ADBFileTransferListener() {
                                    override fun onStart(size: Long) {
                                        println("onStart: size=$size")
                                    }

                                    override fun onProgress(size: Long, progress: Double) {
                                        println("onProgress: size=$size, progress=$progress")
                                    }

                                    override fun onEnd() {
                                        println("onEnd")
                                    }
                                }
                            )
                        }
                    }
                }
            ) {
                Text("上传")
            }
        }
    }
}


/**
 * 顶部导航栏：历史按钮 + 路径栏 + 模式切换按钮。
 */
@Composable
private fun NavigationBar(
    path: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var editMode: Boolean by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.Layout.SpaceSmall),
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = Dimens.Layout.Space, vertical = Dimens.Layout.SpaceSmall)
    ) {
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(Icons.Back, contentDescription = "后退")
        }
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(Icons.Forward, contentDescription = "前进")
        }
        IconButton(onClick = onUp, enabled = (path != FilesPageViewModel.ROOT)) {
            Icon(Icons.Back_2, contentDescription = "上级目录")
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Refresh, contentDescription = "刷新")
        }

        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .weight(1F)
                .height(36.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
        ) {
            if (editMode) {
                PathEditor(
                    path = path,
                    onConfirm = {
                        onNavigate(it)
                        editMode = false
                    },
                    onCancel = { editMode = false }
                )
            } else {
                Breadcrumb(
                    path = path,
                    onNavigate = onNavigate,
                    onBlankClick = { editMode = true }
                )
            }
        }

        IconButton(onClick = { editMode = !editMode }) {
            if (editMode) {
                Icon(Icons.Close, contentDescription = "切换为导航模式")
            } else {
                Icon(Icons.Add, contentDescription = "切换为编辑模式")
            }
        }
    }
}

/**
 * 面包屑模式：每一级目录均可点击跳转，点击空白处切换为编辑模式。
 */
@Composable
private fun Breadcrumb(path: String, onNavigate: (String) -> Unit, onBlankClick: () -> Unit) {
    val segments: List<String> = path.split("/").filter { it.isNotEmpty() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize().clickable(onClick = onBlankClick)
            .padding(horizontal = Dimens.Layout.SpaceSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            BreadcrumbItem(text = "/", current = segments.isEmpty()) { onNavigate(FilesPageViewModel.ROOT) }

            segments.forEachIndexed { index, name ->
                if (index > 0) {
                    Icon(
                        Icons.ScreenShot,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.Icon.Small)
                    )
                }
                val target = "/" + segments.subList(0, index + 1).joinToString("/")
                BreadcrumbItem(text = name, current = (index == segments.lastIndex)) { onNavigate(target) }
            }
        }
        Spacer(Modifier.weight(1F))
    }
}

@Composable
private fun BreadcrumbItem(text: String, current: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.Layout.SpaceSmall, vertical = 2.dp)
    )
}

/**
 * 编辑模式：直接输入路径，回车跳转，Esc 取消。
 */
@Composable
private fun PathEditor(path: String, onConfirm: (String) -> Unit, onCancel: () -> Unit) {
    var value: TextFieldValue by remember(path) {
        mutableStateOf(TextFieldValue(path, selection = TextRange(0, path.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onConfirm(value.text) }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.Layout.Space)
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        onConfirm(value.text)
                        true
                    }

                    Key.Escape -> {
                        onCancel()
                        true
                    }

                    else -> false
                }
            }
    )
}


/**
 * 文件列表：双击目录进入。
 */
@Composable
private fun FileList(state: FilesPageViewModel.ListState, onOpen: (DeviceFile) -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        FilesPageViewModel.ListState.NoDevice -> CenterHint("请先选择设备", modifier)
        FilesPageViewModel.ListState.Loading -> Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is FilesPageViewModel.ListState.Error -> CenterHint("无法打开目录：${state.path}", modifier)
        is FilesPageViewModel.ListState.Success -> {
            if (state.files.isEmpty()) {
                CenterHint("目录为空", modifier)
                return
            }

            var selected: String? by remember(state) { mutableStateOf(null) }

            Column(modifier) {
                FileHeader()
                HorizontalDivider()
                LazyColumn(Modifier.weight(1F).fillMaxWidth()) {
                    items(state.files, key = { it.path }) { file ->
                        FileRow(
                            file = file,
                            selected = (file.path == selected),
                            onClick = { selected = file.path },
                            onDoubleClick = { onOpen(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterHint(text: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FileHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = Dimens.Layout.Padding, vertical = Dimens.Layout.SpaceSmall)
    ) {
        val style = MaterialTheme.typography.labelMedium
        Spacer(Modifier.width(Dimens.Icon.Small + Dimens.Layout.Space))
        Text("名称", style = style, modifier = Modifier.weight(1F))
        Text("大小", style = style, modifier = Modifier.width(100.dp))
        Text("修改时间", style = style, modifier = Modifier.width(140.dp))
        Text("权限", style = style, modifier = Modifier.width(110.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(file: DeviceFile, selected: Boolean, onClick: () -> Unit, onDoubleClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
            .padding(horizontal = Dimens.Layout.Padding, vertical = Dimens.Layout.ListItemPadding)
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Folder else Icons.File,
            contentDescription = null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.Icon.Small)
        )
        Spacer(Modifier.width(Dimens.Layout.Space))
        Text(
            text = if (file.isLink && file.linkTarget != null) "${file.name} → ${file.linkTarget}" else file.name,
            fontStyle = if (file.isLink) FontStyle.Italic else FontStyle.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1F)
        )
        Text(
            text = if (file.isDirectory) "-" else formatSize(file.size),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(100.dp)
        )
        Text(file.modifyTime, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(140.dp))
        Text(file.permissions, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(110.dp))
    }
}

private fun formatSize(size: Long): String {
    if (size < 1024) return "$size B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = size.toDouble()
    var index = -1
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return "%.1f %s".format(value, units[index])
}
