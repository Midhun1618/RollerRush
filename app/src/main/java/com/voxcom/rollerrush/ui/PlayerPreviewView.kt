package com.voxcom.rollerrush.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.voxcom.rollerrush.RollerRushApp
import com.voxcom.rollerrush.data.CharacterData
import com.voxcom.rollerrush.player.Player
import com.voxcom.rollerrush.player.PlayerAnimator
import com.voxcom.rollerrush.player.PlayerSprites
import com.voxcom.rollerrush.utils.Constants

/**
 * Small lightweight view that draws the SAME rig/animator as the game, so the character
 * screen shows exactly what the player will see (and animates while skating on the spot).
 */
class PlayerPreviewView(context: Context) : View(context) {
    private val app = context.applicationContext as RollerRushApp
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private var sprites: PlayerSprites? = null
    private var player: Player? = null
    private var animator: PlayerAnimator? = null
    private var last = 0L

    fun setCharacter(data: CharacterData) {
        val old = sprites
        val s = app.gameAssets.createPlayerSprites(data)
        sprites = s
        player = Player(s).also { it.x = 0f; it.bottomY = 0f }  // preview origin = ground contact point
        animator = PlayerAnimator(player!!)
        old?.recycle()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val p = player ?: return
        val a = animator ?: return
        val now = System.nanoTime()
        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
        last = now

        a.update(dt, Constants.BASE_SPEED)
        val scale = height * 0.8f / 115f                  // skater is ~105 units tall
        canvas.save()
        canvas.translate(width / 2f, height * 0.92f)
        canvas.scale(scale, scale)
        p.draw(canvas, paint)
        canvas.restore()
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player = null; animator = null
        sprites?.recycle(); sprites = null
    }
}
