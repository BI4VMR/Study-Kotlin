package net.bi4vmr.tool.kotlin.external.adb.proxy.common

import net.bi4vmr.tool.kotlin.external.adb.proxy.common.Constant.PARAM_SN

/**
 * URL 定义。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object API {

    /**
     * 请求远程设备开启投屏转发。
     *
     * 请求格式为 HTTP POST ，查询参数 [PARAM_SN] 表示目标设备序列号；返回服务端投屏监听端口。
     * 该路由由可选的投屏适配模块提供。
     */
    const val ADB_CAST: String = "/adb/cast"
}
