package com.github.mkalmousli.floating_mute.fragments

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mkalmousli.floating_mute.BuildConfig
import com.github.mkalmousli.floating_mute.FloatingViewService
import com.github.mkalmousli.floating_mute.MATCH
import com.github.mkalmousli.floating_mute.Mode
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.WRAP
import com.github.mkalmousli.floating_mute.bodyText
import com.github.mkalmousli.floating_mute.card
import com.github.mkalmousli.floating_mute.dp
import com.github.mkalmousli.floating_mute.headline
import com.github.mkalmousli.floating_mute.linLp
import com.github.mkalmousli.floating_mute.modeFlow
import com.github.mkalmousli.floating_mute.navRow
import com.github.mkalmousli.floating_mute.prefShowPercentage
import com.github.mkalmousli.floating_mute.screen
import com.github.mkalmousli.floating_mute.showPercentageFlow
import com.github.mkalmousli.floating_mute.titleText
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private fun open(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .replace(R.id.frameLayout, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val c = requireContext()

        return c.screen {

            addView(c.headline(getString(R.string.app_name)).apply {
                layoutParams = linLp(MATCH, WRAP, dp(4))
            })
            addView(c.bodyText(getString(R.string.app_desc)).apply {
                layoutParams = linLp(MATCH, WRAP, dp(4))
            })

            // --- Enable card ---
            addView(c.card {
                val enableSwitch = MaterialSwitch(c).apply {
                    text = getString(R.string.enable_floating_mute)
                    textSize = 18f
                    layoutParams = linLp(MATCH, WRAP)
                }
                val statusText = c.bodyText("").apply {
                    layoutParams = linLp(MATCH, WRAP, dp(6))
                }
                addView(enableSwitch)
                addView(statusText)

                lifecycleScope.launch {
                    modeFlow.collectLatest { mode ->
                        val running = mode == Mode.Enabled || mode == Mode.Hidden
                        if (enableSwitch.isChecked != running) enableSwitch.isChecked = running
                        statusText.text = getString(
                            R.string.status_line,
                            getString(
                                when (mode) {
                                    Mode.Enabled -> R.string.enabled
                                    Mode.Disabled -> R.string.disabled
                                    Mode.Hidden -> R.string.hidden
                                }
                            )
                        )
                    }
                }

                enableSwitch.setOnClickListener {
                    val intent = Intent(c, FloatingViewService::class.java)
                    if (enableSwitch.isChecked && modeFlow.value == Mode.Disabled) {
                        ContextCompat.startForegroundService(c, intent)
                    } else if (!enableSwitch.isChecked && modeFlow.value != Mode.Disabled) {
                        c.stopService(intent)
                    }
                }
            })

            // --- Quick settings ---
            addView(c.card {
                addView(c.titleText(getString(R.string.quick_settings)))
                val pctSwitch = MaterialSwitch(c).apply {
                    text = getString(R.string.show_volume_percentage)
                    textSize = 16f
                    layoutParams = linLp(MATCH, WRAP, dp(12))
                }
                addView(pctSwitch)
                lifecycleScope.launch {
                    showPercentageFlow.collectLatest {
                        if (pctSwitch.isChecked != it) pctSwitch.isChecked = it
                    }
                }
                pctSwitch.setOnClickListener {
                    val on = pctSwitch.isChecked
                    c.prefShowPercentage = on
                    lifecycleScope.launch { showPercentageFlow.emit(on) }
                }
            })

            // --- Navigation ---
            addView(c.navRow(getString(R.string.themes), getString(R.string.themes_subtitle)) {
                open(ThemeFragment())
            })
            addView(c.navRow(getString(R.string.how_to_use), null) { open(HowToUseFragment()) })
            addView(c.navRow(getString(R.string.about), null) { open(AboutFragment()) })

            // --- Version footer ---
            addView(c.bodyText(
                getString(R.string.version_footer, BuildConfig.VERSION_NAME, BuildConfig.RELEASE_DAY)
            ).apply {
                layoutParams = linLp(MATCH, WRAP, dp(24))
                gravity = Gravity.CENTER_HORIZONTAL
                alpha = 0.7f
            })

            // small brand mark
            addView(ImageView(c).apply {
                ImageViewCompat.setImageTintList(this, null)
                setImageResource(R.drawable.logo)
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                    topMargin = dp(8)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                updatePadding(0, 0, 0, 0)
            })
        }
    }
}
