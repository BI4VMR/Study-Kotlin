package net.bi4vmr.tool.kotlin.external.adb.common.exceptions

/**
 * 远程调用异常。
 *
 * 通用异常类，表示所有类型的业务异常。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
class RPCException(message: String? = null) : Exception(message)
