package net.bi4vmr.tool.kotlin.external.adb.proxy.common.enums

import  net.bi4vmr.tool.kotlin.external.adb.proxy.common.model.PairedPeer

/**
 * 配对模块：对等体类型。
 *
 * 服务端需要记住已批准的客户端，并允许对业务接口的访问。客户端需要记住服务端身份，当服务端身份信息变更时警告用户。
 *
 * 两端的数据结构是相同的，可以复用 [PairedPeer] 表示，并通过本枚举区分类型。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
enum class PeerType {

    /**
     * 服务端记录。
     *
     * 应由客户端存储。
     */
    SERVER,

    /**
     * 客户端记录。
     *
     * 应由服务端存储。
     */
    CLIENT;
}
