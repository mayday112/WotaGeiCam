package com.wotagei.cam.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.wotagei.cam.R
import com.wotagei.cam.camera.FrostCardTable
import com.wotagei.cam.core.CamPill
import com.wotagei.cam.core.Flash
import com.wotagei.cam.core.FrameEffect
import com.wotagei.cam.core.HudItem
import com.wotagei.cam.core.RenderMode
import com.wotagei.cam.core.WbPreset
import com.wotagei.cam.diag.Diag
import com.wotagei.cam.ui.anim.LocalMotion
import com.wotagei.cam.ui.design.HudFrostCard
import com.wotagei.cam.ui.design.HudInkLevel
import com.wotagei.cam.ui.design.WotaColor
import com.wotagei.cam.ui.design.WotaHit
import com.wotagei.cam.ui.design.WotaIconButton
import com.wotagei.cam.ui.design.WotaShape
import com.wotagei.cam.ui.design.WotaSpace
import com.wotagei.cam.ui.design.hudFrostPlate
import com.wotagei.cam.ui.design.pillAnchor
import com.wotagei.cam.ui.design.wotaCard
import com.wotagei.cam.ui.dialog.wbLabelRes
import kotlin.math.roundToInt

/** 顶栏避让量的首帧兜底值（38dp 圆形设置钮 + 上下内边距 4+2 = 44dp） */
internal val TopBarSpace = 44.dp

/** 贴边自由边的设计留白（8dp 令牌） */
internal val HudEdgePad = WotaSpace.s

/** 顶栏定稿的上内边距 */
internal val TopTopPad = 4.dp

/** 顶栏右侧图标钮之间的间隔 */
internal val TopRowGap = 6.dp

internal const val MIN_DB = 20f
internal const val MAX_DB = 110f

@SuppressLint("StaticFieldLeak")
internal object HudFrost {
    private const val REFRESH_MS = 200L

    var live by mutableStateOf(false)
        private set

    var coveredMask by mutableIntStateOf(0)
        private set

    fun isCovered(slot: Int): Boolean =
        slot in 0 until FrostCardTable.SLOT_CAPACITY && (coveredMask and (1 shl slot)) != 0

    var intent by mutableStateOf(false)
        private set

    private var previewProvided = false

    private fun setPreviewProvided(provided: Boolean) {
        previewProvided = provided
    }

    private val locationInWindow = IntArray(2)
    private val tintScratch = FloatArray(3)

