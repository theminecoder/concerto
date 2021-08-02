package me.theminecoder.concerto

import com.destroystokyo.paper.ClientOption
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.fixedRateTimer
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import me.theminecoder.concerto.packets.*
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.npc.NPCDataStore
import net.citizensnpcs.api.npc.NPCRegistry
import net.citizensnpcs.api.trait.trait.Equipment
import net.citizensnpcs.trait.SkinLayers
import net.citizensnpcs.util.PlayerAnimation
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

@Suppress("ProtectedInFinal")
class MirrorTask(private val player: Player) : Listener {

    private var task: BukkitTask? = null
    private var nextUpdate: Instant = Instant.EPOCH
    private var lastLocation: Location = player.location

    fun start() {
        require(task == null) { "Task already started" }
        task = player.server.scheduler.runTaskTimerAsynchronously(Concerto.PLUGIN, this::run, 0, 5)
        player.server.pluginManager.registerEvents(this, Concerto.PLUGIN)
        sendUpdate()
    }

    fun stop() {
        require(task != null) { "Task already stopped" }
        if (task != null) {
            task!!.cancel()
            HandlerList.unregisterAll(this)
            task = null
        }
    }

    private fun run() {
        if (!player.isOnline) {
            this.stop()
            return
        }
        if (player.gameMode != GameMode.SPECTATOR) {
            if (nextUpdate.isBefore(Instant.now()) || lastLocation != player.location) {
                sendUpdate()
            }
        }
    }

    private fun sendUpdate() = sendPacket(MirrorPlayerState(player))

    private fun updateIfPlayer(e: Entity, update: () -> Unit = { sendUpdate() }) {
        if (e == player) {
            update()
        }
    }

    private fun sendPacket(packet: MirrorPacket) {
        val update = { sendNetworkEvent(packet) }
        if (Bukkit.isPrimaryThread()) AsyncDatabase.submit(update) else update()
    }

    @EventHandler
    protected fun onEventUpdate(event: PlayerArmorChangeEvent) = updateIfPlayer(event.player)
    @EventHandler
    protected fun onEventUpdate(event: PlayerSwapHandItemsEvent) = updateIfPlayer(event.player)
    @EventHandler
    protected fun onEventUpdate(event: EntityPickupItemEvent) = updateIfPlayer(event.entity)
    @EventHandler
    protected fun onEventUpdate(event: PlayerDropItemEvent) = updateIfPlayer(event.player)
    @EventHandler
    protected fun onEventUpdate(event: PlayerItemHeldEvent) = updateIfPlayer(event.player)
    @EventHandler
    protected fun onAnimation(event: PlayerAnimationEvent) =
        updateIfPlayer(event.player) {
            sendPacket(MirrorPlayerArmSwingEvent(event.player.uniqueId))
        }
    @EventHandler
    protected fun onSneak(event: PlayerToggleSneakEvent) =
        updateIfPlayer(event.player) {
            sendPacket(MirrorPlayerSneakEvent(event.player.uniqueId, event.isSneaking))
        }
}

@OptIn(ExperimentalTime::class)
object MirrorManager {

    private val npcs = mutableMapOf<UUID, NPCHolder>()

    private val npcId = AtomicInteger()
    private val registry =
        CitizensAPI.createAnonymousNPCRegistry(
            object : NPCDataStore {
                override fun clearData(p0: NPC?) {}
                override fun createUniqueNPCId(p0: NPCRegistry?): Int = npcId.incrementAndGet()
                override fun loadInto(p0: NPCRegistry?) {}
                override fun saveToDisk() {}
                override fun saveToDiskImmediate() {}
                override fun store(p0: NPC?) {}
                override fun storeAll(p0: NPCRegistry?) {}
                override fun reloadFromSource() {}
            })

