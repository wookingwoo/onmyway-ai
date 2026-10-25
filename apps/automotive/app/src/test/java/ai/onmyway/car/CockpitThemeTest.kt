package ai.onmyway.car

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class CockpitThemeTest {
    private fun luminance(hex: String): Double {
        val rgb = hex.removePrefix("#").chunked(2).map { it.toInt(16) / 255.0 }.map { if (it <= .04045) it / 12.92 else ((it + .055) / 1.055).pow(2.4) }
        return .2126 * rgb[0] + .7152 * rgb[1] + .0722 * rgb[2]
    }
    private fun verify(night: Boolean) {
        val p = CockpitTheme.forNight(night)
        for (foreground in listOf(p.ink, p.muted, p.warning)) for (surface in listOf(p.canvas, p.surface, p.secondary)) {
            val a = luminance(foreground); val b = luminance(surface)
            val contrast = (maxOf(a,b) + .05) / (minOf(a,b) + .05)
            assertTrue("$foreground on $surface is $contrast", contrast >= 4.5)
        }
    }
    @Test fun dayTextAndWarningsMeetPleosContrastGuidance() = verify(false)
    @Test fun nightTextAndWarningsMeetPleosContrastGuidance() = verify(true)
}
