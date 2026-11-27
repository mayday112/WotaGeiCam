package com.wotagei.cam

import com.wotagei.cam.ui.MinimalistControlSlot
import com.wotagei.cam.ui.MinimalistCreativeSlot
import com.wotagei.cam.ui.MinimalistHudOrder
import com.wotagei.cam.ui.MinimalistReadoutSlot
import com.wotagei.cam.ui.moveSlot
import com.wotagei.cam.ui.swapSlots
import org.junit.Assert.assertEquals
import org.junit.Test

class MinimalistHudOrderTest {

    @Test
    fun `encode and decode roundtrip preserves custom order`() {
        val custom = MinimalistHudOrder(
            creative = listOf(
                MinimalistCreativeSlot.FLASH,
                MinimalistCreativeSlot.CURVE,
                MinimalistCreativeSlot.REFLINE,
                MinimalistCreativeSlot.MONITOR,
                MinimalistCreativeSlot.DENOISE,
                MinimalistCreativeSlot.FX
            ),
            readout = listOf(
                MinimalistReadoutSlot.FPS,
                MinimalistReadoutSlot.SHUTTER,
                MinimalistReadoutSlot.ISO,
                MinimalistReadoutSlot.EV,
                MinimalistReadoutSlot.WB,
                MinimalistReadoutSlot.ZOOM
            ),
            controls = listOf(
                MinimalistControlSlot.STAB,
                MinimalistControlSlot.LENS,
                MinimalistControlSlot.FOCUS
            )
        )
        val decoded = MinimalistHudOrder.decode(custom.encode())
        assertEquals(custom, decoded)
    }

    @Test
    fun `decode handles null blank corrupt and partial strings safely`() {
        assertEquals(MinimalistHudOrder.DEFAULT, MinimalistHudOrder.decode(null))
        assertEquals(MinimalistHudOrder.DEFAULT, MinimalistHudOrder.decode(""))
        assertEquals(MinimalistHudOrder.DEFAULT, MinimalistHudOrder.decode("invalid;garbage"))

        // Partial + duplicate tokens keep valid order first and backfill missing entries
        val partial = MinimalistHudOrder.decode("FLASH,FLASH,UNKNOWN;ZOOM;;")
        assertEquals(
            listOf(
                MinimalistCreativeSlot.FLASH,
                MinimalistCreativeSlot.REFLINE,
                MinimalistCreativeSlot.MONITOR,
                MinimalistCreativeSlot.CURVE,
                MinimalistCreativeSlot.FX,
                MinimalistCreativeSlot.DENOISE
            ),
            partial.creative
        )
        assertEquals(
            listOf(
                MinimalistReadoutSlot.ZOOM,
                MinimalistReadoutSlot.SHUTTER,
                MinimalistReadoutSlot.ISO,
                MinimalistReadoutSlot.EV,
                MinimalistReadoutSlot.WB,
                MinimalistReadoutSlot.FPS
            ),
            partial.readout
        )
        assertEquals(MinimalistControlSlot.entries, partial.controls)
    }

    @Test
    fun `moveSlot and swapSlots reorder cleanly with bounds safety`() {
        val base = listOf("A", "B", "C")
        assertEquals(listOf("B", "A", "C"), base.moveSlot(0, 1))
        assertEquals(listOf("A", "B", "C"), base.moveSlot(0, -1))
        assertEquals(listOf("C", "B", "A"), base.swapSlots(0, 2))
        assertEquals(base, base.swapSlots(-1, 2))
    }
}
