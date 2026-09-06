package com.github.mkalmousli.floating_mute

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

val Context.sharedPrefs: SharedPreferences
    get() =
    getSharedPreferences("Prefs", Context.MODE_PRIVATE)



val Context.orientation: Orientation get() {
    val v = resources.configuration.orientation

    return if (v == Configuration.ORIENTATION_LANDSCAPE) {
        Orientation.Landscape
    } else {
        Orientation.Portrait
    }
}

fun SharedPreferences.setInt(name: String, value: Int) =
    edit().putInt(name, value).apply()

fun SharedPreferences.setBoolean(name: String, value: Boolean) =
    edit().putBoolean(name, value).apply()

fun SharedPreferences.setString(name: String, value: String) =
    edit().putString(name, value).apply()


/** Convert a dp value to pixels for the current display. */
fun Context.dp(value: Int): Int =
    (value * resources.displayMetrics.density).roundToInt()


private val Context.prefLastXName get() =
    "lastX_" + orientation.name

private val Context.prefLastYName get() =
    "lastY_" + orientation.name

var Context.prefLastX
    get() = sharedPrefs.getInt(prefLastXName, 0)
    set(v) = sharedPrefs.setInt(prefLastXName, v)

var Context.prefLastY
    get() = sharedPrefs.getInt(prefLastYName, 0)
    set(v) = sharedPrefs.setInt(prefLastYName, v)




private val prefShowPercentageName get() =
    "show_percentage"

var Context.prefShowPercentage
    get() = sharedPrefs.getBoolean(prefShowPercentageName, true)
    set(v) = sharedPrefs.setBoolean(prefShowPercentageName, v)




/* ----------------------------- Appearance / theme ----------------------------- */

private val DEF = Appearance.DEFAULT

var Context.prefAppTheme: AppTheme
    get() = runCatching {
        AppTheme.valueOf(sharedPrefs.getString("app_theme", null) ?: AppTheme.System.name)
    }.getOrDefault(AppTheme.System)
    set(v) = sharedPrefs.setString("app_theme", v.name)

var Context.prefButtonSize
    get() = sharedPrefs.getInt("btn_size", DEF.buttonSize)
    set(v) = sharedPrefs.setInt("btn_size", v)

var Context.prefButtonBackgroundColor
    get() = sharedPrefs.getInt("btn_bg", DEF.backgroundColor)
    set(v) = sharedPrefs.setInt("btn_bg", v)

var Context.prefButtonIconColor
    get() = sharedPrefs.getInt("btn_icon", DEF.iconColor)
    set(v) = sharedPrefs.setInt("btn_icon", v)

var Context.prefButtonOpacity
    get() = sharedPrefs.getInt("btn_opacity", DEF.opacity)
    set(v) = sharedPrefs.setInt("btn_opacity", v)

var Context.prefButtonCornerRadius
    get() = sharedPrefs.getInt("btn_corner", DEF.cornerRadius)
    set(v) = sharedPrefs.setInt("btn_corner", v)

var Context.prefPercentageBackgroundColor
    get() = sharedPrefs.getInt("pct_bg", DEF.percentageBackgroundColor)
    set(v) = sharedPrefs.setInt("pct_bg", v)

var Context.prefPercentageTextColor
    get() = sharedPrefs.getInt("pct_text", DEF.percentageTextColor)
    set(v) = sharedPrefs.setInt("pct_text", v)

var Context.prefMoveDelayMs
    get() = sharedPrefs.getInt("move_delay", DEF.moveDelayMs)
    set(v) = sharedPrefs.setInt("move_delay", v)


fun Context.loadAppearance() = Appearance(
    buttonSize = prefButtonSize,
    backgroundColor = prefButtonBackgroundColor,
    iconColor = prefButtonIconColor,
    opacity = prefButtonOpacity,
    cornerRadius = prefButtonCornerRadius,
    percentageBackgroundColor = prefPercentageBackgroundColor,
    percentageTextColor = prefPercentageTextColor,
    moveDelayMs = prefMoveDelayMs,
)

fun Context.saveAppearance(a: Appearance) {
    prefButtonSize = a.buttonSize
    prefButtonBackgroundColor = a.backgroundColor
    prefButtonIconColor = a.iconColor
    prefButtonOpacity = a.opacity
    prefButtonCornerRadius = a.cornerRadius
    prefPercentageBackgroundColor = a.percentageBackgroundColor
    prefPercentageTextColor = a.percentageTextColor
    prefMoveDelayMs = a.moveDelayMs
}

fun applyNightMode(theme: AppTheme) {
    AppCompatDelegate.setDefaultNightMode(
        when (theme) {
            AppTheme.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppTheme.Light -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        }
    )
}



val Context.layoutInflater
    get() = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

/// https://stackoverflow.com/a/78301846
val Context.notificationVolumeFlow
    get() = callbackFlow {
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



fun Context.createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (ignored: Exception) {
        Toast.makeText(this, buildString {
            append(getString(R.string.failed_to_open_url))
            append(": $url")
        }, Toast.LENGTH_SHORT).show()
    }
}


fun Context.createGap(
    width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
    height: Int = 10
) = LinearLayout(this).apply {
    layoutParams = ViewGroup.LayoutParams(
        width,
        height
    )
}

