package com.wotagei.cam.ui

/**
 * Pure-Kotlin geometry calculations for the Minimalist Camera HUD (Landscape & Portrait).
 * Kept free of Android/Compose imports so all layout invariants can be verified in JVM unit tests.
 */
object MinimalistHudMetrics {
    const val LANDSCAPE_LEFT_RAIL_WIDTH_DP = 68f
    const val LANDSCAPE_RIGHT_RAIL_WIDTH_DP = 68f
    const val LANDSCAPE_READOUT_BAR_WIDTH_DP = 58f
    const val LANDSCAPE_READOUT_BAR_HEIGHT_DP = 306f
    const val LANDSCAPE_THUMB_GAP_DP = 12f
    const val LANDSCAPE_EDGE_PAD_DP = 12f

    const val PORTRAIT_TOP_ZONE_HEIGHT_DP = 96f
    const val PORTRAIT_BOTTOM_DECK_HEIGHT_DP = 184f

    /**
     * Computes the horizontal pillarbox width on each side of a centered 16:9 preview.
     * Returns 0f when the safe area is narrower than or equal to the 16:9 preview.
     */
    fun landscapePillarWidthDp(safeWidthDp: Float, safeHeightDp: Float): Float {
        if (safeWidthDp <= 0f || safeHeightDp <= 0f) return 0f
        val previewWidthDp = safeHeightDp * (16f / 9f)
        return ((safeWidthDp - previewWidthDp) / 2f).coerceAtLeast(0f)
    }

    /**
     * Computes the end-padding (distance from the safe area's right edge) for the Landscape
     * Right Vertical Readout Bar so it sits cleanly to the left of the right pillarbox rail
     * with [LANDSCAPE_THUMB_GAP_DP] clearance from the Record button.
     */
    fun landscapeReadoutEndPadDp(): Float =
        LANDSCAPE_EDGE_PAD_DP + LANDSCAPE_RIGHT_RAIL_WIDTH_DP + LANDSCAPE_THUMB_GAP_DP

    /**
     * Computes the remaining clear viewfinder height in Portrait between the top safe zone
     * and the bottom thumb deck.
     */
    fun portraitClearViewfinderHeightDp(safeHeightDp: Float): Float =
        (safeHeightDp - PORTRAIT_TOP_ZONE_HEIGHT_DP - PORTRAIT_BOTTOM_DECK_HEIGHT_DP).coerceAtLeast(0f)

    /**
     * Computes the fraction of the safe area height that remains unobstructed in Portrait.
     */
    fun portraitClearRatio(safeHeightDp: Float): Float {
        if (safeHeightDp <= 0f) return 0f
        return portraitClearViewfinderHeightDp(safeHeightDp) / safeHeightDp
    }
}
