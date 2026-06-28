package net.bi4vmr.tool.java.external.adb;

import com.android.ddmlib.IDevice;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * ADB设备。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class ADBDevice {

    /**
     * DDMLib的Device实例。
     */
    private IDevice rawDevice;


    public ADBDevice(IDevice rawDevice) {
        this.rawDevice = rawDevice;
    }


    public String getSN() {
        return rawDevice.getSerialNumber();
    }

    /**
     * 获取设备状态。
     *
     * @return 状态枚举常量。
     */
    public IDevice.DeviceState getState() {
        return rawDevice.getState();
    }

    /**
     * 是否为 {@link IDevice.DeviceState#ONLINE } 状态。
     *
     * @return 状态标志。
     */
    public boolean isOnline() {
        return rawDevice.isOnline();
    }

    public Process run(String command, boolean redirectError) throws IOException {
        return ADBDeviceUtil.run(this, command, redirectError);
    }

    public int runForStatus(String command) {
        return ADBDeviceUtil.runForStatus(this, command);
    }

    public List<String> runForLines(String command) {
        return ADBDeviceUtil.runForLines(this, command);
    }

    public String getProperty(String key) {
        return ADBDeviceUtil.getProperty(this, key);
    }

    public String getManufacturer() {
        return ADBDeviceUtil.getManufacturer(this);
    }

    public String getBrand() {
        return ADBDeviceUtil.getBrand(this);
    }

    public String getModel() {
        return ADBDeviceUtil.getModel(this);
    }

    public String getOSVersion() {
        return ADBDeviceUtil.getOSVersion(this);
    }

    public String getAPILevel() {
        return ADBDeviceUtil.getAPILevel(this);
    }

    public String getDisplayName() {
        return ADBDeviceUtil.getDisplayName(this);
    }

    public List<DisplayInfo> getDisplays() {
        return ADBDeviceUtil.getDisplays(this);
    }

    public byte[] screenshotAsPNG() {
        return ADBDeviceUtil.screenshotAsPNG(this);
    }

    public boolean screenshotAsPNG(File dest) {
        return ADBDeviceUtil.screenshotAsPNG(this, dest);
    }


    /*
     * ----- 文件系统 -----
     */

    public boolean pushFile(File src, String remote) {
        return ADBDeviceUtil.pushFile(this, src, remote);
    }

    public void syncFileSystem() {
        ADBDeviceUtil.syncFileSystem(this);
    }

    public boolean remount() {
        return ADBDeviceUtil.remount(this);
    }

    /*
     * ----- 网络通信 -----
     */

    public void startRemoteServer() {
        ADBDeviceUtil.startRemoteServer(this);
    }

    public boolean startForward(int localPort, int remotePort) {
        return ADBDeviceUtil.startForward(this, localPort, remotePort);
    }

    public void stopForward(int localPort) {
        ADBDeviceUtil.stopForward(this, localPort);
    }

    public boolean startForward(String pcEndPoint, String deviceEndPoint) {
        return ADBDeviceUtil.startForward(this, pcEndPoint, deviceEndPoint);
    }

    public void stopForward(String pcEndPoint) {
        ADBDeviceUtil.stopForward(this, pcEndPoint);
    }

    public void cleanForward() {
        ADBDeviceUtil.cleanForward(this);
    }


    /*
     * ----- 电源控制 -----
     */

    public void reboot() {
        ADBDeviceUtil.reboot(this);
    }


    /*
     * ----- 内部方法 -----
     */

    public IDevice getRawDevice() {
        return rawDevice;
    }

    // 内部方法：设备状态变更时进行刷新
    protected void updateRawDevice(IDevice rawDevice) {
        this.rawDevice = rawDevice;
    }

    @Override
    public String toString() {
        return "ADBDevice{" +
                "SN=" + getSN() + ", " +
                "State=" + getState() + ", " +
                "RawDevice=" + rawDevice.hashCode() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ADBDevice other = (ADBDevice) o;

        return Objects.equals(rawDevice, other.rawDevice) &&
                Objects.equals(getState(), other.getState());
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawDevice, getState());
    }
}
