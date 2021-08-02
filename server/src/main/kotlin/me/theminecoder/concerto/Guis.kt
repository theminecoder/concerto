package me.theminecoder.concerto

import me.theminecoder.concerto.packets.PlayerTransferModePacket
import me.theminecoder.concerto.packets.PlayerTransferServerPacket
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class ModeSelector : GUI(text("Where to?"), 36) {

    init {
        setUpdateTicks(5)
    }

    override fun onPlayerClick(event: InventoryClickEvent) {
        val mode = ConcertConfig.current.modes.find { it.switcherSlot == event.rawSlot } ?: return
        when {
            event.isLeftClick ->
                AsyncDatabase.submit {
                    sendNetworkEvent(PlayerTransferModePacket(event.whoClicked.uniqueId, mode.id))
                }
            event.isRightClick -> ModeInstanceSelector(mode).open(event.whoClicked as Player)
        }
    }

    override fun populate() {
        ConcertConfig.current.modes.forEach {
            val servers = ServerCache.getByMode(it.id)
            this.inventory.setItem(
                it.switcherSlot,
                item(
                    Material.valueOf(it.icon),
                    text(it.name, NamedTextColor.WHITE),
                    text("${servers.sumOf { it.players.size }} Players", NamedTextColor.GRAY),
                    Component.empty(),
                    text("Click", NamedTextColor.YELLOW, TextDecoration.BOLD) +
                        text(" to join", NamedTextColor.WHITE),
                    text("Right Click", NamedTextColor.YELLOW, TextDecoration.BOLD) +
                        text(" to select server", NamedTextColor.WHITE)))
        }
    }
}

class ModeInstanceSelector(private val mode: ModeConfig) :
    PagedGUI(text("Select Server: ${mode.name}"), 54) {

    init {
        setUpdateTicks(5)
    }

    override val icons: List<ItemStack>
        get() =
            ServerCache.getByMode(mode.id).map {
                item(
                    when {
                        it.players.size < it.maxPlayers - SERVER_PLAYER_BUFFER ->
                            Material.EMERALD_BLOCK
                        it.players.size < it.maxPlayers -> Material.GOLD_BLOCK
                        else -> Material.REDSTONE_BLOCK
                    },
                    text(it.name, NamedTextColor.WHITE),
                    text("${it.players.size}/${it.maxPlayers} Players", NamedTextColor.GRAY),
                    Component.empty(),
                    text("Click", NamedTextColor.YELLOW, TextDecoration.BOLD) +
                        text(" to join", NamedTextColor.WHITE))
            }

    override fun onPlayerClickIcon(event: InventoryClickEvent) {
        if (event.currentItem == null || event.currentItem!!.type == Material.AIR) return
        AsyncDatabase.submit {
            sendNetworkEvent(
                PlayerTransferServerPacket(
                    event.whoClicked.uniqueId,
                    (event.currentItem!!.itemMeta.displayName() as TextComponent).content()))
        }
    }

    override fun populateSpecial() {}
}
