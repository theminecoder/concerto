package me.theminecoder.concerto

import me.theminecoder.concerto.listener.ConnectionListener
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class Concerto : JavaPlugin() {

    companion object {
        val PLUGIN by lazy { getPlugin(Concerto::class.java) }
        val SERVER_NAME: String by lazy { System.getenv("SERVER_NAME") }
        val SERVER_MODE: String by lazy { System.getenv("SERVER_MODE") }
        val SERVER_IP: String by lazy { System.getenv("SERVER_IP") }
        val SERVER_CONFIG = ConcertConfig.current.modes.find { it.id == SERVER_MODE }!!
    }

    override fun onEnable() {
        MirrorManager
        server.pluginManager.registerEvents(ConnectionListener, this)
        server.scheduler.runTaskTimerAsynchronously(
            this,
            { _ ->
                ServerStatus(
                        name = SERVER_NAME,
                        ip = SERVER_IP,
                        port = server.port,
                        players = server.onlinePlayers.map { it.uniqueId },
                        maxPlayers = server.maxPlayers,
                        tps = server.tps[0],
                        terminating = TerminationHandler.isTerminating,
                        mode = SERVER_MODE)
                    .send()
            },
            0,
            20)
    }
}
