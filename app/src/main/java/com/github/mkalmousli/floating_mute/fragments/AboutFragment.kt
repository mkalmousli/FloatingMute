package com.github.mkalmousli.floating_mute.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import com.github.mkalmousli.floating_mute.BuildConfig
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.createGap
import com.github.mkalmousli.floating_mute.openUrl

class AboutFragment : Fragment() {
    val layout by lazy {
        val c = requireContext()

        val contentView = LinearLayout(c).also { contentView ->
            contentView.orientation = LinearLayout.VERTICAL
            contentView.updatePadding(20, 20, 20, 20)



            Button(c).apply {
                text = context.getString(R.string.go_back)
                textSize = 20f
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    requireActivity().supportFragmentManager.popBackStack()
                }
                contentView.addView(this)
            }

            contentView.addView(
                c.createGap(height = 50)
            )

            TextView(c).apply {
                text = getString(R.string.about)
                textSize = 30f
                contentView.addView(this)
            }


            contentView.addView(
                c.createGap(height = 70)
            )

            LinearLayout(c).also { versionView ->
                versionView.orientation = LinearLayout.HORIZONTAL

                TextView(c).apply {
                    text = buildString {
                        append("v")
                        append(BuildConfig.VERSION_NAME)
                    }
                    textSize = 20f
                    updatePadding(right=20)
                    versionView.addView(this)
                }


                TextView(c).apply {
                    text = buildString {
                        append(getString(R.string.released_on))
                        append(" ")
                        append(BuildConfig.RELEASE_DAY)
                    }
                    textSize = 10f
                    alpha = 0.5f
                    versionView.addView(this)
                }

                contentView.addView(versionView)
            }



            LinearLayout(c).also { builtOnView ->
                builtOnView.orientation = LinearLayout.VERTICAL
                builtOnView.updatePadding(top = 10)

                TextView(c).apply {
                    text = getString(R.string.built_on)
                    textSize = 14f
                    updatePadding(right=20)

                    builtOnView.addView(this)
                }


                TextView(c).apply {
                    text = BuildConfig.BUILD_TIME
                    textSize = 8f
                    alpha = 0.5f
                    builtOnView.addView(this)
                }

                contentView.addView(builtOnView)
            }


            contentView.addView(
                c.createGap(height = 70)
            )

            TextView(c).apply {
                text = getString(R.string.this_app_was_created_by_mkalmousli)
                textSize = 20f
                updatePadding(top = 50)
                contentView.addView(this)
            }

            ImageView(c).apply {
                val icon = R.drawable.mk
                ImageViewCompat.setImageTintList(this, null)
                setImageResource(icon)
                layoutParams = ViewGroup.LayoutParams(100, 100)
                contentView.addView(this)
            }

            LinearLayout(c).also { socialsView ->
                socialsView.orientation = LinearLayout.HORIZONTAL
                socialsView.updatePadding(top = 20)

                Button(c).apply {
                    text = "GitHub"
                    textSize = 20f
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        requireContext().openUrl("https://github.com/mkalmousli")
                    }
                    socialsView.addView(this)
                }

                Button(c).apply {
                    text = "Instagram"
                    textSize = 20f
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        requireContext().openUrl("https://instagram.com/mkalmousli")
                    }
                    socialsView.addView(this)
                }

                contentView.addView(socialsView)
            }



            Button(c).apply {
                text = getString(R.string.report_an_issue)
                textSize = 20f
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    requireContext().openUrl("https://github.com/mkalmousli/FloatingMute/issues/new")
                }
                contentView.addView(this)
            }
            Button(c).apply {
                text = getString(R.string.view_source_code_on_github)
                textSize = 20f
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    requireContext().openUrl("https://github.com/mkalmousli/FloatingMute")
                }
                contentView.addView(this)
            }
        }


        ScrollView(c).apply {
            addView(contentView)
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = layout
}