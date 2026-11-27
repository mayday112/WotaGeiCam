package com.wotagei.cam.ui

/**
 * Slot definitions & persistence codec for the Minimalist Camera HUD.
 * Pure Kotlin (no Android/Compose dependencies) so JVM unit tests can verify
 * encoding, decoding, duplicate/corrupt-string fallback, and slot reordering.
 */
enum class MinimalistCreativeSlot {
    REFLINE,
    MONITOR,
    CURVE,
    FX,
    DENOISE,
    FLASH
}

enum class MinimalistReadoutSlot {
    SHUTTER,
    ISO,
    EV,
    WB,
    ZOOM,
    FPS
}

enum class MinimalistControlSlot {
    LENS,
    FOCUS,
    STAB
}

data class MinimalistHudOrder(
    val creative: List<MinimalistCreativeSlot> = MinimalistCreativeSlot.entries,
    val readout: List<MinimalistReadoutSlot> = MinimalistReadoutSlot.entries,
    val controls: List<MinimalistControlSlot> = MinimalistControlSlot.entries
) {
    fun encode(): String = listOf(
        creative.joinToString(",") { it.name },
        readout.joinToString(",") { it.name },
        controls.joinToString(",") { it.name }
    ).joinToString(";")

    companion object {
        val DEFAULT = MinimalistHudOrder()

        fun decode(raw: String?): MinimalistHudOrder {
            if (raw.isNullOrBlank()) return DEFAULT
            val parts = raw.split(";")
            return MinimalistHudOrder(
                creative = decodeGroup(parts.getOrNull(0), MinimalistCreativeSlot.entries),
                readout = decodeGroup(parts.getOrNull(1), MinimalistReadoutSlot.entries),
                controls = decodeGroup(parts.getOrNull(2), MinimalistControlSlot.entries)
            )
        }

        private fun <T : Enum<T>> decodeGroup(raw: String?, all: List<T>): List<T> {
            if (raw.isNullOrBlank()) return all
            val byName = all.associateBy { it.name }
            val result = ArrayList<T>(all.size)
            val seen = HashSet<T>()
            raw.split(",").forEach { token ->
                val item = byName[token.trim()]
                if (item != null && seen.add(item)) {
                    result.add(item)
                }
            }
            all.forEach { item ->
                if (seen.add(item)) result.add(item)
            }
            return result
        }
    }
}

fun <T> List<T>.moveSlot(index: Int, delta: Int): List<T> {
    if (index !in indices) return this
    val target = (index + delta).coerceIn(0, lastIndex)
    if (target == index) return this
    val out = toMutableList()
    val item = out.removeAt(index)
    out.add(target, item)
    return out
}

fun <T> List<T>.swapSlots(first: Int, second: Int): List<T> {
    if (first !in indices || second !in indices || first == second) return this
    val out = toMutableList()
    val tmp = out[first]
    out[first] = out[second]
    out[second] = tmp
    return out
}
