package me.cubesicle

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.utils.Range
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class SphericalPoint(val distance: Double, val theta: Double, val phi: Double) {
    companion object {
        fun random(distanceRange: Range.Double, thetaRange: Range.Double, phiRange: Range.Double): SphericalPoint {
            val d = if (distanceRange.min == distanceRange.max) distanceRange.min
            else Random.nextDouble(distanceRange.min, distanceRange.max)
            val t = if (thetaRange.min == thetaRange.max) thetaRange.min
            else acos(Random.nextDouble(cos(thetaRange.max), cos(thetaRange.min)))
            val p = if (phiRange.min == phiRange.max) phiRange.min
            else Random.nextDouble(phiRange.min, phiRange.max)

            return SphericalPoint(d, t, p)
        }
    }

    val cartesianPoint: Point
        get() = Vec(
            distance * sin(theta) * sin(phi),
            distance * cos(theta),
            distance * sin(theta) * cos(phi),
        )

    fun angleBetween(sphericalPoint: SphericalPoint): Double {
        val theta2 = sphericalPoint.theta
        val phi2 = sphericalPoint.phi

        return acos(sin(theta) * sin(theta2) * cos(phi - phi2) + cos(theta) * cos(theta2))
    }
}