package com.github.mkalmousli.floating_mute

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.updatePadding
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

/**
 * A tiny Material 3 kit. Flat: no card radius, no strokes. Full-bleed rows,
 * large tap targets, clear labels.
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

private fun Context.rippleRes(borderless: Boolean = false): Int {
    val tv = TypedValue()
    theme.resolveAttribute(
        if (borderless) android.R.attr.selectableItemBackgroundBorderless
        else android.R.attr.selectableItemBackground,
        tv, true
    )
    return tv.resourceId
}

fun linLp(w: Int, h: Int, topMargin: Int = 0) = LinearLayout.LayoutParams(w, h).apply {
    this.topMargin = topMargin
}

/** A full-height, vertically-scrolling surface that fills the screen. */
fun Context.screen(build: LinearLayout.() -> Unit): View {
    val column = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        updatePadding(0, 0, 0, dp(28))
        layoutParams = ViewGroup.LayoutParams(MATCH, WRAP)
        build()
    }
    return NestedScrollView(this).apply {
        isFillViewport = true
        setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface))
        addView(column)
    }
}

/** Like [screen] but with a fixed top app bar that stays put while the body scrolls. */
fun Context.screenWithBar(title: String, onBack: () -> Unit, build: LinearLayout.() -> Unit): View {
    val column = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        updatePadding(0, dp(4), 0, dp(28))
        layoutParams = ViewGroup.LayoutParams(MATCH, WRAP)
        build()
    }
    val scroll = NestedScrollView(this).apply {
        isFillViewport = true
        addView(column)
    }
    val divider = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(1))
        setBackgroundColor(themeColor(com.google.android.material.R.attr.colorOutlineVariant))
    }
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(themeColor(com.google.android.material.R.attr.colorSurface))
        addView(topBar(title, onBack))
        addView(divider)
        addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
    }
}

/** Big app identity block for the top of Home: logo, name, one-line description. */
fun Context.heroHeader(name: String, description: String): View = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    setPadding(dp(24), dp(28), dp(24), dp(18))
    layoutParams = linLp(MATCH, WRAP)
    addView(ImageView(this@heroHeader).apply {
        ImageViewCompat.setImageTintList(this, null)
        setImageResource(R.drawable.logo)
        layoutParams = LinearLayout.LayoutParams(dp(84), dp(84))
    })
    addView(MaterialTextView(this@heroHeader).apply {
        text = name
        TextViewCompat.setTextAppearance(
            this, com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium
        )
        setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
        gravity = Gravity.CENTER
        layoutParams = linLp(WRAP, WRAP, dp(12))
    })
    addView(bodyText(description).apply {
        gravity = Gravity.CENTER
        layoutParams = linLp(MATCH, WRAP, dp(6))
    })
}

fun Context.openIcon() = ImageView(this).apply {
    setImageResource(R.drawable.ic_open_in_new)
    imageTintList = ColorStateList.valueOf(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
    layoutParams = LinearLayout.LayoutParams(dp(20), dp(20))
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

/** A left-aligned group heading with room above it. */
fun Context.sectionLabel(text: String) = MaterialTextView(this).apply {
    this.text = text.uppercase()
    TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
    setTextColor(themeColor(com.google.android.material.R.attr.colorPrimary))
    letterSpacing = 0.06f
    setPadding(dp(20), dp(22), dp(20), dp(6))
    layoutParams = linLp(MATCH, WRAP)
}

/** A back arrow + title, used at the top of secondary screens. */
fun Context.topBar(title: String, onBack: () -> Unit): View {
    val back = ImageButton(this).apply {
        setImageResource(R.drawable.ic_arrow_back)
        contentDescription = getString(R.string.back)
        setBackgroundResource(rippleRes(borderless = true))
        val pad = dp(10)
        setPadding(pad, pad, pad, pad)
        layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
        setOnClickListener { onBack() }
    }
    val label = headline(title).apply {
        layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginStart = dp(4) }
    }
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), dp(8), dp(12), dp(8))
        layoutParams = linLp(MATCH, WRAP)
        addView(back)
        addView(label)
    }
}

