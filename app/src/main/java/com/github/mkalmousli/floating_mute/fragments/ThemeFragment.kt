package com.github.mkalmousli.floating_mute.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mkalmousli.floating_mute.Appearance
import com.github.mkalmousli.floating_mute.MATCH
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.WRAP
import com.github.mkalmousli.floating_mute.appearanceFlow
import com.github.mkalmousli.floating_mute.bodyText
import com.github.mkalmousli.floating_mute.bigButton
import com.github.mkalmousli.floating_mute.colorDotBackground
import com.github.mkalmousli.floating_mute.dp
import com.github.mkalmousli.floating_mute.linLp
import com.github.mkalmousli.floating_mute.screen
import com.github.mkalmousli.floating_mute.sectionLabel
import com.github.mkalmousli.floating_mute.themeColor
import com.github.mkalmousli.floating_mute.topBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Fine-tune the floating button: size, feel and colours, with a live preview. */
class ThemeFragment : Fragment() {

    private val swatches = intArrayOf(
        Color.TRANSPARENT,
        0xFFFFFFFF.toInt(), 0xFF1C1B1F.toInt(), 0xFF444444.toInt(), 0xFF8A8A8A.toInt(),
        0xFFE5484D.toInt(), 0xFFF76808.toInt(), 0xFFFFC53D.toInt(), 0xFF46A758.toInt(),
        0xFF12A594.toInt(), 0xFF3E63DD.toInt(), 0xFF6E56CF.toInt(), 0xFFD6409F.toInt(),
    )

    private fun a() = appearanceFlow.value
    private fun update(block: (Appearance) -> Appearance) {
        lifecycleScope.launch { appearanceFlow.emit(block(a())) }
    }

    private fun dp(v: Int) = requireContext().dp(v)
    private fun pad(v: View) = v.apply { setPadding(dp(20), dp(6), dp(20), dp(6)) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val c = requireContext()

        lateinit var previewIcon: ImageView
        lateinit var previewChip: LinearLayout
        lateinit var sizeLabel: TextView
        lateinit var opacityLabel: TextView
        lateinit var cornerLabel: TextView
        lateinit var delayLabel: TextView

        val root = c.screen {

            addView(c.topBar(getString(R.string.themes)) {
                requireActivity().supportFragmentManager.popBackStack()
            })

            // preview
            addView(c.sectionLabel(getString(R.string.preview)))
            previewIcon = ImageView(c).apply { setImageResource(R.drawable.volume_up_50) }
            previewChip = LinearLayout(c).apply {
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(10), dp(10), dp(10))
                addView(previewIcon)
            }
            addView(FrameLayout(c).apply {
                layoutParams = linLp(MATCH, dp(128))
                addView(previewChip, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
            })

            // feel
            addView(c.sectionLabel(getString(R.string.button_shape_feel)))
            sizeLabel = label(); addView(sizeLabel)
            addView(pad(slider(Appearance.MIN_SIZE, Appearance.MAX_SIZE, 4, { a().buttonSize }) {
                update { s -> s.copy(buttonSize = it) }
            }))
            opacityLabel = label(); addView(opacityLabel)
            addView(pad(slider(Appearance.MIN_OPACITY, Appearance.MAX_OPACITY, 5, { a().opacity }) {
                update { s -> s.copy(opacity = it) }
            }))
            cornerLabel = label(); addView(cornerLabel)
            addView(pad(slider(0, Appearance.MAX_CORNER, 2, { a().cornerRadius }) {
                update { s -> s.copy(cornerRadius = it) }
            }))
            delayLabel = label(); addView(delayLabel)
            addView(pad(slider(0, Appearance.MAX_MOVE_DELAY, 25, { a().moveDelayMs }) {
                update { s -> s.copy(moveDelayMs = it) }
            }))

            // colours
            addView(c.sectionLabel(getString(R.string.colors)))
            addView(caption(getString(R.string.button_background)))
            addView(swatchRow({ a().backgroundColor }) { col -> update { it.copy(backgroundColor = col) } })
            addView(caption(getString(R.string.icon_color)))
            addView(swatchRow({ a().iconColor }) { col -> update { it.copy(iconColor = col) } })
            addView(caption(getString(R.string.percentage_background)))
            addView(swatchRow({ a().percentageBackgroundColor }) { col -> update { it.copy(percentageBackgroundColor = col) } })
            addView(caption(getString(R.string.percentage_text)))
            addView(swatchRow({ a().percentageTextColor }) { col -> update { it.copy(percentageTextColor = col) } })

            addView(c.bigButton(getString(R.string.reset_to_defaults)) { confirmReset() }
                .apply { (layoutParams as LinearLayout.LayoutParams).topMargin = dp(24) })
        }

        lifecycleScope.launch {
            appearanceFlow.collectLatest { ap ->
                val size = c.dp(ap.buttonSize)
                previewIcon.layoutParams = (previewIcon.layoutParams ?: ViewGroup.LayoutParams(size, size))
                    .apply { width = size; height = size }
                previewIcon.requestLayout()
                ImageViewCompat.setImageTintList(
                    previewIcon, android.content.res.ColorStateList.valueOf(ap.iconColor)
                )
                previewChip.background = GradientDrawable().apply {
                    setColor(ap.backgroundColor)
                    cornerRadius = c.dp(ap.cornerRadius).toFloat()
                }
                previewChip.alpha = ap.opacity / 100f

                sizeLabel.text = getString(R.string.button_size) + "   ${ap.buttonSize} dp"
                opacityLabel.text = getString(R.string.opacity) + "   ${ap.opacity}%"
                cornerLabel.text = getString(R.string.corner_radius) + "   ${ap.cornerRadius} dp"
                delayLabel.text = getString(R.string.move_sensitivity) + "   ${ap.moveDelayMs} ms"
            }
        }

        return root
    }

