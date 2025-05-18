package com.github.mkalmousli.floating_mute

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.app.PendingIntent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.mkalmousli.floating_mute.databinding.FloatingViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt



class NotificationBroadcastReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onReceive(context: Context?, intent: Intent?) {
        // Retrieve the extras

        val action = intent?.getIntExtra("action", 0) ?: 0

        if (action == 2) {
            // launch the the mainactivity if not already running
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
            return
        }

        if (action == 1) {
            scope.launch {
                modeFlow.emit(Mode.Disabled)
            }
            return
        }

        scope.launch {
            when (modeFlow.value) {
                Mode.Hidden -> scope.launch {
                    modeFlow.emit(Mode.Enabled)
                }
                Mode.Enabled -> scope.launch {
                    modeFlow.emit(Mode.Hidden)
                }
                else -> {}
            }
        }

    }
}




class FloatingViewService : Service() {
    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    private val maxVolumeFlow by lazy {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
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


    private fun convertVolumeToPercentage(volume: Int) =
        ((volume.toDouble() / maxVolumeFlow) * 100).roundToInt()

    private val volumePercentageFlow by lazy {
        MutableStateFlow(0).also { flow ->
            scope.launch {
                volumeFlow.collectLatest {
                    flow.emit(convertVolumeToPercentage(it))
                }
            }
        }
    }




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
    private var isDragging = false

    /**
     * Move the view when the user drag it.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun handleViewMoving() {
        var initialX = prefLastX
        var initialY = prefLastY
        var initialTouchX = 0f
        var initialTouchY = 0f

        binds.root.setOnTouchListener { v, event ->
            val action = event.action

            when (action) {
                MotionEvent.ACTION_DOWN -> {
                    lastDown = System.currentTimeMillis()
                    isDragging = false

                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    holdJob = scope.launch {
                        delay(600)
                        if (!isDragging) {
                            modeFlow.emit(Mode.Hidden)
                        }
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!isDragging && (dx * dx + dy * dy) > 16) { // Small threshold to detect real movement
                        isDragging = true
                    }

                    holdJob?.cancel()

                    if (isDragging) {
                        val newX = initialX + dx.toInt()
                        val newY = initialY + dy.toInt()
                        scope.launch {
                            positionFlow.emit(Pair(newX, newY))
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    holdJob?.cancel()
                    val currentTime = System.currentTimeMillis()
                    val diff = currentTime - lastDown

                    if (!isDragging && diff <= 200) { // Increased to 200ms for better user experience
                        toggleVolume()
                    }
                    isDragging = false
                }

                MotionEvent.ACTION_CANCEL -> {
                    holdJob?.cancel()
                    lastDown = 0L
                    isDragging = false
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

        scope.launch {
            volumeFlow.emit(0)
        }
    }

    /**
     * Un-Mute.
     * If the start volume is 0, we make the new volume half of the maximum volume.
     */
    private fun unMute() {
        val volume = if (startVolume <= 0) {
            1
        } else {
            startVolume
        }

        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            volume,
            AudioManager.FLAG_PLAY_SOUND
        )

        scope.launch {
            volumeFlow.emit(volume)
        }
    }

    /**
     * Toggle the volume based. mute <-> un-mute.
     */
    private fun toggleVolume() = if (isMute()) unMute() else mute()




    private fun handleModeChange(mode: Mode) {

        when (mode) {
            Mode.Enabled -> {
                showNotification(mode)

                windowManager.addView(binds.root, params)
                handleViewMoving()
            }


            Mode.Disabled -> {
                //TODO: Avoid try-catch
                try {
                    windowManager.removeView(binds.root)
                }catch (ignored: Exception) {
                }
                // remove notification
                val notificationManager = NotificationManagerCompat.from(this)
                notificationManager.cancel(NOTIFICATION_ID)

                // kill service
                stopSelf()
            }


            Mode.Hidden -> {
                showNotification(mode)
                windowManager.removeView(binds.root)
            }
        }
    }



    companion object {
        const val NOTIFICATION_ID = 1
    }

    private fun showNotification(mode: Mode) {
        if (mode == Mode.Disabled) {
            return
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.logo)
            setContentTitle("Floating Mute")

            when (mode) {
                Mode.Enabled -> {
                    setContentText("Enabled. Hide or tap here to open the app.")
                }
                else -> {
                    setContentText("Currently hidden. Show, or tap here to open the app.")
                }
            }

            fun createPendingIntent(action: Int): PendingIntent {
                val intent = Intent(this@FloatingViewService, NotificationBroadcastReceiver::class.java)
                intent.putExtra("action", action)
                return PendingIntent.getBroadcast(
                    this@FloatingViewService,
                    action,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE // Specify it as mutable
                )
            }
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
            setAutoCancel(false)

            when (mode) {
                Mode.Enabled -> {
                    addAction(R.drawable.logo, "Hide", createPendingIntent(0))
                }
                Mode.Hidden -> {
                    addAction(R.drawable.logo, "Show", createPendingIntent(0))
                }
                else -> Unit
            }
            addAction(R.drawable.logo, "Stop", createPendingIntent(1))

            setContentIntent(createPendingIntent(2))
        }



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
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

    }


    @SuppressLint("SetTextI18n")
    override fun onCreate() {
        super.onCreate()

        scope.apply {

            launch {
                showPercentageFlow.collectLatest {
                    binds.percentage.visibility = if (it) View.VISIBLE else View.GONE
                }
            }

            launch {
                positionFlow.collectLatest {
                    try {
                        updateViewPos(params, it.first, it.second)
                    }catch (ignored: Exception) {}
                }
            }

            launch {
                volumeFlow.emit(currentVolume)
            }

            launch {
                volumePercentageFlow.collectLatest {
                    binds.percentage.text = "$it%"

                    val icon = when (it) {
                        0 -> R.drawable.volume_off
                        in 1..25 -> R.drawable.volume_up_25
                        in 26..50 -> R.drawable.volume_up_50
                        in 51..94 -> R.drawable.volume_up_75
                        in 95..100 -> R.drawable.volume_max
                        else -> R.drawable.volume_off
                    }

                    binds.icon.setImageResource(icon)
                }
            }

            launch {
                modeFlow.collectLatest {
                    handleModeChange(it)
                }
            }

            launch {
                modeFlow.emit(Mode.Enabled)
            }
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        scope.launch {
            modeFlow.emit(Mode.Disabled)
            handleModeChange(Mode.Disabled)
            scope.cancel()
        }

        super.onDestroy()
    }

}