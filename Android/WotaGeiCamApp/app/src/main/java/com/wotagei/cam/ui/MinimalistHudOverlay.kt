package com.wotagei.cam.ui

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wotagei.cam.R
import com.wotagei.cam.core.CamPill
import com.wotagei.cam.core.HudItem
import com.wotagei.cam.media.VideoThumbnail
import com.wotagei.cam.ui.design.HudInkLevel
import com.wotagei.cam.ui.design.WotaColor
import com.wotagei.cam.ui.design.WotaIconButton
import com.wotagei.cam.ui.design.WotaShape
import com.wotagei.cam.ui.design.WotaSpace
import com.wotagei.cam.ui.design.WotaStroke
import com.wotagei.cam.ui.design.WotaType
import com.wotagei.cam.ui.design.hudFrostPlate
import com.wotagei.cam.ui.design.wotaHudCard
import com.wotagei.cam.ui.widget.AttitudeCard
import com.wotagei.cam.ui.widget.AudioChip

private fun MinimalistReadoutSlot.toHudItem(): HudItem = when (this) {
    MinimalistReadoutSlot.SHUTTER -> HudItem.SHUTTER
    MinimalistReadoutSlot.ISO -> HudItem.ISO
    MinimalistReadoutSlot.EV -> HudItem.EV
    MinimalistReadoutSlot.WB -> HudItem.WB
    MinimalistReadoutSlot.ZOOM -> HudItem.ZOOM
    MinimalistReadoutSlot.FPS -> HudItem.FPS
}

/**
 * Minimalist Camera HUD matching the Figcraft `Landscape` & `Portrait` designs.
 * Reuses [HudCtx], [WotaColor], [RecordButton], [AudioChip], and [AttitudeCard],
 * and reports pill anchors via [HudCtx.anchorOf] so all existing [PillHost] popups work unchanged.
 */
@Composable
fun BoxScope.MinimalistHudOverlay(
    ctx: HudCtx,
    isLandscape: Boolean,
    topBarH: Int,
    visibleEntries: Set<HudEntry>,
    activePop: PillKey? = null,
    hudOrder: MinimalistHudOrder = MinimalistHudOrder.DEFAULT,
    modifier: Modifier = Modifier
) {
    val onOpenFx: () -> Unit = {
        when {
            ctx.echoActive -> ctx.onEchoClick()
            ctx.toneActive -> ctx.onToneClick()
            else -> ctx.onGlowClick()
        }
    }

    Box(modifier.matchParentSize()) {
        if (isLandscape) {
            // Landscape Top-Left: Telemetry + AudioChip (plenty of horizontal width before TopEnd buttons)
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = HudEdgePad, top = TopTopPad),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WotaSpace.xs)
            ) {
                MinimalistTelemetryPill(ctx)
                if (HudEntry.of(CamPill.BT) in visibleEntries) {
                    AudioChip(
                        btConnected = ctx.btConnected,
                        volumePct = ctx.btVolumePct,
                        captureActive = ctx.captureActive,
                        onClick = ctx.onBtClick,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.BT))
                    )
                }
            }
            LandscapeMinimalistChrome(
                ctx = ctx,
                topBarH = topBarH,
                visibleEntries = visibleEntries,
                activePop = activePop,
                hudOrder = hudOrder,
                onOpenFx = onOpenFx
            )
        } else {
            PortraitMinimalistChrome(
                ctx = ctx,
                topBarH = topBarH,
                visibleEntries = visibleEntries,
                activePop = activePop,
                hudOrder = hudOrder,
                onOpenFx = onOpenFx
            )
        }
    }
}

