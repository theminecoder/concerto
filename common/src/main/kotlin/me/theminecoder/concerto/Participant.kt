package me.theminecoder.concerto

import java.time.Instant
import java.util.*
import me.theminecoder.concerto.packets.PlayerDataRefreshPacket
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.save

object ParticipantManager {
    private val cache = mutableMapOf<UUID, Participant>()

    init {
        registerNetworkListener<PlayerDataRefreshPacket> {
            if (!cache.containsKey(it.player)) return@registerNetworkListener
            AsyncDatabase.submit { load(it.player) }
        }
    }

    fun load(player: UUID): Participant? {
        val participant =
            mongoClient.getCollection<Participant>().findOne { Participant::uuid eq player }
        if (participant != null) {
            cache[player] = participant
        }
        return participant
    }

    operator fun get(player: UUID): Participant = cache[player]!!

    fun unload(player: UUID) {
        cache.remove(player)
    }

    fun update(player: UUID, updater: Participant.() -> Unit) {
        val participant = cache[player]!!
        updater.invoke(participant)
        mongoClient.getCollection<Participant>().save(participant)
        sendNetworkEvent(PlayerDataRefreshPacket(player))
    }
}

data class Participant(
    val uuid: UUID,
    var name: String,
    var nameLower: String = name.toLowerCase(), // Makes searching via commands better
    var gameMode: ParticipantGameMode = ParticipantGameMode.ADVENTURE,
    val punishments: List<Punishment> = mutableListOf(),
    val firstJoin: Instant = Instant.now(),
    var lastJoin: Instant = Instant.now(),
    val foundMerch: List<String> = mutableListOf(),
    val selectedMerch: Map<ParticipantMerchSlot, String> = mutableMapOf()
)

enum class ParticipantMerchSlot {
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS
}

enum class ParticipantGameMode {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR
}

data class Punishment(
    val issuer: UUID,
    val type: PunishmentType,
    val start: Instant = Instant.now(),
    val end: Instant?,
    val reason: String
) {
    fun toComponent(): Component {
        return (text("You have been ${type.action}!\n", NamedTextColor.RED, TextDecoration.BOLD) +
                text("Reason: ", NamedTextColor.YELLOW) +
                text(reason + "\n", NamedTextColor.AQUA))
            .let {
                if (this.type != PunishmentType.KICK) {
                    return@let it +
                        text("Ends:", NamedTextColor.YELLOW) +
                        text(
                            if (this.end == null) "Never" else this.end.toString(),
                            NamedTextColor.AQUA)
                } else it
            }
    }
}

enum class PunishmentType(val action: String) {
    KICK("kicked"),
    MUTE("muted"),
    BAN("banned")
}
