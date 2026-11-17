package net.bi4vmr.tool.kotlin.external.adb.common.util

import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayDensity
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplaySize
import net.bi4vmr.tool.kotlin.external.adb.common.model.WindowInfo

/**
 * ADB 输出解析器。
 * <p>
 * 用于解析 ADB 命令的输出结果，提取设备信息等内容。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBOutputParser {

    /**
     * 正则表达式：`adb connect` 成功连接设备的输出语句。
     */
    private val deviceConnectedRegex = """^(?:already )?connected to .+?:\d+$""".toRegex()

    // 正则表达式：屏幕 ID （Android 11 及更高版本）。
    private val displayIDRegex_V11U = """Display id (\d+):""".toRegex()

    // 正则表达式：屏幕 ID （Android 10）。
    private val displayIDRegex_V10 = """displayId (\d+)""".toRegex()

    // 正则表达式：屏幕物理 ID 。
    private val uniqueIDRegex = """uniqueId "([^:]+:[^"]+)"""".toRegex()

    // 正则表达式：屏幕分辨率。
    private val sizeRegex = """real (\d+) x (\d+)""".toRegex()

    // 正则表达式：Activity 列表中的屏幕分组，例如 `Display #0 (activities from top to bottom):` 。
    private val activityDisplayRegex = """^\s*Display #(\d+)""".toRegex()

    // 正则表达式：Activity 历史记录行，例如 `* Hist  #0: ActivityRecord{...}` 。
    private val activityHistRegex = """^\s*\*?\s*Hist\s+#\d+:""".toRegex()

    // 正则表达式：屏幕内的前台 Activity 行，例如 `Resumed: ActivityRecord{...}` （Android 12 及更高版本）、
    // `topResumedActivity=ActivityRecord{...}` 或 `mResumedActivity: ActivityRecord{...}` （旧版本）。
    // 注意：不匹配位于所有屏幕之后的全局 `ResumedActivity:` 行，它仅表示全局焦点所在的 Activity 。
    private val resumedActivityRegex = """^\s*(?:Resumed:|topResumedActivity=|mResumedActivity:)""".toRegex()

    // 正则表达式：`ActivityRecord{hash u0 com.example/.MainActivity t12}` 。
    private val activityRecordRegex = """ActivityRecord\{\S+\s+u(\d+)\s+([^/\s]+)/([^\s}]+)\}?\s+t(-?\d+)""".toRegex()

    // 正则表达式：窗口列表中的窗口标题行，例如 `Window #0 Window{hash u0 NavigationBar0}:` 。
    private val windowHeaderRegex = """^\s*Window #\d+ Window\{(\S+) u(\d+) (.*)\}:\s*$""".toRegex()

    // 正则表达式：窗口所在屏幕 ID 。
    private val windowDisplayIDRegex = """\bmDisplayId=(\d+)""".toRegex()

    // 正则表达式：窗口所属包名。
    private val windowPackageRegex = """\bpackage=(\S+)""".toRegex()

    // 正则表达式：窗口可见状态。
    private val windowVisibleRegex = """\b(?:isVisible|isOnScreen)=true\b""".toRegex()

    // 正则表达式：窗口信息结束行（无缩进或缩进 2 个空格的非空行）。
    private val windowEndRegex = """^(?: {2})?\S""".toRegex()

    // 正则表达式：`dumpsys window displays` 中的屏幕分组，例如 `Display: mDisplayId=0 ...` 。
    private val windowDisplaySectionRegex = """^\s*Display: mDisplayId=(\d+)""".toRegex()

    // 正则表达式：焦点窗口，例如 `mCurrentFocus=Window{hash u0 com.example/com.example.MainActivity}` 。
    // 正则表达式： `wm size` 命令输出。
    private val wmPhysicalSizeRegex = """Physical size:\s*(\d+)x(\d+)""".toRegex()
    private val wmOverrideSizeRegex = """Override size:\s*(\d+)x(\d+)""".toRegex()

    // 正则表达式： `wm density` 命令输出。
    private val wmPhysicalDensityRegex = """Physical density:\s*(\d+)""".toRegex()
    private val wmOverrideDensityRegex = """Override density:\s*(\d+)""".toRegex()

    private val focusedWindowRegex = """\bmCurrentFocus=Window\{(\S+) u(\d+) (.*)\}""".toRegex()


    /**
     * 获取远程设备地址。
     *
     * 如果不是远程设备，则返回原序列号，调用者可以通过 [ConnectionType] 提前判断连接类型。
     *
     * @param[sn] 设备序列号。
     * @return 地址。
     */
    fun getRemoteDeviceHost(sn: String): String {
        return sn.substringBefore(':')
    }

    /**
     * 获取远程设备端口。
     *
     * 如果不是远程设备，则返回默认值 `5555` ，调用者可以通过 [ConnectionType] 提前判断连接类型。
     *
     * @param[sn] 设备序列号。
     * @return 地址。
     */
    fun getRemoteDevicePort(sn: String): Int {
        return sn.substringAfter(':').toIntOrNull() ?: 5555
    }

    /**
     * 判断是否成功连接远程设备。
     *
     * `adb connect` 命令不会返回错误状态，因此需要通过输出文本进行判断。
     *
     * 仅有以下两种输出表示连接成功：
     *
     * `connected to <IP>:<端口>`：新连接的设备。
     *
     * `already connected to <IP>:<端口>`：设备已连接。
     *
     * @param[input] 命令输出内容。
     * @return `true` 表示连接成功，`false` 表示连接失败。
     */
    fun isDeviceConnected(input: String): Boolean {
        return deviceConnectedRegex.containsMatchIn(input.trim().lowercase())
    }

    /**
     * 提取屏幕信息（Android 11 及更高版本）。
     *
     * 从 `cmd display get-displays` 命令的输出结果中提取屏幕 ID 、物理 ID 、分辨率信息。
     *
     * 命令输出示例：
     *
     * ```text
     * Displays:
     * Display id 0: DisplayInfo{"内置屏幕", displayId 0, displayGroupId 0, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS,
     * FLAG_TRUSTED, real 1080 x 2400, largest app 2400 x 2400, smallest app 1080 x 1080, appVsyncOff 1000000,
     * presDeadline 16666666, mode 1, renderFrameRate 60.000004, defaultMode 2, userPreferredModeId -1, supportedModes
     * [{id=1, width=1080, height=2400, fps=60.000004, vsync=60.000004, synthetic=false, alternativeRefreshRates=
     * [90.0, 120.00001], supportedHdrTypes=[1, 2, 3, 4]}, {id=2, width=1080, height=2400, fps=120.00001, vsync=
     * 120.00001, synthetic=false, alternativeRefreshRates=[60.000004, 90.0], supportedHdrTypes=[1, 2, 3, 4]},
     * {id=3, width=1080, height=2400, fps=90.0, vsync=90.0, synthetic=false, alternativeRefreshRates=[60.000004,
     * 120.00001], supportedHdrTypes=[1, 2, 3, 4]}], appsSupportedModes [{id=1, width=1080, height=2400, fps=60.000004,
     * vsync=60.000004, synthetic=false, alternativeRefreshRates=[90.0, 120.00001], supportedHdrTypes=[1, 2, 3, 4]},
     * {id=2, width=1080, height=2400, fps=120.00001, vsync=120.00001, synthetic=false, alternativeRefreshRates=[60.000004,
     * 90.0], supportedHdrTypes=[1, 2, 3, 4]}, {id=3, width=1080, height=2400, fps=90.0, vsync=90.0, synthetic=false,
     * alternativeRefreshRates=[60.000004, 120.00001], supportedHdrTypes=[1, 2, 3, 4]}], hdrCapabilities HdrCapabilities
     * {mSupportedHdrTypes=[1, 2, 3, 4], mMaxLuminance=420.0, mMaxAverageLuminance=210.1615, mMinLuminance=0.323},
     * userDisabledHdrTypes [], minimalPostProcessingSupported false, rotation 0, state ON, committedState ON, type
     * INTERNAL, uniqueId "local:4630946370515662722", app 1080 x 2400, density 440 (394.705 x 394.563) dpi, layerStack
     * 0, colorMode 0, supportedColorModes [0, 7, 9], address {port=130, model=0x40446d302c7877}, deviceProductInfo
     * DeviceProductInfo{name=, manufacturerPnpId=QCM, productId=1, modelYear=null, manufactureDate=ManufactureDate{
     * week=27, year=2006}, connectionToSinkType=0}, removeMode 0, refreshRateOverride 60.000004, brightnessMinimum 0.0,
     * brightnessMaximum 1.0, brightnessDefault 0.07496032, installOrientation ROTATION_0, layoutLimitedRefreshRate null
     * , hdrSdrRatio not_available, thermalRefreshRateThrottling {}, thermalBrightnessThrottlingDataId default},
     * DisplayMetrics{density=2.75, width=1080, height=2400, scaledDensity=2.75, xdpi=394.705, ydpi=394.563},
     * isValid=true
     * ```
     *
     * @param[input] 原始命令内容（分行）。
     * @return 屏幕信息列表。
     */
    @JvmStatic
    fun parseDisplayInfoV11U(input: List<String>): List<DisplayInfo> {
        val displays = mutableListOf<DisplayInfo>()

        input.forEach { line ->
            if (line.trim().isEmpty()) return@forEach

            val displayID = displayIDRegex_V11U.find(line)
                ?.groupValues
                ?.get(1)

            // DisplayID 为必要信息，若未获取到则跳过本行。
            if (displayID == null) {
                return@forEach
            }

            val uniqueID = uniqueIDRegex.find(line)
                ?.groupValues
                ?.get(1)

            val width = sizeRegex.find(line)?.groupValues?.get(1)
            val height = sizeRegex.find(line)?.groupValues?.get(2)
            displays.add(
                DisplayInfo(
                    id = displayID,
                    uniqueID = uniqueID ?: "",
                    width = width?.toIntOrNull() ?: -1,
                    height = height?.toIntOrNull() ?: -1
                )
            )
        }

        return displays
    }

    /**
     * 提取屏幕信息（Android 10）。
     *
     * 从 `dumpsys display | grep mBaseDisplayInfo=DisplayInfo` 命令的输出结果中提取屏幕 ID 、物理 ID 、分辨率信息。
     *
     * 命令输出示例：
     *
     * ```text
     * mBaseDisplayInfo=DisplayInfo{"内置屏幕", displayId 0, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS, FLAG_TRUSTED,
     * real 1080 x 2340, largest app 1080 x 2340, smallest app 1080 x 2340, appVsyncOff 1000000, presDeadline 16666666,
     * mode 1, defaultMode 1, modes [{id=1, width=1080, height=2340, fps=60.000004}], hdrCapabilities HdrCapabilities{
     * mSupportedHdrTypes=[], mMaxLuminance=500.0, mMaxAverageLuminance=500.0, mMinLuminance=0.0},
     * minimalPostProcessingSupported false, rotation 0, state ON, type INTERNAL, uniqueId "local:19260784713820289",
     * app 1080 x 2340, density 440 (409.432 x 409.903) dpi, layerStack 0, colorMode 0, supportedColorModes [0], address
     * {port=129, model=0x446d94e6f668}, deviceProductInfo DeviceProductInfo{name=, manufacturerPnpId=QCM, productId=1,
     * modelYear=null, manufactureDate=ManufactureDate{week=27, year=2006}, relativeAddress=null}, removeMode 0}
     *
     * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", displayId 1, FLAG_SECURE, FLAG_PRESENTATION, FLAG_TRUSTED,
     * real 720 x 480, largest app 720 x 480, smallest app 720 x 480, appVsyncOff 0, presDeadline 33333332, mode 2,
     * defaultMode 2, modes [{id=2, width=720, height=480, fps=60.000004}], hdrCapabilities null,
     * minimalPostProcessingSupported false, rotation 0, state ON, type OVERLAY, uniqueId "overlay:1", app 720 x 480,
     * density 142 (142.0 x 142.0) dpi, layerStack 1, colorMode 0, supportedColorModes [0], deviceProductInfo null,
     * removeMode 0}
     * ```
     *
     * @param[input] 原始命令内容（分行）。
     * @return 屏幕信息列表。
     */
    @JvmStatic
    fun parseDisplayInfoV10(input: List<String>): List<DisplayInfo> {
        val displays = mutableListOf<DisplayInfo>()

        input.forEach { line ->
            if (line.trim().isEmpty()) return@forEach

            val displayID = displayIDRegex_V10.find(line)
                ?.groupValues
                ?.get(1)

            // DisplayID 为必要信息，若未获取到则跳过本行。
            if (displayID == null) {
                return@forEach
            }

            val uniqueID = uniqueIDRegex.find(line)
                ?.groupValues
                ?.get(1)

            val width = sizeRegex.find(line)?.groupValues?.get(1)
            val height = sizeRegex.find(line)?.groupValues?.get(2)
            displays.add(
                DisplayInfo(
                    id = displayID,
                    uniqueID = uniqueID ?: "",
                    width = width?.toIntOrNull() ?: -1,
                    height = height?.toIntOrNull() ?: -1
                )
            )
        }

        return displays
    }

    /**
     * 提取屏幕信息（Android 9 及更低版本）。
     *
     * 从 `dumpsys display | grep mBaseDisplayInfo=DisplayInfo` 命令的输出结果中提取屏幕 ID 、物理 ID 、分辨率信息。
     *
     * 命令输出示例：
     *
     * ```text
     * mBaseDisplayInfo=DisplayInfo{"内置屏幕", uniqueId "local:0", app 1080 x 2160, real 1080 x 2160, largest app
     * 1080 x 2160, smallest app 1080 x 2160, mode 1, defaultMode 1, modes [{id=1, width=1080, height=2160, fps=60.000004}]
     * , colorMode 0, supportedColorModes [0], hdrCapabilities android.view.Display$HdrCapabilities@ced7296a, rotation 0
     * , density 480 (374.0 x 374.0) dpi, layerStack 0, appVsyncOff 1000000, presDeadline 16666666, type BUILT_IN, state
     * ON, FLAG_SECURE, FLAG_SUPPORTS_PROTECTED_BUFFERS, removeMode 0}
     *
     * mBaseDisplayInfo=DisplayInfo{"叠加视图 #1", uniqueId "overlay:1", app 1280 x 720, real 1280 x 720, largest app 1280
     * x 720, smallest app 1280 x 720, mode 2, defaultMode 2, modes [{id=2, width=1280, height=720, fps=60.000004}],
     * colorMode 0, supportedColorModes [0], hdrCapabilities null, rotation 0, density 213 (213.0 x 213.0) dpi,
     * layerStack 1, appVsyncOff 0, presDeadline 33333332, type OVERLAY, state ON, FLAG_PRESENTATION, removeMode 0}
     * ```
     *
     * @param[input] 原始命令内容（分行）。
     * @return 屏幕信息列表。
     */
    @JvmStatic
    fun parseDisplayInfoV9L(input: List<String>): List<DisplayInfo> {
        val displays = mutableListOf<DisplayInfo>()

        input.forEach { line ->
            if (line.trim().isEmpty()) return@forEach

            // Android 9 及更低版本多屏支持不完善，命令输出不会通告显示器 ID ，物理 ID 后的序号与逻辑 ID 一致，提取该字段即可。
            val uniqueID = uniqueIDRegex.find(line)
                ?.groupValues
                ?.get(1)

            // DisplayID 为必要信息，若未获取到则跳过本行。
            if (uniqueID == null) {
                return@forEach
            }

            val uniqueIndex = parseDisplayUniqueIndex(uniqueID)
            val width = sizeRegex.find(line)?.groupValues?.get(1)
            val height = sizeRegex.find(line)?.groupValues?.get(2)
            displays.add(
                DisplayInfo(
                    id = uniqueIndex,
                    uniqueID = uniqueID,
                    width = width?.toIntOrNull() ?: -1,
                    height = height?.toIntOrNull() ?: -1
                )
            )
        }

        return displays
    }

    /**
     * 获取物理 ID 索引编号。
     *
     * 获取屏幕物理 ID 冒号后的索引编号，类似 `local:0` 中的 `0` 。如果格式不符合预期，则返回 [uniqueID] 原值。
     *
     * @param[uniqueID] 物理 ID 。
     * @return 索引编号。
     */
    fun parseDisplayUniqueIndex(uniqueID: String): String {
        return uniqueID.substringAfter(":")
    }

    /**
     * 解析 `pm list packages` 命令输出。
     *
     * @return 包名列表。
     */
    fun parsePMListPackageOutput(input: List<String>): List<String> {
        val list = mutableListOf<String>()
        input.forEach { line ->
            // 将 `package:com.android.music` 转换为 `com.android.music`
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("package:")) {
                val packageName = trimmedLine.removePrefix("package:")
                list.add(packageName)
            }
        }

        return list.sorted()
    }

    /**
     * 解析 `dumpsys activity activities` 命令输出中的 Activity 列表。
     *
     * 每个屏幕内的 Activity 按照从顶层到底层的顺序排列。
     *
     * @param[input] 命令输出。
     * @return Activity 列表。
     */
    fun parseActivities(input: List<String>): List<ActivityInfo> {
        val list = mutableListOf<ActivityInfo>()
        var displayID: String? = null
        input.forEach { line ->
            activityDisplayRegex.find(line)?.let {
                displayID = it.groupValues[1]
                return@forEach
            }
            // 无缩进的非空行表示屏幕分组已结束
            if (line.isNotBlank() && !line.first().isWhitespace()) {
                displayID = null
                return@forEach
            }

            val currentDisplay = displayID ?: return@forEach
            if (!activityHistRegex.containsMatchIn(line)) return@forEach
            parseActivityRecord(line, currentDisplay)?.let { list.add(it) }
        }

        return list
    }

    /**
     * 解析 `dumpsys activity activities` 命令输出中的前台 Activity 。
     *
     * @param[input] 命令输出。
     * @return 各屏幕的前台 Activity 列表，每个屏幕最多一项。
     */
    fun parseResumedActivities(input: List<String>): List<ActivityInfo> {
        val map = linkedMapOf<String, ActivityInfo>()
        var displayID: String? = null
        input.forEach { line ->
            activityDisplayRegex.find(line)?.let {
                displayID = it.groupValues[1]
                return@forEach
            }
            // 无缩进的非空行表示屏幕分组已结束
            if (line.isNotBlank() && !line.first().isWhitespace()) {
                displayID = null
                return@forEach
            }

            val currentDisplay = displayID ?: return@forEach
            if (map.containsKey(currentDisplay)) return@forEach
            if (!resumedActivityRegex.containsMatchIn(line)) return@forEach
            parseActivityRecord(line, currentDisplay)?.let { map[currentDisplay] = it }
        }

        return map.values.toList()
    }

    /**
     * 解析 `dumpsys window windows` 命令输出中的窗口列表。
     *
     * 窗口按照从顶层到底层的顺序排列。
     *
     * @param[input] 命令输出。
     * @return 窗口列表。
     */
    fun parseWindows(input: List<String>): List<WindowInfo> {
        val list = mutableListOf<WindowInfo>()
        var current: WindowInfo? = null

        input.forEach { line ->
            val header = windowHeaderRegex.find(line)
            if (header != null) {
                current?.let { list.add(it) }
                current = WindowInfo(
                    hash = header.groupValues[1],
                    userID = header.groupValues[2].toIntOrNull() ?: -1,
                    title = header.groupValues[3].trim()
                )
                return@forEach
            }

            val window = current ?: return@forEach
            // 窗口属性行缩进至少为 4 个空格（折行内容可能仅缩进 1 个空格），缩进为 0 或 2 个空格的非空行表示当前窗口的信息已结束。
            if (windowEndRegex.containsMatchIn(line)) {
                list.add(window)
                current = null
                return@forEach
            }

            var updated = window
            if (updated.displayID.isEmpty()) {
                windowDisplayIDRegex.find(line)?.let { updated = updated.copy(displayID = it.groupValues[1]) }
            }
            if (updated.packageName.isEmpty()) {
                windowPackageRegex.find(line)?.let { updated = updated.copy(packageName = it.groupValues[1]) }
            }
            if (!updated.visible && windowVisibleRegex.containsMatchIn(line)) {
                updated = updated.copy(visible = true)
            }
            current = updated
        }
        current?.let { list.add(it) }

        // 部分窗口没有 `package=` 字段，尝试从 `包名/类名` 格式的标题中提取包名。
        return list.map {
            if (it.packageName.isEmpty() && it.title.contains('/')) {
                it.copy(packageName = it.title.substringBefore('/').substringAfterLast(' '))
            } else {
                it
            }
        }
    }

    /**
     * 解析 `dumpsys window` 命令输出中的焦点窗口。
     *
     * @param[input] 命令输出。
     * @return 焦点窗口列表。键为屏幕 ID （未知时为空字符串），值为窗口信息（仅包含哈希值、用户 ID 与标题）。
     */
    fun parseFocusedWindows(input: List<String>): Map<String, WindowInfo> {
        val map = linkedMapOf<String, WindowInfo>()
        var displayID = ""
        input.forEach { line ->
            windowDisplaySectionRegex.find(line)?.let {
                displayID = it.groupValues[1]
                return@forEach
            }

            val match = focusedWindowRegex.find(line) ?: return@forEach
            if (map.containsKey(displayID)) return@forEach
            map[displayID] = WindowInfo(
                hash = match.groupValues[1],
                userID = match.groupValues[2].toIntOrNull() ?: -1,
                title = match.groupValues[3].trim(),
                displayID = displayID
            )
        }

        return map
    }

    /**
     * 合并焦点窗口与窗口列表。
     *
     * 通过哈希值关联窗口列表，补全焦点窗口的屏幕 ID 、包名等信息。
     *
     * @param[focused] [parseFocusedWindows] 的解析结果。
     * @param[windows] [parseWindows] 的解析结果。
     * @return 焦点窗口列表。
     */
    fun mergeFocusedWindows(focused: Map<String, WindowInfo>, windows: List<WindowInfo>): List<WindowInfo> {
        val windowMap = windows.associateBy { it.hash }
        return focused.values
            .map { focus ->
                windowMap[focus.hash] ?: run {
                    val packageName = if (focus.title.contains('/')) focus.title.substringBefore('/') else ""
                    focus.copy(packageName = packageName, visible = true)
                }
            }
            .distinctBy { it.hash }
    }

    /**
     * 解析 `wm size` 命令输出。
     *
     * @param[input] 命令输出。
     * @return 分辨率信息。若解析失败或显示屏不存在（输出为 `0x0` ）则返回空值。
     */
    fun parseWMSize(input: List<String>): DisplaySize? {
        var physical: MatchResult? = null
        var override: MatchResult? = null
        input.forEach { line ->
            physical = physical ?: wmPhysicalSizeRegex.find(line)
            override = override ?: wmOverrideSizeRegex.find(line)
        }

        val width = physical?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val height = physical?.groupValues?.get(2)?.toIntOrNull() ?: return null
        if (width <= 0 || height <= 0) return null

        return DisplaySize(
            physicalWidth = width,
            physicalHeight = height,
            overrideWidth = override?.groupValues?.get(1)?.toIntOrNull() ?: -1,
            overrideHeight = override?.groupValues?.get(2)?.toIntOrNull() ?: -1
        )
    }

    /**
     * 解析 `wm density` 命令输出。
     *
     * @param[input] 命令输出。
     * @return 密度信息。若解析失败则返回空值。
     */
    fun parseWMDensity(input: List<String>): DisplayDensity? {
        var physical: Int? = null
        var override: Int? = null
        input.forEach { line ->
            physical = physical ?: wmPhysicalDensityRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
            override = override ?: wmOverrideDensityRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
        }

        val value = physical ?: return null
        if (value <= 0) return null
        return DisplayDensity(physical = value, override = override ?: -1)
    }

    /**
     * 解析 `ActivityRecord{...}` 文本。
     *
     * @param[line] 包含 `ActivityRecord{...}` 的文本。
     * @param[displayID] 屏幕 ID 。
     * @return Activity 信息。若格式不符合预期则返回空值。
     */
    private fun parseActivityRecord(line: String, displayID: String): ActivityInfo? {
        val match = activityRecordRegex.find(line) ?: return null
        val packageName = match.groupValues[2]
        val shortClass = match.groupValues[3]
        // 将 `.MainActivity` 简写形式补全为完整类名
        val className = if (shortClass.startsWith(".")) "$packageName$shortClass" else shortClass
        return ActivityInfo(
            displayID = displayID,
            packageName = packageName,
            className = className,
            taskID = match.groupValues[4].toIntOrNull() ?: -1,
            userID = match.groupValues[1].toIntOrNull() ?: -1
        )
    }
}
