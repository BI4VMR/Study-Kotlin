package net.bi4vmr.tool.kotlin.external.adb.cast.compose

import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastManager
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.model.LocalDevice

/**
 * 本地投屏状态管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal class LocalCastStateManager : CastStateManager() {

    fun start(
        device: LocalDevice,
        displayID: String = "0",
        videoCodec: VideoCodec = VideoCodec.H264,
        bitRate: Int = 8_000_000,
        maxSize: Int = -1,
        maxFPS: Int = 360,
        enableAudio: Boolean = false,
        audioCodec: AudioCodec = AudioCodec.AAC,
        audioBitRate: Int = 128_000
    ) {
        val currentState = stateFlow.value
        if (currentState != ADBCastState.IDLE && currentState != ADBCastState.ERROR) {
            return
        }

        ADBCastManager.start(
            device,
            listener,
            displayID,
            videoCodec,
            bitRate,
            maxSize,
            maxFPS,
            enableAudio,
            audioCodec,
            audioBitRate
        )
    }

    fun stop() {
        if (stateFlow.value != ADBCastState.CASTING) {
            return
        }

        ADBCastManager.stop(listener)
    }
}
