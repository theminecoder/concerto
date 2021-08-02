package me.theminecoder.concerto.packets

import me.theminecoder.concerto.ServerStatus

data class ServerStatusPacket(val status: ServerStatus) : Packet
