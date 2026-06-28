package net.bi4vmr.tool.java.external.adb;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import net.bi4vmr.tool.java.common.base.CLIUtil;
import net.bi4vmr.tool.java.common.base.system.SystemUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ADB工具类。
 * 优先使用用户设置的ADB路径；若未设置则尝试从PATH中查找。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class ADBController {

    /**
     * DDM库回调监听器实例。
     * <p>
     * 工具内部使用，将原始事件处理后转发到 {@link ADBEventListener} 以供调用者监听。
     */
    private static final DDMLibListener ddmLibListener = new DDMLibListener();

    /**
     * ADB设备列表缓存。
     */
    private static final List<ADBDevice> deviceList = new ArrayList<>();

    /**
     * ADB服务监听器列表。
     */
    private static final List<ADBServiceListener> serviceListeners = new ArrayList<>();

    /**
     * ADB事件监听器列表。
     */
    private static final List<ADBEventListener> eventListeners = new ArrayList<>();

    /**
     * 初始化状态。
     */
    private static volatile boolean initialized;

    private ADBController() {
        // 静态工具，不需要实例化。
    }


    /*
     * ----- ADB服务状态管理 -----
     */

    /**
     * 初始化。
     * <p>
     * 调用其他方法前，应当首先调用此方法。
     */
    @SuppressWarnings("BusyWait")
    public static void init() {
        synchronized (deviceList) {
            if (initialized) {
                System.err.println("ADBController: Do NOT call init method repeatedly!");
                return;
            }

            File adbFile = findADBInPath();
            if (adbFile == null) {
                System.err.println("ADB executable not found in [PATH]!");
                return;
            }
            if (!adbFile.canExecute()) {
                System.err.println("ADB executable is not executable! File:[" + adbFile.getAbsolutePath() + "]");
                return;
            }

            // 初始化DDM库，参数表示是否支持连接到应用的JVM进行Debug，目前不使用相关功能。
            AndroidDebugBridge.init(false);
            // 启动ADB的PC端进程
            AndroidDebugBridge.createBridge(adbFile.getAbsolutePath(), false);

            // 等待ADB进程启动完毕
            long waitMillis = 5L;
            while (!isBridgeReady()) {
                try {
                    Thread.sleep(waitMillis);
                    waitMillis += 5L;
                } catch (InterruptedException e) {
                    // 中断处理：放弃本次初始化操作。
                    Thread.currentThread().interrupt();
                    AndroidDebugBridge.disconnectBridge();
                    AndroidDebugBridge.terminate();
                    return;
                }
            }

            initialized = true;
            notifyServiceReady();

            startListenDeviceChange();
            getDevicesInner();
            notifyInitDevices(Collections.unmodifiableList(deviceList));
        }
    }

    /**
     * 判断ADB进程是否就绪。
     *
     * @return {@code true} 表示就绪，可以执行其他命令； {@code false} 表示未就绪。
     */
    private static boolean isBridgeReady() {
        AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
        return bridge != null &&
                bridge.isConnected() &&
                bridge.hasInitialDeviceList();
    }

    /**
     * 获取设备列表。
     */
    private static void getDevicesInner() {
        deviceList.clear();

        IDevice[] rawList = AndroidDebugBridge.getBridge().getDevices();
        for (IDevice rawDevice : rawList) {
            deviceList.add(new ADBDevice(rawDevice));
        }
    }

    /**
     * 以Root模式重启ADB服务。
     *
     * @return {@code true} 表示断开成功；{@code false} 表示断开失败。
     */
    public static boolean restartAsRoot() {
        int code = CLIUtil.runForStatus("adb root");
        return CLIUtil.isSuccess(code);
    }

    /**
     * 终止当前进程与ADB进程的连接。
     */
    public static void terminate() {
        synchronized (deviceList) {
            if (!initialized) {
                System.err.println("ADBController: Not init done, ignore!");
                return;
            }

            stopListenDeviceChange();

            deviceList.clear();
            notifyServiceTerminate();

            AndroidDebugBridge.disconnectBridge();
            AndroidDebugBridge.terminate();

            initialized = false;
        }
    }

    public static void addServiceListener(ADBServiceListener listener) {
        synchronized (serviceListeners) {
            if (listener != null && !serviceListeners.contains(listener)) {
                serviceListeners.add(listener);
            }
        }
    }

    public static void removeServiceListener(ADBServiceListener listener) {
        synchronized (serviceListeners) {
            serviceListeners.remove(listener);
        }
    }

    private static void notifyServiceReady() {
        List<ADBServiceListener> listeners;
        synchronized (serviceListeners) {
            listeners = new ArrayList<>(serviceListeners);
        }

        for (ADBServiceListener listener : listeners) {
            listener.onInitDone();
        }
    }

    private static void notifyServiceTerminate() {
        List<ADBServiceListener> listeners;
        synchronized (serviceListeners) {
            listeners = new ArrayList<>(serviceListeners);
        }

        for (ADBServiceListener listener : listeners) {
            listener.onTerminate();
        }
    }


    /*
     * ----- 设备管理 -----
     */

    /**
     * 获取设备列表。
     * <p>
     * 返回本类缓存的设备列表。
     *
     * @return 设备列表。
     */
    public static List<ADBDevice> getDevices() {
        return Collections.unmodifiableList(deviceList);
    }

    /**
     * 刷新设备列表。
     * <p>
     * 调用DDMLib的接口获取最新设备列表，并同步修改缓存列表、通过回调方法通知设备变更。
     * <p>
     * DDMLib正常工作时会通告设备状态变更事件并更新缓存，仅当少数情况下设备状态未同步才需要调用此方法。
     */
    public static void refreshDevices() {
        synchronized (deviceList) {
            if (!initialized) {
                System.err.println("ADBController: Not init done, ignore!");
                return;
            }

            // 寻找缓存列表与当前列表中不一致的设备，并通过事件接口通知监听者。
            List<ADBDevice> newList = new ArrayList<>();
            IDevice[] rawList = AndroidDebugBridge.getBridge().getDevices();
            for (IDevice rawDevice : rawList) {
                ADBDevice cached = deviceList.stream()
                        .filter(it -> it.getRawDevice() == rawDevice)
                        .findFirst()
                        .orElse(null);
                if (cached == null) {
                    /* 缓存列表不存在该项，视为新连接的设备 */
                    ADBDevice newDevice = new ADBDevice(rawDevice);
                    notifyDeviceConnect(newDevice);
                    newList.add(newDevice);
                } else {
                    /* 缓存列表存在该项 */
                    // 比较缓存项的状态是否与实际状态一致
                    if (cached.getState() != rawDevice.getState()) {
                        cached.updateRawDevice(rawDevice);
                        notifyDeviceStateChange(cached);
                    }
                    newList.add(cached);
                }
            }

            // 寻找缓存列表中存在但新列表中不存在的项，视为断开的设备，并通过事件接口通知监听者。
            List<ADBDevice> absentItems = new ArrayList<>(deviceList);
            absentItems.removeAll(newList);
            absentItems.forEach(ADBController::notifyDeviceDisconnect);

            deviceList.clear();
            deviceList.addAll(newList);
        }
    }

    /**
     * 连接远程设备。
     *
     * @param ip 设备IP地址。
     * @return {@code true} 表示连接成功；{@code false} 表示连接失败。
     */
    public static boolean connectRemoteDevice(String ip) {
        // DDM库只能发送ADB Shell命令，此处直接调用CLI连接设备，因此不需要判断DDM库的状态。
        int code = CLIUtil.runForStatus("adb connect " + ip + ":5555");
        return CLIUtil.isSuccess(code);
    }

    /**
     * 断开远程设备。
     *
     * @param ip 设备IP地址。
     * @return {@code true} 表示断开成功；{@code false} 表示断开失败。
     */
    public static boolean disconnectRemoteDevice(String ip) {
        int code = CLIUtil.runForStatus("adb disconnect " + ip + ":5555");
        return CLIUtil.isSuccess(code);
    }

    /**
     * 注册ADB事件监听器。
     * <p>
     * 该监听器不依赖初始化状态，可以在调用 {@link #init()} 方法前进行注册，以便接收初始设备列表。
     *
     * @param listener 监听器实现。
     */
    public static void addEventListener(ADBEventListener listener) {
        synchronized (eventListeners) {
            if (listener != null && !eventListeners.contains(listener)) {
                eventListeners.add(listener);
            }
        }
    }

    public static void removeEventListener(ADBEventListener listener) {
        synchronized (eventListeners) {
            eventListeners.remove(listener);
        }
    }

    // 通知服务就绪时的初始设备列表
    private static void notifyInitDevices(List<ADBDevice> devices) {
        List<ADBEventListener> listeners;
        synchronized (eventListeners) {
            listeners = new ArrayList<>(eventListeners);
        }

        for (ADBEventListener listener : listeners) {
            listener.onInitDevices(devices);
        }
    }

    // 通知设备连接事件
    private static void notifyDeviceConnect(ADBDevice device) {
        List<ADBEventListener> listeners;
        synchronized (eventListeners) {
            listeners = new ArrayList<>(eventListeners);
        }

        for (ADBEventListener listener : listeners) {
            listener.onDeviceConnect(device);
        }
    }

    // 通知设备断开事件
    private static void notifyDeviceDisconnect(ADBDevice device) {
        List<ADBEventListener> listeners;
        synchronized (eventListeners) {
            listeners = new ArrayList<>(eventListeners);
        }

        for (ADBEventListener listener : listeners) {
            listener.onDeviceDisconnect(device);
        }
    }

    // 通知设备状态变更事件
    private static void notifyDeviceStateChange(ADBDevice device) {
        List<ADBEventListener> listeners;
        synchronized (eventListeners) {
            listeners = new ArrayList<>(eventListeners);
        }

        for (ADBEventListener listener : listeners) {
            listener.onDeviceStateChange(device, device.getState());
        }
    }


    /*
     * ----- 内部监听器 -----
     */

    // 开始监听DDM库的设备事件
    private static void startListenDeviceChange() {
        AndroidDebugBridge.addDeviceChangeListener(ddmLibListener);
    }

    // 停止监听DDM库的设备事件
    private static void stopListenDeviceChange() {
        AndroidDebugBridge.removeDeviceChangeListener(ddmLibListener);
    }

    /**
     * DDM库的设备事件监听器实现类。
     * <p>
     * 监听DDM库的设备连接、断开和状态变化事件，并通过 {@link ADBEventListener} 接口通知外部监听者。
     */
    private static class DDMLibListener implements AndroidDebugBridge.IDeviceChangeListener {

        @Override
        public void deviceConnected(IDevice device) {
            System.out.println("ADBController: DDMLib notify device [" + device + "] connected.");
            ADBDevice newDevice = new ADBDevice(device);
            synchronized (deviceList) {
                deviceList.add(newDevice);
            }
            notifyDeviceConnect(newDevice);
        }

        @Override
        public void deviceDisconnected(IDevice device) {
            System.out.println("ADBController: DDMLib notify device [" + device + "] disconnected.");
            synchronized (deviceList) {
                // 寻找IDevice属性与当前值对应的ADBDevice
                ADBDevice current = deviceList.stream()
                        .filter(it -> it.getRawDevice() == device)
                        .findFirst()
                        .orElse(null);
                if (current != null) {
                    deviceList.remove(current);
                    notifyDeviceDisconnect(current);
                } else {
                    System.err.println("ADBController: DDMLib notify device [" + device + "] disconnected, but not found in cache!");
                }
            }
        }

        @Override
        public void deviceChanged(IDevice device, int changeMask) {
            // 设备属性接口兼容性不佳，部分设备无法通过 `getProperty()` 接口获取属性，因此只使用该接口通告设备状态，忽略其他Mask标志位。
            if (changeMask != IDevice.CHANGE_STATE) {
                return;
            }

            // 有时设备状态会变为空值，此类事件没有任何含义，因此忽略这些事件。
            if (device.getState() == null) {
                return;
            }

            System.out.println("ADBController: DDMLib notify device [" + device + "] state changed.");

            synchronized (deviceList) {
                // 寻找IDevice属性与当前值对应的ADBDevice
                ADBDevice current = deviceList.stream()
                        .filter(it -> it.getRawDevice() == device)
                        .findFirst()
                        .orElse(null);
                if (current != null) {
                    current.updateRawDevice(device);
                    notifyDeviceStateChange(current);
                } else {
                    System.err.println("ADBController: DDMLib notify device [" + device + "] changed, but not found in cache!");
                }
            }
        }
    }


    /*
     * ----- 工具方法 -----
     */

    /**
     * 获取当前平台ADB可执行文件名称。
     *
     * @return 文件名称。
     */
    public static String getADBFileName() {
        return SystemUtil.isWindows() ? "adb.exe" : "adb";
    }

    /**
     * 在环境变量 {@code PATH} 中寻找ADB可执行文件。
     *
     * @return ADB可执行文件。如果未找到则为空值。
     */
    public static File findADBInPath() {
        String adbName = getADBFileName();
        File adbFile = null;
        String[] dirs = SystemUtil.getPathDirectories();
        // 遍历PATH的目录，寻找ADB可执行文件路径。
        for (String dir : dirs) {
            File temp = new File(dir, adbName);
            if (temp.exists()) {
                adbFile = temp;
                break;
            }
        }

        return adbFile;
    }
}
