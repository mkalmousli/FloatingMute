package com.github.mkalmousli.floating_mute.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.github.mkalmousli.floating_mute.BuildConfig
import com.github.mkalmousli.floating_mute.MATCH
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.WRAP
import com.github.mkalmousli.floating_mute.bodyText
import com.github.mkalmousli.floating_mute.card
import com.github.mkalmousli.floating_mute.dp
import com.github.mkalmousli.floating_mute.headline
import com.github.mkalmousli.floating_mute.linLp
import com.github.mkalmousli.floating_mute.openUrl
import com.github.mkalmousli.floating_mute.outlinedButton
import com.github.mkalmousli.floating_mute.screen
import com.github.mkalmousli.floating_mute.tonalButton

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val c = requireContext()

        return c.screen {
            addView(c.outlinedButton(getString(R.string.go_back)) {
                requireActivity().supportFragmentManager.popBackStack()
            })
            addView(c.headline(getString(R.string.about)).apply {
                layoutParams = linLp(MATCH, WRAP, dp(8))
            })

            addView(c.card {
                addView(ImageView(c).apply {
                    ImageViewCompat.setImageTintList(this, null)
                    setImageResource(R.drawable.logo)
                    layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
                })
                addView(c.headline(getString(R.string.app_name)).apply {
                    layoutParams = linLp(MATCH, WRAP, dp(8))
                })
                addView(c.bodyText(
                    getString(R.string.version_footer, BuildConfig.VERSION_NAME, BuildConfig.RELEASE_DAY)
                ))
                addView(c.bodyText(getString(R.string.built_on) + " " + BuildConfig.BUILD_TIME).apply {
                    alpha = 0.6f
                })
                addView(c.bodyText(getString(R.string.this_app_was_created_by_mkalmousli)).apply {
                    layoutParams = linLp(MATCH, WRAP, dp(16))
                })
            })

            addView(c.card {
                addView(c.tonalButton(getString(R.string.website)) { c.openUrl("https://al-mo.de") })
                addView(c.tonalButton("Instagram") { c.openUrl("https://instagram.com/mkalmousli") })
                addView(c.tonalButton(getString(R.string.report_an_issue)) {
                    c.openUrl("https://github.com/mkalmousli/FloatingMute/issues/new")
                })
                addView(c.tonalButton(getString(R.string.view_source_code_on_github)) {
                    c.openUrl("https://github.com/mkalmousli/FloatingMute")
                })
            })

            addView(c.bodyText("GPLv3 · Free Software").apply {
                layoutParams = linLp(MATCH, WRAP, dp(20))
                gravity = Gravity.CENTER_HORIZONTAL
                alpha = 0.7f
            })
        }
    }
}
