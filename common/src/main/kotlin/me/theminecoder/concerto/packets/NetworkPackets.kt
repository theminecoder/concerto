package me.theminecoder.concerto.packets

import java.util.*
import net.kyori.adventure.text.Component

data class PlayerKickPacket(val player: UUID, val reason: Component) : Packet

data class PlayerTransferServerPacket(val player: UUID, val server: String) : Packet

data class PlayerTransferModePacket(val player: UUID, val mode: String) : Packet

data class PlayerDataRefreshPacket(val player: UUID) : Packet
