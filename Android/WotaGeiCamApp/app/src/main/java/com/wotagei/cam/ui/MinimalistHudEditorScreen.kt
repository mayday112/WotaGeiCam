package com.wotagei.cam.ui

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wotagei.cam.R
import com.wotagei.cam.core.CamPill
import com.wotagei.cam.core.HudItem
import com.wotagei.cam.ui.design.WotaColor
import com.wotagei.cam.ui.design.WotaShape
import com.wotagei.cam.ui.design.WotaStroke
import com.wotagei.cam.ui.design.WotaType

private data class EditorSnapshot(
    val order: MinimalistHudOrder,
    val hudMask: Int,
    val pillMask: Int
)

private sealed interface EditorSelection {
    data class Creative(val slot: MinimalistCreativeSlot) : EditorSelection
    data class Readout(val slot: MinimalistReadoutSlot) : EditorSelection
    data class Control(val slot: MinimalistControlSlot) : EditorSelection
    data class ToggleOnly(val pill: CamPill) : EditorSelection
}

@Composable
fun MinimalistHudEditorScreen(
    prefs: SharedPreferences,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var order by remember { mutableStateOf(WotaSettings.hudMinimalOrder(prefs)) }
    var hudMask by remember { mutableIntStateOf(WotaSettings.hudItems(prefs)) }
    var pillMask by remember { mutableIntStateOf(WotaSettings.hudPills(prefs)) }
    var undoStack by remember { mutableStateOf<List<EditorSnapshot>>(emptyList()) }
    var selection by remember { mutableStateOf<EditorSelection?>(null) }
    var resetArmed by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    fun pushUndo() {
        undoStack = (undoStack + EditorSnapshot(order, hudMask, pillMask)).takeLast(30)
        resetArmed = false
        statusText = null
    }

    fun isSlotVisible(sel: EditorSelection): Boolean = when (sel) {
        is EditorSelection.Creative -> when (sel.slot) {
            MinimalistCreativeSlot.REFLINE -> (pillMask and CamPill.REFLINE.bit) != 0
            MinimalistCreativeSlot.MONITOR -> (pillMask and CamPill.MONITOR.bit) != 0
            MinimalistCreativeSlot.CURVE -> (pillMask and CamPill.CURVE.bit) != 0
            MinimalistCreativeSlot.FX -> (pillMask and (CamPill.GLOW.bit or CamPill.ECHO.bit or CamPill.TONE.bit)) != 0
            MinimalistCreativeSlot.DENOISE -> (pillMask and CamPill.DENOISE.bit) != 0
            MinimalistCreativeSlot.FLASH -> (pillMask and CamPill.FLASH.bit) != 0
        }
        is EditorSelection.Readout -> {
            val item = sel.slot.toHudItem()
            (hudMask and item.bit) != 0
        }
        is EditorSelection.Control -> {
            val pill = sel.slot.toCamPill()
            (pillMask and pill.bit) != 0
        }
        is EditorSelection.ToggleOnly -> (pillMask and sel.pill.bit) != 0
    }

    fun toggleVisibility(sel: EditorSelection) {
        pushUndo()
        when (sel) {
            is EditorSelection.Creative -> when (sel.slot) {
                MinimalistCreativeSlot.REFLINE -> pillMask = pillMask xor CamPill.REFLINE.bit
                MinimalistCreativeSlot.MONITOR -> pillMask = pillMask xor CamPill.MONITOR.bit
                MinimalistCreativeSlot.CURVE -> pillMask = pillMask xor CamPill.CURVE.bit
                MinimalistCreativeSlot.FX -> {
                    val fxBits = CamPill.GLOW.bit or CamPill.ECHO.bit or CamPill.TONE.bit
                    pillMask = if ((pillMask and fxBits) != 0) pillMask and fxBits.inv() else pillMask or CamPill.GLOW.bit
                }
                MinimalistCreativeSlot.DENOISE -> pillMask = pillMask xor CamPill.DENOISE.bit
                MinimalistCreativeSlot.FLASH -> pillMask = pillMask xor CamPill.FLASH.bit
            }
            is EditorSelection.Readout -> {
                hudMask = hudMask xor sel.slot.toHudItem().bit
            }
            is EditorSelection.Control -> {
                pillMask = pillMask xor sel.slot.toCamPill().bit
            }
            is EditorSelection.ToggleOnly -> {
                pillMask = pillMask xor sel.pill.bit
            }
        }
    }

    fun moveSelected(delta: Int) {
        val sel = selection ?: return
        when (sel) {
            is EditorSelection.Creative -> {
                val idx = order.creative.indexOf(sel.slot)
                if (idx >= 0) {
                    val next = order.creative.moveSlot(idx, delta)
                    if (next != order.creative) {
                        pushUndo()
                        order = order.copy(creative = next)
                    }
                }
            }
            is EditorSelection.Readout -> {
                val idx = order.readout.indexOf(sel.slot)
                if (idx >= 0) {
                    val next = order.readout.moveSlot(idx, delta)
                    if (next != order.readout) {
                        pushUndo()
                        order = order.copy(readout = next)
                    }
                }
            }
            is EditorSelection.Control -> {
                val idx = order.controls.indexOf(sel.slot)
                if (idx >= 0) {
                    val next = order.controls.moveSlot(idx, delta)
                    if (next != order.controls) {
                        pushUndo()
                        order = order.copy(controls = next)
                    }
                }
            }
            is EditorSelection.ToggleOnly -> Unit
        }
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(WotaColor.bg)
            .safeDrawingPadding()
    ) {
        val isLandscape = maxWidth > maxHeight
        // Simulated 16:9 viewfinder backdrop matching Figcraft frames 41 & P32
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(if (isLandscape) 0.82f else 1f)
                .height(if (isLandscape) maxHeight else maxHeight * 0.72f)
                .background(Color(0xFF1E1B18))
        )

        if (isLandscape) {
            // Landscape Top Bar Zone
            EditorZoneCard(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 104.dp, top = 10.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⠿ Top Bar:",
                        style = WotaType.caption,
                        color = WotaColor.textMid
                    )
                    EditorSlotPill(
                        label = stringResource(R.string.cam_pill_size),
                        visible = isSlotVisible(EditorSelection.ToggleOnly(CamPill.SIZE)),
                        selected = selection == EditorSelection.ToggleOnly(CamPill.SIZE),
                        onClick = { selection = EditorSelection.ToggleOnly(CamPill.SIZE) },
                        onDoubleClick = { toggleVisibility(EditorSelection.ToggleOnly(CamPill.SIZE)) }
                    )
                    EditorSlotPill(
                        label = stringResource(R.string.cam_pill_storage),
                        visible = isSlotVisible(EditorSelection.ToggleOnly(CamPill.STORAGE)),
                        selected = selection == EditorSelection.ToggleOnly(CamPill.STORAGE),
                        onClick = { selection = EditorSelection.ToggleOnly(CamPill.STORAGE) },
                        onDoubleClick = { toggleVisibility(EditorSelection.ToggleOnly(CamPill.STORAGE)) }
                    )
                    EditorSlotPill(
                        label = stringResource(R.string.cam_pill_bt),
                        visible = isSlotVisible(EditorSelection.ToggleOnly(CamPill.BT)),
                        selected = selection == EditorSelection.ToggleOnly(CamPill.BT),
                        onClick = { selection = EditorSelection.ToggleOnly(CamPill.BT) },
                        onDoubleClick = { toggleVisibility(EditorSelection.ToggleOnly(CamPill.BT)) }
                    )
                }
            }

            // Left Creative Rail
            EditorZoneCard(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp, top = 20.dp)
                    .width(84.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    order.creative.forEachIndexed { idx, slot ->
                        val sel = EditorSelection.Creative(slot)
                        EditorSlotRow(
                            prefix = "0.$idx",
                            label = slot.label(),
                            visible = isSlotVisible(sel),
                            selected = selection == sel,
                            onClick = { selection = sel },
                            onDoubleClick = { toggleVisibility(sel) }
                        )
                    }
                }
            }

            // Right Persistent Readouts + Right Controls Rail
            Row(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorZoneCard(Modifier.width(82.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("⠿ Readouts", fontSize = 9.sp, color = WotaColor.textMid)
                        order.readout.forEachIndexed { idx, slot ->
                            val sel = EditorSelection.Readout(slot)
                            EditorSlotRow(
                                prefix = "0.$idx",
                                label = slot.label(),
                                visible = isSlotVisible(sel),
                                selected = selection == sel,
                                onClick = { selection = sel },
                                onDoubleClick = { toggleVisibility(sel) }
                            )
                        }
                    }
                }
                EditorZoneCard(Modifier.width(84.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val levelSel = EditorSelection.ToggleOnly(CamPill.LEVEL)
                        EditorSlotRow(
                            prefix = "•",
                            label = stringResource(R.string.cam_pill_level),
                            visible = isSlotVisible(levelSel),
                            selected = selection == levelSel,
                            onClick = { selection = levelSel },
                            onDoubleClick = { toggleVisibility(levelSel) }
                        )
                        order.controls.forEachIndexed { idx, slot ->
                            val sel = EditorSelection.Control(slot)
                            EditorSlotRow(
                                prefix = "0.$idx",
                                label = slot.label(),
                                visible = isSlotVisible(sel),
                                selected = selection == sel,
                                onClick = { selection = sel },
                                onDoubleClick = { toggleVisibility(sel) }
                            )
                        }
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(WotaColor.rec.copy(alpha = 0.85f))
                        )
                    }
                }
            }
        } else {
            // Portrait Top Bar Zone (P32)
            EditorZoneCard(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⠿ Top:", style = WotaType.caption, color = WotaColor.textMid)
                    listOf(CamPill.SIZE, CamPill.STORAGE, CamPill.BT, CamPill.LEVEL).forEach { pill ->
                        val sel = EditorSelection.ToggleOnly(pill)
                        EditorSlotPill(
                            label = stringResource(pill.labelRes),
                            visible = isSlotVisible(sel),
                            selected = selection == sel,
                            onClick = { selection = sel },
                            onDoubleClick = { toggleVisibility(sel) }
                        )
                    }
                }
            }

            // Portrait Creative + Controls Rows
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 82.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EditorZoneCard(Modifier.width(104.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        order.creative.forEachIndexed { idx, slot ->
                            val sel = EditorSelection.Creative(slot)
                            EditorSlotRow(
                                prefix = "0.$idx",
                                label = slot.label(),
                                visible = isSlotVisible(sel),
                                selected = selection == sel,
                                onClick = { selection = sel },
                                onDoubleClick = { toggleVisibility(sel) }
                            )
                        }
                    }
                }
                EditorZoneCard(Modifier.width(104.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        order.controls.forEachIndexed { idx, slot ->
                            val sel = EditorSelection.Control(slot)
                            EditorSlotRow(
                                prefix = "0.$idx",
                                label = slot.label(),
                                visible = isSlotVisible(sel),
                                selected = selection == sel,
                                onClick = { selection = sel },
                                onDoubleClick = { toggleVisibility(sel) }
                            )
                        }
                    }
                }
            }

            // Portrait Persistent Readouts Strip (P32)
            EditorZoneCard(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⠿", style = WotaType.caption, color = WotaColor.textMid)
                    order.readout.forEach { slot ->
                        val sel = EditorSelection.Readout(slot)
                        EditorSlotPill(
                            label = slot.label(),
                            visible = isSlotVisible(sel),
                            selected = selection == sel,
                            onClick = { selection = sel },
                            onDoubleClick = { toggleVisibility(sel) }
                        )
                    }
                }
            }
        }

        // Center Floating HUD Editor Action Bar (Frames 41 & P32)
        val barShape = RoundedCornerShape(22.dp)
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .width(if (isLandscape) 456.dp else 360.dp)
                .clip(barShape)
                .background(WotaColor.surface.copy(alpha = 0.95f))
                .border(WotaStroke.hairline, WotaColor.acrylicBorder, barShape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = statusText ?: stringResource(R.string.set_hud_editor_hint),
                style = MaterialTheme.typography.bodySmall,
                color = WotaColor.textMid,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val sel = selection
            if (sel != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val vis = isSlotVisible(sel)
                    ActionTextButton(
                        label = stringResource(if (vis) R.string.set_hud_editor_hide else R.string.set_hud_editor_show),
                        color = if (vis) WotaColor.warn else WotaColor.pullOut,
                        onClick = { toggleVisibility(sel) }
                    )
                    if (sel !is EditorSelection.ToggleOnly) {
                        ActionTextButton(
                            label = "◀ ${stringResource(R.string.set_hud_editor_move_up)}",
                            color = WotaColor.textHi,
                            onClick = { moveSelected(-1) }
                        )
                        ActionTextButton(
                            label = "${stringResource(R.string.set_hud_editor_move_down)} ▶",
                            color = WotaColor.textHi,
                            onClick = { moveSelected(1) }
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionTextButton(
                    label = stringResource(R.string.hud_edit_back),
                    color = WotaColor.textHi,
                    onClick = onBack
                )
                ActionTextButton(
                    label = stringResource(R.string.hud_edit_undo),
                    color = if (undoStack.isNotEmpty()) WotaColor.accent else WotaColor.textLo,
                    enabled = undoStack.isNotEmpty(),
                    onClick = {
                        val prev = undoStack.lastOrNull() ?: return@ActionTextButton
                        undoStack = undoStack.dropLast(1)
                        order = prev.order
                        hudMask = prev.hudMask
                        pillMask = prev.pillMask
                        resetArmed = false
                        statusText = context.getString(R.string.hud_edit_undo_done)
                    }
                )
                ActionTextButton(
                    label = stringResource(if (resetArmed) R.string.hud_edit_reset_confirm else R.string.hud_edit_reset),
                    color = if (resetArmed) WotaColor.warn else WotaColor.accent,
                    onClick = {
                        if (!resetArmed) {
                            resetArmed = true
                            statusText = context.getString(R.string.hud_edit_reset_arm_tip)
                        } else {
                            pushUndo()
                            order = MinimalistHudOrder.DEFAULT
                            hudMask = HudItem.DEFAULT_MASK
                            pillMask = CamPill.DEFAULT_MASK
                            resetArmed = false
                            statusText = context.getString(R.string.hud_edit_reset_done)
                        }
                    }
                )
                Box(
                    Modifier
                        .clip(WotaShape.pill)
                        .background(WotaColor.accentSurface)
                        .clickable {
                            WotaSettings.setHudMinimalOrder(prefs, order)
                            prefs.edit()
                                .putInt(WotaSettings.KEY_HUD_ITEMS, hudMask)
                                .putInt(WotaSettings.KEY_HUD_PILLS, pillMask)
                                .putInt(WotaSettings.KEY_HUD_PILLS_SCHEMA, CamPill.MASK_SCHEMA)
                                .commit()
                            Toast.makeText(context, R.string.hud_edit_saved, Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.hud_edit_save),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = WotaColor.onAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorZoneCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .clip(shape)
            .background(WotaColor.hudScrim)
            .border(1.5.dp, WotaColor.accent, shape)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditorSlotRow(
    prefix: String,
    label: String,
    visible: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (visible) 1f else 0.38f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) WotaColor.accentSurface.copy(alpha = 0.45f) else WotaColor.layer.copy(alpha = 0.65f))
            .then(
                if (selected) Modifier.border(WotaStroke.hairline, WotaColor.textHi, RoundedCornerShape(8.dp))
                else Modifier
            )
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$prefix $label" + if (visible) "" else " 👻",
            fontSize = 9.sp,
            lineHeight = 11.sp,
            color = WotaColor.textHi,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditorSlotPill(
    label: String,
    visible: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    Box(
        Modifier
            .alpha(if (visible) 1f else 0.38f)
            .clip(WotaShape.pill)
            .background(if (selected) WotaColor.accentSurface else WotaColor.layer)
            .border(WotaStroke.hairline, if (selected) WotaColor.textHi else WotaColor.outline, WotaShape.pill)
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label + if (visible) "" else " 👻",
            fontSize = 10.sp,
            color = if (selected) WotaColor.onAccent else WotaColor.textHi,
            maxLines = 1
        )
    }
}

@Composable
private fun ActionTextButton(
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

private fun MinimalistReadoutSlot.toHudItem(): HudItem = when (this) {
    MinimalistReadoutSlot.SHUTTER -> HudItem.SHUTTER
    MinimalistReadoutSlot.ISO -> HudItem.ISO
    MinimalistReadoutSlot.EV -> HudItem.EV
    MinimalistReadoutSlot.WB -> HudItem.WB
    MinimalistReadoutSlot.ZOOM -> HudItem.ZOOM
    MinimalistReadoutSlot.FPS -> HudItem.FPS
}

private fun MinimalistControlSlot.toCamPill(): CamPill = when (this) {
    MinimalistControlSlot.LENS -> CamPill.LENS
    MinimalistControlSlot.FOCUS -> CamPill.FOCUS
    MinimalistControlSlot.STAB -> CamPill.STAB
}

@Composable
private fun MinimalistCreativeSlot.label(): String = when (this) {
    MinimalistCreativeSlot.REFLINE -> stringResource(R.string.cam_pill_refline)
    MinimalistCreativeSlot.MONITOR -> stringResource(R.string.cam_pill_monitor)
    MinimalistCreativeSlot.CURVE -> stringResource(R.string.cam_pill_curve)
    MinimalistCreativeSlot.FX -> "FX"
    MinimalistCreativeSlot.DENOISE -> stringResource(R.string.cam_pill_denoise)
    MinimalistCreativeSlot.FLASH -> stringResource(R.string.cam_pill_flash)
}

@Composable
private fun MinimalistReadoutSlot.label(): String = stringResource(toHudItem().labelRes)

@Composable
private fun MinimalistControlSlot.label(): String = stringResource(toCamPill().labelRes)
