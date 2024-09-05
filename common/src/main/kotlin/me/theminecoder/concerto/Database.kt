package me.theminecoder.concerto

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.gson.JsonObject
import com.mongodb.client.MongoDatabase
import io.nats.client.Connection
import io.nats.client.Nats
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import me.theminecoder.concerto.packets.Packet
import org.litote.kmongo.KMongo

private data class DatabaseConfig(val mongo: MongoConfig, val nats: NatsConfig)

private data class MongoConfig(val hostname: String, val port: Int, val database: String)

private data class NatsConfig(val hostname: String, val port: Int)

private data class ConsulConfig(val hostname: String, val port: Int)

private val databaseConfig: DatabaseConfig by lazy {
    GSON.fromJson(File("database.json").readText(), DatabaseConfig::class.java)
}

// this is pre coroutines for me, please be nice
object AsyncDatabase {
    private val threadCount = AtomicInteger(0)
    private val executorService =
        Executors.newCachedThreadPool { r: Runnable? ->
            Thread(r, "Database Thread - " + threadCount.incrementAndGet()).also {
                it.isDaemon = true
            }
        }

    fun submit(runnable: Runnable) {
        executorService.submit {
            try {
                runnable.run()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

val mongoClient: MongoDatabase =
    KMongo.createClient("mongodb://${databaseConfig.mongo.hostname}:${databaseConfig.mongo.port}/")
        .getDatabase(databaseConfig.mongo.database)

val packetListeners: Multimap<Class<out Packet>, Consumer<Packet>> = ArrayListMultimap.create()
private const val CHANNEL = "concerto"
private val natsClient: Connection =
    Nats.connect("nats://${databaseConfig.nats.hostname}:${databaseConfig.nats.port}").also {
        it.createDispatcher().subscribe(CHANNEL) { message ->
            val packetWrapper = GSON.fromJson(message.data.decodeToString(), JsonObject::class.java)
            val packet =
                GSON.fromJson(packetWrapper["data"], Class.forName(packetWrapper["type"].asString))
            @Suppress("UNCHECKED_CAST")
            packetListeners[packet::class.java as Class<out Packet>].forEach { listener ->
                listener.accept(packet as Packet)
            }
        }
    }

inline fun <reified T : Packet> registerNetworkListener(handler: Consumer<T>) {
    @Suppress("UNCHECKED_CAST") packetListeners.put(T::class.java, handler as Consumer<Packet>)
}

fun sendNetworkEvent(packet: Packet) {
    val packetJson = GSON.toJsonTree(packet)
    natsClient.publish(
        CHANNEL,
        GSON.toJson(
                JsonObject().also {
                    it.addProperty("type", packet::class.java.canonicalName)
                    it.add("data", packetJson)
                })
            .toByteArray())
}
