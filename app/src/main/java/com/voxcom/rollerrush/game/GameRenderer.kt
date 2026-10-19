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
import kotlin.math.floor

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
    private val boostTrailOffsets = floatArrayOf(
        13f, 27f, 42f, 58f
    )

    private val boostTrailAlphas = intArrayOf(
        72, 52, 34, 20
    )
    private val sb = StringBuilder(32)

    // ============================================================
// SPEED BOOST: AFTERIMAGES + SPEED PARTICLES
// ============================================================

    private var boostLastNs = 0L
    private var boostTrailPhase = 0f
    private var boostParticleAccumulator = 0f

    private val boostParticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2f
    }

    // Fixed-size particle pool: no particle objects created per frame.
    private val boostParticleCount = 36

    private val boostParticleX = FloatArray(boostParticleCount)
    private val boostParticleY = FloatArray(boostParticleCount)
    private val boostParticleLength = FloatArray(boostParticleCount)
    private val boostParticleLife = FloatArray(boostParticleCount)
    private val boostParticleMaxLife = FloatArray(boostParticleCount)
    private val boostParticleSpeed = FloatArray(boostParticleCount)
    private val boostParticleSlant = FloatArray(boostParticleCount)

    private var boostParticleIndex = 0
    private var boostRandomState = 9371

    private fun boostRandom(): Float {
        boostRandomState =
            (boostRandomState * 1103515245 + 12345) and 0x7fffffff

        return boostRandomState.toFloat() / Int.MAX_VALUE.toFloat()
    }

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
        camera.applyTo(canvas, world.player.x, Constants.GROUND_Y - 65f) // from here on: world units
        val left = camera.visibleLeft
        val right = camera.visibleRight
        val scroll = world.scrollX

        // Parallax background + ground
        drawTiled(canvas, assets.farBackground, AssetManager.TILE_W.toFloat(),
            Constants.GROUND_Y - 200f, 200f, scroll * 0.12f, left, right)

        drawTiled(
            canvas,
            assets.midBackground,
            AssetManager.TILE_W.toFloat(),
            Constants.GROUND_Y - 140f,
            140f,
            scroll * 0.35f,
            left,
            right
        )

