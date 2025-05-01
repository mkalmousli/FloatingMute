package com.github.mkalmousli.floating_mute

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.view.OrientationEventListener
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class App : Application() {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val orientationListener by lazy {
        var lastOrientation: Orientation? = null

        object : OrientationEventListener(this) {
            override fun onOrientationChanged(ignored: Int) {
                val newOrientation = orientation

                if (newOrientation != lastOrientation) {
                    lastOrientation = newOrientation
                    scope.launch {
                        orientationFlow.emit(newOrientation)
                    }
                }
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()



        scope.apply {

            launch {
                showPercentageFlow.emit(prefShowPercentage)
            }

            launch {
                orientationFlow.collectLatest {
                    positionFlow.emit(Pair(prefLastX, prefLastY))
                }
            }


            launch {
                orientationListener.enable()
            }


            launch {
                positionFlow.collectLatest {
                    prefLastX = it.first
                    prefLastY = it.second
                }
            }
        }


        scope.launch {
            modeFlow.collectLatest {
                if (it == Mode.Hidden) {
                    Toast.makeText(this@App, "Floating view is hidden", Toast.LENGTH_SHORT).show()
                }
            }
        }




        /**
         * Listen for system's volume changes.
         */
        val systemVolumeFlow = callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", 0)) {
                        AudioManager.STREAM_MUSIC -> trySend(
                            intent.getIntExtra(
                                "android.media.EXTRA_VOLUME_STREAM_VALUE",
                                0
                            )
                        )
                    }
                }
            }

            registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
            awaitClose { unregisterReceiver(receiver) }
        }

        /**
         * Update the volume flow when the system volume changes.
         */
        scope.launch {
            systemVolumeFlow.collect {
                volumeFlow.emit(it)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        orientationListener.disable()
    }
}