    private var root: View? = null
    private var prefs: SharedPreferences? = null
    private var hosts = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    @Suppress("DEPRECATION")
    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == WotaSettings.KEY_FROST_BLUR) evaluateEnabled()
        }

    private val tick = object : Runnable {
        override fun run() {
            if (hosts <= 0 || !intent) return
            refreshAvailability()
            mainHandler.postDelayed(this, REFRESH_MS)
        }
    }

    @Composable
    fun Host() {
        val context = LocalContext.current
        val view = LocalView.current
        val previewCoords = LocalPreviewCoords.current
        DisposableEffect(context, view) {
            attach(context.applicationContext, view)
            onDispose { detach() }
        }
        DisposableEffect(previewCoords) {
            setPreviewProvided(previewCoords != null)
            if (intent) refreshHeader()
            onDispose { setPreviewProvided(false) }
        }
    }

    private fun attach(appContext: Context, view: View) {
        val p = WotaSettings.of(appContext)
        if (hosts == 0) {
            root = view
            prefs = p
            p.registerOnSharedPreferenceChangeListener(prefsListener)
        }
        hosts++
        evaluateEnabled()
    }

    private fun detach() {
        if (hosts <= 0) return
        hosts--
        if (hosts > 0) return
        val p = prefs
        @Suppress("DEPRECATION")
        p?.unregisterOnSharedPreferenceChangeListener(prefsListener)
        mainHandler.removeCallbacks(tick)
        root = null
        prefs = null
        intent = false
        live = false
        coveredMask = 0
    }

    private fun evaluateEnabled() {
        val p = prefs ?: return
        val next = WotaSettings.frostBlurEnabled(p)
        if (next == intent) return
        intent = next
        mainHandler.removeCallbacks(tick)
        if (next) {
            refreshAvailability()
            mainHandler.postDelayed(tick, REFRESH_MS)
        } else {
            live = false
            coveredMask = 0
        }
    }

    private fun refreshAvailability() {
        if (!intent) {
            if (live) live = false
            if (coveredMask != 0) coveredMask = 0
            return
        }
        refreshHeader()
        val mask = FrostCardTable.coveredSlotMask()
        if (mask != coveredMask) {
            Diag.i("FrostDock", "covered -> 0x${mask.toString(16)}")
            coveredMask = mask
        }
        val drawn = mask != 0
        if (drawn != live) {
            Diag.i("FrostDock", "live -> $drawn")
            live = drawn
        }
    }

    fun refreshHeader() {
        val r = root ?: return
        if (prefs == null) return
        if (r.width <= 0 || r.height <= 0) return
        r.getLocationInWindow(locationInWindow)
        WotaColor.hudFrostTintRgb(tintScratch)
        FrostCardTable.writeHeader(
            rootLeftPx = locationInWindow[0].toFloat(),
            rootTopPx = locationInWindow[1].toFloat(),
            rootWidthPx = r.width.toFloat(),
            rootHeightPx = r.height.toFloat(),
            tintRed = tintScratch[0],
            tintGreen = tintScratch[1],
            tintBlue = tintScratch[2],
            uiEnabled = intent,
            cardSpace = frostSpaceIsViewLocal(previewProvided)
        )
    }
}

internal val LocalHudFrostParent = compositionLocalOf<HudFrostCard?> { null }

internal fun hudFrostForEntry(parent: HudFrostCard?): HudFrostCard? =
    parent ?: hudFrostPlate(radiusPx = -1f, ink = HudInkLevel.SECONDARY)

internal val hudFrostPillPlate: HudFrostCard = hudFrostPlate(radiusPx = -1f, ink = HudInkLevel.SECONDARY)

enum class HudEntryKind { PILL, READOUT }

data class HudEntry private constructor(
    val kind: HudEntryKind,
    val pill: CamPill?,
    val item: HudItem?
) {
    companion object {
        fun of(pill: CamPill) = HudEntry(HudEntryKind.PILL, pill, null)
        fun of(item: HudItem) = HudEntry(HudEntryKind.READOUT, null, item)
    }
}

/** HUD 格 → 就近胶囊 */
internal fun HudItem.pillKey(): PillKey = when (this) {
    HudItem.SHUTTER -> PillKey.SHUTTER
    HudItem.FPS -> PillKey.FPS
    HudItem.BITRATE -> PillKey.BITRATE
    HudItem.ISO -> PillKey.ISO
    HudItem.EV -> PillKey.EV
    HudItem.WB -> PillKey.WB
    HudItem.ZOOM -> PillKey.ZOOM
}

/**
 * 条目 → 就近锚点的 key（唯一一处对照表）。
 * 返回 null 的三类没有就近浮层：CURVE 开整块面板，LEVEL/VOLUME 为读数。
 */
internal fun HudEntry.pillKey(): PillKey? = pill?.let { p ->
    when (p) {
        CamPill.SIZE -> PillKey.SIZE
        CamPill.STORAGE -> PillKey.STORAGE
        CamPill.LENS -> PillKey.LENS
        CamPill.ZOOM -> PillKey.ZOOM
        CamPill.FOCUS -> PillKey.FOCUS
        CamPill.STAB -> PillKey.STAB
        CamPill.BT -> PillKey.BT
        CamPill.REFLINE -> PillKey.REFLINE
        CamPill.MONITOR -> PillKey.MONITOR
        CamPill.FLASH -> PillKey.FLASH
        CamPill.DENOISE -> PillKey.DENOISE
        CamPill.GLOW -> PillKey.GLOW
        CamPill.TONE -> PillKey.TONE
        CamPill.ECHO -> PillKey.ECHO
        CamPill.CURVE -> null
        CamPill.LEVEL, CamPill.VOLUME -> null
    }
} ?: item?.pillKey()

