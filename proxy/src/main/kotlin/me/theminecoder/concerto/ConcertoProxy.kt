package me.theminecoder.concerto

import com.google.inject.Inject
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import java.net.InetSocketAddress
import java.time.Instant
import me.theminecoder.concerto.packets.PlayerKickPacket
import me.theminecoder.concerto.packets.PlayerTransferModePacket
import me.theminecoder.concerto.packets.PlayerTransferServerPacket
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.litote.kmongo.getCollection
import org.litote.kmongo.save

@Suppress("unused")
class ConcertoProxy(@Inject val server: ProxyServer) {
    @Subscribe
    fun onBoot(event: ProxyInitializeEvent) {
        server.scheduler.buildTask(this) {
            ServerCache.ALL.filter { server.getServer(it.name).isEmpty }.forEach {
                println("Registering server ${it.name} (${it.ip}:${it.port})")
                server.registerServer(ServerInfo(it.name, InetSocketAddress(it.ip, it.port)))
            }
            server.allServers.filter { ServerCache.contains(it.serverInfo.name).not() }.forEach {
                println(
                    "Unregistering server ${it.serverInfo.name} (${it.serverInfo.address.hostString}:${it.serverInfo.address.port})")
                server.unregisterServer(it.serverInfo)
            }
        }
        registerNetworkListener<PlayerTransferServerPacket> { packet ->
            server.getPlayer(packet.player).ifPresent {
                it.joinServer(server.getServer(packet.server).get())
            }
        }
        registerNetworkListener<PlayerTransferModePacket> { packet ->
            server.getPlayer(packet.player).ifPresent {
                it.joinServer(server.getServer(getBestServerForMode(packet.mode).name).get())
            }
        }
        registerNetworkListener<PlayerKickPacket> { packet ->
            server.getPlayer(packet.player).ifPresent { it.disconnect(packet.reason) }
        }
    }

    @Subscribe
    fun onPing(event: ProxyPingEvent) {
        event.ping.asBuilder().also {
            it.onlinePlayers(ServerCache.ALL.sumOf { it.players.size })
            it.maximumPlayers(ServerCache.ALL.sumOf { it.maxPlayers })
            it.description(
                LegacyComponentSerializer.legacyAmpersand()
                    .deserializeOr(
                        ConcertConfig.current.motd, it.descriptionComponent.get() as TextComponent))
        }
    }

    @Subscribe
    fun onLogin(event: LoginEvent) {
        var participant = ParticipantManager.load(event.player.uniqueId)
        if (participant == null) {
            participant = Participant(event.player.uniqueId, event.player.username)
            mongoClient.getCollection<Participant>().save(participant)
        }
        val punishment =
            participant.punishments.filter { it.type == PunishmentType.BAN }.firstOrNull {
                it.end == null || it.end!!.isAfter(Instant.now())
            }

        if (punishment != null) {
            event.result = ResultedEvent.ComponentResult.denied(punishment.toComponent())
            ParticipantManager.unload(event.player.uniqueId)
            return
        }
    }

    @Subscribe
    fun onFirstServer(event: PlayerChooseInitialServerEvent) {
        event.setInitialServer(server.getServer(getBestServerForMode("main").name).get())
    }

    private fun getBestServerForMode(mode: String): ServerStatus {
        return ServerCache.getByMode(mode)
            .filterNot { it.terminating }
            .filter { it.players.size < (it.maxPlayers - SERVER_PLAYER_BUFFER) }
            .sortedWith(
                compareByDescending<ServerStatus> { it.players.size }.thenBy { it.name.hashCode() })
            .firstOrNull()
            ?: ServerCache.getByMode(mode).filterNot { it.terminating }.random()
    }
}

fun Player.joinServer(server: RegisteredServer) {
    if (this.currentServer != server) {
        this.createConnectionRequest(server).fireAndForget()
    }
}
