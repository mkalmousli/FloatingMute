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
import com.github.mkalmousli.floating_mute.AppTheme
import com.github.mkalmousli.floating_mute.BuildConfig
import com.github.mkalmousli.floating_mute.FloatingViewService
import com.github.mkalmousli.floating_mute.MATCH
import com.github.mkalmousli.floating_mute.Mode
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.WRAP
import com.github.mkalmousli.floating_mute.appThemeFlow
import com.github.mkalmousli.floating_mute.bodyText
import com.github.mkalmousli.floating_mute.chevron
import com.github.mkalmousli.floating_mute.dp
import com.github.mkalmousli.floating_mute.headline
import com.github.mkalmousli.floating_mute.linLp
import com.github.mkalmousli.floating_mute.modeFlow
import com.github.mkalmousli.floating_mute.openUrl
import com.github.mkalmousli.floating_mute.prefShowPercentage
import com.github.mkalmousli.floating_mute.row
import com.github.mkalmousli.floating_mute.screen
import com.github.mkalmousli.floating_mute.sectionLabel
import com.github.mkalmousli.floating_mute.showPercentageFlow
import com.github.mkalmousli.floating_mute.themeColor
import com.google.android.material.button.MaterialButton
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

            // header
            addView(LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(8))
                addView(ImageView(c).apply {
                    ImageViewCompat.setImageTintList(this, null)
                    setImageResource(R.drawable.logo)
                    layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
                })
                addView(c.headline(getString(R.string.app_name)).apply {
                    layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginStart = dp(12) }
                })
                addView(c.bodyText("v" + BuildConfig.VERSION_NAME).apply { alpha = 0.6f })
            })

            // floating button toggle
            val enableSwitch = MaterialSwitch(c)
            addView(c.row(
                getString(R.string.enable_floating_mute),
                trailing = enableSwitch,
                bindSubtitle = { sub ->
                    lifecycleScope.launch {
                        modeFlow.collectLatest { mode ->
                            val running = mode != Mode.Disabled
                            if (enableSwitch.isChecked != running) enableSwitch.isChecked = running
                            sub.text = getString(
                                when (mode) {
                                    Mode.Enabled -> R.string.enabled
                                    Mode.Disabled -> R.string.disabled
                                    Mode.Hidden -> R.string.hidden
                                }
                            )
                        }
                    }
                },
            ) { enableSwitch.toggle() })
            enableSwitch.setOnCheckedChangeListener { _, checked ->
                val intent = Intent(c, FloatingViewService::class.java)
                if (checked && modeFlow.value == Mode.Disabled) {
                    ContextCompat.startForegroundService(c, intent)
                } else if (!checked && modeFlow.value != Mode.Disabled) {
                    c.stopService(intent)
                }
            }

            // volume %
            val pctSwitch = MaterialSwitch(c)
            addView(c.row(
                getString(R.string.show_volume_percentage),
                trailing = pctSwitch,
            ) { pctSwitch.toggle() })
            lifecycleScope.launch {
                showPercentageFlow.collectLatest {
                    if (pctSwitch.isChecked != it) pctSwitch.isChecked = it
                }
            }
            pctSwitch.setOnCheckedChangeListener { _, on ->
                if (c.prefShowPercentage == on) return@setOnCheckedChangeListener
                c.prefShowPercentage = on
                lifecycleScope.launch { showPercentageFlow.emit(on) }
            }

            // theme, right here on the home screen
            addView(c.sectionLabel(getString(R.string.app_theme)))
            val themeButtons = ArrayList<MaterialButton>()
            addView(LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(16), 0, dp(16), 0)
                layoutParams = linLp(MATCH, WRAP)
                AppTheme.values().forEachIndexed { i, t ->
                    val b = MaterialButton(c).apply {
                        text = getString(
                            when (t) {
                                AppTheme.System -> R.string.theme_system
                                AppTheme.Light -> R.string.theme_light
                                AppTheme.Dark -> R.string.theme_dark
                            }
                        )
                        isAllCaps = false
                        minimumHeight = dp(54)
                        cornerRadius = 0
                        insetTop = 0
                        insetBottom = 0
                        strokeWidth = 0
                        layoutParams = LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                            marginStart = if (i == 0) 0 else dp(2)
                        }
                        setOnClickListener { lifecycleScope.launch { appThemeFlow.emit(t) } }
                    }
                    themeButtons.add(b)
                    addView(b)
                }
            })
            val primary = c.themeColor(com.google.android.material.R.attr.colorPrimary)
            val onPrimary = c.themeColor(com.google.android.material.R.attr.colorOnPrimary)
            val surfaceV = c.themeColor(com.google.android.material.R.attr.colorSurfaceVariant)
            val onSurfaceV = c.themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
            lifecycleScope.launch {
                appThemeFlow.collectLatest { active ->
                    AppTheme.values().forEachIndexed { i, t ->
                        val sel = t == active
                        themeButtons[i].backgroundTintList =
                            android.content.res.ColorStateList.valueOf(if (sel) primary else surfaceV)
                        themeButtons[i].setTextColor(if (sel) onPrimary else onSurfaceV)
                    }
                }
            }

            addView(c.row(
                getString(R.string.appearance),
                subtitle = getString(R.string.appearance_subtitle),
                trailing = c.chevron(),
            ) { open(ThemeFragment()) })
            addView(c.row(
                getString(R.string.how_to_use),
                trailing = c.chevron(),
            ) { open(HowToUseFragment()) })

            // push the about block to the bottom of the screen
            addView(View(c), linLp(MATCH, 0).apply { weight = 1f })

            // about
            addView(c.sectionLabel(getString(R.string.about)))
            addView(LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(20), dp(4), dp(20), dp(4))
                addView(ImageView(c).apply {
                    ImageViewCompat.setImageTintList(this, null)
                    setImageResource(R.drawable.mk)
                    layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
                })
                addView(c.bodyText(getString(R.string.made_by)).apply {
                    layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = dp(12) }
                })
            })
            addView(android.widget.HorizontalScrollView(c).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(dp(12), dp(4), dp(12), dp(4))
                addView(LinearLayout(c).apply {
                    orientation = LinearLayout.HORIZONTAL
                    fun link(text: String, url: String) = addView(c.bodyText(text).apply {
                        setTextColor(c.themeColor(com.google.android.material.R.attr.colorPrimary))
                        setPadding(dp(8), dp(10), dp(8), dp(10))
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
                    "   ·   " + getString(R.string.license_line)
            ).apply {
                setPadding(dp(20), dp(6), dp(20), dp(0))
                alpha = 0.6f
                textSize = 12f
            })
        }
    }
}
