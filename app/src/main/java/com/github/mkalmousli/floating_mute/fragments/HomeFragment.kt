package com.github.mkalmousli.floating_mute.fragments

import android.content.Intent
import android.icu.text.DateFormat
import android.os.Bundle
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
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mkalmousli.floating_mute.FloatingViewService
import com.github.mkalmousli.floating_mute.Mode
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.createGap
import com.github.mkalmousli.floating_mute.modeFlow
import com.github.mkalmousli.floating_mute.showPercentageFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.os.Build;
import com.github.mkalmousli.floating_mute.BuildConfig


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
                text = getString(R.string.app_name)
                textSize = 30f
                contentView.addView(this)
            }


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





            TextView(c).apply {
                text = getString(R.string.app_desc)
                textSize = 15f
                updatePadding(top=40)
                contentView.addView(this)
            }

            contentView.addView(
                c.createGap(height = 100)
            )

            Button(c).apply {
                text = getString(R.string.how_to_use)
                textSize = 20f
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                updatePaddingRelative(start = 50, end = 50)

                setOnClickListener {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, HowToUseFragment())
                        .addToBackStack(null)
                        .commit()
                }

                contentView.addView(this)
            }

            Button(c).apply {
                text = getString(R.string.about)
                textSize = 20f
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                updatePaddingRelative(start = 50, end = 50)

                setOnClickListener {
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, AboutFragment())
                        .addToBackStack(null)
                        .commit()
                }

                contentView.addView(this)
            }



            // create a switch to toggle the floating view
            Switch(c).apply {
                text = getString(R.string.enable_floating_mute)
                textSize = 25f
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
                textSize = 15f
                updatePadding(top=30)
                contentView.addView(this)


                lifecycleScope.launch {
                    modeFlow.collect {
                        text = buildString {
                            append(getString(R.string.status))
                            append(": ")
                            append(when (it) {
                                Mode.Enabled -> getString(R.string.enabled)
                                Mode.Disabled -> getString(R.string.disabled)
                                Mode.Hidden -> getString(R.string.hidden)
                            })
                        }
                    }
                }
            }





            contentView.addView(
                c.createGap(height = 100)
            )

            Switch(c).apply {
                text = getString(R.string.show_volume_percentage)
                textSize = 25f
                isChecked = false

                contentView.addView(this)

                lifecycleScope.launch {
                    showPercentageFlow.collectLatest  {
                        isChecked = it
                    }
                }

                setOnCheckedChangeListener { _, isChecked ->
                    lifecycleScope.launch {
                        showPercentageFlow.emit(isChecked)
                    }
                }
            }

        }


        contentView.addView(
            c.createGap(height = 100)
        )

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