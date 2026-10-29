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
import com.voxcom.rollerrush.player.BodyPart
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

    private val boostParticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2f
    }

    // Fixed-size particle pool: no particle objects created per frame.
    private val boostParticleCount = 36

    private var boostRandomState = 9371


    // Custom PNG particles emitted from both animated skates.
    private val skateParticleCount = 120
    private val skateParticleX = FloatArray(skateParticleCount)
    private val skateParticleY = FloatArray(skateParticleCount)
    private val skateParticleVX = FloatArray(skateParticleCount)
    private val skateParticleVY = FloatArray(skateParticleCount)
    private val skateParticleLife = FloatArray(skateParticleCount)
    private val skateParticleMaxLife = FloatArray(skateParticleCount)
    private val skateParticleStartSize = FloatArray(skateParticleCount)
    private val skateParticleGrowth = FloatArray(skateParticleCount)
    private val skateParticleRotation = FloatArray(skateParticleCount)
    private val skateParticleRotationSpeed = FloatArray(skateParticleCount)
    private val skateParticleJitter = FloatArray(skateParticleCount)
    private val skateParticlePhase = FloatArray(skateParticleCount)

    private var skateParticleIndex = 0
    private var skateEmissionAccumulator = 0f

    private val skateParticlePaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    private val skateParticleRect = RectF()
    private val leftSkatePoint = FloatArray(2)
    private val rightSkatePoint = FloatArray(2)


    private fun boostRandom(): Float {
        boostRandomState =
            (boostRandomState * 1103515245 + 12345) and 0x7fffffff

        return boostRandomState.toFloat() / Int.MAX_VALUE.toFloat()
    }

    /** Pause button in SCREEN pixels (GameView hit-tests touches against it). */
    val pauseRect = RectF()
    private var margin = 0f

    // HUD pickup animation state
    private var hudAnimationTime = 0f
    private var coinPickupPulse = 0f
    private var rubyPickupPulse = 0f
    private var previousHudCoins = -1
    private var previousHudRubies = -1

    private val hudIconRect = RectF()
    private val hudAnimationPaint = Paint(
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
    )

    private var hudAnimationDelta = 0f
    private var hudLastFrameNs = 0L

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
        // Draw the mystery bubble in world space, using the same camera
        // transformation as the road, booster, coins and other entities.
        if (world.mysteryBubble.active) {
            world.mysteryBubble.drawBubble(
                canvas,
                assets.mysteryBubbleBitmap,
                bmpPaint
            )
        }
        canvas.restore()
        // HUD must remain in screen space.
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
            ((now - boostLastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        }
        boostLastNs = now

        val boosting = world.speedBoostActive
        val player = world.player

        if (boosting) {
            boostTrailPhase += dt * 11f
            if (boostTrailPhase > 1000f) boostTrailPhase -= 1000f

            skateEmissionAccumulator += dt

            // Emit from each skate at regular intervals.
            while (skateEmissionAccumulator >= 0.015f) {
                skateEmissionAccumulator -= 0.025f

                getBodyPartWorldPoint(
                    player.leftSkate,
                    player.leftSkate.width / 2f - player.leftSkate.pivotX,
                    player.leftSkate.height / 2f - player.leftSkate.pivotY,
                    leftSkatePoint
                )
                spawnSkateParticle(leftSkatePoint[0], leftSkatePoint[1])

                getBodyPartWorldPoint(
                    player.rightSkate,
                    player.rightSkate.width / 2f - player.rightSkate.pivotX,
                    player.rightSkate.height / 2f - player.rightSkate.pivotY,
                    rightSkatePoint
                )
                spawnSkateParticle(rightSkatePoint[0], rightSkatePoint[1])
            }
        } else {
            // Stop new emissions, but allow existing particles to fade naturally.
            skateEmissionAccumulator = 0f
        }

        // Keep the existing character afterimage effect.
        if (boosting) {
            for (i in boostTrailOffsets.indices) {
                val wave =
                    kotlin.math.sin(boostTrailPhase - i * 0.8f) * 2.5f

                canvas.save()
                canvas.translate(
                    -boostTrailOffsets[i] - wave,
                    wave * 0.45f
                )

                bmpPaint.alpha = boostTrailAlphas[i]
                player.draw(canvas, bmpPaint)
                canvas.restore()
            }
        }

        bmpPaint.alpha = 255

        // Update and draw the custom PNG particles.
        val particleBitmap = player.skateParticleBitmap

        if (!particleBitmap.isRecycled) {
            for (i in 0 until skateParticleCount) {
                if (skateParticleLife[i] <= 0f) continue

                skateParticleLife[i] -= dt

                if (skateParticleLife[i] <= 0f) {
                    skateParticleLife[i] = 0f
                    continue
                }

                val progress = (
                        1f - skateParticleLife[i] /
                                skateParticleMaxLife[i]
                        ).coerceIn(0f, 1f)

                // Backward drift plus a little upward lift.
                skateParticleX[i] += skateParticleVX[i] * dt
                skateParticleY[i] += skateParticleVY[i] * dt

                // Smooth, small jitter so the trail doesn't look mechanical.
                skateParticlePhase[i] += dt * 9f
                val jitterX = kotlin.math.sin(
                    skateParticlePhase[i] + i * 0.7f
                ) * skateParticleJitter[i]
                val jitterY = kotlin.math.cos(
                    skateParticlePhase[i] * 1.3f + i
                ) * skateParticleJitter[i]

                skateParticleRotation[i] +=
                    skateParticleRotationSpeed[i] * dt

                val alpha = (
                        255f * (1f - progress) * (1f - progress * 0.2f)
                        ).toInt().coerceIn(0, 255)

                if (alpha <= 0) continue

                val size = (
                        skateParticleStartSize[i] +
                                skateParticleGrowth[i] * progress
                        ).coerceAtLeast(1f)

                val x = skateParticleX[i] + jitterX
                val y = skateParticleY[i] + jitterY

                skateParticleRect.set(
                    -size / 2f,
                    -size / 2f,
                    size / 2f,
                    size / 2f
                )

                skateParticlePaint.alpha = alpha

                canvas.save()
                canvas.translate(x, y)
                canvas.rotate(skateParticleRotation[i])
                canvas.drawBitmap(
                    particleBitmap,
                    null,
                    skateParticleRect,
                    skateParticlePaint
                )
                canvas.restore()
            }
        }

        skateParticlePaint.alpha = 255
        bmpPaint.alpha = 255
    }

    private fun spawnSkateParticle(
        skateX: Float,
        skateY: Float
    ) {
        val i = skateParticleIndex
        skateParticleIndex = (skateParticleIndex + 1) % skateParticleCount

        val randomA = boostRandom()
        val randomB = boostRandom()
        val randomC = boostRandom()

        skateParticleX[i] = skateX + (randomA - 0.5f) * 5f
        skateParticleY[i] = skateY + (randomB - 0.5f) * 4f

        // The skater faces right, so particles trail towards the left.
        skateParticleVX[i] = -(250f + randomA * 350f)
        skateParticleVY[i] = -(20f + randomB * 80f)

        val life = 0.24f + randomC * 0.32f
        skateParticleLife[i] = life
        skateParticleMaxLife[i] = life

        skateParticleStartSize[i] = 7f + randomA * 9f
        skateParticleGrowth[i] = 12f + randomB * 18f

        skateParticleRotation[i] = randomC * 360f
        skateParticleRotationSpeed[i] = (randomA - 0.5f) * 420f
        skateParticleJitter[i] = 0.5f + randomB * 2.5f
        skateParticlePhase[i] = randomC * 6.28f
    }

    /**
     * Converts a point in a BodyPart's local coordinates into world coordinates,
     * applying the part and all of its parent transforms.
     */
    private fun getBodyPartWorldPoint(
        part: BodyPart,
        localX: Float,
        localY: Float,
        out: FloatArray
    ) {
        var x = localX
        var y = localY
        var current: BodyPart? = part

        while (current != null) {
            val scaledX = x * current.scaleX
            val scaledY = y * current.scaleY

            val radians = Math.toRadians(
                current.rotation.toDouble()
            )
            val cosAngle = kotlin.math.cos(radians).toFloat()
            val sinAngle = kotlin.math.sin(radians).toFloat()

            x = scaledX * cosAngle - scaledY * sinAngle + current.x
            y = scaledX * sinAngle + scaledY * cosAngle + current.y

            current = current.parent
        }

        out[0] = x
        out[1] = y
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

    /** HUD in screen space so it is independent of the world scale. */
    private fun drawHud(canvas: Canvas, world: GameWorld, camera: Camera) {
        val h = camera.screenHeight
        val leftX = margin + h * 0.04f

        // Advance animations using real frame time.
        val now = System.nanoTime()
        val dt = if (hudLastFrameNs == 0L) {
            0f
        } else {
            ((now - hudLastFrameNs) / 1_000_000_000f)
                .coerceIn(0f, 0.05f)
        }

        hudLastFrameNs = now
        hudAnimationTime += dt
        coinPickupPulse = (coinPickupPulse - dt).coerceAtLeast(0f)
        rubyPickupPulse = (rubyPickupPulse - dt).coerceAtLeast(0f)

        val coins = world.runCoins
        val rubies = world.runRubies

        if (previousHudCoins >= 0 && coins > previousHudCoins) {
            coinPickupPulse = 0.24f
        }

        if (previousHudRubies >= 0 && rubies > previousHudRubies) {
            rubyPickupPulse = 0.34f
        }

        previousHudCoins = coins
        previousHudRubies = rubies

        // Score and distance.
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = h * 0.07f

        sb.setLength(0)
        sb.append("Score: ").append(world.score)
        canvas.drawText(
            sb, 0, sb.length,
            leftX, margin + textPaint.textSize, textPaint
        )

        textPaint.textSize = h * 0.045f
        sb.setLength(0)
        sb.append("Distance: ").append(world.distanceMeters).append(" m")
        canvas.drawText(
            sb, 0, sb.length,
            leftX,
            margin + h * 0.07f + textPaint.textSize * 1.3f,
            textPaint
        )

        // Coin and Ruby counters, stacked beside the pause button.
        val rightX = pauseRect.left - margin * 0.7f
        val rowGap = h * 0.075f
        val iconSize = h * 0.052f
        val topY = margin + h * 0.045f

        drawHudCounter(
            canvas, coins, rightX, topY,
            iconSize, assets.coinBitmap, coinPickupPulse
        )

        drawHudCounter(
            canvas, rubies, rightX, topY + rowGap,
            iconSize, assets.rushRubyBitmap, rubyPickupPulse
        )

        // Pause button.
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

    private fun drawHudCounter(
        canvas: Canvas,
        count: Int,
        rightX: Float,
        baselineY: Float,
        iconSize: Float,
        bitmap: Bitmap,
        pulse: Float
    ) {
        textPaint.textSize = iconSize * 1.02f
        textPaint.textAlign = Paint.Align.RIGHT

        sb.setLength(0)
        sb.append(count)
        canvas.drawText(sb, 0, sb.length, rightX, baselineY, textPaint)

        val textWidth = textPaint.measureText(sb, 0, sb.length)
        val progress = (pulse / 0.34f).coerceIn(0f, 1f)
        val pulseScale =
            1f + 0.48f * kotlin.math.sin(progress * Math.PI).toFloat()

        val size = iconSize * pulseScale
        val gap = iconSize * 0.22f
        val centerX = rightX - textWidth - gap - size * 0.5f
        val centerY = baselineY - iconSize * 0.36f

        hudIconRect.set(
            centerX - size * 0.5f,
            centerY - size * 0.5f,
            centerX + size * 0.5f,
            centerY + size * 0.5f
        )

        hudAnimationPaint.alpha = 255
        canvas.drawBitmap(bitmap, null, hudIconRect, hudAnimationPaint)

        if (pulse > 0f) {
            val alpha = (150f * (pulse / 0.34f).coerceIn(0f, 1f)).toInt()

            fill.style = Paint.Style.STROKE
            fill.strokeWidth = (iconSize * 0.055f).coerceAtLeast(1f)
            fill.color = Color.argb(alpha, 255, 235, 150)

            canvas.drawCircle(centerX, centerY, size * 0.58f, fill)

            fill.style = Paint.Style.FILL
        }
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
