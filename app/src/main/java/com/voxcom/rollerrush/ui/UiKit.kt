package com.voxcom.rollerrush.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.voxcom.rollerrush.utils.ScreenUtils

/** Deliberately plain, programmatic UI helpers (the visual design will be replaced later). */
object UiKit {
    val BG = Color.rgb(27, 42, 65)

    fun screen(ctx: Context, vararg views: View): ScrollView {
        val p = ScreenUtils.dp(ctx, 16)
        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(p * 2, p, p * 2, p)
            views.forEach { addView(it) }
        }
        return ScrollView(ctx).apply {
            setBackgroundColor(BG)
            isFillViewport = true
            addView(column, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    fun title(ctx: Context, text: String, sp: Float = 36f) = TextView(ctx).apply {
        this.text = text; textSize = sp; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 0, 0, ScreenUtils.dp(ctx, 8))
    }

    fun label(ctx: Context, text: String, sp: Float = 18f) = TextView(ctx).apply {
        this.text = text; textSize = sp; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        setPadding(0, ScreenUtils.dp(ctx, 4), 0, ScreenUtils.dp(ctx, 4))
    }

    fun button(ctx: Context, text: String, widthDp: Int = 240, onClick: () -> Unit) = Button(ctx).apply {
        this.text = text
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(ScreenUtils.dp(ctx, widthDp), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            val m = ScreenUtils.dp(ctx, 4)
            setMargins(m, m, m, m)
        }
    }

    fun row(ctx: Context, vararg views: View) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        views.forEach { addView(it) }
    }
}