/** Full-width primary action. Big, obvious, no fuss. */
fun Context.bigButton(text: String, onClick: () -> Unit) =
    MaterialButton(this).apply {
        this.text = text
        TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
        isAllCaps = false
        minimumHeight = dp(60)
        cornerRadius = 0
        insetTop = 0
        insetBottom = 0
        layoutParams = linLp(MATCH, dp(60), dp(2)).apply {
            marginStart = dp(16); marginEnd = dp(16)
        }
        setOnClickListener { onClick() }
    }

/**
 * A full-bleed tappable row: bold title, optional subtitle, optional
 * trailing control (switch, chevron). The whole row is the tap target.
 */
fun Context.row(
    title: String,
    subtitle: String? = null,
    trailing: View? = null,
    bindSubtitle: ((MaterialTextView) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
): View {
    val texts = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        addView(MaterialTextView(this@row).apply {
            this.text = title
            TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            textSize = 17f
            setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurface))
        })
        if (subtitle != null || bindSubtitle != null) addView(bodyText(subtitle ?: "").apply {
            layoutParams = linLp(MATCH, WRAP, dp(2))
            bindSubtitle?.invoke(this)
        })
    }
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(68)
        setPadding(dp(20), dp(14), dp(20), dp(14))
        layoutParams = linLp(MATCH, WRAP)
        if (onClick != null) {
            isClickable = true
            isFocusable = true
            setBackgroundResource(rippleRes())
            setOnClickListener { onClick() }
        }
        addView(texts)
        if (trailing != null) {
            trailing.apply {
                if (layoutParams == null) layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
                (layoutParams as? LinearLayout.LayoutParams)?.marginStart = dp(12)
            }
            addView(trailing)
        }
    }
}

fun Context.chevron() = MaterialTextView(this).apply {
    text = "›"
    TextViewCompat.setTextAppearance(this, com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall)
    setTextColor(themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
}

/** Background for one swatch; a checkerboard means "transparent". */
fun Context.colorDotBackground(color: Int, selected: Boolean): Drawable {
    val strokeColor =
        if (selected) themeColor(com.google.android.material.R.attr.colorPrimary)
        else themeColor(com.google.android.material.R.attr.colorOutline)
    val strokePx = dp(if (selected) 3 else 1)
    val transparent = (color ushr 24) == 0

    val fill: Drawable = if (transparent) {
        checkerDrawable(0x33808080, themeColor(com.google.android.material.R.attr.colorSurface), dp(5))
    } else {
        GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
    }
    val ring = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(0x00000000)
        setStroke(strokePx, strokeColor)
    }
    return LayerDrawable(arrayOf(fill, ring))
}

/** Circular colour dot for a swatch picker. */
fun Context.colorDot(color: Int, sizeDp: Int, selected: Boolean): View =
    View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)).apply { marginEnd = dp(12) }
        background = colorDotBackground(color, selected)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: android.graphics.Outline) {
                o.setOval(0, 0, v.width, v.height)
            }
        }
    }

private fun Context.checkerDrawable(dark: Int, light: Int, cellPx: Int): BitmapDrawable {
    val n = cellPx.coerceAtLeast(2)
    val bmp = Bitmap.createBitmap(n * 2, n * 2, Bitmap.Config.ARGB_8888)
    Canvas(bmp).apply {
        drawColor(light)
        val p = Paint().apply { color = dark }
        drawRect(0f, 0f, n.toFloat(), n.toFloat(), p)
        drawRect(n.toFloat(), n.toFloat(), n * 2f, n * 2f, p)
    }
    return BitmapDrawable(resources, bmp).apply {
        setTileModeXY(android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
    }
}
