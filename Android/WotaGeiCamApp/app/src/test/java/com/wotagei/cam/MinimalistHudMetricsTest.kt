package com.wotagei.cam

import com.wotagei.cam.ui.MinimalistHudMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalistHudMetricsTest {

    @Test
    fun landscapeReadoutEndPadClearsRightRailAndThumbGap() {
        val endPad = MinimalistHudMetrics.landscapeReadoutEndPadDp()
        assertEquals(
            MinimalistHudMetrics.LANDSCAPE_EDGE_PAD_DP +
                MinimalistHudMetrics.LANDSCAPE_RIGHT_RAIL_WIDTH_DP +
                MinimalistHudMetrics.LANDSCAPE_THUMB_GAP_DP,
            endPad,
            0.001f
        )
        assertTrue(endPad > MinimalistHudMetrics.LANDSCAPE_RIGHT_RAIL_WIDTH_DP)
    }

    @Test
    fun landscapePillarWidthIsSymmetricAndNonNegative() {
        // 920 x 414 viewport: 16:9 preview width = 414 * 16 / 9 = 736dp => 92dp pillar on each side
        val pillar = MinimalistHudMetrics.landscapePillarWidthDp(safeWidthDp = 920f, safeHeightDp = 414f)
        assertEquals(92f, pillar, 0.01f)
        assertEquals(0f, MinimalistHudMetrics.landscapePillarWidthDp(safeWidthDp = 600f, safeHeightDp = 414f), 0.001f)
    }

    @Test
    fun portraitClearViewfinderLeavesMajorityUnobstructed() {
        val clearH = MinimalistHudMetrics.portraitClearViewfinderHeightDp(safeHeightDp = 920f)
        assertEquals(640f, clearH, 0.001f)
        assertTrue(MinimalistHudMetrics.portraitClearRatio(safeHeightDp = 920f) > 0.65f)
        assertEquals(0f, MinimalistHudMetrics.portraitClearViewfinderHeightDp(safeHeightDp = 100f), 0.001f)
    }
}
