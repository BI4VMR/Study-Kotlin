package net.bi4vmr.tool.kotlin.external.adb.cast.compose

import net.bi4vmr.tool.kotlin.external.adb.cast.ADBCastReceiver
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.AudioCodec
import net.bi4vmr.tool.kotlin.external.adb.cast.enums.VideoCodec
import net.bi4vmr.tool.kotlin.external.adb.proxy.client.ADBProxyClient

/**
 * 远程投屏状态管理。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
internal class RemoteCastStateManager : CastStateManager() {

    suspend fun start(
        client: ADBProxyClient,
        sn: String,
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

        val transferPort = client.startCast(
            sn,
            displayID,
            videoCodec,
            bitRate,
            maxSize,
            maxFPS,
            enableAudio,
            audioCodec,
            audioBitRate
        )

        ADBCastReceiver.start(
            client.host,
            transferPort,
            listener,
            videoCodec,
            enableAudio,
            audioCodec
        )
    }

    fun stop() {
        if (stateFlow.value != ADBCastState.CASTING) {
            return
        }

        ADBCastReceiver.stop(listener)
    }
}
