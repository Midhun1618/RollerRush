package com.voxcom.rollerrush.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.voxcom.rollerrush.data.CharacterData
import com.voxcom.rollerrush.data.SkinCatalog
import com.voxcom.rollerrush.entities.ObstacleType
import com.voxcom.rollerrush.player.PlayerSprites
import com.voxcom.rollerrush.player.Rig
import java.io.IOException
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Loads / generates every bitmap ONCE.
 *
 * For each asset we first look in  assets/<folder>/<name>.png  (see assets/README.txt).
 * If the file is missing a placeholder is drawn procedurally, so the game runs with no artwork.
 * Nothing here is ever called from onDraw: bitmaps are created at load time and re-used.
 */
class AssetManager(private val context: Context) {

    private var loaded = false

    private lateinit var obstacleBitmaps: Array<Bitmap>
    lateinit var coinBitmap: Bitmap
        private set
    lateinit var farBackground: Bitmap
        private set
    lateinit var midBackground: Bitmap
        private set
    lateinit var groundTile: Bitmap
        private set

    fun obstacleBitmap(type: ObstacleType): Bitmap = obstacleBitmaps[type.ordinal]

    /** Idempotent and thread-safe: call from Splash (background thread) and defensively from GameActivity. */
    @Synchronized
    fun ensureLoaded() {
        if (loaded) return
        val s = Constants.BITMAP_SCALE

        obstacleBitmaps = arrayOf(
            obstacle("obstacles/ground.png", 22f, 26f) { c, w, h -> drawCrate(c, w, h) },
            obstacle("obstacles/tall.png", 20f, 58f) { c, w, h -> drawPillar(c, w, h) },
            obstacle("obstacles/moving.png", 24f, 24f) { c, w, h -> drawSpiky(c, w, h) },
            obstacle("obstacles/overhead.png", 38f, 24f) { c, w, h -> drawOverhead(c, w, h) }
        )
        coinBitmap = make("coins/coin.png", (Constants.COIN_RADIUS * 2 * s).toInt(), (Constants.COIN_RADIUS * 2 * s).toInt()) { c, w, h ->
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = Color.rgb(255, 193, 7); c.drawOval(0f, 0f, w, h, p)
            p.color = Color.rgb(255, 224, 102); c.drawOval(w * 0.15f, h * 0.15f, w * 0.85f, h * 0.85f, p)
            p.color = Color.rgb(255, 193, 7); c.drawRect(w * 0.44f, h * 0.28f, w * 0.56f, h * 0.72f, p)
        }

        // Parallax layers are drawn at 2 px / unit and must tile horizontally.
        val bg = 2
        farBackground = make("background/far.png", TILE_W * bg, 200 * bg) { c, w, h -> drawHills(c, w, h) }
        midBackground = make("background/mid.png", TILE_W * bg, 140 * bg) { c, w, h -> drawBuildings(c, w, h) }
        groundTile = make("background/ground.png", 120 * 3, Constants.GROUND_THICKNESS.toInt() * 3) { c, w, h -> drawGround(c, w, h) }
        loaded = true
    }