@Composable
private fun BoxScope.LandscapeMinimalistChrome(
    ctx: HudCtx,
    topBarH: Int,
    visibleEntries: Set<HudEntry>,
    activePop: PillKey?,
    hudOrder: MinimalistHudOrder,
    onOpenFx: () -> Unit
) {
    // Left Pillarbox Rail (Creative & Monitoring)
    Column(
        Modifier
            .align(Alignment.CenterStart)
            .padding(start = MinimalistHudMetrics.LANDSCAPE_EDGE_PAD_DP.dp, top = (topBarH / 2).dp)
            .wotaHudCard(
                shape = RoundedCornerShape(16.dp),
                frost = hudFrostPlate(radiusPx = 16f, ink = HudInkLevel.SECONDARY)
            )
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        hudOrder.creative.forEach { slot ->
            when (slot) {
                MinimalistCreativeSlot.REFLINE -> if (HudEntry.of(CamPill.REFLINE) in visibleEntries) {
                    WotaIconButton(
                        image = Icons.Filled.GridOn,
                        description = stringResource(R.string.cam_p_refline),
                        selected = ctx.refLineOn || activePop == PillKey.REFLINE,
                        onClick = ctx.onRefLineClick,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.REFLINE))
                    )
                }
                MinimalistCreativeSlot.MONITOR -> if (HudEntry.of(CamPill.MONITOR) in visibleEntries) {
                    MiniPillButton(
                        label = ctx.monitorLabel,
                        selected = ctx.monitorActive || activePop == PillKey.MONITOR,
                        onClick = ctx.onMonitorClick,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.MONITOR))
                    )
                }
                MinimalistCreativeSlot.CURVE -> if (HudEntry.of(CamPill.CURVE) in visibleEntries) {
                    MiniPillButton(
                        label = stringResource(R.string.cam_p_curve),
                        selected = ctx.curveOn,
                        onClick = ctx.onCurveClick,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.CURVE))
                    )
                }
                MinimalistCreativeSlot.FX -> {
                    val fxVisible = HudEntry.of(CamPill.GLOW) in visibleEntries ||
                        HudEntry.of(CamPill.ECHO) in visibleEntries ||
                        HudEntry.of(CamPill.TONE) in visibleEntries
                    if (fxVisible) {
                        val fxOpen = activePop == PillKey.GLOW || activePop == PillKey.ECHO || activePop == PillKey.TONE
                        MiniPillButton(
                            label = "FX",
                            selected = fxOpen || ctx.glowActive || ctx.echoActive || ctx.toneActive,
                            enabled = ctx.glowUsable || ctx.echoUsable || ctx.toneUsable,
                            onClick = onOpenFx,
                            modifier = ctx.anchorOf(HudEntry.of(CamPill.GLOW))
                                .then(ctx.anchorOf(HudEntry.of(CamPill.ECHO)))
                                .then(ctx.anchorOf(HudEntry.of(CamPill.TONE)))
                        )
                    }
                }
                MinimalistCreativeSlot.DENOISE -> if (HudEntry.of(CamPill.DENOISE) in visibleEntries) {
                    MiniPillButton(
                        label = ctx.denoiseLabel,
                        selected = ctx.denoiseActive || activePop == PillKey.DENOISE,
                        onClick = ctx.onDenoiseClick,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.DENOISE))
                    )
                }
                MinimalistCreativeSlot.FLASH -> if (HudEntry.of(CamPill.FLASH) in visibleEntries) {
                    WotaIconButton(
                        image = flashIcon(ctx.flash),
                        description = stringResource(R.string.cam_p_flash),
                        selected = ctx.flash != com.wotagei.cam.core.Flash.OFF || activePop == PillKey.FLASH,
                        onClick = ctx.onFlashClick,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.FLASH))
                            .alpha(if (ctx.flashUsable) 1f else 0.45f)
                    )
                }
            }
        }
    }

    // Bottom-Left Gallery Thumb
    MiniGalleryButton(
        lastUri = ctx.lastUri,
        onClick = ctx.onThumbClick,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = MinimalistHudMetrics.LANDSCAPE_EDGE_PAD_DP.dp, bottom = WotaSpace.s)
    )

    // Right Vertical Readout Bar (next to Record Button, separated by thumb gap)
    Column(
        Modifier
            .align(Alignment.CenterEnd)
            .padding(end = MinimalistHudMetrics.landscapeReadoutEndPadDp().dp)
            .width(MinimalistHudMetrics.LANDSCAPE_READOUT_BAR_WIDTH_DP.dp)
            .wotaHudCard(
                shape = RoundedCornerShape(16.dp),
                frost = hudFrostPlate(radiusPx = 16f, ink = HudInkLevel.SECONDARY)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        hudOrder.readout.map { it.toHudItem() }.forEach { item ->
            val rawValue = if (HudEntry.of(item) in visibleEntries) ctx.readoutValue(item) else null
            if (rawValue != null) {
                val aeLockedItem = ctx.aeLocked && (item == HudItem.SHUTTER || item == HudItem.ISO)
                val value = if (aeLockedItem) "LOCK" else rawValue
                val locked = ctx.recording && item !in setOf(
                    HudItem.ISO, HudItem.EV, HudItem.WB, HudItem.ZOOM
                )
                VerticalReadoutSlot(
                    label = stringResource(item.labelRes),
                    value = value,
                    locked = locked,
                    active = activePop == item.pillKey(),
                    accent = item == HudItem.EV || item == HudItem.WB,
                    warn = aeLockedItem,
                    onClick = { ctx.onReadoutOpen(item) },
                    modifier = ctx.anchorOf(HudEntry.of(item))
                )
            }
        }
    }

    // Right Pillarbox Rail (Attitude, Lens, Record Button, Focus, Stabilization)
    Column(
        Modifier
            .align(Alignment.CenterEnd)
            .padding(end = MinimalistHudMetrics.LANDSCAPE_EDGE_PAD_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (ctx.levelEnabled) {
            AttitudeCard(roll = ctx.roll, pitch = ctx.pitch, enabled = true, card = false)
        }
        val firstControl = hudOrder.controls.firstOrNull()
        val afterControls = hudOrder.controls.drop(1)
        val renderControl: @Composable (MinimalistControlSlot) -> Unit = { slot ->
            when (slot) {
                MinimalistControlSlot.LENS -> if (HudEntry.of(CamPill.LENS) in visibleEntries) {
                    MiniPillButton(
                        label = ctx.lensLabel,
                        selected = false,
                        onClick = ctx.onOpenLensPanel,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.LENS))
                    )
                }
                MinimalistControlSlot.FOCUS -> if (HudEntry.of(CamPill.FOCUS) in visibleEntries) {
                    MiniPillButton(
                        label = ctx.focusLabel,
                        selected = ctx.focusActive || activePop == PillKey.FOCUS,
                        enabled = ctx.focusUsable,
                        onClick = ctx.onFocusClick,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.FOCUS))
                    )
                }
                MinimalistControlSlot.STAB -> if (HudEntry.of(CamPill.STAB) in visibleEntries) {
                    MiniPillButton(
                        label = ctx.stabLabel,
                        selected = ctx.stabActive || activePop == PillKey.STAB,
                        onClick = ctx.onStabClick,
                        modifier = ctx.anchorOf(HudEntry.of(CamPill.STAB))
                    )
                }
            }
        }
        if (firstControl != null) renderControl(firstControl)
        RecordButton(
            recording = ctx.recording,
            busy = ctx.busy,
            onClick = ctx.onRecordClick
        )
        afterControls.forEach { renderControl(it) }
    }
}

