package me.theminecoder.concerto.packets

import java.util.*
import net.kyori.adventure.text.Component

data class ChatBroadcastPacket(val message: Component) : Packet

data class PlayerChatPacket(val player: UUID, val mode: String, val message: String) : Packet