    /** Builds the 15 bitmaps of a skater from the selected skins. Call when a game / preview starts, not per frame. */
    fun createPlayerSprites(ch: CharacterData): PlayerSprites {
        val skin = SkinCatalog.colorFor(ch.headSkin)
        val hair = SkinCatalog.colorFor(ch.hairSkin)
        val shirt = SkinCatalog.colorFor(ch.torsoSkin)
        val pants = SkinCatalog.colorFor(ch.legSkin)
        val skate = SkinCatalog.colorFor(ch.skateSkin)

        fun part(id: String, part: String, wU: Float, hU: Float, draw: (Canvas, Float, Float) -> Unit): Bitmap =
            make("player/$part.png", (wU * Constants.BITMAP_SCALE).toInt(), (hU * Constants.BITMAP_SCALE).toInt(), draw)

        fun limb(id: String, name: String, len: Float, thick: Float, color: Int) =
            part(id, name, thick, len + thick) { c, w, h -> roundRect(c, 0f, 0f, w, h, w / 2f, color) }

        fun handBmp(id: String, color: Int) = part(id, "hand", Rig.HAND_SIZE, Rig.HAND_SIZE) { c, w, h ->
            paint(color).let { c.drawOval(0f, 0f, w, h, it) }
        }

        fun skateBmp(color: Int) = part(ch.skateSkin, "skate", Rig.SKATE_W, Rig.SKATE_H) { c, w, h -> drawSkate(c, w, h, color) }

        val far = 0.72f // far-side limbs are darker to suggest depth
        return PlayerSprites(
            head = part(ch.headSkin, "head", Rig.HEAD_W, Rig.HEAD_H) { c, w, h -> drawHead(c, w, h, skin) },
            hair = part(ch.hairSkin, "hair", Rig.HAIR_W, Rig.HAIR_H) { c, w, h -> roundRect(c, 0f, 0f, w, h, h * 0.45f, hair) },
            torso = part(ch.torsoSkin, "torso", Rig.TORSO_W, Rig.TORSO_H) { c, w, h -> roundRect(c, 0f, 0f, w, h - 2f * Constants.BITMAP_SCALE, w * 0.3f, shirt) },
            upperArm = limb(ch.torsoSkin, "arm", Rig.UPPER_ARM_LEN, Rig.UPPER_ARM_THICK, shirt),
            forearm = limb(ch.headSkin, "forearm", Rig.FOREARM_LEN, Rig.FOREARM_THICK, skin),
            hand = handBmp(ch.headSkin, skin),
            thigh = limb(ch.legSkin, "thigh", Rig.THIGH_LEN, Rig.THIGH_THICK, pants),
            shin = limb(ch.legSkin, "shin", Rig.SHIN_LEN, Rig.SHIN_THICK, pants),
            skate = skateBmp(skate),
            upperArmFar = limb(ch.torsoSkin, "arm_far", Rig.UPPER_ARM_LEN, Rig.UPPER_ARM_THICK, darken(shirt, far)),
            forearmFar = limb(ch.headSkin, "forearm_far", Rig.FOREARM_LEN, Rig.FOREARM_THICK, darken(skin, far)),
            handFar = handBmp(ch.headSkin, darken(skin, far)),
            thighFar = limb(ch.legSkin, "thigh_far", Rig.THIGH_LEN, Rig.THIGH_THICK, darken(pants, far)),
            shinFar = limb(ch.legSkin, "shin_far", Rig.SHIN_LEN, Rig.SHIN_THICK, darken(pants, far)),
            skateFar = part(ch.skateSkin, "skate_far", Rig.SKATE_W, Rig.SKATE_H) { c, w, h -> drawSkate(c, w, h, darken(skate, far)) }
        )
    }

    fun release() {
        if (!loaded) return
        obstacleBitmaps.forEach { it.recycle() }
        listOf(coinBitmap, farBackground, midBackground, groundTile).forEach { it.recycle() }
        loaded = false
    }

    // ------------------------------------------------------------------ helpers
    private fun obstacle(path: String, wU: Float, hU: Float, draw: (Canvas, Float, Float) -> Unit): Bitmap =
        make(path, (wU * Constants.BITMAP_SCALE).toInt(), (hU * Constants.BITMAP_SCALE).toInt(), draw)