@Composable
private fun BoxScope.PortraitMinimalistChrome(
    ctx: HudCtx,
    topBarH: Int,
    visibleEntries: Set<HudEntry>,
    activePop: PillKey?,
    hudOrder: MinimalistHudOrder,
    onOpenFx: () -> Unit
) {
    // Portrait Row 1 (TopStart): compact AudioChip only — leaves full space for the 4 TopEnd circular buttons
    if (HudEntry.of(CamPill.BT) in visibleEntries) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = HudEdgePad, top = TopTopPad + 4.dp)
        ) {
            AudioChip(
                btConnected = ctx.btConnected,
                volumePct = ctx.btVolumePct,
                captureActive = ctx.captureActive,
                onClick = ctx.onBtClick,
                modifier = ctx.anchorOf(HudEntry.of(CamPill.BT))
            )
        }
    }

    // Portrait Row 2 (below TopChrome): Telemetry Pill on left + AttitudeCard on right (zero overlap)
    Row(
        Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(start = HudEdgePad, end = HudEdgePad, top = (topBarH + 4).dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        MinimalistTelemetryPill(ctx)
        if (ctx.levelEnabled) {
            AttitudeCard(roll = ctx.roll, pitch = ctx.pitch, enabled = true, card = true)
        }
    }

    // Portrait Bottom Thumb Deck (3 rows: Creative Rail + Pro Readout Strip + Primary Shutter Row)
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = WotaSpace.s, vertical = WotaSpace.s),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Creative Rail
        Row(
            Modifier
                .fillMaxWidth()
                .wotaHudCard(
                    shape = RoundedCornerShape(16.dp),
                    frost = hudFrostPlate(radiusPx = 16f, ink = HudInkLevel.SECONDARY)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            hudOrder.creative.forEach { slot ->
                when (slot) {
                    MinimalistCreativeSlot.REFLINE -> if (HudEntry.of(CamPill.REFLINE) in visibleEntries) {
                        MiniPillButton(
                            label = stringResource(R.string.cam_refline_grid),
                            selected = ctx.refLineOn || activePop == PillKey.REFLINE,
                            onClick = ctx.onRefLineClick,
                            modifier = ctx.anchorOf(HudEntry.of(CamPill.REFLINE))
                        )
                    }
                    MinimalistCreativeSlot.MONITOR -> if (HudEntry.of(CamPill.MONITOR) in visibleEntries) {
                        MiniPillButton(
                            label = ctx.monitorLabel,
                            selected = ctx.monitorActive || activePop == PillKey.MONITOR,
                            onClick = ctx.onMonitorClick,
                            modifier = ctx.anchorOf(HudEntry.of(CamPill.MONITOR))
                        )
                    }
                    MinimalistCreativeSlot.CURVE -> if (HudEntry.of(CamPill.CURVE) in visibleEntries) {
                        MiniPillButton(
                            label = stringResource(R.string.cam_p_curve),
                            selected = ctx.curveOn,
                            onClick = ctx.onCurveClick,
                            modifier = ctx.anchorOf(HudEntry.of(CamPill.CURVE))
                        )
                    }
                    MinimalistCreativeSlot.FX -> {
                        val fxVisible = HudEntry.of(CamPill.GLOW) in visibleEntries ||
                            HudEntry.of(CamPill.ECHO) in visibleEntries ||
                            HudEntry.of(CamPill.TONE) in visibleEntries
                        if (fxVisible) {
                            val fxOpen = activePop == PillKey.GLOW || activePop == PillKey.ECHO || activePop == PillKey.TONE
                            MiniPillButton(
                                label = "FX",
                                selected = fxOpen || ctx.glowActive || ctx.echoActive || ctx.toneActive,
                                enabled = ctx.glowUsable || ctx.echoUsable || ctx.toneUsable,
                                onClick = onOpenFx,
                                modifier = ctx.anchorOf(HudEntry.of(CamPill.GLOW))
                                    .then(ctx.anchorOf(HudEntry.of(CamPill.ECHO)))
                                    .then(ctx.anchorOf(HudEntry.of(CamPill.TONE)))
                            )
                        }
                    }
                    MinimalistCreativeSlot.DENOISE -> if (HudEntry.of(CamPill.DENOISE) in visibleEntries) {
                        MiniPillButton(
                            label = ctx.denoiseLabel,
                            selected = ctx.denoiseActive || activePop == PillKey.DENOISE,
                            onClick = ctx.onDenoiseClick,
                            modifier = ctx.anchorOf(HudEntry.of(CamPill.DENOISE))
                        )
                    }
                    MinimalistCreativeSlot.FLASH -> Unit
                }
            }
        }

        // Row 2: Horizontal Pro Readout Strip
        Row(
            Modifier
                .fillMaxWidth()
                .wotaHudCard(
                    shape = RoundedCornerShape(14.dp),
                    frost = hudFrostPlate(radiusPx = 14f, ink = HudInkLevel.SECONDARY)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            hudOrder.readout.map { it.toHudItem() }.forEach { item ->
                val rawValue = if (HudEntry.of(item) in visibleEntries) ctx.readoutValue(item) else null
                if (rawValue != null) {
                    val aeLockedItem = ctx.aeLocked && (item == HudItem.SHUTTER || item == HudItem.ISO)
                    val value = if (aeLockedItem) "LOCK" else rawValue
                    val locked = ctx.recording && item !in setOf(
                        HudItem.ISO, HudItem.EV, HudItem.WB, HudItem.ZOOM
                    )
                    HorizontalReadoutChip(
                        label = stringResource(item.labelRes),
                        value = value,
                        locked = locked,
                        active = activePop == item.pillKey(),
                        warn = aeLockedItem,
                        onClick = { ctx.onReadoutOpen(item) },
                        modifier = ctx.anchorOf(HudEntry.of(item))
                    )
                }
            }
        }

        // Row 3: Primary Shutter Row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniGalleryButton(lastUri = ctx.lastUri, onClick = ctx.onThumbClick)
            val firstControl = hudOrder.controls.firstOrNull()
            val afterControls = hudOrder.controls.drop(1)
            val renderControl: @Composable (MinimalistControlSlot) -> Unit = { slot ->
                when (slot) {
                    MinimalistControlSlot.LENS -> if (HudEntry.of(CamPill.LENS) in visibleEntries) {
                        MiniPillButton(
                            label = ctx.lensLabel,
                            selected = false,
                            onClick = ctx.onOpenLensPanel,
                            modifier = ctx.anchorOf(HudEntry.of(CamPill.LENS))
                        )
                    }
                    MinimalistControlSlot.FOCUS -> if (HudEntry.of(CamPill.FOCUS) in visibleEntries) {
                        MiniPillButton(
                            label = ctx.focusLabel,
                            selected = ctx.focusActive,
                            enabled = ctx.focusUsable,
                            onClick = ctx.onFocusClick,
                            modifier = ctx.anchorOf(HudEntry.of(CamPill.FOCUS))
                        )
                    }
                    MinimalistControlSlot.STAB -> if (HudEntry.of(CamPill.STAB) in visibleEntries) {
                        MiniPillButton(
                            label = ctx.stabLabel,
                            selected = ctx.stabActive,
                            onClick = ctx.onStabClick,
                            modifier = ctx.anchorOf(HudEntry.of(CamPill.STAB))
                        )
                    }
                }
            }
            if (firstControl != null) renderControl(firstControl)
            RecordButton(recording = ctx.recording, busy = ctx.busy, onClick = ctx.onRecordClick)
            afterControls.forEach { renderControl(it) }
        }
    }
}

