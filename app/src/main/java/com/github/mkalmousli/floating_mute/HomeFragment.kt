package com.github.mkalmousli.floating_mute

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch



class HomeFragment : Fragment() {

    private val layout by lazy {
        val c = requireContext()

        val contentView = LinearLayout(c).also { contentView ->
            contentView.orientation = LinearLayout.VERTICAL
            contentView.updatePadding(20, 20, 20, 20)



            ImageView(c).apply {
                val icon = R.drawable.logo
                ImageViewCompat.setImageTintList(this, null)
                setImageResource(icon)
                layoutParams = ViewGroup.LayoutParams(200, 200)
                contentView.addView(this)
            }

            TextView(c).apply {
                text = "Floating Mute"
                textSize = 30f
                contentView.addView(this)
            }


            LinearLayout(c).also { versionView ->
                versionView.orientation = LinearLayout.HORIZONTAL

                TextView(c).apply {
                    text = "v1.0.0"
                    textSize = 20f
                    updatePadding(right=20)
                    versionView.addView(this)
                }


                TextView(c).apply {
                    text = "05.04.2025"
                    textSize = 10f
                    alpha = 0.5f
                    versionView.addView(this)
                }

                contentView.addView(versionView)
            }


            TextView(c).apply {
                text = "This app allows you to mute or unmute the volume of your phone with a single click."
                textSize = 15f
                updatePadding(top=40)
                contentView.addView(this)
            }



            // create a switch to toggle the floating view
            Switch(c).apply {
                text = "Enable Floating View"
                textSize = 30f
                isChecked = false
                updatePadding(top=120)

                contentView.addView(this)

                lifecycleScope.launch {
                    modeFlow.collectLatest  {
                        isChecked = it == Mode.Enabled || it == Mode.Hidden
                    }
                }

                setOnCheckedChangeListener { _, isChecked ->
                    val intent = Intent(requireContext(), FloatingViewService::class.java)
                    val currentMode = modeFlow.value

                    if (isChecked && currentMode == Mode.Disabled) {
                        requireContext().startService(intent)
                    } else if (!isChecked && (currentMode == Mode.Enabled || currentMode == Mode.Hidden)) {
                        requireContext().stopService(intent)
                    }
                }
            }


            TextView(c).apply {
                text = "Status: Disabled"
                textSize = 15f
                updatePadding(top=30)
                contentView.addView(this)


                lifecycleScope.launch {
                    modeFlow.collect {
                        text = "Status: ${it.name}"
                    }
                }
            }




            TextView(c).apply {
                text = "Created by @mkalmousli"
                textSize = 14f
                updatePadding(top=120)
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
                socialsView.updatePadding(top=20)

                Button(c).apply {
                    text = "GitHub"
                    textSize = 13f
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        requireContext().openUrl("https://github.com/mkalmousli")
                    }
                    socialsView.addView(this)
                }

                Button(c).apply {
                    text = "Instagram"
                    textSize = 13f
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    setOnClickListener {
                        requireContext().openUrl("https://instagram.com/mkalmousli")
                    }
                    socialsView.addView(this)
                }

                contentView.addView(socialsView)
            }



            Button(c).apply {
                text = "Report an issue"
                textSize = 13f
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    requireContext().openUrl("https://github.com/mkalmousli/FloatingMute/issues/new")
                }
                contentView.addView(this)
            }
            Button(c).apply {
                text = "View source code on GitHub"
                textSize = 13f
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
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