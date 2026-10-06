package com.voxcom.rollerrush.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.voxcom.rollerrush.data.GamePreferences
import com.voxcom.rollerrush.utils.AssetManager
import com.voxcom.rollerrush.utils.Constants

/**
 * Draws one frame from the GameWorld state. Everything it needs (Paints, Rects, StringBuilder)
 * is allocated once here; render() itself allocates nothing and decodes nothing.
 */
class GameRenderer(private val assets: AssetManager, private val prefs: GamePreferences) {

    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val skyPaint = Paint()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val debugPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; typeface = Typeface.DEFAULT_BOLD; setShadowLayer(4f, 0f, 2f, Color.BLACK)
    }
    private val dst = RectF()
    private val sb = StringBuilder(32)

    /** Pause button in SCREEN pixels (GameView hit-tests touches against it). */
    val pauseRect = RectF()
    private var margin = 0f

    fun onSizeChanged(w: Int, h: Int) {
        skyPaint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
            Color.rgb(110, 198, 255), Color.rgb(232, 246, 255), Shader.TileMode.CLAMP)
        textPaint.textSize = h * 0.07f
        margin = h * 0.05f
        val btn = h * 0.13f
        pauseRect.set(w - margin - btn, margin, w - margin, margin + btn)
    }

    fun render(canvas: Canvas, world: GameWorld, camera: Camera) {
        // Sky is drawn in screen space so it fills letterboxed areas on tall tablets too.
        canvas.drawRect(0f, 0f, camera.screenWidth, camera.screenHeight, skyPaint)

        canvas.save()
        camera.applyTo(canvas, Constants.PLAYER_X, Constants.GROUND_Y - 65f) // from here on: world units
        val view = camera.viewWidth
        val scroll = world.scrollX

        // Parallax background + ground
        drawTiled(canvas, assets.farBackground, AssetManager.TILE_W.toFloat(),
            Constants.GROUND_Y - 200f, 200f, scroll * 0.12f, view)
        drawTiled(canvas, assets.midBackground, AssetManager.TILE_W.toFloat(),
            Constants.GROUND_Y - 140f, 140f, scroll * 0.35f, view)
        fill.color = Color.rgb(74, 78, 105)
        canvas.drawRect(-200f, Constants.GROUND_Y, view + 200f, Constants.GROUND_Y + 600f, fill) // below-tile fill
        drawTiled(canvas, assets.groundTile, 120f, Constants.GROUND_Y, Constants.GROUND_THICKNESS, scroll, view)

        // Entities
        val coins = world.coins
        for (i in 0 until coins.size) { val c = coins[i]; if (c.active) c.draw(canvas, assets, bmpPaint) }
        val obs = world.obstacles
        for (i in 0 until obs.size) { val o = obs[i]; if (o.active) o.draw(canvas, assets, bmpPaint) }
        val pu = world.powerUps
        for (i in 0 until pu.size) { val p = pu[i]; if (p.active) p.draw(canvas, assets, fill) }

        world.player.draw(canvas, bmpPaint)

        drawForeground(canvas, scroll, view)
        if (prefs.debugHitboxes) drawDebug(canvas, world)
        canvas.restore()

        drawHud(canvas, world, camera)
    }

    /** Tiles [bmp] horizontally, scrolling by [scroll]. Slight overlap hides seams. */
    private fun drawTiled(canvas: Canvas, bmp: Bitmap, tileW: Float, top: Float, h: Float, scroll: Float, viewW: Float) {
        var x = -(scroll % tileW)
        while (x < viewW) {
            dst.set(x, top, x + tileW + 0.5f, top + h)
            canvas.drawBitmap(bmp, null, dst, bmpPaint)
            x += tileW
        }
    }

    /** Simple foreground: dark posts scrolling faster than the ground (adds depth). */
    private fun drawForeground(canvas: Canvas, scroll: Float, view: Float) {
        fill.color = Color.argb(200, 30, 32, 48)
        val spacing = 260f
        var x = -((scroll * 1.35f) % spacing)
        while (x < view) {
            canvas.drawRect(x, Constants.GROUND_Y + 30f, x + 8f, Constants.GROUND_Y + 60f, fill)
            x += spacing
        }
    }

    private fun drawDebug(canvas: Canvas, world: GameWorld) {
        debugPaint.color = Color.GREEN
        canvas.drawRect(world.player.bodyHitbox, debugPaint)
        canvas.drawRect(world.player.feetHitbox, debugPaint)
        debugPaint.color = Color.RED
        for (i in 0 until world.obstacles.size) { val o = world.obstacles[i]; if (o.active) canvas.drawRect(o.hitbox, debugPaint) }
        debugPaint.color = Color.YELLOW
        for (i in 0 until world.coins.size) { val c = world.coins[i]; if (c.active) canvas.drawCircle(c.centerX, c.centerY, Constants.COIN_PICKUP_RADIUS, debugPaint) }
    }

    /** HUD in screen space so it is independent of the world scale. */
    private fun drawHud(canvas: Canvas, world: GameWorld, camera: Camera) {
        val h = camera.screenHeight
        val leftX = margin + h * 0.04f   // a little extra to stay clear of display cut-outs

        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = h * 0.07f
        sb.setLength(0); sb.append("Score: ").append(world.score)
        canvas.drawText(sb, 0, sb.length, leftX, margin + textPaint.textSize, textPaint)
        textPaint.textSize = h * 0.045f
        sb.setLength(0); sb.append("Distance: ").append(world.distanceMeters).append(" m")
        canvas.drawText(sb, 0, sb.length, leftX, margin + h * 0.07f + textPaint.textSize * 1.3f, textPaint)

        // Speed boost state. It is deliberately compact so it never dominates the game view.
        if (world.speedBoostActive || world.speedBoostAvailable) {
            textPaint.textSize = h * 0.038f
            textPaint.textAlign = Paint.Align.LEFT
            sb.setLength(0)
            if (world.speedBoostActive) sb.append("BOOST!") else sb.append("BOOST READY • DOUBLE TAP")
            canvas.drawText(sb, 0, sb.length, leftX, margin + h * 0.07f + textPaint.textSize * 2.8f, textPaint)
        }

        // Coins: gold dot + number, right-aligned just left of the pause button.
        textPaint.textSize = h * 0.07f
        textPaint.textAlign = Paint.Align.RIGHT
        sb.setLength(0); sb.append(world.runCoins)
        val rightX = pauseRect.left - margin * 0.7f
        canvas.drawText(sb, 0, sb.length, rightX, margin + textPaint.textSize, textPaint)
        val tw = textPaint.measureText(sb, 0, sb.length)
        fill.color = Color.rgb(255, 193, 7)
        val r = h * 0.03f
        canvas.drawCircle(rightX - tw - r * 1.6f, margin + textPaint.textSize * 0.62f, r, fill)

        // Pause button "||"
        fill.color = Color.argb(120, 0, 0, 0)
        canvas.drawRoundRect(pauseRect, 12f, 12f, fill)
        fill.color = Color.WHITE
        val bw = pauseRect.width() * 0.14f
        val cx = pauseRect.centerX()
        val top = pauseRect.top + pauseRect.height() * 0.25f
        val bottom = pauseRect.bottom - pauseRect.height() * 0.25f
        canvas.drawRect(cx - bw * 1.6f, top, cx - bw * 0.6f, bottom, fill)
        canvas.drawRect(cx + bw * 0.6f, top, cx + bw * 1.6f, bottom, fill)
    }
}
