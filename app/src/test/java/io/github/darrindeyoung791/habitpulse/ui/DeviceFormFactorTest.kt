package io.github.darrindeyoung791.habitpulse.ui

import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceFormFactorTest {

    private fun windowSizeClassFor(widthDp: Int, heightDp: Int): WindowSizeClass =
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp)

    private fun form(widthDp: Int, heightDp: Int): DeviceFormInfo =
        classifyDeviceForm(widthDp.dp, heightDp.dp, windowSizeClassFor(widthDp, heightDp))

    // ---------- isTabletDevice: min(宽,高) >= 600 ----------

    @Test
    fun `phone portrait is not tablet`() {
        val info = form(widthDp = 400, heightDp = 800)
        assertFalse(info.isTabletDevice)
        assertFalse(info.isLandscape)
        assertFalse(info.isTabletLandscape)
        assertFalse(info.isPhoneLandscape)
    }

    @Test
    fun `tablet landscape is tablet`() {
        val info = form(widthDp = 1280, heightDp = 800)
        assertTrue(info.isTabletDevice)
        assertTrue(info.isLandscape)
        assertTrue(info.isTabletLandscape)
    }

    @Test
    fun `phone landscape wide width is not tablet`() {
        val info = form(widthDp = 640, heightDp = 360)
        assertFalse(info.isTabletDevice)
        assertTrue(info.isLandscape)
        assertTrue(info.isPhoneLandscape)
    }

    @Test
    fun `tablet boundary exactly 600 min edge is tablet`() {
        val info = form(widthDp = 600, heightDp = 800)
        assertTrue(info.isTabletDevice)
    }

    @Test
    fun `tablet boundary just below 600 min edge is not tablet`() {
        val info = form(widthDp = 599, heightDp = 800)
        assertFalse(info.isTabletDevice)
    }

    @Test
    fun `square 500x500 is not tablet`() {
        val info = form(widthDp = 500, heightDp = 500)
        assertFalse(info.isTabletDevice)
        assertTrue(info.isLandscape)
    }

    @Test
    fun `square 800x800 is tablet and landscape`() {
        val info = form(widthDp = 800, heightDp = 800)
        assertTrue(info.isTabletDevice)
        assertTrue(info.isLandscape)
        assertTrue(info.isTabletLandscape)
    }

    // ---------- isLandscape: 宽 >= 高 ----------

    @Test
    fun `landscape when width equals height`() {
        val info = form(widthDp = 800, heightDp = 800)
        assertTrue(info.isLandscape)
    }

    @Test
    fun `portrait when height greater than width`() {
        val info = form(widthDp = 360, heightDp = 800)
        assertFalse(info.isLandscape)
    }

    // ---------- isWideLayout: 横屏且宽 >= 840 ----------

    @Test
    fun `wide layout enabled at 840 landscape`() {
        val info = form(widthDp = 840, heightDp = 360)
        assertTrue(info.isWideLayout)
    }

    @Test
    fun `wide layout disabled at 839 landscape`() {
        val info = form(widthDp = 839, heightDp = 360)
        assertFalse(info.isWideLayout)
    }

    @Test
    fun `wide layout disabled when portrait even with wide width`() {
        val info = form(widthDp = 1200, heightDp = 1600)
        assertFalse(info.isLandscape)
        assertFalse(info.isWideLayout)
    }

    @Test
    fun `phone landscape 640 is not wide layout`() {
        val info = form(widthDp = 640, heightDp = 360)
        assertTrue(info.isLandscape)
        assertFalse(info.isWideLayout)
    }

    // ---------- isLargeWindow: 宽 >= 1200 ----------

    @Test
    fun `large window at exactly 1200`() {
        val info = form(widthDp = 1200, heightDp = 800)
        assertTrue(info.isLargeWindow)
    }

    @Test
    fun `large window disabled at 1199`() {
        val info = form(widthDp = 1199, heightDp = 800)
        assertFalse(info.isLargeWindow)
    }

    @Test
    fun `xlarge window also large`() {
        val info = form(widthDp = 2000, heightDp = 1000)
        assertTrue(info.isLargeWindow)
    }

    @Test
    fun `large window independent of landscape`() {
        val info = form(widthDp = 1300, heightDp = 1400)
        assertTrue(info.isLargeWindow)
        assertFalse(info.isLandscape)
        assertFalse(info.isWideLayout)
    }

    // ---------- 原始数据透传 ----------

    @Test
    fun `exposes width and height dp`() {
        val info = form(widthDp = 400, heightDp = 800)
        assertEquals(400f, info.windowWidthDp.value, 0f)
        assertEquals(800f, info.windowHeightDp.value, 0f)
    }

    @Test
    fun `exposes size class matching width bucket`() {
        val info = form(widthDp = 1280, heightDp = 800)
        assertTrue(info.windowSizeClass.isWidthAtLeastBreakpoint(LARGE_SCREEN_MIN_WIDTH_DP))
    }
}
