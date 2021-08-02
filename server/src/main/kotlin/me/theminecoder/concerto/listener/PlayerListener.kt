package me.theminecoder.concerto.listener

import me.theminecoder.concerto.*
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityPortalEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*

object PlayerListener {

    private val DISABLED_COMMANDS = listOf("me")

    @EventHandler(ignoreCancelled = true)
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        if (event.foodLevel < 20) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityAirChange(event: EntityAirChangeEvent) {
        if (event.amount < 20) {
            event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        event.keepInventory = true
        event.drops.clear()
        event.deathMessage = null
        event.entity.spigot().respawn()
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        event.respawnLocation = Concerto.SERVER_CONFIG.spawn.toLocation()
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        var cmd = event.message.split(" ").toTypedArray()[0]
        if (cmd.startsWith("/")) cmd = cmd.substring(1)
        if (DISABLED_COMMANDS.contains(cmd.toLowerCase())) event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }
        //        if (event.hasBlock()) {
        //            val interaction: WorldConfig.ConfigInteracton =
        //                Event.getWorldConfig()
        //                    .getInteractions()
        //                    .stream()
        //                    .filter { config ->
        //                        config.getLocation().getX() === event.clickedBlock!!.x &&
        //                            config.getLocation().getY() === event.clickedBlock!!.y &&
        //                            config.getLocation().getZ() === event.clickedBlock!!.z
        //                    }
        //                    .findFirst()
        //                    .orElse(null)
        //            if (interaction != null) {
        //                interaction
        //                    .getInteraction()
        //                    .getAction()
        //                    .accept(event.player, interaction.getConfig())
        //                return
        //            }
        //        }
        val heldItem = event.item
        if (heldItem == null || heldItem.type == Material.AIR) return
        if (heldItem.type == Material.COMPASS) {
            ModeSelector().open(event.player)
            return
        }
        //        if (heldItem.type == Material.ARMOR_STAND) {
        //            MerchGUI(event.player).open(event.player)
        //        }
        //        if (FoundationGameServices.getServerMode().isStage() &&
        //            event.hand == EquipmentSlot.HAND &&
        //            (event.player.inventory.heldItemSlot == 7 ||
        //                event.player.inventory.heldItemSlot == 8)) {
        //            val eventPlayer: EventPlayer =
        //                EventPlayerManager.getEventPlayerMap().get(event.player.uniqueId)
        //            if (heldItem.type == Material.ENDER_EYE) {
        //                event.isCancelled = true
        //                eventPlayer.teleportTo(event.player, StageLocation.CROWD)
        //                return
        //            }
        //            if (heldItem.type == Material.NETHER_STAR) {
        //                event.isCancelled = true
        //                eventPlayer.teleportTo(event.player, StageLocation.VIP_AREA)
        //                return
        //            }
        //            if (heldItem.type == Material.MUSIC_DISC_13) {
        //                event.isCancelled = true
        //                eventPlayer.teleportTo(event.player, StageLocation.ON_STAGE)
        //                return
        //            }
        //        }
    }

    @EventHandler(ignoreCancelled = true) fun onPlayerQuit(event: PlayerQuitEvent) {}

    @EventHandler(ignoreCancelled = true)
    fun onPlayerPortal(event: PlayerPortalEvent) {
        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityPortal(event: EntityPortalEvent) {
        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.clickedInventory === event.whoClicked.inventory) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        AsyncDatabase.submit {
            ParticipantManager.update(event.player.uniqueId) {
                gameMode = event.newGameMode.toConcerto()
            }
        }
    }
}
