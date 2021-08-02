package me.theminecoder.concerto

import com.destroystokyo.paper.SkinParts
import me.theminecoder.concerto.packets.MirrorLocation
import me.theminecoder.concerto.packets.MirrorPlayerSkinLayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
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

fun Merch.toItem() =
    item(
        Material.getMaterial(item)!!,
        text(name, NamedTextColor.WHITE),
        lore = lore.map { text(it, NamedTextColor.GRAY) }.toTypedArray())
        .apply {
            itemMeta =
                itemMeta.also {
                    if (color != null) {
                        require(it is LeatherArmorMeta) { "Must be leather armor to use colors" }
                        it.setColor(
                            TextColor.fromHexString(color!!).let {
                                Color.fromRGB(it!!.red(), it.green(), it.blue())
                            })
                    }
                }
        }

val ParticipantMerchSlot.asBukkit: EquipmentSlot
    get() =
        when (this) {
            ParticipantMerchSlot.HELMET -> EquipmentSlot.HEAD
            ParticipantMerchSlot.CHESTPLATE -> EquipmentSlot.CHEST
            ParticipantMerchSlot.LEGGINGS -> EquipmentSlot.LEGS
            ParticipantMerchSlot.BOOTS -> EquipmentSlot.FEET
        }

val EquipmentSlot.asConcerto: ParticipantMerchSlot
    get() =
        when (this) {
            EquipmentSlot.HEAD -> ParticipantMerchSlot.HELMET
            EquipmentSlot.CHEST -> ParticipantMerchSlot.CHESTPLATE
            EquipmentSlot.LEGS -> ParticipantMerchSlot.LEGGINGS
            EquipmentSlot.FEET -> ParticipantMerchSlot.BOOTS
            else -> throw IllegalArgumentException("Cannot use hand for equipment")
        }

fun item(material: Material, name: Component, vararg lore: Component, amount: Int = 1): ItemStack =
    ItemStack(material, amount).apply {
        itemMeta =
            itemMeta.apply {
                displayName(name)
                lore(lore.toList())
            }
    }
