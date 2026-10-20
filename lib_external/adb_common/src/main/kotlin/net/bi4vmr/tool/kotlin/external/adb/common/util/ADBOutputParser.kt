package net.bi4vmr.tool.kotlin.external.adb.common.util

/**
 * ADB 输出解析器。
 * <p>
 * 用于解析 ADB 命令的输出结果，提取设备信息等内容。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object ADBOutputParser {

    // 正则表达式：屏幕 ID 匹配（Android 11 及更高版本）。
    private val displayIDRegex_V11U = """Display id (\d+):""".toRegex()

    // 正则表达式：屏幕 ID 匹配（Android 11 及更高版本）。
    private val displayIDRegex_V10 = """displayId (\d+)""".toRegex()

    private val uniqueIDRegex = """uniqueId "([^:]+:[^"]+)"""".toRegex()

    private val sizeRegex = """real (\d+) x (\d+)""".toRegex()


    /**
     * 提取屏幕信息（Android 11及更高版本）。
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
     * 提取屏幕信息（Android 10及更低版本）。
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
     * 提取屏幕信息（Android 9及更低版本）。
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

            val uniqueIndex = parseUniqueIndex(uniqueID)
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
    fun parseUniqueIndex(uniqueID: String): String {
        return uniqueID.substringAfter(":")
    }
}