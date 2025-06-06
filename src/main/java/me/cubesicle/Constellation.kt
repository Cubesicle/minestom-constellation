package me.cubesicle

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.PlayerHand
import net.minestom.server.event.EventListener
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.event.player.PlayerMoveEvent
import net.minestom.server.instance.Instance
import net.minestom.server.network.NetworkBuffer
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.scoreboard.Sidebar
import net.minestom.server.utils.Range
import java.net.InetSocketAddress
import kotlin.math.PI

class Constellation(val stars: List<Star>, val origin: Point) {
    companion object {
        fun random(
            servers: Map<String, InetSocketAddress>,
            origin: Point,
            distance: Double,
            thetaRange: Range.Double,
            phiRange: Range.Double
        ): Constellation {
            val stars = ArrayList<Star>()
            servers.keys.forEach {
                stars.add(
                    Star(
                        it, SphericalPoint.random(
                            Range.Double(distance),
                            thetaRange,
                            phiRange,
                        )
                    )
                )
            }
            return Constellation(stars, origin)
        }
    }

    var instance: Instance? = null
        private set
    private var selectedStar: Star? = null
    private val sidebar = run {
        val s = Sidebar(Component.text(""))
        val lines = 3
        for (i in 1..lines) {
            s.createLine(
                Sidebar.ScoreboardLine(
                    i.toString(),
                    Component.text(""),
                    lines - (i - 1),
                    Sidebar.NumberFormat.blank()
                )
            )
        }

        s
    }

    private val listeners = arrayOf(
        EventListener.builder(InstanceTickEvent::class.java).handler { event ->
            stars.forEachIndexed { i, star ->
                val nextStar = stars[(i + 1) % stars.size]
                val startPoint = origin.add(star.point.cartesianPoint)
                val endPoint = origin.add(nextStar.point.cartesianPoint)
                val startScale = if (ServerStats.stats[star.name] != null) 0.25f else 0f
                val endScale = if (ServerStats.stats[nextStar.name] != null) 0.25f else 0f
                drawLine(event.instance, startPoint, endPoint, startScale, endScale, 0.05)
            }
        }.build(),
        EventListener.builder(PlayerMoveEvent::class.java).handler { event ->
            val playerPos = event.newPosition
            val playerTheta = (playerPos.pitch + 90) / 180 * PI
            val playerPhi = -playerPos.yaw / 180 * PI

            var smallestAngle: Double? = null
            var closestStar: Star? = null
            stars.forEach {
                val angle = SphericalPoint(1.0, playerTheta, playerPhi).angleBetween(it.point)
                if (smallestAngle == null || angle < smallestAngle!!) {
                    smallestAngle = angle
                    closestStar = it
                }
            }

            selectedStar?.isGlowing = false
            selectedStar = if (smallestAngle != null && smallestAngle!! <= PI / 64) {
                closestStar
            } else {
                null
            }
            selectedStar?.isGlowing = true

            updateSidebar()
        }.build(),
        EventListener.builder(PlayerHandAnimationEvent::class.java).handler { event ->
            if (selectedStar != null && event.hand == PlayerHand.MAIN) {
                event.player.sendPluginMessage("bungeecord:main", NetworkBuffer.makeArray { buffer: NetworkBuffer ->
                    buffer.write(NetworkBuffer.STRING_IO_UTF8, "Connect")
                    buffer.write(NetworkBuffer.STRING_IO_UTF8, selectedStar!!.name)
                })
            }
        }.build(),
    )

    fun setInstance(instance: Instance) {
        remove()

        this.instance = instance

        stars.forEach {
            it.setInstance(instance, origin)
        }
        listeners.forEach {
            instance.eventNode().addListener(it)
        }
    }

    fun remove() {
        if (instance == null) return

        listeners.forEach {
            instance!!.eventNode().removeListener(it)
        }
        instance!!.players.firstOrNull()?.let { sidebar.removeViewer(it) }
        stars.forEach {
            it.remove()
        }

        instance = null
    }

    fun updateServerStats() {
        stars.forEach {
            it.updateDisplayName()
        }
        updateSidebar()
    }

    private fun drawLine(
        instance: Instance,
        startPoint: Point,
        endPoint: Point,
        startScale: Float,
        endScale: Float,
        spacing: Double
    ) {
        val startVec = Vec(startPoint.x(), startPoint.y(), startPoint.z())
        val endVec = Vec(endPoint.x(), endPoint.y(), endPoint.z())

        val points = (startPoint.distance(endPoint) / spacing).toInt()
        for (i in 0..points) {
            val player = instance.players.firstOrNull()
            val scale = startScale + (endScale - startScale) * (i.toFloat() / points)
            if (scale <= 0f) continue

            val particle = Particle.Dust(Key.key(""), 13, Color.WHITE, scale)
            player?.sendPacket(ParticlePacket(particle, startVec.lerp(endVec, i.toDouble() / points), Vec(0.0), 0f, 1))
        }
    }

    private fun updateSidebar() {
        val player = instance?.players?.firstOrNull() ?: return

        if (selectedStar != null) {
            val stat = ServerStats.stats[selectedStar!!.name]
            val mm = MiniMessage.miniMessage()
            sidebar.setTitle(Component.text(selectedStar!!.name))
            sidebar.updateLineContent(
                "1",
                mm.deserialize("Status: ${if (stat != null) "<green>Online" else "<red>Offline"}")
            )
            sidebar.updateLineContent(
                "2",
                Component.text("Players: ${if (stat?.playersOnline != null) stat.playersOnline else "???"}")
            )
            sidebar.updateLineContent(
                "3",
                Component.text("Version: ${if (stat?.gameVersion != null) stat.gameVersion else "???"}")
            )
            sidebar.addViewer(player)
        } else {
            sidebar.removeViewer(player)
        }
    }

    class Star(val name: String, val point: SphericalPoint) {
        var isGlowing = false
            set(value) {
                entity.isGlowing = value
                field = value
            }

        private val entity = Entity(EntityType.SHULKER_BULLET)

        fun setInstance(instance: Instance, origin: Point) {
            updateDisplayName()
            entity.isCustomNameVisible = true
            entity.setNoGravity(true)
            entity.setInstance(instance, origin.add(point.cartesianPoint))
        }

        fun updateDisplayName() {
            val mm = MiniMessage.miniMessage()
            entity.customName = mm.deserialize(if (ServerStats.stats[name] != null) name else "<dark_gray>$name")
        }

        fun remove() {
            entity.remove()
        }
    }
}