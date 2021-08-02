package me.theminecoder.concerto

import com.google.gson.GsonBuilder
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import net.dongliu.gson.GsonJava8TypeAdapterFactory
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

val GSON =
    GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapterFactory(GsonJava8TypeAdapterFactory())
        .also { GsonComponentSerializer.gson().populator().apply(it) }
        .create()

@OptIn(ExperimentalTime::class)
object TerminationHandler {

    var isTerminating: Boolean = false
        private set

    init {
        fixedRateTimer("Termination Checker", period = 1.seconds.toLong(TimeUnit.MILLISECONDS)) {
            if (!isTerminating && File(".die").exists()) {
                isTerminating = true
            }
        }
    }
}
