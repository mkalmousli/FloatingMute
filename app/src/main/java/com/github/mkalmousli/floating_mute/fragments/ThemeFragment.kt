package com.github.mkalmousli.floating_mute.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mkalmousli.floating_mute.Appearance
import com.github.mkalmousli.floating_mute.AppTheme
import com.github.mkalmousli.floating_mute.R
import com.github.mkalmousli.floating_mute.appThemeFlow
import com.github.mkalmousli.floating_mute.appearanceFlow
import com.github.mkalmousli.floating_mute.createGap
import com.github.mkalmousli.floating_mute.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * A single screen where the user can shape the whole look of the app:
 * the UI theme (system / light / dark) and every visual aspect of the
 * floating button, with a live preview.
 */
class ThemeFragment : Fragment() {

    private val swatches = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF303030.toInt(), 0xFF757575.toInt(),
        0xFFF44336.toInt(), 0xFFFF9800.toInt(), 0xFFFFEB3B.toInt(), 0xFF4CAF50.toInt(),
        0xFF009688.toInt(), 0xFF2196F3.toInt(), 0xFF3F51B5.toInt(), 0xFF9C27B0.toInt(),
    )

    private fun dp(v: Int) = requireContext().dp(v)

    private fun current() = appearanceFlow.value

    private fun update(block: (Appearance) -> Appearance) {
        lifecycleScope.launch { appearanceFlow.emit(block(current())) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val c = requireContext()

        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            updatePadding(dp(16), dp(16), dp(16), dp(16))
        }

        root.addView(Button(c).apply {
            text = getString(R.string.go_back)
            setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        })

        root.addView(TextView(c).apply {
            text = getString(R.string.themes)
            textSize = 28f
            updatePadding(top = dp(12), bottom = dp(12))
        })

        /* ---------- live preview ---------- */
        val previewIcon = ImageView(c).apply {
            setImageResource(R.drawable.volume_up_50)
        }
        val previewBox = LinearLayout(c).apply {
            gravity = Gravity.CENTER
            addView(previewIcon)
        }
        root.addView(FrameLayout(c).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(160)
            )
            setBackgroundColor(0x11888888)
            addView(previewBox, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ))
        })
        root.addView(c.createGap(height = dp(16)))

        fun renderPreview(a: Appearance) {
            val size = c.dp(a.buttonSize)
            previewIcon.layoutParams = previewIcon.layoutParams.apply {
                width = size; height = size
            }
            previewIcon.requestLayout()
            ImageViewCompat.setImageTintList(
                previewIcon, android.content.res.ColorStateList.valueOf(a.iconColor)
            )
            previewBox.background = GradientDrawable().apply {
                setColor(a.backgroundColor)
                cornerRadius = c.dp(a.cornerRadius).toFloat()
            }
            previewBox.setPadding(c.dp(10), c.dp(10), c.dp(10), c.dp(10))
            previewBox.alpha = a.opacity / 100f
        }

        /* ---------- app theme ---------- */
        root.addView(sectionTitle(getString(R.string.app_theme)))
        val themeRow = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val themeButtons = AppTheme.values().map { t ->
            Button(c).apply {
                text = when (t) {
                    AppTheme.System -> getString(R.string.theme_system)
                    AppTheme.Light -> getString(R.string.theme_light)
                    AppTheme.Dark -> getString(R.string.theme_dark)
                }
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { lifecycleScope.launch { appThemeFlow.emit(t) } }
                themeRow.addView(this)
            }
        }
        root.addView(themeRow)
        lifecycleScope.launch {
            appThemeFlow.collectLatest { active ->
                AppTheme.values().forEachIndexed { i, t ->
                    themeButtons[i].alpha = if (t == active) 1f else 0.45f
                }
            }
        }

        /* ---------- sliders ---------- */
        val sizeLabel = sectionTitle("")
        val opacityLabel = sectionTitle("")
        val cornerLabel = sectionTitle("")
        val delayLabel = sectionTitle("")

        root.addView(sizeLabel)
        root.addView(slider(Appearance.MIN_SIZE, Appearance.MAX_SIZE, { current().buttonSize }) { v ->
            update { it.copy(buttonSize = v) }
        })
        root.addView(opacityLabel)
        root.addView(slider(Appearance.MIN_OPACITY, Appearance.MAX_OPACITY, { current().opacity }) { v ->
            update { it.copy(opacity = v) }
        })
        root.addView(cornerLabel)
        root.addView(slider(0, Appearance.MAX_CORNER, { current().cornerRadius }) { v ->
            update { it.copy(cornerRadius = v) }
        })
        root.addView(delayLabel)
        root.addView(slider(0, Appearance.MAX_MOVE_DELAY, { current().moveDelayMs }) { v ->
            update { it.copy(moveDelayMs = v) }
        })

        /* ---------- colors ---------- */
        root.addView(sectionTitle(getString(R.string.button_background)))
        root.addView(swatchRow { color -> update { it.copy(backgroundColor = color) } })

        root.addView(sectionTitle(getString(R.string.icon_color)))
        root.addView(swatchRow { color -> update { it.copy(iconColor = color) } })

        root.addView(sectionTitle(getString(R.string.percentage_background)))
        root.addView(swatchRow { color -> update { it.copy(percentageBackgroundColor = color) } })

        root.addView(sectionTitle(getString(R.string.percentage_text)))
        root.addView(swatchRow { color -> update { it.copy(percentageTextColor = color) } })

        root.addView(c.createGap(height = dp(24)))
        root.addView(Button(c).apply {
            text = getString(R.string.reset_to_defaults)
            setOnClickListener { update { Appearance.DEFAULT } }
        })
        root.addView(c.createGap(height = dp(48)))

        // keep every label and the preview in sync with the live appearance
        lifecycleScope.launch {
            appearanceFlow.collectLatest { a ->
                renderPreview(a)
                sizeLabel.text = getString(R.string.button_size) + ": ${a.buttonSize} dp"
                opacityLabel.text = getString(R.string.opacity) + ": ${a.opacity}%"
                cornerLabel.text = getString(R.string.corner_radius) + ": ${a.cornerRadius} dp"
                delayLabel.text = getString(R.string.move_sensitivity) + ": ${a.moveDelayMs} ms"
            }
        }

        return ScrollView(c).apply { addView(root) }
    }

    private fun sectionTitle(text: String) = TextView(requireContext()).apply {
        this.text = text
        textSize = 15f
        updatePadding(top = dp(18), bottom = dp(4))
    }

    private fun slider(
        min: Int,
        max: Int,
        get: () -> Int,
        onChange: (Int) -> Unit,
    ) = SeekBar(requireContext()).apply {
        this.max = max - min
        progress = (get() - min).coerceIn(0, this.max)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) onChange(p + min)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        lifecycleScope.launch {
            appearanceFlow.collectLatest {
                val target = (get() - min).coerceIn(0, this@apply.max)
                if (target != progress) progress = target
            }
        }
    }

    private fun swatchRow(onPick: (Int) -> Unit): View {
        val c = requireContext()
        val row = LinearLayout(c).apply { orientation = LinearLayout.HORIZONTAL }
        for (color in swatches) {
            row.addView(View(c).apply {
                layoutParams = LinearLayout.LayoutParams(c.dp(34), c.dp(34)).apply {
                    marginEnd = c.dp(8)
                }
                background = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = c.dp(6).toFloat()
                    setStroke(c.dp(1), Color.GRAY)
                }
                setOnClickListener { onPick(color) }
            })
        }
        return android.widget.HorizontalScrollView(c).apply { addView(row) }
    }
}
