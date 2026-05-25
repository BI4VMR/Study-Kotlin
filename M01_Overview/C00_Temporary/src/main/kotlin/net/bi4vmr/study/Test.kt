package net.bi4vmr.study

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver

private const val ADB_PATH = "C:\\Software\\Android-SDK\\platform-tools\\adb.exe"

/**
 * 通过 ddmlib executeShellCommand 读取单个属性值。
 */
fun getDeviceProp(device: IDevice, key: String): String {
    val receiver = object : IShellOutputReceiver {
        val output = StringBuilder()
        override fun addOutput(data: ByteArray, offset: Int, length: Int) {
            output.append(String(data, offset, length))
            // println(output)
        }

        override fun flush() {
            println("exec done!")
            println(output)
        }

        override fun isCancelled(): Boolean = false
    }
    device.executeShellCommand("getprop $key", receiver)
    return ""
}

/**
 * TODO 添加描述。
 *
 * @author BI4VMR@outlook.com
 * @since 1.0.0
 */
fun main() {
    AndroidDebugBridge.init(false)
    val b = AndroidDebugBridge.createBridge(ADB_PATH, false)
    AndroidDebugBridge.addDeviceChangeListener(object : AndroidDebugBridge.IDeviceChangeListener {
        override fun deviceConnected(device: IDevice) {
            println("CONNECTED: ${device.serialNumber} state=${device.state}")
        }

        override fun deviceDisconnected(device: IDevice) {
            println("DISCONNECTED: ${device.serialNumber}")
        }

        override fun deviceChanged(device: IDevice, changeMask: Int) {
            println("CHANGED: ${device.serialNumber} state=${device.state} mask=$changeMask")
            if (device.state == IDevice.DeviceState.ONLINE) {
                Thread {
                    val model = getDeviceProp(device, "ro.product.model")
                    val manufacturer = getDeviceProp(device, "ro.product.manufacturer")
                    val sdk = getDeviceProp(device, "ro.build.version.sdk")
                    val androidVer = getDeviceProp(device, "ro.build.version.release")
                    val abi = getDeviceProp(device, "ro.product.cpu.abi")
                    println("--- 设备信息 [${device.serialNumber}] ---")
                    println("  型号: $manufacturer $model")
                    println("  Android: $androidVer (API $sdk)")
                    println("  CPU ABI: $abi")
                }.start()
            }
        }
    })

    // 保持进程存活
    while (true) Thread.sleep(1000)
}
