package com.voxcom.rollerrush.ui

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import com.voxcom.rollerrush.utils.ScreenUtils

/** Translucent overlay on top of GameActivity. Result codes tell GameActivity what to do. Back = resume. */
class PauseActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(170, 0, 0, 0))
            addView(UiKit.title(this@PauseActivity, "PAUSED", 36f))
            addView(UiKit.button(this@PauseActivity, "RESUME") { setResult(RESULT_RESUME); finish() })
            addView(UiKit.button(this@PauseActivity, "RESTART") { setResult(RESULT_RESTART); finish() })
            addView(UiKit.button(this@PauseActivity, "HOME") { setResult(RESULT_HOME); finish() })
        }
        setContentView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onBackPressed() { setResult(RESULT_RESUME); finish() }

    companion object {
        const val RESULT_RESUME = Activity.RESULT_FIRST_USER
        const val RESULT_RESTART = Activity.RESULT_FIRST_USER + 1
        const val RESULT_HOME = Activity.RESULT_FIRST_USER + 2
    }
}