    private fun confirmReset() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reset_confirm_title)
            .setMessage(R.string.reset_confirm_body)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.reset_to_defaults) { _, _ ->
                update { Appearance.DEFAULT }
            }
            .show()
    }

    private fun label() = requireContext().bodyText("").apply {
        setPadding(dp(20), dp(16), dp(20), dp(2))
        setTextColor(requireContext().themeColor(com.google.android.material.R.attr.colorOnSurface))
    }

    private fun caption(text: String) = requireContext().bodyText(text).apply {
        setPadding(dp(20), dp(14), dp(20), dp(2))
    }

    private fun slider(
        min: Int, max: Int, step: Int,
        get: () -> Int, onChange: (Int) -> Unit,
    ): Slider {
        fun snap(raw: Int): Float {
            val clamped = raw.coerceIn(min, max)
            val ticks = Math.round((clamped - min) / step.toFloat())
            return (min + ticks * step).coerceIn(min, max).toFloat()
        }
        return Slider(requireContext()).apply {
            valueFrom = min.toFloat()
            valueTo = max.toFloat()
            stepSize = step.toFloat()
            value = snap(get())
            layoutParams = linLp(MATCH, WRAP)
            addOnChangeListener { _, v, fromUser -> if (fromUser) onChange(v.toInt()) }
            lifecycleScope.launch {
                appearanceFlow.collectLatest {
                    val target = snap(get())
                    if (target != value) value = target
                }
            }
        }
    }

    private fun swatchRow(selected: () -> Int, onPick: (Int) -> Unit): View {
        val c = requireContext()
        val row = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(20), dp(6), dp(20), dp(6))
        }
        val dots = swatches.map { color ->
            val v = View(c).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) }
                background = c.colorDotBackground(color, color == selected())
                contentDescription =
                    if ((color ushr 24) == 0) getString(R.string.transparent) else null
                setOnClickListener { onPick(color) }
            }
            color to v
        }
        dots.forEach { row.addView(it.second) }

        var last = selected()
        lifecycleScope.launch {
            appearanceFlow.collectLatest {
                val sel = selected()
                if (sel != last) {
                    last = sel
                    dots.forEach { (color, v) -> v.background = c.colorDotBackground(color, color == sel) }
                }
            }
        }
        return HorizontalScrollView(c).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }
}
