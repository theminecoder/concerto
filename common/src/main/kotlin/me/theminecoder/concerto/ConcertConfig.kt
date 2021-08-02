package me.theminecoder.concerto

import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

data class Config(val motd: String, val modes: List<ModeConfig> = listOf())

data class ModeConfig(
    val id: String,
    val name: String,
    val icon: String,
    val switcherSlot: Int,
    val spawn: Position,
    val portalSpawn: Position?
)

data class Position(val x: Double, val y: Double, val z: Double)

@OptIn(ExperimentalTime::class)
object ConcertConfig {
    lateinit var current: Config
        private set

    init {
        reload()
        fixedRateTimer("Config Reloader", period = 5.seconds.toLong(TimeUnit.MILLISECONDS)) {
            reload()
        }
    }

    private fun reload() {
        TODO("Where to get config?????")
    }
}
