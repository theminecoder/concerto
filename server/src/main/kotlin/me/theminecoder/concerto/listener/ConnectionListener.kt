package me.theminecoder.concerto.listener

import me.theminecoder.concerto.*
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent

object ConnectionListener : Listener {
    @EventHandler
    fun onAsyncPrePlayerLogin(event: AsyncPlayerPreLoginEvent) {
        ParticipantManager.load(event.uniqueId)
    }

    @EventHandler
    fun onPrePlayerLogin(event: PlayerLoginEvent) {
        if (event.player.hasPermission("concerto.fullbypass")) event.allow()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (event.player.hasPermission("concerto.mirror")) MirrorTask(event.player).start()
        event.player.gameMode = ParticipantManager[event.player.uniqueId].gameMode.toBukkit()
        event.player.inventory.clear()
        event.player.inventory.setItem(
            0,
            item(
                Material.COMPASS,
                text("Server Switcher ", NamedTextColor.GREEN) +
                    text("(Right Click)", NamedTextColor.GRAY)))
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        ParticipantManager.unload(event.player.uniqueId)
    }
}
