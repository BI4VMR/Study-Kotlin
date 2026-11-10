package net.bi4vmr.tool.kotlin.external.adb.model

import com.android.ddmlib.IDevice
import com.android.ddmlib.SyncException
import com.android.ddmlib.SyncService
import net.bi4vmr.tool.java.common.base.file.FilePathUtil
import net.bi4vmr.tool.java.common.base.system.CLIUtil
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFileTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFilesTransferListener
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceState
import net.bi4vmr.tool.kotlin.external.adb.common.enums.DeviceType
import net.bi4vmr.tool.kotlin.external.adb.common.model.ADBDevice
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.util.ADBCLIUtil
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.Terminal
import net.bi4vmr.tool.kotlin.gui.interaction.terminal.Terminals
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * ADB 本地设备。
 *
 * [getSN] 、 [getState] 、 [getType] 、 [isOnline] 方法为内存读写，不必在子线程调用；其他方法耗时可能较长，建议在非 UI 线程调用。
 *
 * 其他方法均为 [ADBCLIUtil] 相关功能的快捷调用，例如： [ADBCLIUtil.screenshotAsPNG] 在此处可以针对当前设备调用，省略 `sn` 参数。
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
     * @param[redirectError] 是否重定向错误输出流。默认值为 `true` 。
     * @return [Process] 实例，永不为空，进程启动失败时抛出异常。
     * @throws[IOException] 进程启动失败。
     */
    @JvmOverloads
    fun run(command: String, redirectError: Boolean = true): Process =
        ADBCLIUtil.run(getSN(), command, redirectError)

    /**
     * 执行 ADB Shell 命令并获取返回值。
     *
     * @param[command] 命令语句。
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    fun runForStatus(command: String): Int =
        ADBCLIUtil.runForStatus(getSN(), command)

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @param[charset] 字符集名称。默认为空值，表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmOverloads
    fun runForLines(
        command: String,
        redirectError: Boolean = true,
        charset: String? = null
    ): List<String>? =
        ADBCLIUtil.runForLines(getSN(), command, redirectError, charset)

    /**
     * 执行 ADB Shell 命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据并转为文本。
     *
     * @param[command] 命令语句。
     * @param[redirectError] 是否将错误输出重定向到标准输出。默认为 `true` 。
     * @param[charset] 字符集名称。默认为空值，表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空值。
     */
    @JvmOverloads
    fun runForText(command: String, redirectError: Boolean = true, charset: String? = null): String? =
        ADBCLIUtil.runForText(getSN(), command, redirectError, charset)

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
     * @return 索引序号。无法获取时返回 `0` 。
     */
    fun getDisplayUniqueIndex(displayID: String): String = ADBCLIUtil.getDisplayUniqueIndex(getSN(), displayID)

    /**
     * 获取屏幕截图。
     *
     * @param[displayID] 显示屏 ID 。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID 。
     * @return 二进制数据。若出现错误则返回空值。
     */
    @JvmOverloads
    fun screenshotAsPNG(displayID: String? = null): ByteArray? =
        ADBCLIUtil.screenshotAsPNG(getSN(), displayID)

    /**
     * 获取屏幕截图并保存至 PC 存储设备。
     *
     * @param[file] 目标文件。
     * @param[displayID] 显示屏 ID 。默认值为主屏幕。方法内部已自动转换，填写逻辑 ID 即可，请勿填写物理 ID 。
     * @return `true` 表示操作成功； `false` 表示操作失败。
     */
    @JvmOverloads
    fun screenshotAsPNG(file: File, displayID: String? = null): Boolean =
        ADBCLIUtil.screenshotAsPNG(getSN(), file, displayID)

    /**
     * 打开交互式 Shell 。
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
     * @return 状态码。若出现异常则返回 `-1` 。
     */
    fun root(): Int = ADBCLIUtil.root(getSN())

    /**
     * 重启。
     *
     * @param[option] 附加命令，可以携带 `recovery` 、 `fastboot` 、 `bootloader` 等选项，默认为空。
     * @return 状态码。若出现异常则返回 `-1` 。
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
            return ddmDevice.avdName
        }

        val name = "${getBrand()} ${getModel()}"
        return if (name.trim().isBlank()) default else name
    }

    /**
     * 获取设备类型。
     *
     * @return 设备类型。
     */
    fun getType(): DeviceType {
        if (ddmDevice.isEmulator) {
            return DeviceType.EMULATOR
        }

        val sn = getSN()
        return if (
        // `adb connect` 命令主动连接的设备
            sn.contains(':') ||
            // Android 11 开始新增扫码配对方式连接的设备
            sn.contains("._adb-tls-connect.")
        ) {
            DeviceType.NETWORK
        } else {
            DeviceType.USB
        }
    }


    /*
     * ----- 文件系统 -----
     */

    /**
     * 推送文件到设备。
     *
     * DDMLib 封装，适用于需要获取进度或随时取消的场景。
     *
     * @param[local] 源文件（PC 端）。
     * @param[remote] 目标路径（Android 端）。如果以 `/` 结尾表示将文件写入该目录并保持原名，否则表示使用路径指定的文件名称。
     * @throws[FileNotFoundException] 源文件不存在或不可读。
     * @throws[IllegalStateException] 源文件指向目录。
     * @throws[IllegalArgumentException] 未指明目标路径。
     * @throws[IOException] 目标路径不可写入。
     * @throws[SyncException] 传输失败或用户取消任务。具体错误类型可通过 [SyncException.SyncError] 获取。
     */
    fun pushFile(local: File, remote: String, listener: ADBFileTransferListener? = null) {
        /* 参数校验 */
        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        if (!local.canRead()) {
            throw FileNotFoundException("Input path not exist or readable! Path:[${local}]")
        }

        if (local.isDirectory) {
            throw IllegalStateException("Input is a directory! Path:[${local}]")
        }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val remoteIsDir = (remote.length > 1) && (remote.last() == '/')
        val remotePath = if (remoteIsDir) {
            "$remote${local.name}"
        } else {
            remote
        }

        // ADB 将数据传输至临时文件后才判断目标路径是否可写，提前判断并报错可节省大文件传输的耗时。
        val remoteParent = FilePathUtil.getParent(remotePath)
        val state = runForStatus("test -w \"$remoteParent\"")
        if (!CLIUtil.isSuccess(state)) {
            throw IOException("Remote directory cannot be write! Path:[$remoteParent]")
        }

        // DDMLib 进度回调实现
        val monitor = object : SyncService.ISyncProgressMonitor {

            val totalSize: Long = local.length()
            var transferredSize: Long = 0L
            val cancelled = AtomicBoolean(false)


            init {
                // 监听用户发出的取消指令
                listener?.setCancelNotifier {
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
                listener?.onStart(totalSize)
            }

            /**
             * 回调方法：传输结束。
             *
             * 如果中途被取消则不会回调该方法。
             */
            override fun stop() {
                listener?.onEnd(false)
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
                    listener?.onProgress(transferredSize, Double.NaN)
                } else {
                    val progress = transferredSize.toDouble() / totalSize.toDouble()
                    listener?.onProgress(transferredSize, progress)
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
            syncService.pushFile(local.absolutePath, remotePath, monitor)
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
     * @throws[FileNotFoundException] 源目录不存在或不可读。
     * @throws[IllegalStateException] 源文件不是目录。
     * @throws[IllegalArgumentException] 未指明目标路径。
     * @throws[IOException] 目标目录无法创建或不可写入。
     * @throws[SyncException] 传输失败或用户取消推送。
     */
    fun pushDirectory(local: File, remote: String, listener: ADBFilesTransferListener? = null) {
        /* 参数校验 */
        if (!local.canRead()) {
            throw FileNotFoundException("Input path not exist or readable! Path:[${local}]")
        }

        if (!local.isDirectory) {
            throw IllegalStateException("Input is not a directory! Path:[${local}]")
        }

        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }

        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val remoteIsDir = (remote.length > 1) && (remote.last() == '/')
        val remoteRoot = if (remoteIsDir) {
            "$remote${local.name}"
        } else {
            remote.trimEnd('/').ifEmpty { "/" }
        }

        // 收集待传输的目录与文件，保持遍历顺序以便按目录层级创建。
        val localDirs = mutableListOf<File>()
        val localFiles = mutableListOf<File>()
        local.walkTopDown().forEach {
            when {
                it.isDirectory -> localDirs.add(it)
                it.isFile -> localFiles.add(it)
            }
        }

        fun toRemotePath(file: File): String {
            val relative = file.relativeTo(local).invariantSeparatorsPath
            return if (relative.isEmpty()) remoteRoot else "${remoteRoot.trimEnd('/')}/$relative"
        }

        // 用户取消时，同时通知正在传输的文件。
        val currentFileListener = AtomicReference<ADBFileTransferListener?>(null)
        listener?.setCancelNotifier {
            currentFileListener.get()?.notifyCancel()
        }

        val totalSize = localFiles.sumOf { it.length() }
        var totalTransferred = 0L

        fun notifyTotalProgress() {
            val progress = if (totalSize <= 0) Double.NaN else totalTransferred.toDouble() / totalSize.toDouble()
            listener?.onProgress(totalTransferred, progress)
        }

        fun cancelAndThrow(): Nothing {
            listener?.onEnd(true)
            throw SyncException(SyncException.SyncError.CANCELED)
        }

        if (listener?.isCancelled() == true) {
            cancelAndThrow()
        }

        // 预先创建所有目录，空目录也会被保留。
        localDirs.forEach { dir ->
            val remoteDir = toRemotePath(dir)
            val state = runForStatus("mkdir -p \'$remoteDir\'")
            if (!CLIUtil.isSuccess(state)) {
                throw IOException("Remote directory cannot be created! Path:[$remoteDir]")
            }
        }

        listener?.onStart(localFiles.size, totalSize)

        localFiles.forEachIndexed { index, file ->
            val remotePath = toRemotePath(file)
            val baseTransferred = totalTransferred

            // 将单文件事件转换为目录传输事件
            val fileListener = object : ADBFileTransferListener() {
                override fun onStart(size: Long) {
                    listener?.onFileStart(index, file, remotePath, size)
                }

                override fun onProgress(size: Long, progress: Double) {
                    listener?.onFileProgress(index, file, size, progress)
                    totalTransferred = baseTransferred + size
                    notifyTotalProgress()
                }
            }

            // 先登记再检查，避免两者之间发出的取消指令丢失。
            currentFileListener.set(fileListener)
            if (listener?.isCancelled() == true) {
                cancelAndThrow()
            }

            try {
                pushFile(file, remotePath, fileListener)
            } catch (e: SyncException) {
                if (e.errorCode == SyncException.SyncError.CANCELED) {
                    listener?.onFileEnd(index, file, true)
                    cancelAndThrow()
                }
                throw e
            }

            // 空文件不会触发进度回调，此处校正总体进度。
            totalTransferred = baseTransferred + file.length()
            if (file.length() <= 0) {
                notifyTotalProgress()
            }
            listener?.onFileEnd(index, file, false)
        }

        listener?.onEnd(false)
    }

    /**
     * 推送文件到 Android 设备。
     *
     * `adb push` 命令封装，支持目录，适用于无需获取进度的简单场景。
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
     * @param[local] 目标文件（PC 端）。如果以 `/` 结尾表示将文件写入该目录并保持原名，否则表示使用路径指定的文件名称。
     * @throws[FileNotFoundException] 目标目录不可写。
     * @throws[IllegalStateException] 源文件指向目录。
     * @throws[IllegalArgumentException] 未指明目标路径。
     * @throws[IOException] 目标路径不可写入。
     * @throws[SyncException] 传输失败或用户取消任务。具体错误类型可通过 [SyncException.SyncError] 获取。
     */
    fun pullFile(remote: String, local: File, listener: ADBFileTransferListener? = null) {
        /* 参数校验 */
        if (remote.isBlank()) {
            throw IllegalArgumentException("Remote path is empty!")
        }


        /* 特殊规则：目标路径以 `/` 结尾时，将写入该目录并保持原名。 */
        val remoteFileName = FilePathUtil.getFileName(remote)
        val localPath = if (local.isDirectory) {
            if (!local.canWrite()) {
                throw FileNotFoundException("Local path not exist or writeable! Path:[${local}]")
            }

            "${local.absolutePath}${File.separator}${remoteFileName}"
        } else {
            val parent = local.parentFile
            if (!parent.canWrite()) {
                throw FileNotFoundException("Local path not exist or writeable! Path:[${parent}]")
            }

            local.absolutePath
        }

        // 检查设备上是否存在该文件
        val isFile: Boolean = CLIUtil.isSuccess(runForStatus("test -f \"$remote\""))
        if (!isFile) {
            throw FileNotFoundException("Remote file not exist or is directory! Path:[$remote]")
        }

        // 读取文件大小（DDMLib 输入路径时不会回调文件大小，需要通过命令读取。）
        val fileSize: Long = runForText("stat -c %s \"$remote\"")?.toLongOrNull() ?: 0L

        // DDMLib 进度回调实现
        val monitor = object : SyncService.ISyncProgressMonitor {

            var transferredSize: Long = 0L
            val cancelled = AtomicBoolean(false)


            init {
                // 监听用户发出的取消指令
                listener?.setCancelNotifier {
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
                listener?.onStart(fileSize)
            }

            /**
             * 回调方法：传输结束。
             *
             * 如果中途被取消则不会回调该方法。
             */
            override fun stop() {
                listener?.onEnd(false)
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
                    listener?.onProgress(transferredSize, Double.NaN)
                } else {
                    val progress = transferredSize.toDouble() / fileSize.toDouble()
                    listener?.onProgress(transferredSize, progress)
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
     * 从 Android 设备拉取文件。
     *
     * `adb pull` 命令封装，支持目录，适用于无需获取进度的简单场景。
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
