package net.bi4vmr.tool.java.external.adb;

import com.android.ddmlib.IDevice;
import net.bi4vmr.tool.java.common.base.CLIUtil;
import net.bi4vmr.tool.java.common.base.io.BaseIOUtil;
import net.bi4vmr.tool.java.common.base.io.FileIOUtil;
import net.bi4vmr.tool.java.common.base.io.IOUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * ADB设备工具。
 * <p>
 * 获取 {@link ADBDevice} 的设备信息、执行命令等， {@link ADBDevice} 实例也引用了本类的静态方法，便于针对特定设备进行操作。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class ADBDeviceUtil {

    // 执行ADB Shell命令。
    public static Process run(ADBDevice device, String command, boolean redirectError) throws IOException {
        String cmd = buildCommand(device, command);
        if (cmd == null) {
            throw new IllegalArgumentException("Empty command!");
        }
        return CLIUtil.run(cmd, redirectError);
    }

    // 执行ADB Shell命令，并获取状态码。
    public static int runForStatus(ADBDevice device, String command) {
        String cmd = buildCommand(device, command);
        if (cmd == null) {
            System.err.println("ADBController: Empty command, ignore!");
            return -1;
        }

        return CLIUtil.runForStatus(cmd);
    }

    public static List<String> runForLinesUnsafe(ADBDevice device, String command) throws IOException, InterruptedException {
        String cmd = buildCommand(device, command);
        if (cmd == null) {
            throw new IllegalArgumentException("Empty command!");
        }

        return CLIUtil.runForLinesUnsafe(cmd);
    }

    public static List<String> runForLines(ADBDevice device, String command) {
        String cmd = buildCommand(device, command);
        if (cmd == null) {
            System.err.println("ADBController: Empty command, ignore!");
        }

        return CLIUtil.runForLines(cmd);
    }

    // 如果输入包括前缀，则将其去除，最终加上当前设备的序号。
    private static String buildCommand(ADBDevice device, String command) {
        // 如果命令以adb shell或adb exec-out开头，则去掉该前缀
        if (command.startsWith("adb shell ")) {
            command = command.substring("adb shell ".length());
        } else if (command.startsWith("adb exec-out ")) {
            command = command.substring("adb exec-out ".length());
        }

        // 跳过空命令，防止进入交互式环境导致调用线程被无限阻塞。
        if (command.isBlank()) {
            return null;
        }

        return String.format("adb -s %s exec-out %s", device.getSN(), command);
    }

    public static boolean isADBCommand(String command) {
        return command.startsWith("adb ");
    }


    /*
     * ----- 设备信息 -----
     */

    public static String getProperty(ADBDevice device, String key) {
        List<String> lines = runForLines(device, "getprop " + key);
        if (lines.isEmpty()) {
            return "";
        }
        return lines.get(0).trim();
    }

    public static String getManufacturer(ADBDevice device) {
        return getProperty(device, "ro.product.manufacturer");
    }

    public static String getBrand(ADBDevice device) {
        return getProperty(device, "ro.product.brand");
    }

    public static String getModel(ADBDevice device) {
        return getProperty(device, "ro.product.model");
    }

    public static String getOSVersion(ADBDevice device) {
        return getProperty(device, "ro.build.version.release");
    }

    public static String getAPILevel(ADBDevice device) {
        return getProperty(device, "ro.build.version.sdk");
    }

    public static String getDisplayName(ADBDevice device) {
        if (device.getState() != IDevice.DeviceState.ONLINE) {
            return "设备未就绪";
        }

        if (device.getRawDevice().isEmulator()) {
            String avdName = device.getRawDevice().getAvdName();
            if (avdName == null) {
                avdName = "AVD模拟器";
            }
            return avdName;
        }

        return getBrand(device) + " " + getModel(device);
    }

    public static List<DisplayInfo> getDisplays(ADBDevice device) {
        // 首先尝试使用 cmd display get-displays 命令（适用于新版本设备）
        List<String> lines = runForLines(device, "cmd display get-displays");
        List<DisplayInfo> displays = ADBOutputParser.parseDisplayInfo(lines);

        // 如果解析失败(老版本设备不支持该命令),则使用 dumpsys display displays | grep DisplayDeviceInfo 作为备选方案
        if (displays.isEmpty()) {
            System.err.println("ADBDeviceUtil: cmd display get-displays failed, trying dumpsys display...");
            lines = runForLines(device, "dumpsys display displays | grep DisplayDeviceInfo");
            displays = ADBOutputParser.parseDisplayInfoFromDumpsys(lines);
        }

        return displays;
    }

    public static byte[] screenshotAsPNG(ADBDevice device) {
        try {
            String cmd = String.format("adb -s %s exec-out screencap -p", device.getSN());
            String[] cmdArray = CLIUtil.parseCommand(cmd);
            Process process = new ProcessBuilder(cmdArray)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return BaseIOUtil.readAsBytes(process.getInputStream());
        } catch (Exception e) {
            System.err.println("ADBController: Screenshot failed! Reason:[" + e.getMessage() + "]");
            return new byte[0];
        }
    }

    public static boolean screenshotAsPNG(ADBDevice device, File dest) {
        try {
            String cmd = String.format("adb -s %s exec-out screencap -p", device.getSN());
            String[] cmdArray = CLIUtil.parseCommand(cmd);
            Process process = new ProcessBuilder(cmdArray)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            FileIOUtil.saveToFile(process.getInputStream(), dest, IOUtil.BUFFER_SIZE_8MB);
            int state = process.waitFor();
            return CLIUtil.isSuccess(state);
        } catch (Exception e) {
            System.err.println("ADBController: Screenshot failed! Reason:[" + e.getMessage() + "]");
            return false;
        }
    }


    /*
     * ----- 文件系统 -----
     */

    public static boolean pushFile(ADBDevice device, File src, String remote) {
        String cmd = String.format("adb -s %s push \"%s\" \"%s\"", device.getSN(), src.getAbsolutePath(), remote);
        int status = CLIUtil.runForStatus(cmd);
        return CLIUtil.isSuccess(status);
    }

    public static void syncFileSystem(ADBDevice device) {
        runForStatus(device, "sync");
    }

    public static boolean remount(ADBDevice device) {
        int code = runForStatus(device, "remount");
        return CLIUtil.isSuccess(code);
    }


    /*
     * ----- 网络通信 -----
     */

    public static void startRemoteServer(ADBDevice device) {
        String cmd = String.format("adb -s %s tcpip 5555", device.getSN());
        CLIUtil.runForStatus(cmd);
    }

    public static boolean startForward(ADBDevice device, int pcPort, int devicePort) {
        String cmd = String.format("adb -s %s forward tcp:%d tcp:%d", device.getSN(), pcPort, devicePort);
        int status = CLIUtil.runForStatus(cmd);
        return CLIUtil.isSuccess(status);
    }

    public static void stopForward(ADBDevice device, int pcPort) {
        String cmd = String.format("adb -s %s forward --remove tcp:%d", device.getSN(), pcPort);
        CLIUtil.runForStatus(cmd);
    }

    public static boolean startForward(ADBDevice device, String pcEndPoint, String deviceEndPoint) {
        String cmd = String.format("adb -s %s forward %s %s", device.getSN(), pcEndPoint, deviceEndPoint);
        int status = CLIUtil.runForStatus(cmd);
        return CLIUtil.isSuccess(status);
    }

    public static void stopForward(ADBDevice device, String pcEndPoint) {
        String cmd = String.format("adb -s %s forward --remove %s", device.getSN(), pcEndPoint);
        CLIUtil.runForStatus(cmd);
    }

    public static void cleanForward(ADBDevice device) {
        String cmd = String.format("adb -s %s forward --remove-all", device.getSN());
        CLIUtil.runForStatus(cmd);
    }


    /*
     * ----- 电源控制 -----
     */

    public static void reboot(ADBDevice device) {
        runForStatus(device, "reboot");
    }
}
