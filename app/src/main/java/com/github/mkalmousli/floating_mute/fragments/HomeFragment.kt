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
import com.github.mkalmousli.floating_mute.openUrl
import com.github.mkalmousli.floating_mute.prefShowPercentage
import com.github.mkalmousli.floating_mute.screen
import com.github.mkalmousli.floating_mute.showPercentageFlow
import com.github.mkalmousli.floating_mute.themeColor
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private fun open(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.frag_enter, R.anim.frag_exit,
                R.anim.frag_pop_enter, R.anim.frag_pop_exit
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

            // header: logo + name
            addView(LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(c).apply {
                    ImageViewCompat.setImageTintList(this, null)
                    setImageResource(R.drawable.logo)
                    layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
                })
                addView(c.headline(getString(R.string.app_name)).apply {
                    layoutParams = linLp(WRAP, WRAP).apply { marginStart = dp(10) }
                })
            })

            // enable + status in one line
            addView(c.card {
                val enableSwitch = MaterialSwitch(c).apply {
                    text = getString(R.string.enable_floating_mute)
                    layoutParams = linLp(MATCH, WRAP)
                }
                val statusText = c.bodyText("").apply {
                    layoutParams = linLp(MATCH, WRAP, dp(4))
                }
                addView(enableSwitch)
                addView(statusText)

                lifecycleScope.launch {
                    modeFlow.collectLatest { mode ->
                        val running = mode != Mode.Disabled
                        if (enableSwitch.isChecked != running) enableSwitch.isChecked = running
                        statusText.text = getString(
                            when (mode) {
                                Mode.Enabled -> R.string.enabled
                                Mode.Disabled -> R.string.disabled
                                Mode.Hidden -> R.string.hidden
                            }
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

                val pctSwitch = MaterialSwitch(c).apply {
                    text = getString(R.string.show_volume_percentage)
                    layoutParams = linLp(MATCH, WRAP, dp(10))
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

            addView(c.navRow(getString(R.string.themes), getString(R.string.themes_subtitle)) {
                open(ThemeFragment())
            })
            addView(c.navRow(getString(R.string.how_to_use)) { open(HowToUseFragment()) })

            // about, folded in
            addView(c.card {
                addView(LinearLayout(c).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(ImageView(c).apply {
                        ImageViewCompat.setImageTintList(this, null)
                        setImageResource(R.drawable.mk)
                        layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
                    })
                    addView(c.bodyText(getString(R.string.made_by)).apply {
                        layoutParams = linLp(WRAP, WRAP).apply { marginStart = dp(10) }
                    })
                })

                addView(android.widget.HorizontalScrollView(c).apply {
                    isHorizontalScrollBarEnabled = false
                    layoutParams = linLp(MATCH, WRAP, dp(4))
                    addView(LinearLayout(c).apply {
                        orientation = LinearLayout.HORIZONTAL
                        fun link(text: String, url: String) = addView(c.bodyText(text).apply {
                            setTextColor(c.themeColor(com.google.android.material.R.attr.colorPrimary))
                            setPadding(dp(4), dp(6), dp(12), dp(6))
                            isClickable = true
                            setOnClickListener { c.openUrl(url) }
                        })
                        link(getString(R.string.website), "https://al-mo.de")
                        link("Instagram", "https://instagram.com/mkalmousli")
                        link(getString(R.string.report_an_issue), "https://github.com/mkalmousli/FloatingMute/issues/new")
                        link(getString(R.string.view_source_code_on_github), "https://github.com/mkalmousli/FloatingMute")
                    })
                })

                addView(c.bodyText(
                    getString(R.string.version_footer, BuildConfig.VERSION_NAME, BuildConfig.RELEASE_DAY) +
                        "  ·  " + getString(R.string.license_line)
                ).apply {
                    layoutParams = linLp(MATCH, WRAP, dp(8))
                    alpha = 0.7f
                    textSize = 11f
                })
            })
        }
    }
}
