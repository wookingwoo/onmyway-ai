package ai.onmyway.car

/** Semantic native tokens shared by the live service and sample experience. */
data class CockpitPalette(val canvas: String, val surface: String, val ink: String,
                          val muted: String, val secondary: String, val warning: String)

object CockpitTheme {
    const val TOUCH_DP = 80
    const val HEADING_SP = 40f
    const val ACTION_SP = 25f
    fun forNight(night: Boolean) = if (night)
        CockpitPalette("#141516", "#222426", "#F2F3F4", "#B4B8BC", "#303336", "#F3CE8D")
    else CockpitPalette("#F5F6F7", "#FFFFFF", "#202124", "#62676C", "#E9ECEF", "#795313")
}