    /** Try file in assets/, else draw the placeholder. */
    private fun make(path: String, w: Int, h: Int, draw: (Canvas, Float, Float) -> Unit): Bitmap {
        loadFromAssets(path, w, h)?.let { return it }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp), w.toFloat(), h.toFloat())
        return bmp
    }

    private fun loadFromAssets(path: String, w: Int, h: Int): Bitmap? = try {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)?.let {
                if (it.width == w && it.height == h) it else Bitmap.createScaledBitmap(it, w, h, true).also { _ -> it.recycle() }
            }
        }
    } catch (e: IOException) { null }

    private fun paint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

    private fun roundRect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int) {
        c.drawRoundRect(RectF(l, t, r, b), rad, rad, paint(color))
    }

    private fun darken(color: Int, f: Float) =
        Color.rgb((Color.red(color) * f).toInt(), (Color.green(color) * f).toInt(), (Color.blue(color) * f).toInt())

    // ------------------------------------------------------- placeholder drawing
    private fun drawHead(c: Canvas, w: Float, h: Float, skin: Int) {
        c.drawOval(0f, 0f, w, h, paint(skin))
        c.drawCircle(w * 0.72f, h * 0.42f, w * 0.07f, paint(Color.BLACK)) // eye: shows facing direction (right)
    }

    private fun drawSkate(c: Canvas, w: Float, h: Float, color: Int) {
        roundRect(c, 0f, 0f, w, h * 0.55f, h * 0.15f, color)                 // boot
        val dark = paint(Color.rgb(40, 40, 40))
        c.drawRect(w * 0.05f, h * 0.55f, w * 0.95f, h * 0.65f, dark)          // plate
        for (cx in floatArrayOf(0.2f, 0.5f, 0.8f)) c.drawCircle(w * cx, h * 0.80f, h * 0.2f, dark) // wheels
    }

    private fun drawCrate(c: Canvas, w: Float, h: Float) {
        c.drawRect(0f, 0f, w, h, paint(Color.rgb(160, 110, 60)))
        val p = paint(Color.rgb(100, 65, 30)).apply { style = Paint.Style.STROKE; strokeWidth = w * 0.08f }
        c.drawRect(w * 0.04f, h * 0.04f, w * 0.96f, h * 0.96f, p)
        c.drawLine(0f, 0f, w, h, p); c.drawLine(w, 0f, 0f, h, p)
    }

    private fun drawPillar(c: Canvas, w: Float, h: Float) {
        c.drawRect(0f, 0f, w, h, paint(Color.rgb(90, 100, 120)))
        val stripe = paint(Color.rgb(255, 214, 10))
        var y = h * 0.1f
        while (y < h) { c.drawRect(0f, y, w, y + h * 0.06f, stripe); y += h * 0.16f }
    }

    private fun drawOverhead(c: Canvas, w: Float, h: Float) {
        c.drawRoundRect(RectF(0f, h * 0.20f, w, h * 0.80f), h * 0.20f, h * 0.20f,
            paint(Color.rgb(220, 55, 55)))
        val stripe = paint(Color.rgb(255, 214, 10))
        var x = -h
        while (x < w) {
            c.save()
            c.rotate(-28f, x + h * 0.5f, h * 0.5f)
            c.drawRect(x, h * 0.38f, x + h * 0.30f, h * 0.62f, stripe)
            c.restore()
            x += h * 0.75f
        }
    }

    private fun drawSpiky(c: Canvas, w: Float, h: Float) {
        val p = paint(Color.rgb(214, 40, 40))
        val path = Path()
        val spikes = 8
        for (i in 0 until spikes * 2) {
            val ang = i * PI.toFloat() / spikes
            val r = if (i % 2 == 0) 0.5f else 0.32f
            val x = w / 2f + kotlin.math.cos(ang) * w * r
            val y = h / 2f + sin(ang) * h * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close(); c.drawPath(path, p)
        c.drawCircle(w / 2f, h / 2f, w * 0.15f, paint(Color.WHITE))
    }

    private fun drawHills(c: Canvas, w: Float, h: Float) {
        // Integer number of sine periods across the tile => the edges match => seamless tiling.
        val p = paint(Color.rgb(157, 180, 192))
        val twoPi = 2f * PI.toFloat()
        for (x in 0 until w.toInt()) {
            val t = x / w
            val top = h - (0.45f * h + 0.2f * h * sin(twoPi * 2f * t) + 0.12f * h * sin(twoPi * 5f * t + 1f))
            c.drawRect(x.toFloat(), top, x + 1f, h, p)
        }
    }

    private fun drawBuildings(c: Canvas, w: Float, h: Float) {
        val rng = Random(7)
        val body = paint(Color.rgb(92, 107, 115))
        val win = paint(Color.rgb(200, 210, 150))
        var x = 0f
        while (x < w) {
            val bw = minOf((60f + rng.nextFloat() * 50f) * 2f, w - x)
            val bh = (50f + rng.nextFloat() * 80f) * 2f
            c.drawRect(x, h - bh, x + bw, h, body)
            var wy = h - bh + 12f
            while (wy < h - 16f) { var wx = x + 10f; while (wx < x + bw - 14f) { c.drawRect(wx, wy, wx + 8f, wy + 10f, win); wx += 22f }; wy += 26f }
            x += bw + 4f
        }
    }

    private fun drawGround(c: Canvas, w: Float, h: Float) {
        c.drawRect(0f, 0f, w, h, paint(Color.rgb(74, 78, 105)))
        c.drawRect(0f, 0f, w, h * 0.1f, paint(Color.rgb(154, 140, 152)))            // kerb
        c.drawRect(w * 0.16f, h * 0.5f, w * 0.5f, h * 0.55f, paint(Color.rgb(201, 173, 167))) // lane dash
    }

    companion object { const val TILE_W = 1000 }
}
