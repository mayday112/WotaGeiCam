package com.wotagei.cam.ui

import com.wotagei.cam.FakePrefs
import com.wotagei.cam.media.WotaNav
import com.wotagei.cam.source.KotlinSourceScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStayAwakeTest {

    @Test
    fun `播放中常亮判定纯函数`() {
        // Player & Compare 在 enabled + playing 时判定为 true
        assertTrue(playbackHoldsScreen(WotaNav.PLAYER, playing = true, enabled = true))
        assertTrue(playbackHoldsScreen(WotaNav.COMPARE, playing = true, enabled = true))

        // playing 为 false 时不常亮
        assertFalse(playbackHoldsScreen(WotaNav.PLAYER, playing = false, enabled = true))
        assertFalse(playbackHoldsScreen(WotaNav.COMPARE, playing = false, enabled = true))

        // enabled 开关关闭时一律不常亮
        assertFalse(playbackHoldsScreen(WotaNav.PLAYER, playing = true, enabled = false))
        assertFalse(playbackHoldsScreen(WotaNav.COMPARE, playing = true, enabled = false))

        // 其余路由一律不常亮
        assertFalse(playbackHoldsScreen(ROUTE_CAMERA, playing = true, enabled = true))
        assertFalse(playbackHoldsScreen(ROUTE_SETTINGS, playing = true, enabled = true))
        assertFalse(playbackHoldsScreen(WotaNav.GALLERY, playing = true, enabled = true))
        assertFalse(playbackHoldsScreen(null, playing = true, enabled = true))
    }

    @Test
    fun `播放中常亮设置默认值与读写`() {
        val prefs = FakePrefs()
        // 默认开 (true)
        assertTrue("默认开", WotaSettings.playerStayAwake(prefs))

        prefs.map[WotaSettings.KEY_PLAYER_STAY_AWAKE] = false
        assertFalse("设置关后读取为关", WotaSettings.playerStayAwake(prefs))

        prefs.map[WotaSettings.KEY_PLAYER_STAY_AWAKE] = true
        assertTrue("设置开后读取为开", WotaSettings.playerStayAwake(prefs))
    }

    @Test
    fun `播放页与对比页接线上报播放激活状态`() {
        val player = KotlinSourceScan.codeOnly(KotlinSourceScan.mainSourceText("player/PlayerScreen.kt"))
        assertTrue("PlayerScreen 必须上报 onPlaybackActiveChanged(playing)", player.contains("onPlaybackActiveChanged(playing)"))

        val compare = KotlinSourceScan.codeOnly(KotlinSourceScan.mainSourceText("player/CompareScreen.kt"))
        assertTrue("CompareScreen 必须上报 onPlaybackActiveChanged(playing)", compare.contains("onPlaybackActiveChanged(playing)"))

        val main = KotlinSourceScan.codeOnly(KotlinSourceScan.mainSourceText("ui/MainActivity.kt"))
        assertTrue("MainActivity 必须有 onPlaybackActiveChanged 分发入口", main.contains("onPlaybackActiveChanged(playing: Boolean)"))
        assertTrue("MainActivity 中 Player composable 接上线", main.contains("onPlaybackActiveChanged = { windowOwner?.onPlaybackActiveChanged(it) }"))
    }
}

