package com.github.mkalmousli.floating_mute

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView

/**
 * A tiny Material 3 building-block kit so every screen looks like one app:
 * rounded cards on a surface, generous spacing, real type scale.
 */

val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** dp -> px, usable from inside view-builder lambdas. */
fun View.dp(value: Int): Int = context.dp(value)

fun Context.themeColor(attr: Int): Int {
    val tv = TypedValue()
    theme.resolveAttribute(attr, tv, true)
    return tv.data
}

fun linLp(w: Int, h: Int, topMargin: Int = 0) = LinearLayout.LayoutParams(w, h).apply {
    this.topMargin = topMargin
}

/** A vertically-scrolling screen with comfortable page margins. */
fun Context.screen(build: LinearLayout.() -> Unit): View {
    val column = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        updatePadding(dp(16), dp(16), dp(16), dp(32))
        layoutParams = ViewGroup.LayoutParams(MATCH, WRAP)
        build()
    }
    return NestedScrollView(this).apply {
        isFillViewport = true
        setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface))
        addView(column)
    }
}

/** A filled surface card holding a vertical stack of content. */
fun Context.card(topMargin: Int = dp(12), build: LinearLayout.() -> Unit): MaterialCardView {
    val inner = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        updatePadding(dp(20), dp(20), dp(20), dp(20))
        build()
    }
    return MaterialCardView(this).apply {
        layoutParams = linLp(MATCH, WRAP, topMargin)
        radius = dp(24).toFloat()
        cardElevation = 0f
        setCardBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceVariant))
        addView(inner)
    }
}

fun Context.headline(text: String) = MaterialTextView(this).apply {
    this.text = text
    TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall)
    setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
}

fun Context.titleText(text: String) = MaterialTextView(this).apply {
    this.text = text
    TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
    setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
}

fun Context.bodyText(text: String) = MaterialTextView(this).apply {
    this.text = text
    TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
    setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
}

fun Context.sectionLabel(text: String) = MaterialTextView(this).apply {
    this.text = text.uppercase()
    TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
    setTextColor(themeColor(com.google.android.material.R.attr.colorPrimary))
    letterSpacing = 0.08f
    layoutParams = linLp(MATCH, WRAP, dp(20))
}

fun Context.filledButton(text: String, onClick: () -> Unit) =
    MaterialButton(this).apply {
        this.text = text
        layoutParams = linLp(MATCH, WRAP, dp(8))
        setOnClickListener { onClick() }
    }

fun Context.tonalButton(text: String, onClick: () -> Unit) =
    MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        this.text = text
        layoutParams = linLp(MATCH, WRAP, dp(8))
        setOnClickListener { onClick() }
    }

fun Context.outlinedButton(text: String, onClick: () -> Unit) =
    MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
        this.text = text
        layoutParams = linLp(MATCH, WRAP, dp(8))
        setOnClickListener { onClick() }
    }

/** A tappable row: label + supporting text on the left, chevron on the right. */
fun Context.navRow(title: String, subtitle: String? = null, onClick: () -> Unit): View {
    val texts = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        addView(titleText(title))
        if (subtitle != null) addView(bodyText(subtitle))
    }
    val chevron = MaterialTextView(this).apply {
        text = "›"
        TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall)
        setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
    }
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        updatePadding(dp(20), dp(16), dp(20), dp(16))
        addView(texts)
        addView(chevron)
    }
    return MaterialCardView(this).apply {
        layoutParams = linLp(MATCH, WRAP, dp(8))
        radius = dp(20).toFloat()
        cardElevation = 0f
        isClickable = true
        isFocusable = true
        setCardBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurfaceVariant))
        addView(row)
        setOnClickListener { onClick() }
    }
}

fun Context.spacer(height: Int) = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(MATCH, dp(height))
}