@Composable
private fun MinimalistTelemetryPill(ctx: HudCtx) {
    Row(
        Modifier
            .wotaHudCard(
                shape = WotaShape.pill,
                frost = hudFrostPlate(radiusPx = -1f, ink = HudInkLevel.SECONDARY)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (ctx.recording) {
            Box(Modifier.size(7.dp).background(WotaColor.rec, CircleShape))
            Text(
                text = ctx.recStateLabel ?: ctx.elapsedLabel,
                style = WotaType.mono.copy(fontSize = 11.sp),
                color = WotaColor.rec
            )
        }
        Text(
            text = ctx.sizeLabel,
            style = WotaType.caption.copy(fontWeight = FontWeight.Medium),
            color = WotaColor.textHi,
            modifier = ctx.anchorOf(HudEntry.of(CamPill.SIZE)).clickable(onClick = ctx.onSizeClick)
        )
        Text(text = "|", style = WotaType.caption, color = WotaColor.textLo)
        Text(
            text = ctx.capacityLabel,
            style = WotaType.caption,
            color = if (ctx.freeLow) WotaColor.warn else WotaColor.textMid,
            modifier = ctx.anchorOf(HudEntry.of(CamPill.STORAGE)).clickable(onClick = ctx.onFreeClick)
        )
        if (ctx.aeLocked) {
            Text(text = "|", style = WotaType.caption, color = WotaColor.textLo)
            Text(
                text = stringResource(R.string.cam_ae_lock),
                style = WotaType.mono.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = WotaColor.warn
            )
        }
    }
}

@Composable
private fun VerticalReadoutSlot(
    label: String,
    value: String,
    locked: Boolean,
    active: Boolean = false,
    accent: Boolean,
    warn: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (active) Modifier.background(WotaColor.accentSurface) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (locked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = WotaColor.textLo,
                    modifier = Modifier.size(8.dp)
                )
            }
            Text(
                text = label.uppercase(),
                fontSize = 8.sp,
                lineHeight = 10.sp,
                color = if (active) WotaColor.onAccent else WotaColor.textLo,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        Text(
            text = value,
            style = WotaType.mono.copy(fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold),
            color = when {
                active -> WotaColor.onAccent
                locked -> WotaColor.textLo
                warn -> WotaColor.warn
                accent -> WotaColor.accent
                else -> WotaColor.textHi
            },
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun HorizontalReadoutChip(
    label: String,
    value: String,
    locked: Boolean,
    active: Boolean = false,
    warn: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (active) Modifier.background(WotaColor.accentSurface) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            lineHeight = 10.sp,
            color = if (active) WotaColor.onAccent else WotaColor.textLo,
            maxLines = 1
        )
        Text(
            text = if (locked) "🔒$value" else value,
            style = WotaType.mono.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold),
            color = when {
                active -> WotaColor.onAccent
                locked -> WotaColor.textLo
                warn -> WotaColor.warn
                else -> WotaColor.textHi
            },
            maxLines = 1
        )
    }
}

@Composable
private fun MiniPillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(WotaShape.pill)
            .background(if (selected) WotaColor.accentSurface else WotaColor.layer)
            .border(WotaStroke.hairline, WotaColor.outline, WotaShape.pill)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) WotaColor.onAccent else WotaColor.textHi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniGalleryButton(
    lastUri: android.net.Uri?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WotaColor.surface)
            .border(WotaStroke.hairline, WotaColor.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (lastUri != null) {
            VideoThumbnail(uri = lastUri, modifier = Modifier.size(40.dp))
        } else {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = stringResource(R.string.cam_gallery_entry),
                tint = WotaColor.textMid,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
