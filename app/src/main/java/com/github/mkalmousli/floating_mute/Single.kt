package com.github.mkalmousli.floating_mute

import kotlinx.coroutines.flow.MutableStateFlow


val volumeFlow = MutableStateFlow(0)

enum class Mode {
    Enabled,
    Disabled,
    Hidden
}
val modeFlow = MutableStateFlow(Mode.Disabled)

val showPercentageFlow = MutableStateFlow(false)

enum class Orientation {
    Portrait,
    Landscape
}
val orientationFlow = MutableStateFlow(Orientation.Portrait)


val positionFlow = MutableStateFlow(Pair(0, 0))


/**
 * How the app UI follows the system dark mode.
 */
enum class AppTheme {
    System,
    Light,
    Dark
}
val appThemeFlow = MutableStateFlow(AppTheme.System)


/**
 * Everything the user can tweak about the look and the feel of the
 * floating button. All values are stored in [android.content.SharedPreferences]
 * and mirrored through [appearanceFlow] so every screen and the service
 * stay in sync live.
 */
data class Appearance(
    /** Button (and icon) size in dp. */
    val buttonSize: Int = 50,
    /** Background color of the button. */
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    /** Tint applied to the volume icon. */
    val iconColor: Int = 0xFF303030.toInt(),
    /** Whole-button opacity, 20..100 (%). */
    val opacity: Int = 100,
    /** Corner radius of the button background in dp. */
    val cornerRadius: Int = 14,
    /** Background color of the volume-percentage label. */
    val percentageBackgroundColor: Int = 0xFF303030.toInt(),
    /** Text color of the volume-percentage label. */
    val percentageTextColor: Int = 0xFFFFFFFF.toInt(),
    /**
     * How long (ms) the finger must stay down before a movement is treated
     * as a drag. Keeps quick mute/unmute taps from turning into a move.
     */
    val moveDelayMs: Int = 150,
) {
    companion object {
        val DEFAULT = Appearance()
        const val MIN_SIZE = 32
        const val MAX_SIZE = 120
        const val MIN_OPACITY = 20
        const val MAX_OPACITY = 100
        const val MAX_CORNER = 40
        const val MAX_MOVE_DELAY = 500
    }
}
val appearanceFlow = MutableStateFlow(Appearance.DEFAULT)
