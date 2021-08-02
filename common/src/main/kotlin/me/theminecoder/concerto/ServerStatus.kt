package me.theminecoder.concerto

import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import me.theminecoder.concerto.packets.ServerStatusPacket

@OptIn(ExperimentalTime::class)
object ServerCache {
    private val cache = mutableMapOf<String, ServerStatus>()

    init {
        fixedRateTimer("ServerCacheCleaner", period = 1.seconds.toLong(TimeUnit.MILLISECONDS)) {
            val expireTime = Instant.now().minusSeconds(5)
            cache.entries.removeIf { it.value.updatedAt.isBefore(expireTime) }
        }
        registerNetworkListener<ServerStatusPacket> { this[it.status.name] = it.status }
    }

    operator fun get(id: String) = cache[id]
    operator fun set(id: String, status: ServerStatus) = cache.put(id, status)

    fun contains(id: String) = cache.containsKey(id)

    val ALL: Collection<ServerStatus> = cache.values
    fun getByMode(mode: String) = cache.values.filter { it.mode == mode }
}

const val SERVER_PLAYER_BUFFER = 10

data class ServerStatus(
    val name: String,
    val ip: String,
    val port: Int,
    val mode: String,
    val players: List<UUID>,
    val maxPlayers: Int,
    val tps: Double,
    val terminating: Boolean,
    val updatedAt: Instant = Instant.now()
) {
    fun send() = sendNetworkEvent(ServerStatusPacket(this))
}
