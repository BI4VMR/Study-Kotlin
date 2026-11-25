package net.bi4vmr.tool.kotlin.external.adb.model

import com.android.ddmlib.FileListingService
import com.android.ddmlib.IDevice
import com.android.ddmlib.SyncException
import com.android.ddmlib.SyncService
import net.bi4vmr.tool.java.common.base.file.FilePathUtil
import net.bi4vmr.tool.java.common.base.file.FileUtil
import net.bi4vmr.tool.java.common.base.general.TextUtil
import net.bi4vmr.tool.java.common.base.system.CLIUtil
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFileTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFolderTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.enums.PackageType
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayDensity
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplaySize
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.WindowInfo
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBCLIUtil
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.Terminal
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.Terminals
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ADB 本地设备。
 *
 * [getSN]、[getState]、[isOnline] 方法为内存读写，不必在子线程调用；其他方法耗时可能较长，建议在非 UI 线程调用。
 *
 * 除了引用 [ddmDevice] 的方法，其他方法均为 [ADBCLIUtil] 的快捷调用，例如：[ADBCLIUtil.screenshotAsPNG] 在此处可以针对当前设备调用，
 * 省略 `sn` 参数。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
data class LocalDevice(

    /**
     * 设备状态。
     */
    private val ddmState: IDevice.DeviceState,

    /**
     * DDMLib 的 Device 实例。
     */
    val ddmDevice: IDevice
) : ADBDevice {

    /*
     * ----- 连接信息 -----
     */

    /**
     * 获取序列号。
     *
     * @return 序列号。
     */
    override fun getSN(): String = ddmDevice.serialNumber

    /**
     * 获取设备状态。
     *
     * @return 枚举常量。
     */
    override fun getState(): DeviceState {
        return when (ddmState) {
            IDevice.DeviceState.ONLINE -> DeviceState.ONLINE
            IDevice.DeviceState.UNAUTHORIZED -> DeviceState.UNAUTHORIZED
            IDevice.DeviceState.AUTHORIZING -> DeviceState.AUTHORIZING
            IDevice.DeviceState.BOOTLOADER -> DeviceState.BOOTLOADER
            IDevice.DeviceState.FASTBOOTD -> DeviceState.FASTBOOTD
            IDevice.DeviceState.RECOVERY -> DeviceState.RECOVERY
            IDevice.DeviceState.SIDELOAD -> DeviceState.SIDELOAD
            IDevice.DeviceState.OFFLINE -> DeviceState.OFFLINE
            IDevice.DeviceState.DISCONNECTED -> DeviceState.DISCONNECTED
        }
    }


    /*
     * ----- 常用命令 -----
     */

    /**
     * 执行 ADB Shell 命令。
     *
     * @param[command] 命令语句。
     * @param[redirectError] 是否重定向错误输出流。默认值为 `true`。
     * @return [Process] 实例，永不为空，进程启动失败时抛出异常。
     * @throws[IOException] 进程启动失败。
     */
    @JvmOverloads
    fun run(command: String, redirectError: Boolean = true): Process =
        ADBCLIUtil.run(getSN(), command, redirectError)

    /**
     * 执行 ADB Shell 命令并获取状态码。
     *
     * @param[command] 命令语句。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    fun runForStatus(command: String): Int =
        ADBCLIUtil.runForStatus(getSN(), command)

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true`。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmOverloads
    fun runForLines(command: String, redirectError: Boolean = true): List<String>? =
        ADBCLIUtil.runForLines(getSN(), command, redirectError)

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true`。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmOverloads
    fun runForText(command: String, redirectError: Boolean = true): String? =
        ADBCLIUtil.runForText(getSN(), command, redirectError)

    /**
     * 执行 ADB Shell 命令并获取错误消息。
     * <p>
     * 执行命令并阻塞当前线程，读取错误输出并转为文本。
     *
     * @param[command] 命令语句。
     * @return 命令的错误输出文本。若出现异常则返回空值。
     */
    fun runForError(command: String): String? =
        ADBCLIUtil.runForError(getSN(), command)

    /**
     * 打开交互式 Shell。
     *
     * @param[terminal] 终端程序，可以传入 [Terminals] 内置枚举，也可以传入 [Terminal] 接口自定义实现。默认值为空，此时使用默认终端。
     * @param[newWindow] 是否在新窗口中打开。默认值为是。部分终端程序不支持标签页，即使该参数设为否也会在新窗口中打开。
     * @param[keep] 初始命令执行完毕是否保持窗口。默认值为是。
     * @return 终端程序进程。
     * @throws[IllegalStateException] 没有可用的终端程序。
     */
    @JvmOverloads
    fun openShell(
        terminal: Terminal? = null,
        newWindow: Boolean = true,
        keep: Boolean = true
    ): Process =
        ADBCLIUtil.openShell(getSN(), terminal, newWindow, keep)

    /**
     * 切换为 Root 权限。
     *
     * @return 状态码。若出现异常则返回 `-1`。
     */
    fun root(): Int = ADBCLIUtil.root(getSN())

    /**
     * 重启。
     *
     * @param[option] 附加命令，可以携带 `recovery`、`fastboot`、`bootloader` 等选项，默认为空。
     * @return 状态码。若出现异常则返回 `-1`。
     */
    @JvmOverloads
    fun reboot(option: String = ""): Int = ADBCLIUtil.reboot(getSN(), option)


    /*
     * ----- 设备信息 -----
     */

    /**
     * 获取设备属性。
     *
     * @param[key] 属性键名。
     * @return 属性值。若属性不存在则返回空值。
     */
    fun getProperty(key: String): String? = ADBCLIUtil.getProperty(getSN(), key)

    /**
     * 获取设备属性。
     *
     * @param[key] 属性键名。
     * @param[default] 默认值。
     * @return 属性值。若属性不存在则返回默认值。
     */
    fun getProperty(key: String, default: String): String =
        ADBCLIUtil.getProperty(getSN(), key, default)

    /**
     * 获取设备制造商。
     *
     * @return 制造商。若设备未声明则为空字符串。
     */
    fun getManufacturer(): String = ADBCLIUtil.getManufacturer(getSN())

    /**
     * 获取设备品牌。
     *
     * @return 品牌。若设备未声明则为空字符串。
     */
    fun getBrand(): String = ADBCLIUtil.getBrand(getSN())

    /**
     * 获取设备型号。
     *
     * @return 型号。若设备未声明则为空字符串。
     */
    fun getModel(): String = ADBCLIUtil.getModel(getSN())

    /**
     * 获取设备 Android 系统版本。
     *
     * @return 系统版本号。若设备未声明则为空字符串。
     */
    fun getOSVersion(): String = ADBCLIUtil.getOSVersion(getSN())

    /**
     * 获取设备 Android API Level。
     *
     * @return API Level。若设备未声明则为空字符串。
     */
    fun getAPILevel(): String = ADBCLIUtil.getAPILevel(getSN())

    /**
     * 获取设备名称。
     *
     * 若为物理机则显示 `<品牌> <型号>` ；若为模拟器则显示模拟器名称。
     *
     * 该方法耗时平均值为 `200` 毫秒左右，请勿在 UI 线程中调用。
     *
     * @param[default] 获取失败时的名称。默认值为“设备不可用”。
     * @return 设备名称。
     */
    @JvmOverloads
    fun getName(default: String = "Unknown"): String {
        if (ddmDevice.isEmulator) {
            val avdName = runCatching {
                ddmDevice.avdData.get(5, TimeUnit.SECONDS)?.name
            }.getOrNull()
            if (!avdName.isNullOrEmpty()) {
                return avdName
            }
        }

        val name = "${getBrand()} ${getModel()}"
        return if (name.trim().isBlank()) default else name
    }

    /**
     * 获取设备类型。
     *
     * @return 设备类型。
     */
    fun getType(): ConnectionType {
        if (ddmDevice.isEmulator) {
            return ConnectionType.EMULATOR
        }

        val sn = getSN()
        return if (
        // `adb connect` 命令主动连接的设备
            sn.contains(':') ||
            // Android 11 开始新增扫码配对方式连接的设备
            sn.contains("._adb-tls-connect.")
        ) {
            ConnectionType.NETWORK
        } else {
            ConnectionType.USB
        }
    }


    /*
     * ----- 图形界面 -----
     */

    /**
     * 获取显示屏列表。
     *
     * @return 显示屏列表。
     */
    fun getDisplays(): List<DisplayInfo> = ADBCLIUtil.getDisplays(getSN())

    /**
     * 获取显示屏物理 ID 索引。
     *
     * @param[displayID] 逻辑 ID 。
     * @return 索引序号。无法获取时返回 `0`。
     */
    fun getDisplayUniqueIndex(displayID: String): String = ADBCLIUtil.getDisplayUniqueIndex(getSN(), displayID)

    /**
     * 获取显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回空值。
     *
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return 分辨率信息。若显示屏不存在则返回空值。
     */
    fun getDisplaySize(displayID: String = "0"): DisplaySize? =
        ADBCLIUtil.getDisplaySize(getSN(), displayID)

    /**
     * 设置显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[width] 宽度。
     * @param[height] 高度。
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmOverloads
    fun setDisplaySize(width: Int, height: Int, displayID: String = "0"): Boolean =
        ADBCLIUtil.setDisplaySize(getSN(), width, height, displayID)

    /**
     * 还原显示屏分辨率。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmOverloads
    fun resetDisplaySize(displayID: String = "0"): Boolean =
        ADBCLIUtil.resetDisplaySize(getSN(), displayID)

    /**
     * 获取显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回空值。
     *
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return 密度信息。若出现异常则返回空值。
     */
    @JvmOverloads
    fun getDisplayDensity(displayID: String = "0"): DisplayDensity? =
        ADBCLIUtil.getDisplayDensity(getSN(), displayID)

    /**
     * 设置显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[density] 密度。
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmOverloads
    fun setDisplayDensity(density: Int, displayID: String = "0"): Boolean =
        ADBCLIUtil.setDisplayDensity(getSN(), density, displayID)

    /**
     * 还原显示屏像素密度。
     *
     * 在 Android 10 以下版本中，`wm` 命令不支持调整主屏以外的其他屏幕，如果 ID 非 `0` 必然返回 `false`。
     *
     * @param[displayID] 显示屏 ID。默认值为 `0`，表示主屏。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmOverloads
    fun resetDisplayDensity(displayID: String = "0"): Boolean =
        ADBCLIUtil.resetDisplayDensity(getSN(), displayID)

    /**
     * 获取 Activity 列表。
     *
     * @param[displayID] 显示屏 ID。默认为空值，表示列出所有显示屏的 Activity。
     * @return Activity 列表。命令执行失败时返回空列表。每个显示屏内按照从顶层到底层的顺序排列。
     */
    @JvmOverloads
    fun getActivities(displayID: String? = null): List<ActivityInfo> =
        ADBCLIUtil.getActivities(getSN(), displayID)

    /**
     * 获取 Window 列表。
     *
     * @param[displayID] 显示屏 ID。默认为空值，表示列出所有显示屏的 Window。
     * @return Window 列表。命令执行失败时返回空列表。每个显示屏内按照从顶层到底层的顺序排列。
     */
    @JvmOverloads
    fun getWindows(displayID: String? = null): List<WindowInfo> =
        ADBCLIUtil.getWindows(getSN(), displayID)

    /**
     * 获取全局焦点信息。
     *
     * Android 9 及更低版本多屏 API 不完善，焦点所在屏幕始终为 `0`。
     *
     * @return 全局焦点信息。
     */
    fun getGlobalFocusInfo(): FocusInfo =
        ADBCLIUtil.getGlobalFocusInfo(getSN())

    /**
     * 获取屏幕截图。
     *
     * @param[displayID] 显示屏 ID。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID。
     * @return 二进制数据。若出现错误则返回空值。
     */
    @JvmOverloads
    fun screenshotAsPNG(displayID: String? = null): ByteArray? =
        ADBCLIUtil.screenshotAsPNG(getSN(), displayID)

    /**
     * 获取屏幕截图并保存至 PC 存储设备。
     *
     * @param[file] 目标文件。
     * @param[displayID] 显示屏 ID 。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmOverloads
    fun screenshotAsPNG(file: File, displayID: String? = null): Boolean =
        ADBCLIUtil.screenshotAsPNG(getSN(), file, displayID)


    /*
     * ----- 文件系统 -----
     */

    /**
     * 获取根文件节点。
     *
     * @return 根节点。
     */
    fun getFilesRoot(): FileListingService.FileEntry = ddmDevice.fileListingService.root

    /**
     * 获取子文件节点。
     *
     * DDMLib 内部固定使用 `ls -l` 查询目录内容，无法看到隐藏文件；若要处理隐藏文件，应自行实现命令输出解析。
     *
     * @param[dir] 父节点。若为空值表示使用根节点。
     * @return 文件节点数组。
     */
    fun listFiles(dir: FileListingService.FileEntry? = null): Array<FileListingService.FileEntry> {
        val fService = ddmDevice.fileListingService
        val target = dir ?: fService.root
        return fService.getChildrenSync(target)
    }

    /**
     * 推送文件到设备。
     *
     * DDMLib 封装，适用于需要获取进度或随时取消的场景。
     *
     * @param[local] 源文件（PC 端）。
     * @param[remote] 目标路径（Android 端）。如果以 `/` 结尾表示将文件写入目标目录并保持原名，否则表示使用目标路径指定的文件名称。
     * @param[refreshMediaDB] 文件传输完毕后是否刷新媒体数据库。默认值为 `true`。传输多个文件至同一目录时可设为 `false` 并在传输完毕后
     * 统一调用一次 [mediaScan] 方法。
     * @param[listener] 传输进度监听器。
     * @throws[FileNotFoundException] 源文件不存在或不可读。
     * @throws[IllegalStateException] 源文件指向目录。
     * @throws[IllegalArgumentException] 未指明目标路径。
     * @throws[IOException] 目标路径不可写入。
     * @throws[SyncException] 传输失败或用户取消任务。具体错误类型可通过 [SyncException.SyncError] 获取。
     */
    fun pushFile(
        local: String,
        remote: String,
        refreshMediaDB: Boolean = true,
        listener: ADBFileTransferListener
    ) {
        /* 参数校验 */
        if (local.isBlank()) {
            throw IllegalArgumentException("Local path is empty!")
        }

        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        val localFile = File(local)
        if (!localFile.canRead()) {
            throw FileNotFoundException("Input path not exist or readable! Path:[${local}]")
        }

        if (!localFile.isFile) {
            throw IllegalStateException("Input is not file! Path:[${local}]")
        }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val remoteIsDir = FilePathUtil.isDirectory(remote)
        val remotePath = if (remoteIsDir) {
            "$remote${FilePathUtil.getFileName(local)}"
        } else {
            remote
        }


        val remoteParent = FilePathUtil.getParent(remotePath)
        if (TextUtil.isBlank(remoteParent)) {
            throw IllegalArgumentException("Remote path parent is empty! Path:[$remotePath]")
        }

        // 如果目标目录不存在则自动创建
        runForStatus("mkdir -p \'$remoteParent\'")
        // ADB 将数据传输至临时文件后才判断目标路径是否可写，提前判断并报错可节省大文件传输的耗时。
        val state = runForStatus("test -w \'$remoteParent\'")
        if (!CLIUtil.isSuccess(state)) {
            throw IOException("Remote directory cannot be write! Path:[$remoteParent]")
        }

        // DDMLib 进度回调实现
        val monitor = object : SyncService.ISyncProgressMonitor {

            val totalSize: Long = localFile.length()

            var transferredSize: Long = 0L

            val cancelled = AtomicBoolean(false)

            init {
                // 监听用户发出的取消指令
                listener.setCancelNotifier {
                    cancelled.set(true)
                }
            }

            /**
             * 回调方法：传输开始。
             *
             * @param[totalWork] 文件大小（字节）。该值受到 [Integer.MAX_VALUE] 的限制无法表示大于 `2GB` 的数据，最好使用其他方法
             * 获取文件大小。
             */
            override fun start(totalWork: Int) {
                listener.onStart(totalSize)
            }

            /**
             * 回调方法：传输结束。
             *
             * 如果中途被取消则不会回调该方法。
             */
            override fun stop() {
                listener.onEnd()
            }

            /**
             * 回调方法：子任务开始。
             *
             * @param[name] 文件名称。
             */
            override fun startSubTask(name: String?) {
                // 传输单个文件无需使用该方法
            }

            /**
             * 回调方法：传输进度更新。
             *
             * @param[work] 当前分块大小（字节）。
             */
            override fun advance(work: Int) {
                transferredSize += work

                // 文件为空或无法获取长度时，以 `NaN` 通告进度。
                if (totalSize <= 0) {
                    listener.onProgress(transferredSize, Double.NaN)
                } else {
                    val progress = transferredSize.toDouble() / totalSize.toDouble()
                    listener.onProgress(transferredSize, progress)
                }
            }

            /**
             * 回调方法：是否取消传输。
             *
             * 传输每个数据分块前都会回调一次该方法，若返回 `true` 则取消本次传输。
             */
            override fun isCanceled(): Boolean {
                return cancelled.get()
            }
        }

        val syncService = ddmDevice.syncService
            ?: throw IllegalStateException("Cannot get SyncService for device [${getSN()}]!")

        syncService.use { syncService ->
            syncService.pushFile(localFile.absolutePath, remotePath, monitor)
        }

        if (refreshMediaDB) {
            mediaScan(remoteParent)
        }
    }

    /**
     * 推送目录到设备。
     *
     * DDMLib 封装，递归传输目录内的所有文件（包括空目录），支持获取每个文件的状态、总体进度以及随时取消。
     *
     * @param[local] 源目录（PC 端）。
     * @param[remote] 目标路径（Android 端）。如果以 `/` 结尾则表示将源目录写入该目录并保持原名，否则表示源目录的内容写入路径指定
     * 的目录。
     * @param[listener] 传输进度监听器。
     * @throws[FileNotFoundException] 源目录不存在或不可读。
     * @throws[IllegalStateException] 源文件不是目录。
     * @throws[IllegalArgumentException] 未指明目标路径。
     * @throws[IOException] 目标目录无法创建或不可写入。
     * @throws[SyncException] 传输失败或用户取消推送。
     */
    fun pushFolder(local: String, remote: String, listener: ADBFolderTransferListener) {
        /* 参数校验 */
        if (local.isBlank()) {
            throw IllegalArgumentException("Local path is empty!")
        }

        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        val localFolder = File(local)
        if (!localFolder.canRead()) {
            throw FileNotFoundException("Input path not exist or readable! Path:[${local}]")
        }

        if (!localFolder.isDirectory) {
            throw IllegalStateException("Input is not directory! Path:[${local}]")
        }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val remoteIsDir = FilePathUtil.isDirectory(remote)
        val remotePath = if (remoteIsDir) {
            "$remote${localFolder.name}"
        } else {
            remote
        }


        /* 计算文件总数 */
        var totalCount = 0
        localFolder.walkTopDown().forEach {
            if (it.isFile) totalCount++
        }
        listener.onStart(totalCount)

        var currentIndex = 0
        var currentPath = FileUtil.PATH_INVALID
        val cancelled = AtomicBoolean(false)
        // 单个文件传输任务监听器
        val fileListener: ADBFileTransferListener = object : ADBFileTransferListener() {

            override fun onStart(size: Long) {
                currentIndex++

                // 只有目录时，以 `NaN` 通告进度。
                if (totalCount <= 0) {
                    listener.onProgress(currentIndex, Double.NaN)
                } else {
                    val progress = currentIndex.toDouble() / totalCount.toDouble()
                    listener.onProgress(currentIndex, progress)
                }

                listener.onFileStart(currentPath, size)
            }

            override fun onProgress(size: Long, progress: Double) {
                listener.onFileProgress(size, progress)
            }

            override fun onEnd() {
                listener.onFileEnd()
            }
        }

        // 收到取消指令时，通知正在传输的任务。
        listener.setCancelNotifier {
            cancelled.set(true)
            fileListener.notifyCancel()
        }


        /* 深度优先遍历源目录，处理每个元素。 */
        localFolder.walkTopDown().forEach {
            if (cancelled.get()) {
                throw SyncException(SyncException.SyncError.CANCELED)
            }

            // 获取当前文件相对于源目录的路径，并拼接在目标路径之后。
            val relative = it.relativeTo(localFolder).invariantSeparatorsPath
            if (relative.isBlank()) return@forEach
            val target = "$remotePath/$relative"

            // 创建目录
            if (it.isDirectory) {
                runForStatus("mkdir -p \'$target\'")
                return@forEach
            }

            // 跳过非普通文件的特殊文件节点
            if (!it.isFile) return@forEach

            currentPath = it.absolutePath
            pushFile(it.absolutePath, target, false, fileListener)
        }

        mediaScan(remote)

        listener.onEnd()
    }

    /**
     * 推送文件到 Android 设备。
     *
     * `adb push` 命令封装，支持处理目录，适用于无需获取进度的简单场景。
     *
     * @param[local] 源路径（PC 端）。
     * @param[remote] 目标路径（Android 端）。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun pushFileByCLI(local: File, remote: String): Boolean = ADBCLIUtil.pushFile(getSN(), local, remote)

    /**
     * 从设备下载文件。
     *
     * DDMLib 封装，适用于需要获取进度或随时取消的场景。
     *
     * @param[remote] 源路径（Android 端）。
     * @param[local] 目标路径（PC 端）。如果以 `/` 结尾表示将文件写入该目录并保持原名，否则表示使用路径指定的文件名称。
     * @param[listener] 传输进度监听器。
     * @throws[FileNotFoundException] 目标目录不可写。
     * @throws[IllegalStateException] 源文件指向目录。
     * @throws[IllegalArgumentException] 未指明源路径或目标路径。
     * @throws[IOException] 目标路径不可写入。
     * @throws[SyncException] 传输失败或用户取消任务。具体错误类型可通过 [SyncException.SyncError] 获取。
     */
    fun pullFile(remote: String, local: String, listener: ADBFileTransferListener) {
        /* 参数校验 */
        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        if (local.isBlank()) {
            throw IllegalArgumentException("Local path is empty!")
        }

        // 检查设备上是否存在该文件
        val isFile: Boolean = CLIUtil.isSuccess(runForStatus("test -f \'$remote\'"))
        if (!isFile) {
            throw FileNotFoundException("Remote file not exist or is directory! Path:[$remote]")
        }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val remoteFileName = FilePathUtil.getFileName(remote)
        val localFile = File(local)
        val localPath = if (FilePathUtil.isDirectory(local)) {
            /* 本地路径指向目录 */
            localFile.mkdirs()
            // 检查目录是否创建成功
            if (!localFile.canWrite()) {
                throw FileNotFoundException("Local path not exist or writeable! Path:[${local}]")
            }
            // 返回 `<本地目录>/<远程文件名称>`
            "${localFile.absolutePath}${File.separator}${remoteFileName}"
        } else {
            /* 本地路径指向文件 */
            val parent = localFile.absoluteFile.parentFile
            // 创建父目录
            parent.mkdirs()
            if (!parent.canWrite()) {
                throw FileNotFoundException("Local path not exist or writeable! Path:[${parent}]")
            }
            // 返回本地文件路径
            localFile.absolutePath
        }

        // 读取文件大小（DDMLib 输入路径时不会回调文件大小，需要通过命令读取。）
        val fileSize: Long = runForText("stat -c %s \'$remote\'")?.toLongOrNull() ?: 0L

        // DDMLib 进度回调实现
        val monitor = object : SyncService.ISyncProgressMonitor {

            var transferredSize: Long = 0L
            val cancelled = AtomicBoolean(false)


            init {
                // 监听用户发出的取消指令
                listener.setCancelNotifier {
                    cancelled.set(true)
                }
            }


            /**
             * 回调方法：传输开始。
             *
             * @param[totalWork] 文件大小（字节）。该值受到 [Integer.MAX_VALUE] 的限制无法表示大于 `2GB` 的数据，最好使用其他方法
             * 获取文件大小。
             */
            override fun start(totalWork: Int) {
                listener.onStart(fileSize)
            }

            /**
             * 回调方法：传输结束。
             *
             * 如果中途被取消则不会回调该方法。
             */
            override fun stop() {
                listener.onEnd()
            }

            /**
             * 回调方法：子任务开始。
             *
             * @param[name] 文件名称。
             */
            override fun startSubTask(name: String?) {
                // 传输单个文件无需使用该方法
            }

            /**
             * 回调方法：传输进度更新。
             *
             * @param[work] 当前分块大小（字节）。
             */
            override fun advance(work: Int) {
                transferredSize += work

                // 文件为空或无法获取长度时，以 `NaN` 通告进度。
                if (fileSize <= 0) {
                    listener.onProgress(transferredSize, Double.NaN)
                } else {
                    val progress = transferredSize.toDouble() / fileSize.toDouble()
                    listener.onProgress(transferredSize, progress)
                }
            }

            /**
             * 回调方法：是否取消传输。
             *
             * 传输每个数据分块前都会回调一次该方法，若返回 `true` 则取消本次传输。
             */
            override fun isCanceled(): Boolean {
                return cancelled.get()
            }
        }

        val syncService = ddmDevice.syncService
            ?: throw IllegalStateException("Cannot get SyncService for device [${getSN()}]!")

        syncService.use { syncService ->
            syncService.pullFile(remote, localPath, monitor)
        }
    }

    /**
     * 从设备下载目录。
     *
     * DDMLib 封装，递归传输目录内的所有文件（包括空目录），支持获取每个文件的状态、总体进度以及随时取消。
     *
     * @param[remote] 源目录（Android 端）。
     * @param[local] 目标路径（PC 端）。如果以 `/` 结尾则表示将源目录写入该目录并保持原名，否则表示源目录的内容写入路径指定
     * 的目录。
     * @param[listener] 传输进度监听器。
     * @throws[FileNotFoundException] 源目录不存在或目标目录不可写。
     * @throws[IllegalArgumentException] 未指明源路径或目标路径。
     * @throws[IOException] 无法获取源目录的文件列表。
     * @throws[SyncException] 传输失败或用户取消任务。
     */
    fun pullFolder(remote: String, local: String, listener: ADBFolderTransferListener) {
        /* 参数校验 */
        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        if (local.isBlank()) {
            throw IllegalArgumentException("Local path is empty!")
        }

        // 检查设备上是否存在该目录
        val isDir: Boolean = CLIUtil.isSuccess(runForStatus("test -d \'$remote\'"))
        if (!isDir) {
            throw FileNotFoundException("Remote directory not exist or is file! Path:[$remote]")
        }

        // 移除源路径末尾可能存在的 `/`。
        val remoteRoot = remote.trimEnd('/').ifEmpty { "/" }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val localRoot = if (FilePathUtil.isDirectory(local)) {
            File(local, FilePathUtil.getFileName(remoteRoot))
        } else {
            File(local)
        }

        localRoot.mkdirs()
        if (!localRoot.canWrite()) {
            throw FileNotFoundException("Local path is not writeable! Path:[$localRoot]")
        }

        /* 获取源目录中的所有目录与文件 */
        val dirs: List<String> = runForLines("find \'$remoteRoot\' -type d", false)
            ?.filter { it.isNotBlank() }
            ?: throw IOException("Can not access remote directory! Path:[$remoteRoot]")
        val files: List<String> = runForLines("find \'$remoteRoot\' -type f", false)
            ?.filter { it.isNotBlank() }
            ?: throw IOException("Can not access remote directory! Path:[$remoteRoot]")


        // 获取当前路径相对于参考目录的相对路径
        fun relativeOf(path: String): String =
            path.removePrefix(remoteRoot).trimStart('/')


        /* 在本地创建目录结构（包括空目录） */
        dirs.forEach {
            File(localRoot, relativeOf(it)).mkdirs()
        }

        val totalCount = files.size
        listener.onStart(totalCount)

        var currentIndex = 0
        var currentPath = FileUtil.PATH_INVALID
        val cancelled = AtomicBoolean(false)
        // 单个文件传输任务监听器
        val fileListener: ADBFileTransferListener = object : ADBFileTransferListener() {

            override fun onStart(size: Long) {
                currentIndex++

                // 只有目录时，以 `NaN` 通告进度。
                if (totalCount <= 0) {
                    listener.onProgress(currentIndex, Double.NaN)
                } else {
                    val progress = currentIndex.toDouble() / totalCount.toDouble()
                    listener.onProgress(currentIndex, progress)
                }

                listener.onFileStart(currentPath, size)
            }

            override fun onProgress(size: Long, progress: Double) {
                listener.onFileProgress(size, progress)
            }

            override fun onEnd() {
                listener.onFileEnd()
            }
        }

        // 收到取消指令时，通知正在传输的任务。
        listener.setCancelNotifier {
            cancelled.set(true)
            fileListener.notifyCancel()
        }


        /* 依次下载每个文件，保持相对路径。 */
        files.forEach {
            if (it.isBlank()) return@forEach
            if (cancelled.get()) {
                throw SyncException(SyncException.SyncError.CANCELED)
            }

            currentPath = it
            pullFile(it, File(localRoot, relativeOf(it)).absolutePath, fileListener)
        }

        listener.onEnd()
    }

    /**
     * 从 Android 设备拉取文件。
     *
     * `adb pull` 命令封装，支持处理目录，适用于无需获取进度的简单场景。
     *
     * @param[remote] 源路径（Android 端）。
     * @param[local] 目标路径（PC 端）。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun pullFileByCLI(remote: String, local: File): Boolean = ADBCLIUtil.pullFile(getSN(), remote, local)

    /**
     * 同步文件系统。
     *
     * 部分设备写入文件后需执行此命令，否则其他进程可能读取到不完整的数据。
     */
    fun syncFileSystem() = ADBCLIUtil.syncFileSystem(getSN())

    /**
     * 将系统分区挂载为可读写。
     *
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun remount(): Boolean = ADBCLIUtil.remount(getSN())

    /**
     * 触发媒体扫描。
     *
     * @param[path] 扫描路径。默认值为根目录 `/`。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    @JvmOverloads
    fun mediaScan(path: String = "/"): Boolean = ADBCLIUtil.mediaScan(getSN(), path)


    /*
     * ----- 网络通信 -----
     */

    /**
     * 开启 TCP/IP 远程控制。
     *
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun startRemoteControl(): Boolean = ADBCLIUtil.startRemoteControl(getSN())

    /**
     * 关闭 TCP/IP 远程控制。
     *
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun stopRemoteControl(): Boolean = ADBCLIUtil.stopRemoteControl(getSN())

    /**
     * 开启 ADB 端口转发（TCP）。
     *
     * @param[pcPort] PC 端口。
     * @param[devicePort] 设备端口。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun startForward(pcPort: Int, devicePort: Int): Boolean =
        ADBCLIUtil.startForward(getSN(), pcPort, devicePort)

    /**
     * 关闭指定的 ADB 端口转发（TCP）。
     *
     * @param[pcPort] PC 端口。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun stopForward(pcPort: Int): Boolean = ADBCLIUtil.stopForward(getSN(), pcPort)

    /**
     * 开启 ADB 端口转发（通用端点格式）。
     *
     * @param[pcEndPoint] PC 端点（如 `tcp:8080`、`localabstract:xxx`）。
     * @param[deviceEndPoint] 设备端点。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun startForward(pcEndPoint: String, deviceEndPoint: String): Boolean =
        ADBCLIUtil.startForward(getSN(), pcEndPoint, deviceEndPoint)

    /**
     * 关闭指定的 ADB 端口转发（通用端点格式）。
     *
     * @param[pcEndPoint] PC 端点。
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun stopForward(pcEndPoint: String): Boolean = ADBCLIUtil.stopForward(getSN(), pcEndPoint)

    /**
     * 清除当前设备的所有 ADB 端口转发。
     *
     * @return `true` 表示操作成功；`false` 表示操作失败。
     */
    fun cleanForward(): Boolean = ADBCLIUtil.cleanForward(getSN())


    /*
     * ----- 软件包管理 -----
     */

    /**
     * 获取软件包列表。
     *
     * @return 包名列表。永不为空值，命令执行异常时返回空列表。
     */
    @JvmOverloads
    fun listPackages(type: PackageType = PackageType.ALL): List<String> = ADBCLIUtil.listPackages(getSN(), type)

    /**
     * 安装 PC 端软件包。
     *
     * @param[file] APK 文件。
     * @throws[Exception] 安装失败。消息为 ADB 命令输出的错误信息。
     */
    fun installPackage(file: File) = ADBCLIUtil.installPackage(getSN(), file)

    /**
     * 卸载软件包。
     *
     * @param[packageName] 包名。
     * @param[userID] 用户 ID。对于预装应用等组件无法直接卸载，可以尝试在特定用户空间将其标记为伪卸载。默认为空值，表示全局卸载。
     * @param[keepData] 是否保留数据。默认不保留数据。
     * @throws[Exception] 卸载失败。消息为 ADB 命令输出的错误信息。
     */
    @JvmOverloads
    fun uninstallPackage(packageName: String, userID: String? = null, keepData: Boolean = false) =
        ADBCLIUtil.uninstallPackage(getSN(), packageName, userID, keepData)

    /**
     * 启用软件包。
     *
     * @param[packageName] 包名。
     * @param[userID] 用户 ID。默认为空值，表示不指定用户。
     * @throws[Exception] 启用失败。消息为 `pm` 命令输出的错误信息。
     */
    @JvmOverloads
    fun enablePackage(packageName: String, userID: String? = null) =
        ADBCLIUtil.enablePackage(getSN(), packageName, userID)

    /**
     * 禁用软件包。
     *
     * 优先使用 `pm disable` 命令；若执行失败则回退至 `pm disable-user` 命令。
     *
     * @param[packageName] 包名。
     * @param[userID] 用户 ID。默认为空值，表示不指定用户。
     * @throws[Exception] 禁用失败。消息为 `pm` 命令输出的错误信息。
     */
    @JvmOverloads
    fun disablePackage(packageName: String, userID: String? = null) =
        ADBCLIUtil.disablePackage(getSN(), packageName, userID)


    /*
     * ----- 内部方法 -----
     */

    /**
     * 将 UTF-8 路径转为 ISO-8859-1 路径。
     *
     * DDMLib `com.google.android.tools:ddmlib:r13` 等早期版本要求路径以 ISO_8859_1 传输，否则非 ASCII 字符会显示为 `?`；
     * Google 仓库中的 `com.android.tools.ddms:ddmlib:32.5.1` 等较新版本已支持 UTF-8 路径，无需调用本方法。
     *
     * @return 编码后的路径。
     */
    private fun String.toDDMLibPath(): String =
        String(this.toByteArray(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)
}