/** 一帧 HUD 的全部输入：每颗控件的读数、开关态与动作 */
data class HudCtx(
    val recording: Boolean,
    val busy: Boolean,
    val recStateLabel: String?,
    val elapsedLabel: String,
    val sizeLabel: String,
    val capacityLabel: String,
    val freeLow: Boolean,
    val zoomLabel: String,
    val focusLabel: String,
    val stabLabel: String,
    val stabActive: Boolean,
    val focusActive: Boolean,
    val refLineOn: Boolean,
    val monitorLabel: String,
    val monitorActive: Boolean,
    val curveOn: Boolean,
    val denoiseLabel: String,
    val denoiseActive: Boolean,
    val onDenoiseClick: () -> Unit,
    val glowLabel: String,
    val glowActive: Boolean,
    val glowUsable: Boolean,
    val onGlowClick: () -> Unit,
    val toneLabel: String,
    val toneActive: Boolean,
    val toneUsable: Boolean,
    val onToneClick: () -> Unit,
    val echoLabel: String,
    val echoActive: Boolean,
    val echoUsable: Boolean,
    val onEchoClick: () -> Unit,
    val flash: Flash,
    val lensLabel: String,
    val btConnected: Boolean,
    val btVolumePct: Int,
    val captureActive: Boolean,
    val levelEnabled: Boolean,
    val roll: Float,
    val pitch: Float,
    val db: Float,
    val lastUri: Uri?,
    val aeLocked: Boolean,
    val readoutValue: @Composable (HudItem) -> String?,
    val anchorOf: (HudEntry) -> Modifier,
    val onSizeClick: () -> Unit,
    val onFreeClick: () -> Unit,
    val onRefLineClick: () -> Unit,
    val onMonitorClick: () -> Unit,
    val onFlashClick: () -> Unit,
    val onCurveClick: () -> Unit,
    val onZoomClick: () -> Unit,
    val onFocusClick: () -> Unit,
    val onStabClick: () -> Unit,
    val onBtClick: () -> Unit,
    val onOpenLensPanel: () -> Unit,
    val onRecordClick: () -> Unit,
    val onThumbClick: () -> Unit,
    val onReadoutOpen: (HudItem) -> Unit,
    val focusUsable: Boolean = true,
    val flashUsable: Boolean = true
)

internal fun flashIcon(flash: Flash): ImageVector = when (flash) {
    Flash.OFF -> Icons.Filled.FlashOff
    Flash.ON -> Icons.Filled.FlashOn
    Flash.AUTO -> Icons.Filled.FlashAuto
    Flash.TORCH -> Icons.Filled.FlashlightOn
}

internal fun effectShortRes(effect: FrameEffect): Int = when (effect) {
    FrameEffect.ZEBRA -> R.string.cam_effect_zebra
    FrameEffect.PEAKING -> R.string.cam_effect_peaking
    FrameEffect.NONE -> R.string.cam_p_monitor
}

@Composable
internal fun wbShort(mode: WbPreset, kelvin: Int): String =
    if (mode == WbPreset.MANUAL) "${kelvin}K" else stringResource(wbLabelRes(mode))

internal fun warnRows(
    deviceFailedHighFps: Boolean,
    audioDegraded: Boolean,
    legacy: Boolean,
    effect: FrameEffect,
    renderMode: RenderMode,
    segNearCap: Boolean = false,
    approxManual: Boolean = false
): List<Int> = buildList {
    if (deviceFailedHighFps) add(R.string.cam_state_high_fps)
    if (audioDegraded) add(R.string.no_audio_record_tip)
    if (legacy) add(R.string.capability_limited_tip)
    if (effect != FrameEffect.NONE && renderMode == RenderMode.DIRECT) add(R.string.cam_direct_no_effect)
    if (segNearCap) add(R.string.cam_state_seg_near_cap)
    if (approxManual) add(R.string.cam_state_approx_manual)
}

