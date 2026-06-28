package net.bi4vmr.tool.java.external.adb;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ADB输出解析器。
 * <p>
 * 用于解析ADB命令的输出结果，提取设备显示信息等内容。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class ADBOutputParser {

    /**
     * 解析 dumpsys display displays | grep DisplayDeviceInfo 的输出字符串(备用方法,适用于老版本设备)。
     * <p>
     * 当 {@link #parseDisplayInfo(List)} 方法无法解析到屏幕信息时(通常是设备版本较老),可以使用此方法作为备选方案。
     * 此方法解析 {@code dumpsys display displays | grep DisplayDeviceInfo} 命令的输出,提取显示信息。
     * <p>
     * 示例输入:
     * <pre>
     * DisplayDeviceInfo{"内置屏幕": uniqueId="local:0", 1080 x 2160, modeId 1, defaultModeId 1, supportedModes [{id=1, width=1080, height=2160, fps=60.000004}], ...}
     * </pre>
     *
     * @param lines 传入的 dumpsys display 原始文本行列表
     * @return 解析后的 DisplayInfo 列表
     */
    public static List<DisplayInfo> parseDisplayInfoFromDumpsys(List<String> lines) {
        List<DisplayInfo> displayList = new ArrayList<>();

        if (lines == null || lines.isEmpty()) {
            return displayList;
        }

        // 编译正则表达式,用于匹配 DisplayDeviceInfo 输出中的关键信息
        // 匹配 uniqueId 后的完整值,例如 local:0 / virtual:1 等,保留前缀
        Pattern physicalIdPattern = Pattern.compile("uniqueId\\s*(?:=\\s*)?\"([^\"]+)\"");
        // 匹配首个 supportedModes 数组元素: supportedModes [{id=X, width=W, height=H, ...}]
        Pattern supportedModesPattern = Pattern.compile("supportedModes\\s*\\[\\{id=(\\d+),\\s*width=(\\d+),\\s*height=(\\d+)");

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String physicalID = null;
            int id = -1;
            int width = -1;
            int height = -1;

            // 1. 匹配物理ID (从 uniqueId 后提取,为空则保持可选)
            Matcher physicalMatcher = physicalIdPattern.matcher(line);
            if (physicalMatcher.find()) {
                physicalID = physicalMatcher.group(1);
            }

            // 2. 匹配首个 supportedModes 数组元素中的 id, width, height
            Matcher modesMatcher = supportedModesPattern.matcher(line);
            if (modesMatcher.find()) {
                try {
                    id = Integer.parseInt(modesMatcher.group(1));
                    width = Integer.parseInt(modesMatcher.group(2));
                    height = Integer.parseInt(modesMatcher.group(3));
                } catch (NumberFormatException e) {
                    System.err.println("ADBOutputParser: Failed to parse display modes: " + e.getMessage());
                    continue;
                }
            }

            // 3. 只有成功解析出必要信息时,才组装成 Bean 放入列表
            if (id != -1 && width != -1 && height != -1) {
                DisplayInfo info = new DisplayInfo(id);
                info.setPhysicalID(physicalID);
                info.setWidth(width);
                info.setHeight(height);
                displayList.add(info);
            }
        }

        return displayList;
    }

    /**
     * 解析 dumpsys display 的输出字符串
     *
     * @param lines 传入的 cmd 原始文本
     * @return 解析后的 DisplayInfo 列表
     */
    public static List<DisplayInfo> parseDisplayInfo(List<String> lines) {
        List<DisplayInfo> displayList = new ArrayList<>();

        // 2. 编译正则表达式，用于在每个区块内精准匹配我们需要的数据
        // displayId 和 uniqueId 中的完整值,保留前缀,例如 local:0 / virtual:1
        Pattern idPattern = Pattern.compile("Display id (\\d+):.*?uniqueId\\s*(?:=\\s*)?\"([^\"]+)\"");
        // real 分辨率 (例如: real 1920 x 720)
        Pattern resolutionPattern = Pattern.compile("real (\\d+) x (\\d+)");

        // 循环处理每个区块（注意：split 后的第一个元素由于没有 "Display id " 开头，通常是空字符串，直接跳过）
        for (String block : lines) {
            if (block.trim().isEmpty()) {
                continue;
            }

            int id = -1;
            String physicalID = null;
            int width = -1;
            int height = -1;

            // 3. 匹配 逻辑ID 和 物理ID
            Matcher idMatcher = idPattern.matcher(block);
            if (idMatcher.find()) {
                id = Integer.parseInt(idMatcher.group(1));
                physicalID = idMatcher.group(2);
                // System.out.println("id: " + id);
                // System.out.println("physicalID: " + physicalID);
            }

            DisplayInfo info = new DisplayInfo(id);
            info.setPhysicalID(physicalID);

            // 4. 匹配 宽度 和 高度
            Matcher resMatcher = resolutionPattern.matcher(block);
            if (resMatcher.find()) {
                width = Integer.parseInt(resMatcher.group(1));
                height = Integer.parseInt(resMatcher.group(2));
                info.setWidth(width);
                info.setHeight(height);
            }

            // 5. 只有成功解析出必要信息时，才组装成 Bean 放入列表
            if (id != -1) {
                displayList.add(info);
            }
        }

        return displayList;
    }
}
