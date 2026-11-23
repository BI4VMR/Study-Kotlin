package net.bi4vmr.tool.kotlin.external.adb.proxy.common.callbacks

import net.bi4vmr.tool.kotlin.external.adb.common.callback.ADBFileTransferListener

/**
 * ADB 远程文件传输监听器。
 *
 * 该监听器事件通知线程与方法调用线程一致，若需要在 UI 中展示进度，需注意线程切换。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
abstract class RemoteFileTransferListener : ADBFileTransferListener() {

}
