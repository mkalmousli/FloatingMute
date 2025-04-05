package com.github.mkalmousli.floating_mute

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class HomeFragment : Fragment() {

    private val layout by lazy {
        val c = requireContext()

        val contentView = LinearLayout(c).also { contentView ->
            contentView.orientation = LinearLayout.VERTICAL

            TextView(c).apply {
                text = "Floating Mute"
                textSize = 30f
                contentView.addView(this)
            }

            TextView(c).apply {
                text = "v1.0"
                textSize = 21f
                contentView.addView(this)
            }

            // add a padding view to separate the title and the switch
            View(c).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
                contentView.addView(this)
            }

            // create a switch to toggle the floating view
            Switch(c).apply {
                text = "Enable Floating View"
                isChecked = false
                contentView.addView(this)

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        requireContext().startService(Intent(requireContext(), FloatingViewService::class.java))
                    } else if (requireContext().stopService(Intent(requireContext(), FloatingViewService::class.java))) {
                        // If the service was stopped successfully, we can assume that it was running.
                        // Otherwise, we can assume that it was not running.
                        println("Service stopped successfully")
                    }
                    else {
                        requireContext().stopService(Intent(requireContext(), FloatingViewService::class.java))
                    }
                }
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
    ): View? = layout

}