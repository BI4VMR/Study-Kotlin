package net.bi4vmr.tool.kotlin.external.scrcpy.ui

import net.bi4vmr.tool.java.external.adb.ADBDevice
import net.bi4vmr.tool.java.external.adb.DisplayInfo
import net.bi4vmr.tool.kotlin.external.scrcpy.ScreenCastManager
import net.bi4vmr.tool.kotlin.external.scrcpy.VideoCodec
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.SwingUtilities

/**
 * 屏幕投射独立窗口。
 *
 * 封装 [ScreenCastPanel]，作为独立的 Swing 窗口使用。
 * 仅展示视频画面，不包含任何远程控制通道。
 *
 * 典型用法：
 * ```kotlin
 * ScreenCastManager.setServerFilePath("/path/to/scrcpy-server")
 * ScreenCastManager.setServerVersion("2.7")
 *
 * val window = ScreenCastWindow("My Device")
 * window.connect(device)
 * ```
 *
 * @param title 窗口标题。
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class ScreenCastWindow @JvmOverloads constructor(
    title: String = "Screen Cast"
) : JFrame(title) {

    /** 屏幕投射显示控件，供外部访问（如需嵌入其他容器时复用）。 */
    val castPanel = ScreenCastPanel()

    init {
        layout = BorderLayout()
        add(castPanel, BorderLayout.CENTER)

        // 设置合理的初始大小；首帧到达后 ScreenCastPanel 会自动调整父窗口尺寸
        preferredSize = Dimension(540, 960)
        defaultCloseOperation = DISPOSE_ON_CLOSE

        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent) {
                disconnect()
            }
        })

        pack()
    }

    /**
     * 连接设备并启动屏幕投射，同时显示窗口。
     *
     * @param device      目标 ADB 设备。
     * @param display     投屏的屏幕；`null` 表示使用默认屏幕。
     * @param videoCodec  视频编码类型，默认 H.264。
     * @param bitRate     视频码率（bps），默认 8 Mbps。
     * @param maxFPS      最大帧率，默认 60 fps。
     */
    @JvmOverloads
    fun connect(
        device: ADBDevice,
        display: DisplayInfo? = null,
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxFPS: Int = 60
    ) {
        SwingUtilities.invokeLater { isVisible = true }

        ScreenCastManager.start(
            device = device,
            listener = castPanel.castListener,
            display = display,
            videoCodec = videoCodec,
            bitRate = bitRate,
            maxFPS = maxFPS,
            remoteControl = false
        )
    }

    /**
     * 停止屏幕投射并释放资源。
     *
     * 关闭窗口时会自动调用本方法。
     */
    fun disconnect() {
        ScreenCastManager.stop(castPanel.castListener)
    }
}
