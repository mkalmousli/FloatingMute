package com.github.mkalmousli.floating_mute.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mkalmousli.floating_mute.MATCH
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.WRAP
import com.github.mkalmousli.floating_mute.bodyText
import com.github.mkalmousli.floating_mute.dp
import com.github.mkalmousli.floating_mute.linLp
import com.github.mkalmousli.floating_mute.screenWithBar

class HowToUseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val c = requireContext()

        return c.screenWithBar(getString(R.string.how_to_use), {
            requireActivity().supportFragmentManager.popBackStack()
        }) {
            getString(R.string.instructions).split("\n").filter { it.isNotBlank() }
                .forEach { line ->
                    addView(c.bodyText("•  " + line.trim()).apply {
                        textSize = 16f
                        layoutParams = linLp(MATCH, WRAP, dp(14))
                        setPadding(dp(20), 0, dp(20), 0)
                    })
                }
        }
    }
}
