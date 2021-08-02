package me.theminecoder.concerto

import com.destroystokyo.paper.SkinParts
import me.theminecoder.concerto.packets.MirrorLocation
import me.theminecoder.concerto.packets.MirrorPlayerSkinLayer
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector

fun SkinParts.toMirrorMap() =
    mapOf(
        MirrorPlayerSkinLayer.CAPE to hasCapeEnabled(),
        MirrorPlayerSkinLayer.JACKET to hasJacketEnabled(),
        MirrorPlayerSkinLayer.LEFT_SLEEVE to hasLeftSleeveEnabled(),
        MirrorPlayerSkinLayer.RIGHT_SLEEVE to hasRightSleeveEnabled(),
        MirrorPlayerSkinLayer.LEFT_PANTS to hasLeftPantsEnabled(),
        MirrorPlayerSkinLayer.RIGHT_PANTS to hasRightPantsEnabled(),
        MirrorPlayerSkinLayer.HAT to hasHatsEnabled(),
    )

fun Location.toMirrorLocation() =
    MirrorLocation(x, y, z, direction.x, direction.y, direction.z, yaw, pitch)

fun MirrorLocation.toLocation(world: World = Bukkit.getWorlds()[0]) =
    Location(world, x, y, z, yaw, pitch).apply { direction = Vector(dirX, dirY, dirZ) }

fun Position.toLocation(world: World = Bukkit.getWorlds()[0]) = Location(world, x, y, z)

fun GameMode.toConcerto() =
    when (this) {
        GameMode.SURVIVAL -> ParticipantGameMode.SURVIVAL
        GameMode.CREATIVE -> ParticipantGameMode.SURVIVAL
        GameMode.ADVENTURE -> ParticipantGameMode.SURVIVAL
        GameMode.SPECTATOR -> ParticipantGameMode.SURVIVAL
    }

fun ParticipantGameMode.toBukkit() =
    when (this) {
        ParticipantGameMode.SURVIVAL -> GameMode.SURVIVAL
        ParticipantGameMode.CREATIVE -> GameMode.SURVIVAL
        ParticipantGameMode.ADVENTURE -> GameMode.SURVIVAL
        ParticipantGameMode.SPECTATOR -> GameMode.SURVIVAL
    }

fun item(material: Material, name: Component, vararg lore: Component, amount: Int = 1): ItemStack =
    ItemStack(material, amount).apply {
        itemMeta =
            itemMeta.apply {
                displayName(name)
                lore(lore.toList())
            }
    }
