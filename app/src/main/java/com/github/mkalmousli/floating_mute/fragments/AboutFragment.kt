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
                text = "Go Back"
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
                text = "About"
                textSize = 30f
                contentView.addView(this)
            }


            contentView.addView(
                c.createGap(height = 70)
            )

            TextView(c).apply {
                text = "v2.0.0"
                textSize = 20f
                contentView.addView(this)
            }
            TextView(c).apply {
                text = "Released on 2025.05.01"
                textSize = 15f
                contentView.addView(this)
            }

            contentView.addView(
                c.createGap(height = 70)
            )

            TextView(c).apply {
                text = "This app was created by @mkalmousli"
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
                text = "Report an issue"
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
                text = "View source code on GitHub"
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