package com.github.mkalmousli.floating_mute

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.app.PendingIntent
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.mkalmousli.floating_mute.databinding.FloatingViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt



class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, FloatingViewService::class.java)
        context?.stopService(serviceIntent)
    }
}

class FloatingViewService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val binds by lazy {
        FloatingViewBinding.inflate(layoutInflater)
    }

    private var params =
        LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                LayoutParams.TYPE_PHONE
            },
            LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )


    /**
     * Update the x and y position of the view and save it to SharedPreferences.
     */
    private fun updateViewPos(params: LayoutParams, x: Int, y: Int) {
        params.x = x
        params.y = y
        windowManager.updateViewLayout(binds.root, params)
        prefLastX = x
        prefLastY = y
    }

    private var holdJob: Job? = null
    private var lastDown: Long = 0L

    /**
     * Move the view when the user drag it.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun handleViewMoving() {
        var initialX = prefLastX
        var initialY = prefLastX
        var initialTouchX = 0f
        var initialTouchY = 0f



        binds.root.setOnTouchListener { v, event ->
            val action = event.action

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    lastDown = System.currentTimeMillis()

                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    holdJob = CoroutineScope(Dispatchers.Main).launch {
                        delay(600)
                        lastDown = 0
                        hide()
                    }

                }

                MotionEvent.ACTION_MOVE -> {
                    holdJob?.cancel()
                    val newX = initialX + (event.rawX - initialTouchX).toInt()
                    val newY = initialY + (event.rawY - initialTouchY).toInt()
                    updateViewPos(params, newX, newY)
                }

                MotionEvent.ACTION_UP -> {
                    val currentTime = System.currentTimeMillis()
                    val diff = currentTime - lastDown

                    if (diff <= 100) {
                        toggleVolume()
                        holdJob?.cancel()
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    holdJob?.cancel()
                    lastDown = 0L
                }
            }
            true
        }

    }

    /**
     * The start volume.
     * We store the volume before muting and restore it back when un-mute.
     */
    private var startVolume = 0


    /**
     * Get the current volume of the device.
     */
    private val currentVolume get() =
        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    private val currentVolumePercentage get() =
        ((currentVolume.toDouble()  / maxVolume) * 100).roundToInt()


    /**
     * Get the maximum allowed volume on the device.
     */
    private val maxVolume get() =
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    /**
     * Check whether is current volume in the mute state.
     */
    private fun isMute() = currentVolume == 0

    /**
     * Mute.
     */
    private fun mute() {
        startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            0,
            AudioManager.FLAG_PLAY_SOUND
        )

        updateIcon()
        updatePercentage()
    }

    /**
     * Un-Mute.
     * If the start volume is 0, we make the new volume half of the maximum volume.
     */
    private fun unMute() {
        val volume = if (startVolume <= 0) {
            maxVolume / 2
        } else {
            startVolume
        }

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            AudioManager.FLAG_PLAY_SOUND
        )

        updateIcon()
        updatePercentage()
    }

    /**
     * Toggle the volume based. mute <-> un-mute.
     */
    private fun toggleVolume() {
        if (isMute()) {
            unMute()
        } else {
            mute()
        }
    }


    /**
     * Update the icon based on the current volume.
     */
    private fun updateIcon() {

        val icon = when (currentVolumePercentage) {
            0 -> R.drawable.volume_off
            in 1..25 -> R.drawable.volume_up_25
            in 26..50 -> R.drawable.volume_up_50
            in 51..94 -> R.drawable.volume_up_75
            in 95..100 -> R.drawable.volume_max
            else -> R.drawable.volume_off
        }

        binds.icon.setImageResource(icon)
    }

    @SuppressLint("SetTextI18n")
    /**
     * Calculate the volume percentage and show it.
     */
    private fun updatePercentage() {
        binds.percentage.text = "${currentVolumePercentage}%"
    }

    private val orientationListener by lazy {
        var lastOrientation: Orientation? = null

        object : OrientationEventListener(this) {
            override fun onOrientationChanged(_orientation: Int) {
                val newOrientation = orientation

                if (newOrientation != lastOrientation) {
                    lastOrientation = newOrientation

                    updateViewPos(params, prefLastX, prefLastY)
                }
            }
        }
    }


    override fun onCreate() {
        super.onCreate()

        val intent = Intent(this, NotificationBroadcastReceiver::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(baseContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.volume_max)
            .setContentTitle("My Notification")
            .setContentText("This is a notification from my app.")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(R.drawable.volume_max, "H", pendingIntent)
//            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(1, notificationBuilder.build())

        /// Init
        updateIcon()
        updatePercentage()

        /// Set view listeners
        handleViewMoving()

//        binds.apply {
//            root.apply {
////                icon.setOnClickListener {
////                    toggleVolume()
////                }
////
////                icon.setOnLongClickListener {
////                    hideUI()
////                    true
////                }
//            }
//        }

        /// Make the view float!
        windowManager.addView(binds.root, params)

        /// Listen to rotation events.
        orientationListener.enable()

        /// Listen to when volume changes, and update the floating view.
        CoroutineScope(Dispatchers.IO).launch {
            notificationVolumeFlow.collect {
                launch(Dispatchers.Main) {
                    updateIcon()
                    updatePercentage()
                }
            }
        }


        sharedPrefs.registerOnSharedPreferenceChangeListener(this)
    }


    private fun hide() {
        orientationListener.disable()
        windowManager.removeView(binds.root)
    }


    override fun onBind(intent: Intent?) = null

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
//        println(key)
    }

    override fun onDestroy() {
        println("Killing the service...")
        super.onDestroy()
    }

}