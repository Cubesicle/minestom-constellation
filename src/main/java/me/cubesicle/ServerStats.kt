package me.cubesicle

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object ServerStats {
    val stats: ConcurrentMap<String, Pinger> = ConcurrentHashMap()

    fun update() {
        config.servers.forEach { (name, address) ->
            val pinger = Pinger(address.hostName, address.port)
            if (pinger.fetchData()) {
                stats[name] = pinger
            } else {
                stats.remove(name)
            }
        }
    }
}