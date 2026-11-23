package com.wotagei.cam.stance

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import android.content.Context
import com.wotagei.cam.R
import com.wotagei.cam.ui.design.WotaSwitch

/**
 * 站位提示浮层内容（M3b，用户十七稿弹窗要求逐项）：
 * 精确度 β 滑杆（+重置+说明）／自动·手动模式／手动人数 1~5／GPU·NPU 加速开关
 * （联动设置页"使用GPU"=KEY_DEFAULT_RENDER==GPU 才可开）／打开·扫描模型目录按钮
 * （+模型存放目录文本标注）／当前模型·判定头·安全区版本号文本／模型更新说明。
 * 半透明深底卡（浮在预览上可读；与站位线"透明浮层"同为 Popup 层，不进成片）。
 */
@Composable
fun StancePopupContent(
    state: StanceController.UiState,
    gpuAllowed: Boolean,
    modelDirText: String,
    onEnabled: (Boolean) -> Unit,
    onBeta: (Float) -> Unit,
    onBetaReset: () -> Unit,
    onMode: (String, Int) -> Unit,
    onGpuMode: (String) -> Unit,
    onLineInterval: (Long) -> Unit,
    onPollInterval: (Long) -> Unit,
    onBrighten: (Boolean) -> Unit,
    onImportFromDir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .widthIn(min = 264.dp, max = 320.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xCC1C1C24))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ① 总开关
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.cam_stance_title), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.cam_stance_sub), color = Color(0xFF9A9AA6), fontSize = 11.sp)
            }
            WotaSwitch(checked = state.enabled, onCheckedChange = onEnabled)
        }

        if (state.enabled) {
            // ② 精确度 β（D3：大=宽松/小=严格）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cam_stance_precision), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    String.format("β=%.2f", state.beta),
                    color = Color(0xFFFFCC66), fontSize = 12.sp,
                )
                androidx.compose.material3.TextButton(onClick = onBetaReset) {
                    Text(stringResource(R.string.cam_stance_reset), color = Color(0xFF9A9AA6), fontSize = 11.sp)
                }
            }
            androidx.compose.material3.Slider(
                value = state.beta,
                onValueChange = onBeta,
                valueRange = StanceSpec.BETA_MIN..StanceSpec.BETA_MAX,
            )
            Text(
                stringResource(R.string.cam_stance_precision_note),
                color = Color(0xFF9A9AA6), fontSize = 10.sp,
            )

            // ③ 人数模式：自动 / 手动 1~5
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.cam_stance_count), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                ModeChip(stringResource(R.string.cam_stance_count_auto), state.mode == JudgementEngine.MODE_AUTO) {
                    onMode(JudgementEngine.MODE_AUTO, state.nManual)
                }
                ModeChip(stringResource(R.string.cam_stance_count_manual), state.mode == JudgementEngine.MODE_MANUAL) {
                    onMode(JudgementEngine.MODE_MANUAL, state.nManual)
                }
                if (state.mode == JudgementEngine.MODE_MANUAL) {
                    // 手动人数 1~5（步进器；自动模式隐藏）
                    StepButton("−") { onMode(JudgementEngine.MODE_MANUAL, state.nManual - 1) }
                    Text("${state.nManual}", color = Color.White, fontSize = 14.sp)
                    StepButton("+") { onMode(JudgementEngine.MODE_MANUAL, state.nManual + 1) }
                }
            }

            // ③b 轮询节奏双间隔（用户 r06 口径：轮内每号位间隔 / 两轮之间静默）
            IntervalSlider(
                stringResource(R.string.cam_stance_intra_gap),
                stringResource(R.string.cam_stance_intra_gap_sub),
                state.lineIntervalMs,
                onLineInterval,
            )
            IntervalSlider(
                stringResource(R.string.cam_stance_inter_gap),
                stringResource(R.string.cam_stance_inter_gap_sub),
                state.pollIntervalMs,
                onPollInterval,
            )

            // ④ GPU/NPU 加速三态（r03：自动=白名单裁决 / 强制=跳白名单实验（Maleoon 等
            //    新 GPU 白名单未收录但有 GLES compute 能力，运行期验证失败自动回退） / 关）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cam_stance_gpu_title), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                ModeChip(stringResource(R.string.cam_stance_gpu_auto), gpuAllowed && state.gpuMode == StanceDetector.MODE_AUTO, gpuAllowed) {
                    onGpuMode(StanceDetector.MODE_AUTO)
                }
                ModeChip(stringResource(R.string.cam_stance_gpu_force), gpuAllowed && state.gpuMode == StanceDetector.MODE_FORCE, gpuAllowed) {
                    onGpuMode(StanceDetector.MODE_FORCE)
                }
                ModeChip(stringResource(R.string.cam_stance_gpu_off), !gpuAllowed || state.gpuMode == StanceDetector.MODE_OFF) {
                    onGpuMode(StanceDetector.MODE_OFF)
                }
            }
            Text(
                stringResource(
                    R.string.cam_stance_gpu_current,
                    state.backendName.ifEmpty { "CPU" },
                    if (gpuAllowed) "" else stringResource(R.string.cam_stance_gpu_direct_disabled),
                ),
                color = Color(0xFF9A9AA6), fontSize = 10.sp,
            )
            Text(
                stringResource(R.string.cam_stance_gpu_note),
                color = Color(0xFF77777F), fontSize = 10.sp,
            )

            // ⑤ 预处理提亮（夜间识别，§6 默认关）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.cam_stance_brighten), color = Color.White, fontSize = 13.sp)
                    Text(stringResource(R.string.cam_stance_brighten_sub), color = Color(0xFF9A9AA6), fontSize = 10.sp)
                }
                WotaSwitch(checked = state.brighten, onCheckedChange = onBrighten)
            }

            // ⑥ 模型目录 + 扫描导入（用户十七稿：无法直接跳转 → 文本标注 + 扫描按钮）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cam_stance_model_dir), color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onImportFromDir) {
                    Text(stringResource(R.string.cam_stance_scan_import), color = Color(0xFF66AAFF), fontSize = 11.sp)
                }
            }
            Text(
                modelDirText,
                color = Color(0xFF77777F), fontSize = 9.sp,
            )

            // ⑦ 版本号 + 模型更新说明
            Text(
                state.versionText,
                color = Color(0xFF9A9AA6), fontSize = 10.sp,
            )
            Text(
                stringResource(R.string.cam_stance_model_note),
                color = Color(0xFF77777F), fontSize = 10.sp,
            )

            state.fallbackTip?.let {
                Text(it, color = Color(0xFFFFAA55), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun IntervalSlider(
    label: String,
    note: String,
    valueMs: Long,
    onChange: (Long) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("${valueMs / 1000.0}s", color = Color(0xFFFFCC66), fontSize = 12.sp)
        }
        androidx.compose.material3.Slider(
            value = valueMs.toFloat(),
            onValueChange = { onChange((it.toLong() / 500L) * 500L) },   // 0.5s 步进
            valueRange = 500f..10000f,
        )
        Text(note, color = Color(0xFF9A9AA6), fontSize = 10.sp)
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        color = when {
            !enabled -> Color(0xFF77777F)
            selected -> Color(0xFF111118)
            else -> Color.White
        },
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFFFCC66) else Color(0xFF33333D))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Text(
        symbol,
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier
            .background(Color(0xFF33333D), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}