// NEW: decorative roadside layer, behind the road.
        drawRoadsideScenery(canvas, scroll, left, right)

        fill.color = Color.rgb(74, 78, 105)
        canvas.drawRect(
            left - 50f,
            Constants.GROUND_Y,
            right + 50f,
            Constants.GROUND_Y + 600f,
            fill
        )

        drawTiled(
            canvas,
            assets.groundTile,
            120f,
            Constants.GROUND_Y,
            Constants.GROUND_THICKNESS,
            scroll,
            left,
            right
        )

        // Intro building (part of the world, scrolls left naturally)
        val introBuildingOffsetX = -100f
        val introX = -scroll + introBuildingOffsetX
        val introRight = introX + Constants.INTRO_BACKGROUND_WORLD_WIDTH

        if (introX < right && introRight > left) {
            dst.set(introX, 0f, introRight, Constants.INTRO_BACKGROUND_WORLD_HEIGHT)
            canvas.drawBitmap(assets.introBackground, null, dst, bmpPaint)
        }

        // Entities
        val coins = world.coins
        for (i in 0 until coins.size) {
            val c = coins[i]
            if (c.active) c.draw(canvas, assets, bmpPaint)
        }

        if (world.rushRuby.active) {
            world.rushRuby.draw(canvas, assets, bmpPaint)
        }

        val obs = world.obstacles
        for (i in 0 until obs.size) {
            val o = obs[i]
            if (o.active) o.draw(canvas, assets, bmpPaint)
        }

        val pu = world.powerUps
        for (i in 0 until pu.size) {
            val p = pu[i]
            if (p.active) p.draw(canvas, assets, fill)
        }

        // Booster trails must appear behind the actual skater.
        drawSpeedBoostEffect(canvas, world)
        world.player.draw(canvas, bmpPaint)
        drawForeground(canvas, scroll, left, right)
        if (prefs.debugHitboxes) drawDebug(canvas, world)
        canvas.restore()

        drawHud(canvas, world, camera)
    }

    private fun drawSpeedBoostEffect(
        canvas: Canvas,
        world: GameWorld
    ) {
        val now = System.nanoTime()

        val dt = if (boostLastNs == 0L) {
            0f
        } else {
            ((now - boostLastNs) / 1_000_000_000f)
                .coerceIn(0f, 0.05f)
        }

        boostLastNs = now

        val boosting = world.speedBoostActive
        val player = world.player

        // Animate the afterimage trail.
        if (boosting) {
            boostTrailPhase += dt * 11f

            if (boostTrailPhase > 1000f) {
                boostTrailPhase -= 1000f
            }

            // Spawn speed particles.
            boostParticleAccumulator += dt

            while (boostParticleAccumulator >= 0.035f) {
                boostParticleAccumulator -= 0.035f
                spawnBoostParticle(player.x, player.bottomY)
            }
        } else {
            boostParticleAccumulator = 0f
        }

        // ========================================================
        // 1. CHARACTER AFTERIMAGES
        // ========================================================
        if (boosting) {
            for (i in boostTrailOffsets.indices) {
                val wave =
                    kotlin.math.sin(boostTrailPhase - i * 0.8f) * 2.5f

                canvas.save()

                // Move each copy progressively farther backwards.
                canvas.translate(
                    -boostTrailOffsets[i] - wave,
                    wave * 0.45f
                )

                // Older copies are more transparent.
                bmpPaint.alpha = boostTrailAlphas[i]

                player.draw(canvas, bmpPaint)

                canvas.restore()
            }
        }

        // Restore full opacity before drawing particles or the real player.
        bmpPaint.alpha = 255

        // ========================================================
        // 2. SPEED PARTICLES
        // ========================================================
        for (i in 0 until boostParticleCount) {
            if (boostParticleLife[i] <= 0f) continue

            boostParticleX[i] -= boostParticleSpeed[i] * dt
            boostParticleLife[i] -= dt

            val life = boostParticleLife[i]

            if (life <= 0f) {
                boostParticleLife[i] = 0f
                continue
            }

            val progress = (
                    1f - life / boostParticleMaxLife[i]
                    ).coerceIn(0f, 1f)

            val alpha = (
                    210f * (1f - progress) *
                            (1f - progress * 0.35f)
                    ).toInt().coerceIn(0, 210)

            if (alpha <= 0) continue

            val x = boostParticleX[i]
            val y = boostParticleY[i]
            val length = boostParticleLength[i]

            boostParticlePaint.color = Color.argb(
                alpha, 70, 220, 255
            )

            boostParticlePaint.strokeWidth =
                1.2f + (1f - progress) * 2.2f

            canvas.drawLine(
                x,
                y,
                x - length,
                y + boostParticleSlant[i],
                boostParticlePaint
            )

            // Occasional brighter streaks.
            if (i % 5 == 0 && progress < 0.7f) {
                boostParticlePaint.color = Color.argb(
                    (alpha * 0.65f).toInt(),
                    220, 250, 255
                )

                boostParticlePaint.strokeWidth = 1.4f

                canvas.drawLine(
                    x,
                    y,
                    x - length * 0.55f,
                    y + boostParticleSlant[i] * 0.5f,
                    boostParticlePaint
                )
            }
        }

        // ========================================================
        // 3. SUBTLE ACCENT NEAR THE SKATES
        // ========================================================
        if (boosting) {
            val pulse = 0.5f +
                    0.5f * kotlin.math.sin(boostTrailPhase * 1.7f)

            boostParticlePaint.color = Color.argb(
                (45f + pulse * 45f).toInt(),
                60, 210, 255
            )

            boostParticlePaint.strokeWidth = 2.2f

            val playerCenterX = player.x + 18f
            val groundY = player.bottomY - 5f

            canvas.drawLine(
                playerCenterX - 4f,
                groundY,
                playerCenterX - 24f - pulse * 8f,
                groundY + 2f,
                boostParticlePaint
            )
        }

        // Keep shared Paint opacity safe for subsequent drawing.
        bmpPaint.alpha = 255
    }

    private fun spawnBoostParticle(
        playerX: Float,
        playerBottomY: Float
    ) {
        val i = boostParticleIndex

        boostParticleIndex =
            (boostParticleIndex + 1) % boostParticleCount

        // Spawn in the region just behind and around the skater.
        boostParticleX[i] =
            playerX + 5f + boostRandom() * 32f

        boostParticleY[i] =
            playerBottomY - 8f - boostRandom() * 68f

        boostParticleLength[i] =
            12f + boostRandom() * 38f

        val life =
            0.20f + boostRandom() * 0.22f

        boostParticleLife[i] = life
        boostParticleMaxLife[i] = life

        boostParticleSpeed[i] =
            320f + boostRandom() * 480f

        boostParticleSlant[i] =
            (boostRandom() - 0.5f) * 12f
    }

    /** Tiles [bmp] horizontally, scrolling by [scroll]. Slight overlap hides seams. */
    /** Tiles [bmp] across the whole visible range [left, right]. Slight overlap hides seams. */
    private fun drawTiled(canvas: Canvas, bmp: Bitmap, tileW: Float, top: Float, h: Float,
                          scroll: Float, left: Float, right: Float) {
        var x = floor((left + scroll) / tileW) * tileW - scroll
        while (x < right) {
            dst.set(x, top, x + tileW + 0.5f, top + h)
            canvas.drawBitmap(bmp, null, dst, bmpPaint)
            x += tileW
        }
    }

    private fun drawForeground(canvas: Canvas, scroll: Float, left: Float, right: Float) {
        fill.color = Color.argb(200, 30, 32, 48)
        val spacing = 260f
        val s = scroll * 1.35f
        var x = floor((left + s) / spacing) * spacing - s
        while (x < right) {
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
    private fun drawRoadsideScenery(
        canvas: Canvas,
        scroll: Float,
        left: Float,
        right: Float
    ) {
        // Each placement repeats in world space, independently of gameplay.
        // The ground is drawn afterward, covering the lower part of scenery.

        val spacing = 220f
        var worldX = floor((left + scroll) / spacing) * spacing

        while (worldX < right + scroll) {
            val screenX = worldX - scroll

            // Deterministic pattern: no random allocation during rendering.
            val index = (floor(worldX / spacing).toInt() % 12 + 12) % 12

            when (index) {
                0, 1, 2, 3 -> {
                    dst.set(
                        screenX,
                        Constants.GROUND_Y - 68f,
                        screenX + 150f,
                        Constants.GROUND_Y
                    )
                    canvas.drawBitmap(assets.roadsideDamagedGrill, null, dst, bmpPaint)
                }

                4 -> {
                    dst.set(
                        screenX,
                        Constants.GROUND_Y - 58f,
                        screenX + 115f,
                        Constants.GROUND_Y
                    )
                    canvas.drawBitmap(assets.roadsideGrill, null, dst, bmpPaint)
                }

                5, 9 -> {
                    dst.set(
                        screenX,
                        Constants.GROUND_Y - 42f,
                        screenX + 55f,
                        Constants.GROUND_Y
                    )
                    canvas.drawBitmap(assets.roadsideShrub, null, dst, bmpPaint)
                }

                6, 10 -> {
                    dst.set(
                        screenX,
                        Constants.GROUND_Y - 46f,
                        screenX + 62f,
                        Constants.GROUND_Y
                    )
                    canvas.drawBitmap(assets.roadsideOldGrill, null, dst, bmpPaint)
                }

                7 -> {
                    dst.set(
                        screenX,
                        Constants.GROUND_Y - 112f,
                        screenX + 150f,
                        Constants.GROUND_Y
                    )
                    canvas.drawBitmap(assets.roadsideShop, null, dst, bmpPaint)
                }
            }

            worldX += spacing
        }
    }
}
