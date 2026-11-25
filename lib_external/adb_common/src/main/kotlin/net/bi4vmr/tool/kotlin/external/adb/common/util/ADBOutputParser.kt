package net.bi4vmr.tool.kotlin.external.adb.common.util

import net.bi4vmr.tool.kotlin.external.adb.common.enums.ConnectionType
import net.bi4vmr.tool.kotlin.external.adb.common.model.ActivityInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayDensity
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplaySize
import net.bi4vmr.tool.kotlin.external.adb.common.model.FocusInfo
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

    /**
     * 正则表达式：屏幕 ID（Android 11 及更高版本）。
     */
    private val displayIDRegex_V11U = """Display id (\d+):""".toRegex()

    /**
     * 正则表达式：屏幕 ID（Android 10）。
     */
    private val displayIDRegex_V10 = """displayId (\d+)""".toRegex()

    /**
     * 正则表达式：屏幕物理 ID。
     */
    private val uniqueIDRegex = """uniqueId "([^:]+:[^"]+)"""".toRegex()

    /**
     * 正则表达式：屏幕分辨率。
     */
    private val sizeRegex = """real (\d+) x (\d+)""".toRegex()

    /**
     * 正则表达式：`wm size` 命令输出（物理尺寸）。
     */
    private val wmPhysicalSizeRegex = """Physical size:\s*(\d+)x(\d+)""".toRegex()

    /**
     * 正则表达式：`wm size` 命令输出（用户改写尺寸）。
     */
    private val wmOverrideSizeRegex = """Override size:\s*(\d+)x(\d+)""".toRegex()

    /**
     * 正则表达式：`wm density` 命令输出（物理尺寸）。
     */
    private val wmPhysicalDensityRegex = """Physical density:\s*(\d+)""".toRegex()

    /**
     * 正则表达式：`wm density` 命令输出（用户改写尺寸）。
     */
    private val wmOverrideDensityRegex = """Override density:\s*(\d+)""".toRegex()

    /**
     * 正则表达式：Activity 列表中的屏幕分组。
     *
     * 例如：`Display #0 (activities from top to bottom)`。
     */
    private val activityDisplayRegex = """^\s*Display #(\d+)""".toRegex()

    /**
     * 正则表达式：Activity 历史记录行。
     *
     * 例如：`* Hist  #0: ActivityRecord`。
     */
    private val activityHistRegex = """^\s*\*?\s*Hist\s+#\d+:""".toRegex()

    /**
     * 正则表达式：ActivityRecord。
     *
     * 格式：`ActivityRecord{<对象 Hash> u<用户 ID> <Activity 完整名称> t<任务 ID>}`。
     */
    private val activityRecordRegex = """ActivityRecord\{(\S+)\s+u(\d+)\s+([^/\s]+)/([^\s}]+)\}?\s+t(-?\d+)""".toRegex()

    /**
     * 正则表达式： Activity 详情中的进程信息。
     *
     * 格式：`app=ProcessRecord{<对象 Hash> <进程 ID>:<进程名称>/u0a100}`。
     */
    private val activityProcessRecordRegex = """app=ProcessRecord\{\S+\s+(\d+):([^/\s}]+)""".toRegex()

    /**
     * 正则表达式：Activity 详情中的启动者 UID。
     */
    private val activityLaunchedFromUIDRegex = """\blaunchedFromUid=(-?\d+)""".toRegex()

    /**
     * 正则表达式：Activity 详情中的启动者包名。
     */
    private val activityLaunchedFromPackageRegex = """\blaunchedFromPackage=(\S+)""".toRegex()

    /**
     * 正则表达式：Activity 详情中的启动 Intent 信息。
     */
    private val activityIntentRegex = """^\s*(?:Intent\s*|intent=)\{\s*(.*?)\s*\}\s*$""".toRegex()

    /**
     * 正则表达式：Activity 详情中的 TaskAffinity。
     */
    private val activityTaskAffinityRegex = """\btaskAffinity=(?:\d+:)?(\S+)""".toRegex()

    /**
     * 正则表达式：Activity 列表中当前屏幕内状态为 Resumed 的实例。
     */
    private val resumeActivityRegex = """^\s*(?:Resumed:|ResumedActivity:)""".toRegex()

    /**
     * 正则表达式：Activity 信息中当前焦点窗口信息。
     *
     * 格式：`Window{<对象 Hash> u<用户 ID> <窗口名称>}`。
     *
     * 一些版本的变体格式：
     *
     * - Android 11 : Window{d16f04d mode=0 rootTaskId=1 u0 NotificationShade}
     * - Android 15 : Window{da5e969 u0 NotificationShade}
     */
    private val focusedWindowRegex = """\bmCurrentFocus=Window\{(\S+?)(?:\s+.*?)?\s+u\d+\s+(\S+?)\}""".toRegex()

    /**
     * 正则表达式：Window 信息中焦点屏幕信息。
     *
     * 格式：`Display: mDisplayId=<屏幕 ID>`。
     *
     * 多屏设备中每个屏幕都会有一段该格式的行，需要结合 `mTopFocusedDisplayId` 确定焦点屏幕。
     */
    private val focusedDisplayRegex = """^\s*Display: mDisplayId=(\d+)""".toRegex()

    /**
     * 正则表达式：全局焦点所在的屏幕 ID。
     *
     * 格式：`mTopFocusedDisplayId=<屏幕 ID>`。
     */
    private val topFocusedDisplayRegex = """\bmTopFocusedDisplayId=(\d+)""".toRegex()

    /**
     * 正则表达式：窗口列表中的窗口标题行。
     *
     * 格式：`Window #<序号> Window{<Hash> u<用户 ID> <窗口标题>}:`。
     */
    private val windowHeaderRegex =
        """^\s*Window #\d+ Window\{(\S+?)(?:\s+.*?)??\s+u(-?\d+)\s+(.+?)\}:\s*$""".toRegex()

    /**
     * 正则表达式：窗口所在的屏幕 ID。
     */
    private val windowDisplayIDRegex = """\bmDisplayId=(\d+)""".toRegex()

    /**
     * 正则表达式：窗口所属包名。
     */
    private val windowPackageRegex = """\bpackage=(\S+)""".toRegex()

    /**
     * 正则表达式：窗口可见状态。
     */
    private val windowVisibleRegex = """\bisVisible=(true|false)\b""".toRegex()


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
     * 解析 `wm size` 命令输出。
     *
     * 命令输出示例：
     *
     * ```text
     * Physical size: 1080x2160
     * Override size: 1080x1920
     * ```
     *
     * @param[input] 命令输出。
     * @return 分辨率信息。若解析失败则返回空值。
     */
    fun parseWMSize(input: List<String>): DisplaySize? {
        var physical: MatchResult? = null
        var override: MatchResult? = null
        input.forEach { line ->
            physical = physical ?: wmPhysicalSizeRegex.find(line)
            override = override ?: wmOverrideSizeRegex.find(line)
        }

        /* 物理尺寸为必要信息，若未获取到则返回空值。 */
        var width = -1
        var height = -1
        physical?.let {
            width = it.groupValues[1].toIntOrNull() ?: return null
            height = it.groupValues[2].toIntOrNull() ?: return null
        }
        if (width <= 0 || height <= 0) return null

        /* 解析用户覆写尺寸 */
        var overrideWidth = -1
        var overrideHeight = -1
        override?.let {
            overrideWidth = it.groupValues[1].toIntOrNull() ?: -1
            overrideHeight = it.groupValues[2].toIntOrNull() ?: -1
        }

        return DisplaySize(
            physicalWidth = width,
            physicalHeight = height,
            overrideWidth = overrideWidth,
            overrideHeight = overrideHeight
        )
    }

    /**
     * 解析 `wm density` 命令输出。
     *
     * 命令输出示例：
     *
     * ```text
     * Physical density: 480
     * Override density: 160
     * ```
     *
     * @param[input] 命令输出。
     * @return 像素密度信息。若解析失败则返回空值。
     */
    fun parseWMDensity(input: List<String>): DisplayDensity? {
        var physical: MatchResult? = null
        var override: MatchResult? = null
        input.forEach { line ->
            physical = physical ?: wmPhysicalDensityRegex.find(line)
            override = override ?: wmOverrideDensityRegex.find(line)
        }

        /* 物理像素密度为必要信息，若未获取到则返回空值。 */
        var physicalDensity = 0
        physical?.let {
            physicalDensity = it.groupValues[1].toIntOrNull() ?: 0
        }
        if (physicalDensity <= 0) return null

        val overrideDensity = override?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return DisplayDensity(physicalDensity, overrideDensity)
    }

    /**
     * 解析 `dumpsys activity activities` 命令输出的 Activity 列表。
     *
     * 命令输出示例：
     *
     * ```text
     * Display #0 (activities from top to bottom):
     * Task{aa315c0 #93 type=standard A=10156:net.bi4vmr.study.ui.compose.graphic U=0 visible=false visibleRequested=false
     * mode=fullscreen translucent=true sz=2}
     * mLastPausedActivity: ActivityRecord{162549369 u0 net.bi4vmr.study.ui.compose.graphic/net.bi4vmr.study.shadow.TestUIBaseShadow t93}
     * mLastNonFullscreenBounds=Rect(280, 640 - 801, 1720) isSleeping=false
     *   * Hist  #1: ActivityRecord{162549369 u0 net.bi4vmr.study.ui.compose.graphic/net.bi4vmr.study.shadow.TestUIBaseShadow t93}
     *     packageName=net.bi4vmr.study.ui.compose.graphic processName=net.bi4vmr.study.ui.compose.graphic
     *     launchedFromUid=10156 launchedFromPackage=net.bi4vmr.study.ui.compose.graphic launchedFromFeature=null userId=0
     *     app=ProcessRecord{264c5b5 18434:net.bi4vmr.study.ui.compose.graphic/u0a156}
     *     Intent { xflg=0x4 cmp=net.bi4vmr.study.ui.compose.graphic/net.bi4vmr.study.shadow.TestUIBaseShadow }
     *     rootOfTask=false task=Task{aa315c0 #93 type=standard A=10156:net.bi4vmr.study.ui.compose.graphic}
     *     taskAffinity=10156:net.bi4vmr.study.ui.compose.graphic
     *     <此处省略部分输出...>
     *   * Hist  #0: ActivityRecord{152553714 u0 net.bi4vmr.study.ui.compose.graphic/net.bi4vmr.study.MainActivity t93}
     *     packageName=net.bi4vmr.study.ui.compose.graphic processName=net.bi4vmr.study.ui.compose.graphic
     *     launchedFromUid=2000 launchedFromPackage=com.android.shell launchedFromFeature=null userId=0
     *     app=ProcessRecord{264c5b5 18434:net.bi4vmr.study.ui.compose.graphic/u0a156}
     *     Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10000000 xflg=0x4 cmp=net.bi4vmr.study.ui.compose.graphic/net.bi4vmr.study.MainActivity }
     *     rootOfTask=true task=Task{aa315c0 #93 type=standard A=10156:net.bi4vmr.study.ui.compose.graphic}
     *     taskAffinity=10156:net.bi4vmr.study.ui.compose.graphic
     *
     * Display #1 (activities from top to bottom):
     * <此处省略部分输出...>
     * ```
     *
     * 每个屏幕内的 Activity 按照从顶层到底层的顺序排列。
     *
     * @param[input] 命令输出。
     * @return Activity 列表。
     */
    fun parseDumpActivities(input: List<String>): List<ActivityInfo> {
        val results = mutableListOf<ActivityInfo>()

        var currentDisplayID: String? = null
        var currentActivity: ActivityInfo? = null
        // 输出内容层级按照缩进区分，因此进入每个窗口区块时需要记录当前缩进量，根据后续行的缩进量可判断当前区块是否结束。
        var currentIndent = 0

        // 若先前内容已解析到 Activity 信息，将其提交至结果列表，并清空暂存变量。
        fun commitActivityInfo() {
            currentActivity?.let { results.add(it) }
            currentActivity = null
        }

        input.forEach { line ->
            if (line.isBlank()) return@forEach

            // 匹配屏幕分组 `Display #X`
            activityDisplayRegex.find(line)?.let {
                commitActivityInfo()
                currentDisplayID = it.groupValues[1]
                return@forEach
            }

            // 屏幕分组是顶层元素，遇到无缩进的非空行，表示当前屏幕分组区块结束。
            if (!line.first().isWhitespace()) {
                commitActivityInfo()
                currentDisplayID = null
                return@forEach
            }

            // 若先前内容已解析屏幕信息，继续解析 Activity 信息，否则忽略当前行。
            val displayID = currentDisplayID ?: return@forEach

            /*
             * 若为 ResumedActivity 行，则查找现有 Activity 中对应的记录，将属性置位。
             *
             * 该信息在每个显示屏区块的末尾出现，其中的 Activity 应当已解析到结果列表中。
             */
            if (resumeActivityRegex.containsMatchIn(line)) {
                val match = activityRecordRegex.find(line) ?: return@forEach
                val hash = match.groupValues[1]
                // 寻找旧的表项，将其替换为新的记录。
                val i = results.indexOfFirst { it.displayID == displayID && it.hash == hash }
                if (i >= 0) {
                    val old = results.removeAt(i)
                    val new = old.copy(resumed = true)
                    results.add(i, new)
                }
                return@forEach
            }

            // 计算当前行的缩进量
            val indent = line.indexOfFirst { !it.isWhitespace() }

            // 当前行是每条 Activity 记录的起始行。
            if (activityHistRegex.containsMatchIn(line)) {
                commitActivityInfo()
                // 未匹配到关键信息则跳过本行
                val match = activityRecordRegex.find(line) ?: return@forEach
                // 已匹配到关键信息，解析 Activity 名称等属性。
                val hash = match.groupValues[1]
                val userID = match.groupValues[2].toIntOrNull() ?: -1
                val packageName = match.groupValues[3]
                val className = match.groupValues[4]
                val taskID = match.groupValues[5].toIntOrNull() ?: -1
                currentActivity = ActivityInfo(
                    packageName = packageName,
                    className = className,
                    displayID = displayID,
                    userID = userID,
                    taskID = taskID,
                    hash = hash
                )
                currentIndent = indent
                return@forEach
            }

            // 读取到缩进量小于 `Hist` 行，说明当前 Activity 区块已结束。
            val activity = currentActivity ?: return@forEach
            if (indent <= currentIndent) {
                commitActivityInfo()
                return@forEach
            }
            // 当前 Activity 区块未结束，则尝试解析剩余属性。
            currentActivity = parseActivityDetail(line, activity)
        }
        // 遍历结束，如有遗留的 Activity 信息则提交至结果列表。
        commitActivityInfo()

        return results
    }

    /**
     * 解析 Window 信息。
     *
     * 从 `dumpsys window windows` 命令的输出结果中提取 Window 信息。
     *
     * 每个屏幕内的 Window 按照从顶层到底层的顺序排列。
     *
     * @param[input] 原始命令内容（分行）。
     * @return Window 列表。
     */
    fun parseDumpWindows(input: List<String>): List<WindowInfo> {
        val results = mutableListOf<WindowInfo>()

        var currentWindow: WindowInfo? = null
        // 输出内容层级按照缩进区分，因此进入每个窗口区块时需要记录当前缩进量，根据后续行的缩进量可判断当前区块是否结束。
        var currentIndent = 0

        // 若先前内容已解析到 Window 信息，将其提交至结果列表，并清空暂存变量。
        fun commitWindowInfo() {
            currentWindow?.let {
                // 部分版本可能没有 `package=` 字段，如果标题格式为 `包名/类名`，可以从此处提取包名。
                if (it.packageName.isEmpty() && it.title.contains('/')) {
                    val new = it.copy(packageName = it.title.substringBefore('/').substringAfterLast(' '))
                    results.add(new)
                } else {
                    results.add(it)
                }
            }
            currentWindow = null
        }

        input.forEach { line ->
            if (line.isBlank()) return@forEach

            // 计算当前行的缩进量
            val indent = line.indexOfFirst { !it.isWhitespace() }

            // 开始标志：匹配窗口信息首行 `Window #X`，解析必要属性。
            windowHeaderRegex.find(line)?.let {
                commitWindowInfo()
                currentWindow = WindowInfo(
                    title = it.groupValues[3].trim(),
                    userID = it.groupValues[2].toIntOrNull() ?: -1,
                    hash = it.groupValues[1]
                )
                currentIndent = indent
                return@forEach
            }

            // 若先前内容已解析窗口信息首行，继续解析窗口详情，否则忽略该行。
            val window = currentWindow ?: return@forEach

            /*
             * 结束标志：遇到缩进小于或等于标题行，且与标题行缩进相差偶数个空格的行。
             *
             * 在先前逻辑中，对于多个连续的 Window 区块，匹配到新的 Window 首行时将会提交前一个窗口信息。
             *
             * 该逻辑用于判断当前 Window 区块结束且后续内容非另一个 Window 区块，防止部分系统输出 Window 不连续遍历无关内容干扰属性解析。
             *
             * 输出缩进通常以 2 为步进，部分系统可能会将换行内容缩进 1 个空格，因此仅当缩进差值为 2 时才认为先前区块结束。
             */
            if (indent <= currentIndent && (currentIndent - indent) % 2 == 0) {
                commitWindowInfo()
                return@forEach
            }

            /* 解析可选属性 */
            var updated = window
            if (window.displayID.isEmpty()) {
                windowDisplayIDRegex.find(line)?.let {
                    updated = updated.copy(displayID = it.groupValues[1])
                }
            }
            if (window.packageName.isEmpty()) {
                windowPackageRegex.find(line)?.let {
                    updated = updated.copy(packageName = it.groupValues[1])
                }
            }
            if (window.visible == null) {
                windowVisibleRegex.find(line)?.let {
                    updated = updated.copy(visible = it.groupValues[1].toBoolean())
                }
            }
            currentWindow = updated
        }
        // 遍历结束，如有遗留的窗口信息则提交至结果列表。
        commitWindowInfo()

        return results
    }

    /**
     * 解析全局焦点信息。
     *
     * 解析 `dumpsys window | grep -iE "Display: mDisplayId=|mCurrentFocus=|mFocusedApp=|FocusedWindow=|TopFocused"`
     * 命令输出内容，获取当前焦点所在的 Activity 与 Window 信息。
     *
     * 命令输出示例：
     *
     * ```text
     * Display: mDisplayId=0
     * mCurrentFocus=Window{fb324e6 u0 com.android.settings/com.android.settings.SubSettings}
     * mFocusedApp=AppWindowToken{a2fe24f token=Token{33174ae ActivityRecord{1d39097 u11 com.android.launcher/.HomeActivity t1100753}}}
     *   mFocusedWindow=Window{fb324e6 u0 com.android.settings/com.android.settings.SubSettings}
     *   mFocusedApp=Token{33174ae ActivityRecord{ad46929 u0 com.android.settings/.SubSettings t6}}
     * Display: mDisplayId=1
     *   mCurrentFocus=...
     *   mFocusedApp=...
     * mTopFocusedDisplayId=0
     * ```
     *
     * @param[input] 命令输出内容。
     * @return 焦点信息。
     */
    fun parseGlobalFocusInfo(input: List<String>): FocusInfo {
        // 各屏幕的焦点信息，键为屏幕 ID。
        val displayInfos = mutableMapOf<String, FocusInfo>()

        var currentDisplayID: String? = null
        var currentInfo: FocusInfo? = null
        // 全局焦点所在的屏幕 ID
        var focusedDisplayID: String? = null

        // 若先前内容已解析到屏幕分段的焦点信息，将其提交至结果表，并清空暂存变量。
        fun commitFocusInfo() {
            val displayID = currentDisplayID ?: return
            displayInfos[displayID] = currentInfo ?: FocusInfo(displayID = displayID)
            currentDisplayID = null
            currentInfo = null
        }

        input.forEach { line ->
            if (line.isBlank()) return@forEach

            // 匹配全局焦点屏幕 `mTopFocusedDisplayId=<ID>`，它表示最终需要输出的屏幕。
            topFocusedDisplayRegex.find(line)?.let {
                focusedDisplayID = it.groupValues[1]
                commitFocusInfo()
                return@forEach
            }

            // 匹配屏幕分段首行 `Display: mDisplayId=X`，开始解析当前屏幕的焦点信息。
            focusedDisplayRegex.find(line)?.let {
                commitFocusInfo()
                val displayID = it.groupValues[1]
                currentDisplayID = displayID
                currentInfo = FocusInfo(displayID = displayID)
                return@forEach
            }

            // 若尚未进入屏幕分段，则忽略当前行。
            val displayID = currentDisplayID ?: return@forEach
            var info = currentInfo ?: FocusInfo(displayID = displayID)

            if (line.contains("mFocusedApp=")) {
                /* 解析焦点 Activity 信息 */
                activityRecordRegex.find(line)?.let {
                    val hash = it.groupValues[1]
                    val packageName = it.groupValues[3]
                    val className = it.groupValues[4]
                    info = info.copy(activityHash = hash, activityPackage = packageName, activityClass = className)
                }
            } else if (line.contains("mCurrentFocus=")) {
                /* 解析焦点窗口信息 */
                focusedWindowRegex.find(line)?.let {
                    val hash = it.groupValues[1]
                    val name = it.groupValues[2].trim()
                    info = info.copy(windowName = name, windowHash = hash)
                }
            }
            currentInfo = info
        }
        // 遍历结束，如有遗留的屏幕分段信息则提交至结果表。
        commitFocusInfo()

        // 返回 `mTopFocusedDisplayId` 指定屏幕的焦点信息；若未找到该字段则返回无效值。
        return focusedDisplayID?.let { displayInfos[it] } ?: FocusInfo()
    }

    /**
     * 解析全局焦点信息（Android 9 及更低版本）。
     *
     * Android 9 及更低版本不支持多屏，焦点屏幕 ID 固定为 `0`。
     *
     * 命令输出示例：
     *
     * ```text
     *   mCurrentFocus=Window{b18fe80 u0 com.meizu.flyme.launcher/com.meizu.flyme.launcher.Launcher}
     *   mFocusedApp=AppWindowToken{ddeb129 token=Token{53359ac ActivityRecord{c79b7fe u0 com.meizu.flyme.launcher/.Launcher t136}}}
     * ```
     *
     * @param[input] 命令输出内容。
     * @return 焦点信息。
     */
    fun parseGlobalFocusInfoV9L(input: List<String>): FocusInfo {
        var result = FocusInfo(displayID = "0")
        input.forEach { line ->
            if (line.isBlank()) return@forEach

            if (line.contains("mFocusedApp=")) {
                activityRecordRegex.find(line)?.let {
                    result = result.copy(
                        activityHash = it.groupValues[1],
                        activityPackage = it.groupValues[3],
                        activityClass = it.groupValues[4]
                    )
                }
            } else if (line.contains("mCurrentFocus=")) {
                focusedWindowRegex.find(line)?.let {
                    result = result.copy(
                        windowHash = it.groupValues[1],
                        windowName = it.groupValues[2].trim()
                    )
                }
            }
        }

        return result
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
     * 解析 `dumpsys activity activities` 命令输出中 `Hist` 行之后的 Activity 详情。
     *
     * @param[line] 详情行。
     * @param[info] 当前已解析的 Activity 信息。
     * @return 补充详情后的 Activity 信息。
     */
    private fun parseActivityDetail(line: String, info: ActivityInfo): ActivityInfo {
        var result = info

        activityIntentRegex.find(line)?.let {
            result = result.copy(intent = it.groupValues[1])
        }

        if (line.trimStart().startsWith("app=")) {
            activityProcessRecordRegex.find(line)?.let {
                result = result.copy(
                    processID = it.groupValues[1].toIntOrNull() ?: -1,
                    processName = it.groupValues[2]
                )
            }
        }

        activityLaunchedFromUIDRegex.find(line)?.let {
            result = result.copy(launchedFromUID = it.groupValues[1].toIntOrNull() ?: -1)
        }

        activityLaunchedFromPackageRegex.find(line)?.let {
            result = result.copy(launchedFromPackage = it.groupValues[1])
        }

        activityTaskAffinityRegex.find(line)?.let {
            result = result.copy(taskAffinity = it.groupValues[1])
        }

        return result
    }
}
