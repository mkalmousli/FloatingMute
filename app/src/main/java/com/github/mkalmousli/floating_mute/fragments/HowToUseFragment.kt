package com.github.mkalmousli.floating_mute.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.github.mkalmousli.floating_mute.createGap

class HowToUseFragment : Fragment() {
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
                text = "How to use?"
                textSize = 30f
                contentView.addView(this)
            }


            contentView.addView(
                c.createGap(height = 100)
            )

            val instructions = arrayListOf(
                "Tap the floating button to mute or unmute.",
                "Drag it to move it.",
                "Long press to disable the it.",
                "The notification allows you to hide and show it at any time.",
                "You can also quit the app by tapping on 'exit' in the notification.",
                "Tap on the notification to open the app so you can change the settings."
            )

            for (line in instructions) {
                TextView(c).apply {
                    text = line
                    textSize = 20f
                    contentView.addView(this)
                }

                contentView.addView(
                    c.createGap(height = 20)
                )
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