/** 顶栏固定件：设置入口 + 能力/权限告警条 */
@Composable
fun BoxScope.HudTopChrome(
    audioDegraded: Boolean,
    legacy: Boolean,
    deviceFailedHighFps: Boolean,
    effect: FrameEffect,
    renderMode: RenderMode,
    lowFpsNote: String?,
    onSettingsClick: () -> Unit,
    onHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    segNearCap: Boolean = false,
    approxManual: Boolean = false
) {
    val density = LocalDensity.current
    val motion = LocalMotion.current
    Column(
        modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .onSizeChanged { onHeightChanged(with(density) { it.height.toDp() }.value.roundToInt()) }
            .animateContentSize(motion.intSize)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = HudEdgePad, end = HudEdgePad, top = TopTopPad, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TopRowGap)
        ) {
            Spacer(Modifier.weight(1f))
            WotaIconButton(
                image = Icons.Filled.Settings,
                description = stringResource(R.string.cam_settings),
                onClick = onSettingsClick
            )
        }
        val warns = warnRows(
            deviceFailedHighFps = deviceFailedHighFps,
            audioDegraded = audioDegraded,
            legacy = legacy,
            effect = effect,
            renderMode = renderMode,
            segNearCap = segNearCap,
            approxManual = approxManual
        )
        if (warns.isNotEmpty() || lowFpsNote != null) {
            Column(
                Modifier.padding(start = 10.dp, end = 10.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                warns.forEach { res ->
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.labelSmall,
                        color = WotaColor.warn,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .wotaCard(WotaShape.pill)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                if (lowFpsNote != null) {
                    Text(
                        text = lowFpsNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = WotaColor.warn,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .wotaCard(WotaShape.pill)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/** 录制键：外圈常驻，内部圆点（待机）↔ 方角块（停止），中间态用转圈 */
@Composable
internal fun RecordButton(
    recording: Boolean,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) motion.pressScale else 1f, motion.press)
    Box(
        modifier
            .size(WotaHit.recordTouch)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (recording && !busy) {
            val grow = androidx.compose.animation.core.rememberInfiniteTransition().animateFloat(
                initialValue = 0.94f,
                targetValue = 1.14f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    androidx.compose.animation.core.keyframes {
                        durationMillis = 1_600
                        0.94f at 0
                        1.14f at 800 using androidx.compose.animation.core.LinearOutSlowInEasing
                        0.94f at 1_600
                    }
                )
            )
            Box(
                Modifier
                    .size(WotaHit.recordRing)
                    .graphicsLayer {
                        scaleX = grow.value
                        scaleY = grow.value
                        alpha = 1f - (grow.value - 0.94f) / 0.2f * 0.72f
                    }
                    .border(2.dp, WotaColor.rec.copy(alpha = 0.5f), CircleShape)
            )
        }
        Box(
            Modifier
                .size(WotaHit.recordRing)
                .border(3.dp, if (recording) WotaColor.rec else WotaColor.textHi, CircleShape)
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(color = WotaColor.rec, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            } else if (recording) {
                Box(Modifier.size(WotaHit.recordStop).background(WotaColor.rec, RoundedCornerShape(5.dp)))
            } else {
                Box(Modifier.size(WotaHit.recordDot).background(WotaColor.rec, CircleShape))
            }
        }
    }
}

/** 把控件的窗口矩形回报进锚点表 */
fun Modifier.pillAnchorReport(table: MutableMap<PillKey, IntRect>, key: PillKey): Modifier =
    pillAnchor { rect -> if (table[key] != rect) table[key] = rect }
