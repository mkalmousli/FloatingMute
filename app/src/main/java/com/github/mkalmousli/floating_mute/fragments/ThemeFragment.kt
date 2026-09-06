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
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mkalmousli.floating_mute.AppTheme
import com.github.mkalmousli.floating_mute.Appearance
import com.github.mkalmousli.floating_mute.MATCH
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.WRAP
import com.github.mkalmousli.floating_mute.appThemeFlow
import com.github.mkalmousli.floating_mute.appearanceFlow
import com.github.mkalmousli.floating_mute.bodyText
import com.github.mkalmousli.floating_mute.card
import com.github.mkalmousli.floating_mute.dp
import com.github.mkalmousli.floating_mute.headline
import com.github.mkalmousli.floating_mute.linLp
import com.github.mkalmousli.floating_mute.outlinedButton
import com.github.mkalmousli.floating_mute.screen
import com.github.mkalmousli.floating_mute.sectionLabel
import com.github.mkalmousli.floating_mute.themeColor
import com.github.mkalmousli.floating_mute.titleText
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * One place to shape the whole look of the app: the UI theme and every
 * visual aspect of the floating button, with a live preview.
 */
class ThemeFragment : Fragment() {

    private val swatches = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFF1C1B1F.toInt(), 0xFF444444.toInt(), 0xFF8A8A8A.toInt(),
        0xFFE5484D.toInt(), 0xFFF76808.toInt(), 0xFFFFC53D.toInt(), 0xFF46A758.toInt(),
        0xFF12A594.toInt(), 0xFF3E63DD.toInt(), 0xFF6E56CF.toInt(), 0xFFD6409F.toInt(),
    )

    private fun a() = appearanceFlow.value
    private fun update(block: (Appearance) -> Appearance) {
        lifecycleScope.launch { appearanceFlow.emit(block(a())) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val c = requireContext()

        lateinit var previewIcon: ImageView
        lateinit var previewChip: LinearLayout
        lateinit var sizeLabel: android.widget.TextView
        lateinit var opacityLabel: android.widget.TextView
        lateinit var cornerLabel: android.widget.TextView
        lateinit var delayLabel: android.widget.TextView

        val root = c.screen {

            addView(c.outlinedButton(getString(R.string.go_back)) {
                requireActivity().supportFragmentManager.popBackStack()
            })
            addView(c.headline(getString(R.string.themes)).apply {
                layoutParams = linLp(MATCH, WRAP, dp(8))
            })

            // --- live preview ---
            addView(c.card {
                addView(c.sectionLabel(getString(R.string.preview)))
                previewIcon = ImageView(c).apply { setImageResource(R.drawable.volume_up_50) }
                previewChip = LinearLayout(c).apply {
                    gravity = Gravity.CENTER
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    addView(previewIcon)
                }
                addView(FrameLayout(c).apply {
                    layoutParams = linLp(MATCH, dp(150), dp(8))
                    addView(previewChip, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER))
                })
            })

            // --- app theme ---
            addView(c.card {
                addView(c.titleText(getString(R.string.app_theme)))
                val group = MaterialButtonToggleGroup(c).apply {
                    isSingleSelection = true
                    layoutParams = linLp(MATCH, WRAP, dp(12))
                }
                val ids = IntArray(AppTheme.values().size)
                AppTheme.values().forEachIndexed { i, t ->
                    val b = MaterialButton(
                        c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
                    ).apply {
                        id = View.generateViewId()
                        text = getString(
                            when (t) {
                                AppTheme.System -> R.string.theme_system
                                AppTheme.Light -> R.string.theme_light
                                AppTheme.Dark -> R.string.theme_dark
                            }
                        )
                        layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
                    }
                    ids[i] = b.id
                    group.addView(b)
                }
                addView(group)
                group.addOnButtonCheckedListener { _, checkedId, isChecked ->
                    if (!isChecked) return@addOnButtonCheckedListener
                    val idx = ids.indexOf(checkedId)
                    if (idx >= 0) lifecycleScope.launch { appThemeFlow.emit(AppTheme.values()[idx]) }
                }
                lifecycleScope.launch {
                    appThemeFlow.collectLatest {
                        val id = ids[it.ordinal]
                        if (group.checkedButtonId != id) group.check(id)
                    }
                }
            })

            // --- button shape & feel ---
            addView(c.card {
                addView(c.titleText(getString(R.string.button_shape_feel)))

                sizeLabel = bodyLabel()
                addView(sizeLabel)
                addView(slider(Appearance.MIN_SIZE, Appearance.MAX_SIZE, 4, { a().buttonSize }) { v ->
                    update { it.copy(buttonSize = v) }
                })

                opacityLabel = bodyLabel()
                addView(opacityLabel)
                addView(slider(Appearance.MIN_OPACITY, Appearance.MAX_OPACITY, 5, { a().opacity }) { v ->
                    update { it.copy(opacity = v) }
                })

                cornerLabel = bodyLabel()
                addView(cornerLabel)
                addView(slider(0, Appearance.MAX_CORNER, 2, { a().cornerRadius }) { v ->
                    update { it.copy(cornerRadius = v) }
                })

                delayLabel = bodyLabel()
                addView(delayLabel)
                addView(slider(0, Appearance.MAX_MOVE_DELAY, 25, { a().moveDelayMs }) { v ->
                    update { it.copy(moveDelayMs = v) }
                })
            })

            // --- colors ---
            addView(c.card {
                addView(c.titleText(getString(R.string.colors)))

                addView(c.sectionLabel(getString(R.string.button_background)))
                addView(swatchRow { color -> update { it.copy(backgroundColor = color) } })

                addView(c.sectionLabel(getString(R.string.icon_color)))
                addView(swatchRow { color -> update { it.copy(iconColor = color) } })

                addView(c.sectionLabel(getString(R.string.percentage_background)))
                addView(swatchRow { color -> update { it.copy(percentageBackgroundColor = color) } })

                addView(c.sectionLabel(getString(R.string.percentage_text)))
                addView(swatchRow { color -> update { it.copy(percentageTextColor = color) } })
            })

            addView(c.outlinedButton(getString(R.string.reset_to_defaults)) {
                update { Appearance.DEFAULT }
            }.apply { layoutParams = linLp(MATCH, WRAP, dp(16)) })
        }

        lifecycleScope.launch {
            appearanceFlow.collectLatest { ap ->
                val size = c.dp(ap.buttonSize)
                previewIcon.layoutParams = (previewIcon.layoutParams ?: ViewGroup.LayoutParams(size, size)).apply {
                    width = size; height = size
                }
                previewIcon.requestLayout()
                ImageViewCompat.setImageTintList(
                    previewIcon, android.content.res.ColorStateList.valueOf(ap.iconColor)
                )
                previewChip.background = GradientDrawable().apply {
                    setColor(ap.backgroundColor)
                    cornerRadius = c.dp(ap.cornerRadius).toFloat()
                }
                previewChip.alpha = ap.opacity / 100f

                sizeLabel.text = getString(R.string.button_size) + ":  ${ap.buttonSize} dp"
                opacityLabel.text = getString(R.string.opacity) + ":  ${ap.opacity}%"
                cornerLabel.text = getString(R.string.corner_radius) + ":  ${ap.cornerRadius} dp"
                delayLabel.text = getString(R.string.move_sensitivity) + ":  ${ap.moveDelayMs} ms"
            }
        }

        return root
    }

    private fun bodyLabel() = requireContext().bodyText("").apply {
        layoutParams = linLp(MATCH, WRAP, dp(16))
    }

    private fun slider(
        min: Int,
        max: Int,
        step: Int,
        get: () -> Int,
        onChange: (Int) -> Unit,
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

    private fun swatchRow(onPick: (Int) -> Unit): View {
        val c = requireContext()
        val row = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            updatePadding(top = dp(8), bottom = dp(4))
        }
        for (color in swatches) {
            row.addView(View(c).apply {
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply { marginEnd = dp(10) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke(dp(1), c.themeColor(com.google.android.material.R.attr.colorOutline))
                }
                setOnClickListener { onPick(color) }
            })
        }
        return HorizontalScrollView(c).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
        }
    }

    private fun dp(v: Int) = requireContext().dp(v)
}
