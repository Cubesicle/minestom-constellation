package me.cubesicle

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.source.decodeFromStream
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.*
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.utils.Range
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import kotlin.concurrent.fixedRateTimer
import kotlin.io.path.Path
import kotlin.math.PI

val logger: Logger = LoggerFactory.getLogger(::main.javaClass.name.split('$')[0])

val config = run {
    val configPath = Path("config.toml")
    if (!configPath.toFile().exists()) {
        val stream = object {}.javaClass.getResourceAsStream("/default-config.toml")
        stream.use {
            if (it != null) Files.copy(it, configPath)
        }
    }
    Toml.decodeFromStream<Config>(configPath.toFile().inputStream())
}

// Initialization
val minecraftServer: MinecraftServer = MinecraftServer.init()

// Create the instance
val instanceManager = MinecraftServer.getInstanceManager()
val instanceContainer = instanceManager.createInstanceContainer()
val spawnPoint = Pos(0.0, 500.0, 0.0, 0f, -45f)
val constellationMap = HashMap<Instance, Constellation>()

val globalEventHandler = MinecraftServer.getGlobalEventHandler()

fun main() {
    VelocityProxy.enable(config.forwardingSecret)

    instanceContainer.setChunkSupplier(::LightingChunk)

    globalEventHandler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
        val sharedInstance = instanceManager.createSharedInstance(instanceContainer)
        sharedInstance.time = 18000
        sharedInstance.timeRate = 0
        createConstellation(sharedInstance)

        event.spawningInstance = sharedInstance
        event.player.respawnPoint = spawnPoint
    }
    globalEventHandler.addListener(PlayerSpawnEvent::class.java) { event ->
        val player = event.player
        player.gameMode = GameMode.ADVENTURE
        player.isFlying = true
        player.sendMessage("<gurt> yo")
        player.sendMessage("<gurt> left-click selects a server")
        player.sendMessage("<gurt> swap item button generates a new constellation")
    }
    globalEventHandler.addListener(PlayerChatEvent::class.java) { event ->
        event.isCancelled = true
    }
    globalEventHandler.addListener(PlayerMoveEvent::class.java) { event ->
        event.player.isFlying = true
        event.player.velocity = Vec(0.0)
        event.newPosition = event.newPosition.withCoord(spawnPoint.x(), spawnPoint.y(), spawnPoint.z())
    }
    globalEventHandler.addListener(PlayerSwapItemEvent::class.java) { event ->
        val instance = event.instance
        removeConstellation(instance)
        createConstellation(instance)
    }
    globalEventHandler.addListener(PlayerDisconnectEvent::class.java) { event ->
        val instance = event.instance
        removeConstellation(instance)
        instanceManager.unregisterInstance(instance)
    }

    fixedRateTimer("Server Pinger", false, 0, 1000) {
        Constellation.updateServerStats()
        constellationMap.values.forEach {
            it.updateSidebar()
        }
    }

    val host = config.bind.hostName
    val port = config.bind.port
    minecraftServer.start(host, port)
    logger.info("Listening on $host:$port")
}

fun createConstellation(instance: Instance) {
    val constellation = Constellation.random(
        config.servers,
        spawnPoint.withY(spawnPoint.y + EntityType.PLAYER.registry().eyeHeight()),
        10.0,
        Range.Double(4 * PI / 11 - PI / 4, 7 * PI / 11 - PI / 4),
        Range.Double(-PI / 4, PI / 4),
    )
    constellation.setInstance(instance)

    constellationMap[instance] = constellation
}

fun removeConstellation(instance: Instance) {
    constellationMap[instance]?.remove()
    constellationMap.remove(instance)
}