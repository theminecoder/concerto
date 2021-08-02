package me.theminecoder.concerto.packets

import java.util.*

interface MirrorPacket : Packet

@Suppress("ArrayInDataClass")
data class MirrorPlayerState(
    val player: UUID,
    val name: String,
    val location: MirrorLocation,
    val activeLayers: Map<MirrorPlayerSkinLayer, Boolean>,
    val mainHand: ByteArray,
    val offHand: ByteArray,
    val helmet: ByteArray,
    val chestplate: ByteArray,
    val leggings: ByteArray,
    val boots: ByteArray,
) : MirrorPacket {
    companion object
}

data class MirrorLocation(
    val x: Double,
    val y: Double,
    val z: Double,
    val dirX: Double,
    val dirY: Double,
    val dirZ: Double,
    val yaw: Float,
    val pitch: Float
)

data class MirrorPlayerSneakEvent(val player: UUID, val sneaking: Boolean) : MirrorPacket

data class MirrorPlayerArmSwingEvent(val player: UUID) : MirrorPacket

enum class MirrorPlayerSkinLayer {
    CAPE,
    JACKET,
    LEFT_SLEEVE,
    RIGHT_SLEEVE,
    LEFT_PANTS,
    RIGHT_PANTS,
    HAT
}
