package com.github.mkalmousli.floating_mute

import android.app.Application
import android.view.OrientationEventListener
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class App : Application() {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val orientationListener by lazy {
        var lastOrientation: Orientation? = null

        object : OrientationEventListener(this) {
            override fun onOrientationChanged(ignored: Int) {
                val newOrientation = orientation
                if (newOrientation != lastOrientation) {
                    lastOrientation = newOrientation
                    scope.launch { orientationFlow.emit(newOrientation) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Apply the saved UI theme + Material You colors before any activity shows.
        applyNightMode(prefAppTheme)
        DynamicColors.applyToActivitiesIfAvailable(this)

        scope.apply {

            launch {
                appThemeFlow.emit(prefAppTheme)
                appThemeFlow.collectLatest {
                    prefAppTheme = it
                    applyNightMode(it)
                }
            }

            launch {
                appearanceFlow.emit(loadAppearance())
                appearanceFlow.collectLatest { saveAppearance(it) }
            }

            launch {
                showPercentageFlow.emit(prefShowPercentage)
                showPercentageFlow.collectLatest { prefShowPercentage = it }
            }

            launch {
                orientationFlow.collectLatest {
                    positionFlow.emit(Pair(prefLastX, prefLastY))
                }
            }

            launch { orientationListener.enable() }

            launch {
                positionFlow.collectLatest {
                    prefLastX = it.first
                    prefLastY = it.second
                }
            }

            // Keep our volume mirror in sync with the system volume.
            launch {
                systemMusicVolumeFlow().collect { volumeFlow.emit(it) }
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        orientationListener.disable()
    }
}
