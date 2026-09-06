package com.github.mkalmousli.floating_mute

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.app.PendingIntent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.widget.ImageViewCompat
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


private const val s = "Hide"

class FloatingViewService : Service() {
    private val windowManager: WindowManager by lazy {
        getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private var viewAdded = false

    private val longPressMs = 600L

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
     * Move the view to (x, y) without touching SharedPreferences.
     * Persisting on every animation frame was a major source of jank and
     * made the button feel "too sensitive" while dragging.
     */
    private fun moveViewTo(x: Int, y: Int) {
        params.x = x
        params.y = y
        if (viewAdded) {
            try {
                windowManager.updateViewLayout(binds.root, params)
            } catch (ignored: Exception) {
            }
        }
    }

    /** Persist the current position (call once, when a drag ends). */
    private fun persistViewPos() {
        prefLastX = params.x
        prefLastY = params.y
        scope.launch { positionFlow.emit(Pair(params.x, params.y)) }
    }


    private var currentAppearance = Appearance.DEFAULT

    /** Apply the user's theme choices to the floating button. */
    private fun applyAppearance(a: Appearance) {
        currentAppearance = a

        val sizePx = dp(a.buttonSize)
        binds.icon.layoutParams = binds.icon.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
        ImageViewCompat.setImageTintList(binds.icon, ColorStateList.valueOf(a.iconColor))

        binds.root.background = GradientDrawable().apply {
            setColor(a.backgroundColor)
            cornerRadius = dp(a.cornerRadius).toFloat()
        }
        binds.root.alpha = (a.opacity.coerceIn(Appearance.MIN_OPACITY, 100)) / 100f

        binds.percentage.setBackgroundColor(a.percentageBackgroundColor)
        binds.percentage.setTextColor(a.percentageTextColor)

        if (viewAdded) {
            try {
                windowManager.updateViewLayout(binds.root, params)
            } catch (ignored: Exception) {
            }
        }
    }


    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    private val screenHeight get() = resources.displayMetrics.heightPixels

    private var holdJob: Job? = null
    private var lastDown: Long = 0L
    private var isDragging = false
    private var longPressConsumed = false
    private var inHideZone = false

    /** The finger is close enough to the bottom edge to drop-to-hide. */
    private fun isInHideZone(rawY: Float): Boolean =
        rawY >= screenHeight - dp(96)

    /**
     * Handle taps, long-press and dragging of the floating button.
     *
     * - short tap  -> mute / unmute
     * - long press -> toggle the volume-percentage label
     * - drag       -> move the button (only after [Appearance.moveDelayMs])
     * - drag to the bottom edge and release -> hide (like chat bubbles)
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun handleViewMoving() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        binds.root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastDown = System.currentTimeMillis()
                    isDragging = false
                    longPressConsumed = false
                    inHideZone = false

                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    holdJob?.cancel()
                    holdJob = scope.launch {
                        delay(longPressMs)
                        if (!isDragging) {
                            longPressConsumed = true
                            togglePercentage()
                        }
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    val movedFar = (dx * dx + dy * dy) > touchSlop * touchSlop
                    val heldLongEnough =
                        System.currentTimeMillis() - lastDown >= currentAppearance.moveDelayMs

                    if (!isDragging && !longPressConsumed && movedFar && heldLongEnough) {
                        isDragging = true
                        holdJob?.cancel()
                    }

                    if (isDragging) {
                        moveViewTo(initialX + dx.toInt(), initialY + dy.toInt())

                        val nowInZone = isInHideZone(event.rawY)
                        if (nowInZone != inHideZone) {
                            inHideZone = nowInZone
                            binds.root.alpha = if (nowInZone) {
                                0.35f
                            } else {
                                currentAppearance.opacity / 100f
                            }
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    holdJob?.cancel()
                    val diff = System.currentTimeMillis() - lastDown

                    when {
                        isDragging && inHideZone -> {
                            binds.root.alpha = currentAppearance.opacity / 100f
                            scope.launch { modeFlow.emit(Mode.Hidden) }
                        }
                        isDragging -> persistViewPos()
                        !longPressConsumed && diff < longPressMs -> toggleVolume()
                    }
                    isDragging = false
                    inHideZone = false
                }

                MotionEvent.ACTION_CANCEL -> {
                    holdJob?.cancel()
                    lastDown = 0L
                    isDragging = false
                    inHideZone = false
                    binds.root.alpha = currentAppearance.opacity / 100f
                }
            }
            true
        }
    }

    /** Toggle the volume-percentage label and remember the choice. */
    private fun togglePercentage() {
        val next = !showPercentageFlow.value
        prefShowPercentage = next
        scope.launch { showPercentageFlow.emit(next) }
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




    private fun addFloatingView() {
        if (viewAdded) return
        // Restore the saved position (e.g. after being hidden via drag-to-bottom).
        params.x = prefLastX
        params.y = prefLastY
        binds.root.alpha = currentAppearance.opacity / 100f
        try {
            windowManager.addView(binds.root, params)
            viewAdded = true
        } catch (ignored: Exception) {
        }
    }

    private fun removeFloatingView() {
        if (!viewAdded) return
        try {
            windowManager.removeView(binds.root)
        } catch (ignored: Exception) {
        }
        viewAdded = false
    }

    private fun handleModeChange(mode: Mode) {
        when (mode) {
            Mode.Enabled -> {
                showNotification(mode)
                addFloatingView()
            }

            Mode.Disabled -> {
                removeFloatingView()
                NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
                stopSelf()
            }

            Mode.Hidden -> {
                showNotification(mode)
                removeFloatingView()
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
            setContentTitle(getString(R.string.app_name))

            when (mode) {
                Mode.Enabled -> {
                    setContentText(getString(R.string.enabled_notification))
                }
                else -> {
                    setContentText(getString(R.string.disabled_notification))
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
                    addAction(R.drawable.transparent, getString(R.string.hide), createPendingIntent(0))
                }
                Mode.Hidden -> {
                    addAction(R.drawable.transparent, getString(R.string.show), createPendingIntent(0))
                }
                else -> Unit
            }
            addAction(R.drawable.transparent, getString(R.string.stop), createPendingIntent(1))

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

        params.x = prefLastX
        params.y = prefLastY
        applyAppearance(loadAppearance())
        handleViewMoving()

        scope.apply {

            launch {
                showPercentageFlow.collectLatest {
                    binds.percentage.visibility = if (it) View.VISIBLE else View.GONE
                }
            }

            launch {
                appearanceFlow.collectLatest { applyAppearance(it) }
            }

            launch {
                positionFlow.collectLatest {
                    if (!isDragging) moveViewTo(it.first, it.second)
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