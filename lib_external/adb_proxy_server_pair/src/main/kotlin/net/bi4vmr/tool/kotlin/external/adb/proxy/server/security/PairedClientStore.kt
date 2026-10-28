package net.bi4vmr.tool.kotlin.external.adb.proxy.server.security

import java.util.concurrent.CopyOnWriteArrayList

/**
 * 已配对客户端的存储接口，由上层应用实现持久化。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.2
 */
interface PairedClientStore {

    fun list(): List<PairedClient>

    /**
     * 保存配对信息，已存在相同指纹的记录时覆盖。
     */
    fun save(client: PairedClient)

    fun remove(fingerprint: String)
}

/**
 * 内存实现，重启后配对信息丢失。
 */
class InMemoryPairedClientStore : PairedClientStore {

    private val clients = CopyOnWriteArrayList<PairedClient>()

    override fun list(): List<PairedClient> = clients.toList()

    override fun save(client: PairedClient) {
        clients.removeAll { it.fingerprint == client.fingerprint }
        clients.add(client)
    }

    override fun remove(fingerprint: String) {
        clients.removeAll { it.fingerprint == fingerprint }
    }
}