    init {
        registerNetworkListener<MirrorPlayerState> {
            if (Bukkit.getPlayer(it.player) != null) return@registerNetworkListener
            Bukkit.getScheduler().runTask(Concerto.PLUGIN) { _ ->
                val npc =
                    npcs
                        .computeIfAbsent(it.player) { _ ->
                            val npc =
                                registry.createNPC(
                                    EntityType.PLAYER, it.player, npcId.incrementAndGet(), it.name)
                            npc.isFlyable = true
                            npc.spawn(it.location.toLocation())
                            (npc.entity as LivingEntity).removeWhenFarAway = false
                            NPCHolder(npc)
                        }
                        .apply { exires = Instant.now().plusSeconds(3) }
                        .npc
                npc.teleport(it.location.toLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN)
                npc.getTrait(Equipment::class.java).apply {
                    set(Equipment.EquipmentSlot.HAND, ItemStack.deserializeBytes(it.mainHand))
                    set(Equipment.EquipmentSlot.OFF_HAND, ItemStack.deserializeBytes(it.offHand))
                    set(Equipment.EquipmentSlot.HELMET, ItemStack.deserializeBytes(it.helmet))
                    set(
                        Equipment.EquipmentSlot.CHESTPLATE,
                        ItemStack.deserializeBytes(it.chestplate))
                    set(Equipment.EquipmentSlot.LEGGINGS, ItemStack.deserializeBytes(it.leggings))
                    set(Equipment.EquipmentSlot.BOOTS, ItemStack.deserializeBytes(it.boots))
                }
                npc.getTrait(SkinLayers::class.java).apply {
                    setVisible(SkinLayers.Layer.CAPE, it.activeLayers[MirrorPlayerSkinLayer.CAPE]!!)
                    setVisible(
                        SkinLayers.Layer.JACKET, it.activeLayers[MirrorPlayerSkinLayer.JACKET]!!)
                    setVisible(
                        SkinLayers.Layer.LEFT_SLEEVE,
                        it.activeLayers[MirrorPlayerSkinLayer.LEFT_SLEEVE]!!)
                    setVisible(
                        SkinLayers.Layer.RIGHT_SLEEVE,
                        it.activeLayers[MirrorPlayerSkinLayer.RIGHT_SLEEVE]!!)
                    setVisible(
                        SkinLayers.Layer.LEFT_PANTS,
                        it.activeLayers[MirrorPlayerSkinLayer.LEFT_PANTS]!!)
                    setVisible(
                        SkinLayers.Layer.RIGHT_PANTS,
                        it.activeLayers[MirrorPlayerSkinLayer.RIGHT_PANTS]!!)
                    setVisible(SkinLayers.Layer.HAT, it.activeLayers[MirrorPlayerSkinLayer.HAT]!!)
                }
            }
        }
        registerNetworkListener<MirrorPlayerSneakEvent> {
            npcs[it.player]?.npc?.apply {
                (if (it.sneaking) PlayerAnimation.SNEAK else PlayerAnimation.STOP_SNEAKING).play(
                    entity as Player)
            }
        }
        registerNetworkListener<MirrorPlayerArmSwingEvent> {
            npcs[it.player]?.npc?.apply { PlayerAnimation.ARM_SWING.play(entity as Player) }
        }
        fixedRateTimer("NPC Cleaner", period = 1.seconds.toLong(TimeUnit.MILLISECONDS)) {
            val now = Instant.now()
            npcs.entries.removeIf {
                it.value.exires.isBefore(now).also { check -> if (check) it.value.npc.destroy() }
            }
        }
    }

    private class NPCHolder(val npc: NPC, var exires: Instant = Instant.now())
}

fun MirrorPlayerState(it: Player): MirrorPlayerState =
    MirrorPlayerState(
        it.uniqueId,
        it.name,
        it.location.toMirrorLocation(),
        it.getClientOption(ClientOption.SKIN_PARTS).toMirrorMap(),
        it.equipment!!.itemInMainHand.serializeAsBytes(),
        it.equipment!!.itemInOffHand.serializeAsBytes(),
        it.equipment!!.helmet!!.serializeAsBytes(),
        it.equipment!!.chestplate!!.serializeAsBytes(),
        it.equipment!!.leggings!!.serializeAsBytes(),
        it.equipment!!.boots!!.serializeAsBytes())
