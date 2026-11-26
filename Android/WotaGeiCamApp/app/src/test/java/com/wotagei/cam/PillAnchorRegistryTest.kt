package com.wotagei.cam

import com.wotagei.cam.core.CamPill
import com.wotagei.cam.core.HudItem
import com.wotagei.cam.ui.HudEntry
import com.wotagei.cam.ui.PillKey
import com.wotagei.cam.ui.pillAnchorWriters
import com.wotagei.cam.ui.pillKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 就近胶囊锚点登记表的完整性（审查 S2-2，§69「浮层甩到屏幕原点」缺陷族的结构闸）。
 *
 * 复发机理：某颗控件加了 `pop = PillKey.X` 触发点与 [com.wotagei.cam.ui.PillHost] 的内容分支，
 * 却没人把锚点挂到控件上 → `pillAnchors[X]` 恒为 null → 弹窗按 `IntRect.Zero` 永久钉在左上角。
 * 这种错 lint 不报、编译不报、JVM 也摸不到，只有真机点得到（蓝牙那颗就是）。
 *
 * B4 之后写入方收成了**一处**（[com.wotagei.cam.ui.HudEntryItem] 里 `ctx.anchorOf(entry)`），
 * 所以这里把三道闸换成四道：
 * - [everyPillKeyHasAnchorWriter]：表逐个 key 覆盖 [PillKey.values]，新增 key 忘了登记就红。
 *   取"全覆盖"而不是"只覆盖有触发点的 key"，是因为后者要在测试里再抄一份触发点清单，两处都可能漏抄。
 * - [everyWriterNamesTheEntryItemBranch]：B4 起锚点只有一个挂点（HudEntryItem 分支）；10-01 布局批起
 *   合法写入方有**两族**——HudEntryItem 网格分支，或 `CameraScreen.` 固定读数（BITRATE 移出网格后
 *   锚点随渲染点迁移）。断言认这两类前缀；真正强闸仍是 [everyPillKeyIsOwnedByAnEntry] + §69 运行时兜底。
 * - [everyPillKeyIsOwnedByAnEntry]：每个 PillKey 都必须能从某颗可编辑条目映射到（[HudEntry.pillKey]）。
 *   这张对照表漏一条，那颗控件就永远不会报锚点，浮层又回原点——这一条是 B4 新增的结构闸。
 * - [entriesWithoutPillKeyAreExactlyTheReadOnlyOnes]：反过来钉「没有就近浮层」的那三条
 *   （曲线开整块面板、水平仪与音量表是读数），防止有人新加 PillKey 却把它做成没归属的孤儿。
 */
class PillAnchorRegistryTest {

    @Test
    fun everyPillKeyHasAnchorWriter() {
        val keys = PillKey.values().toSet()
        assertEquals("PillKey 的数量与登记表不一致：新增 key 必须同步登记锚点写入方", keys.size, pillAnchorWriters.size)
        assertEquals(keys, pillAnchorWriters.keys)
    }

    @Test
    fun everyWriterNamesTheEntryItemBranch() {
        // 写入方不能是空占位：每行都得点名 HudEntryItem 的哪条分支（= 哪个 when 臂挂的锚点），
        // 否则"登记了但没人挂"这种洞照样过得去
        pillAnchorWriters.forEach { (key, writer) ->
            assertTrue("$key 的写入方写得太少：'$writer'", writer.length > 12)
            // 10-01 布局批起合法写入方有两族：HudEntryItem 网格分支，或 CameraScreen 的
            // 固定读数（BITRATE 移出网格后锚点随渲染点迁移）。断言只认这两类前缀，
            // 防"登记了但没人挂"的洞借换家混进来
            assertTrue(
                "$key 的写入方没落在已知锚点挂点上：'$writer'",
                writer.startsWith("HudLayer.HudEntryItem(") || writer.startsWith("CameraScreen.")
            )
            assertTrue("$key 的写入方没点名条目：'$writer'", writer.contains(key.name) || writer.contains("HudItem."))
        }
    }

    private val allEntries = CamPill.ALL.map { HudEntry.of(it) } + HudItem.values().map { HudEntry.of(it) }

    @Test
    fun everyPillKeyIsOwnedByAnEntry() {
        val owned = allEntries.mapNotNull { it.pillKey() }.toSet()
        assertEquals(
            "有 PillKey 在 HudEntry.pillKey 里没有归属：那颗控件永远不会报锚点，浮层会弹到屏幕原点",
            PillKey.values().toSet(),
            owned
        )
    }

    @Test
    fun entriesWithoutPillKeyAreExactlyTheReadOnlyOnes() {
        val noKey = allEntries.filter { it.pillKey() == null }.mapNotNull { it.pill }.toSet()
        assertEquals(
            "只有曲线（整块面板）与水平仪、音量表（纯读数）三条不该有就近浮层；多出来或少了都要改代码而不是改这里",
            setOf(
                CamPill.CURVE,
                CamPill.LEVEL,
                CamPill.VOLUME
            ),
            noKey
        )
    }

    @Test
    fun zoomAnchorHasOneWriterPerState() {
        val zoom = pillAnchorWriters[PillKey.ZOOM].orEmpty()
        assertTrue(
            "ZOOM 的登记要同时写明竖 Dock 与读数块的接管关系：'$zoom'",
            zoom.contains("HudReadoutZone")
        )
        assertTrue(allEntries.count { it.pillKey() == PillKey.ZOOM } == 2)
        val owners = allEntries.filter { it.pillKey() == PillKey.ZOOM }
        assertEquals(listOf(HudEntry.of(CamPill.ZOOM), HudEntry.of(HudItem.ZOOM)), owners)
    }
}
