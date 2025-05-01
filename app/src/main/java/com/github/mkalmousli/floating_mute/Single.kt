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