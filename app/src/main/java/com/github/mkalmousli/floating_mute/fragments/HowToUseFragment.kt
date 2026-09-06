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
import com.github.mkalmousli.floating_mute.card
import com.github.mkalmousli.floating_mute.dp
import com.github.mkalmousli.floating_mute.linLp
import com.github.mkalmousli.floating_mute.screen
import com.github.mkalmousli.floating_mute.topBar

class HowToUseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val c = requireContext()

        return c.screen {
            addView(c.topBar(getString(R.string.how_to_use)) {
                requireActivity().supportFragmentManager.popBackStack()
            })

            addView(c.card {
                getString(R.string.instructions).split("\n").filter { it.isNotBlank() }
                    .forEachIndexed { i, line ->
                        addView(c.bodyText(line.trim()).apply {
                            layoutParams = linLp(MATCH, WRAP, if (i == 0) 0 else dp(10))
                        })
                    }
            })
        }
    }
}
