package net.bi4vmr.tool.java.external.adb;

import com.android.ddmlib.IDevice;

import java.util.List;

/**
 * ADB事件监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public interface ADBEventListener {

    default void onInitDevices(List<ADBDevice> devices) {
        // 默认不作响应
    }

    default void onDeviceConnect(ADBDevice device) {
        // 默认不作响应
    }

    default void onDeviceDisconnect(ADBDevice device) {
        // 默认不作响应
    }

    default void onDeviceStateChange(ADBDevice device, IDevice.DeviceState state) {
        // 默认不作响应
    }
}
